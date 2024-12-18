package io.hhplus.tdd.point.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PointHistory {

    private long id;
    private long userId;
    private long amount;
    private TransactionType type;
    private long updateMillis;
}
