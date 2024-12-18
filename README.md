# hhplus-1st-week  

# 동시성 제어 방식  
애플리케이션 레벨에서 동시성 이슈를 제어하기 위해서 ConcurrentHashMap과 ReentrantLock의 조합을 사용했습니다.  
여러 스레드가 동시에 동일한 유저ID에 대한 작업을 실행하지 못하도록, 락 획득과 반환을 제어합니다.   
ConcurrentHashMap은 유저ID를 Key 값으로 별도의 ReentrantLock 객체를 생성하여 매핑했습니다.  
ConcurrentHashMap은 내부적으로 세그먼트 단위로 접근이 제한되기 때문에 thread-safe하고 성능이 최적화됩니다.  
ReentrantLock은 특정 자원에 대한 락이 이미 걸려있을 때 이전 작업이 종료될 때까지 blocking 상태로 대기할 수 있습니다.  
락이 풀리는 속도보다 요청이 쌓이는 속도가 더 빨라서 요청이 계속 쌓이면 OutOfMemoryException이 발생할 수 있기 때문에 대기하는 시간은 최대 10초로 제한했습니다.

```java
@Service
@Slf4j
public class PointLockService {

    private final ConcurrentHashMap<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    private ReentrantLock getLock(long userId) {
        return lockMap.computeIfAbsent(userId, k -> new ReentrantLock());
    }

    public <T> T executeWithLock(long userId, Callable<T> task) throws InterruptedException {
        ReentrantLock lock = getLock(userId);

        if (lock.isLocked()) {
            log.info("[ThreadId:{}] Lock for userId {} is already taken. Waiting...", Thread.currentThread().getId(), userId);
        }

        if (!lock.tryLock(10, TimeUnit.SECONDS)) {  // userId에 해당하는 락을 획득(최대 10초까지만 대기)
            log.info("[ThreadId:{}] Thread for userId {} is destroyed after 10 sec", Thread.currentThread().getId(), userId);
            return null;
        }

        try {
            log.info("[ThreadId:{}] Lock acquired for userId {}. Starting task...", Thread.currentThread().getId(), userId);
            return task.call();
        } catch (Exception e) {
            throw new RuntimeException("Unexpected internal exception occurred", e);
        } finally {
            lock.unlock();
            log.info("[ThreadId:{}] Lock released for userId {}.", Thread.currentThread().getId(), userId);
        }
    }
}
```

# 동시성 제어 통합 테스트
아래의 케이스에 대해서도 로직이 의도대로 동작하는지 테스트하는데 중점을 두었습니다.  
1. 하나의 유저ID로 자원에 동시에 접근할 때 lock이 정상 동작하여 먼저 자원에 접근한 순서대로 처리되는지   
2. 서로 다른 유저ID로 자원에 동시 접근할 때 lock이 발생되지 않고 병렬 처리되는지  

## 1. 하나의 유저 ID에 동시 접근
통합 테스트의 프로세스는 아래와 같습니다.  
1) 유저ID : 0 으로 500포인트씩 10회 충전  
2) 함수는 비동기로 실행되며 완전히 완료될 때까지 대기  
3) 모두 완료되면 유저ID : 0 인 회원의 보유 포인트가 5000포인트인지 확인  

```java  
@Test
void 회원1명_총5000포인트_10회충전_동시성이슈테스트() throws InterruptedException {
    long userId = 0L;
    String url = String.format("http://localhost:%s/point/%d/charge", port, userId);
    ChargeDto.Request request = new ChargeDto.Request(500);

    int threadCount = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
        new Thread(() -> {
            try {
                webClient.patch()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(ChargeDto.Response.class)
                        .block();
            } finally {
                latch.countDown();
            }
        }).start();
    }

    latch.await();

    PointDto.Response finalResponse = webClient.get()
            .uri(String.format("http://localhost:%s/point/%d", port, userId))
            .retrieve()
            .bodyToMono(PointDto.Response.class)
            .block();

    assertNotNull(finalResponse);
    assertEquals(5000, finalResponse.getPoint());
}
```

아래 이미지처럼 자원에 먼저 도달한 스레드부터 요청이 처리되고, 이후 스레드들은 락 획득&반납을 반복하며 순차적으로 처리되는 것을 확인할 수 있습니다.  
대기 시간때문에 총 테스트 시간은 6.78초로 상대적으로 느립니다.  
![스크린샷 2024-12-19 003424](https://github.com/user-attachments/assets/942dd139-2d6b-4cf4-9715-22d3e9907b60)

## 2. 서로 다른 유저가 동시 접근
통합 테스트의 프로세스는 아래와 같습니다.  
1) 유저ID : 1~10 으로 500포인트씩 각 1회 충전  
2) 함수는 비동기로 실행되며 완전히 완료될 때까지 대기  
3) 모두 완료되면 각 유저의 보유 포인트가 500포인트인지 확인  

```java
@Test
void 회원10명_각500포인트_10회충전_성능테스트() throws InterruptedException {
    long[] userIds = new long[]{1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L};
    ChargeDto.Request request = new ChargeDto.Request(500);

    int threadCount = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);

    // 응답을 저장할 리스트
    List<ChargeDto.Response> responses = Collections.synchronizedList(new ArrayList<>());

    for (int i = 0; i < threadCount; i++) {
        long currentUserId = userIds[i];
        new Thread(() -> {
            try {
                String url = String.format("http://localhost:%s/point/%d/charge", port, currentUserId);
                ChargeDto.Response response = webClient.patch()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(ChargeDto.Response.class)
                        .block();

                responses.add(response);
            } finally {
                latch.countDown();
            }
        }).start();
    }

    latch.await();  // 모든 스레드가 끝날 때까지 대기

    // 모든 유저의 포인트가 500씩 추가되었는지 확인
    for (long userId : userIds) {
        ChargeDto.Response response = responses.stream()
                .filter(user -> user.getId() == userId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("User point not found"));

        long expectedPoint = 500;
        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals(expectedPoint, response.getPoint());
    }
}
```

유저ID를 키값으로 락을 걸기 때문에 아래 이미지처럼 대기하는 스레드는 없습니다.  
테스트 경과 시간이 2.336 초로, 대기 시간이 없기 때문에 1번 테스트에 비해 상대적으로 처리 속도가 빠른 것을 확인할 수 있습니다.   
![스크린샷 2024-12-19 004200](https://github.com/user-attachments/assets/dc9533bf-7d08-4f08-8333-012c745d530b)
