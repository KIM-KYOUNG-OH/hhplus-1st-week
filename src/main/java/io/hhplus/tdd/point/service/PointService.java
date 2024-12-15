package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.model.PointHistory;
import io.hhplus.tdd.point.model.UserPoint;

import java.util.List;

public interface PointService {
    UserPoint findUserPointBy(long userId);

    List<PointHistory> findPointHistoriesBy(long userId);

    UserPoint chargeUserPoint(long userId, long amount);

    UserPoint usePoint(long userId, long amount);
}
