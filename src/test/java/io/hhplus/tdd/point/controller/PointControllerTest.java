package io.hhplus.tdd.point.controller;

import io.hhplus.tdd.point.model.ChargeRequest;
import io.hhplus.tdd.point.model.UserPoint;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PointControllerTest {

    @LocalServerPort
    private int port;

    private final WebClient webClient = WebClient.create();

    @Test
    void 한명회원대상_총5000포인트충전_동시성이슈테스트() throws InterruptedException {
        long userId = 1L;
        String url = String.format("http://localhost:%s/point/%d/charge", port, userId);
        ChargeRequest chargeRequest = new ChargeRequest(500);

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    // 각 스레드가 독립적으로 포인트 충전 요청을 보냄
                    UserPoint response = webClient.patch()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(chargeRequest)
                            .retrieve()
                            .bodyToMono(UserPoint.class)
                            .block();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        UserPoint finalResponse = webClient.get()
                .uri(String.format("http://localhost:%s/point/%d", port, userId))
                .retrieve()
                .bodyToMono(UserPoint.class)
                .block();

        assertNotNull(finalResponse);
        assertEquals(5000, finalResponse.point());
    }
}