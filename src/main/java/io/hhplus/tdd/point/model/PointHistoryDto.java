package io.hhplus.tdd.point.model;

import io.hhplus.tdd.point.entity.TransactionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class PointHistoryDto {

    @NoArgsConstructor
    @Getter
    @Setter
    public static class Response {
        private long id;
        private long userId;
        private long amount;
        private TransactionType type;
        private long updateMillis;
    }
}
