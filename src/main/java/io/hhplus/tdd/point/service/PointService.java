package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.exception.InsufficientPointBalanceException;
import io.hhplus.tdd.point.exception.MaximumPointExceededException;
import io.hhplus.tdd.point.model.PointHistory;
import io.hhplus.tdd.point.model.TransactionType;
import io.hhplus.tdd.point.model.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointLockService pointLockService;
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private static final long MAX_USER_POINT = 50000L;
    private static final long MIN_USER_POINT = 0L;

    public UserPoint findUserPointBy(long id) {
        return userPointTable.selectById(id);
    }

    public List<PointHistory> findPointHistoriesBy(long userId) {
        return Optional.ofNullable(pointHistoryTable.selectAllByUserId(userId))
                .orElse(Collections.emptyList());
    }

    public UserPoint chargeUserPoint(long userId, long amount) {
        try {
            return pointLockService.executeWithLock(userId, () -> {
                UserPoint findUserPoint = userPointTable.selectById(userId);
                long postAmount = findUserPoint.point() + amount;
                if (postAmount > MAX_USER_POINT) {
                    throw new MaximumPointExceededException("Exceed maximum user point limit");
                }

                UserPoint userPoint = userPointTable.insertOrUpdate(userId, postAmount);

                pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, Instant.now().getEpochSecond());

                return userPoint;
            });
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was interrupted", e);
        }
    }

    public UserPoint usePoint(long userId, long amount) {
        UserPoint findUserPoint = userPointTable.selectById(userId);
        long postAmount = findUserPoint.point() - amount;
        if (postAmount < MIN_USER_POINT) {
            throw new InsufficientPointBalanceException("Point balance is not enough");
        }

        UserPoint userPoint = userPointTable.insertOrUpdate(userId, postAmount);

        pointHistoryTable.insert(userId, amount, TransactionType.USE, Instant.now().getEpochSecond());

        return userPoint;
    }
}
