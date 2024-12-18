package io.hhplus.tdd.point.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class PointDto {

    @NoArgsConstructor
    @Getter
    @Setter
    public static class Response {
        private long id;
        private long point;
        private long updateMillis;
    }
}
