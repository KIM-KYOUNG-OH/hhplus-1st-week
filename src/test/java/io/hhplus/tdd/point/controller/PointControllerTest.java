package io.hhplus.tdd.point.controller;

import io.hhplus.tdd.point.model.ChargeDto;
import io.hhplus.tdd.point.model.PointDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PointControllerTest {

    @LocalServerPort
    private int port;

    private final WebClient webClient = WebClient.create();

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
}