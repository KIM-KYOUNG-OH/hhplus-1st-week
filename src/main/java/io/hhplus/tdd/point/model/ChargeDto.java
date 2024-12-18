package io.hhplus.tdd.point.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class ChargeDto {

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class Request {
        private long amount;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class Response {
        private long id;
        private long point;
        private long updateMillis;
    }
}
