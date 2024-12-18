package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.exception.InsufficientPointBalanceException;
import io.hhplus.tdd.point.exception.MaximumPointExceededException;
import io.hhplus.tdd.point.entity.PointHistory;
import io.hhplus.tdd.point.entity.TransactionType;
import io.hhplus.tdd.point.entity.UserPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private PointLockService pointLockService;

    @InjectMocks
    private PointService pointService;

    @Test
    void 포인트조회_성공() {
        UserPoint userPoint = new UserPoint(1, 1000, System.currentTimeMillis());
        when(userPointTable.selectById(1L)).thenReturn(userPoint);

        UserPoint result = pointService.findUserPointBy(1L);

        assertEquals(1000, result.getPoint());
        verify(userPointTable, times(1)).selectById(1L);
    }

    @Test
    void 포인트내역조회_성공() {
        List<PointHistory> pointHistories = new ArrayList<>();
        PointHistory pointHistory = new PointHistory(1L, 100, 1000, TransactionType.CHARGE, System.currentTimeMillis());
        pointHistories.add(pointHistory);
        when(pointHistoryTable.selectAllByUserId(1L)).thenReturn(pointHistories);

        List<PointHistory> result = pointService.findPointHistoriesBy(1L);

        assertEquals(1, result.size());
        assertEquals(1000, result.get(0).getAmount());
        verify(pointHistoryTable, times(1)).selectAllByUserId(1L);
    }

    @Test
    void 포인트충전_성공() throws InterruptedException {
        when(pointLockService.executeWithLock(eq(1L), any(Callable.class)))
                .thenAnswer(invocation -> {
                    Callable<UserPoint> task = invocation.getArgument(1);
                    return task.call();
                });
        UserPoint userPoint = new UserPoint(1L, 1000, System.currentTimeMillis());
        UserPoint postUserPoint = new UserPoint(1L, 1500, System.currentTimeMillis());
        when(userPointTable.selectById(1L)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(1L, postUserPoint.getPoint())).thenReturn(postUserPoint);

        UserPoint result = pointService.chargeUserPoint(1L, 500);

        assertEquals(1500, result.getPoint());
        verify(userPointTable, times(1)).selectById(1L);
        verify(userPointTable, times(1)).insertOrUpdate(1L, 1500);
    }

    @Test
    void 포인트충전_실패_최대잔고초과() throws InterruptedException {
        when(pointLockService.executeWithLock(eq(1L), any(Callable.class)))
                .thenAnswer(invocation -> {
                    Callable<UserPoint> task = invocation.getArgument(1);
                    return task.call();
                });
        UserPoint userPoint = new UserPoint(1L, 1000, System.currentTimeMillis());
        when(userPointTable.selectById(1L)).thenReturn(userPoint);

        assertThrows(MaximumPointExceededException.class, () -> pointService.chargeUserPoint(1L, 5_000_000L));
    }


    @Test
    void 포인트사용_성공() {
        UserPoint userPoint = new UserPoint(1L, 1000, System.currentTimeMillis());
        UserPoint postUserPoint = new UserPoint(1L, 700, System.currentTimeMillis());
        when(userPointTable.selectById(1L)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(1L, postUserPoint.getPoint())).thenReturn(postUserPoint);

        UserPoint result = pointService.usePoint(1L, 300);

        assertEquals(700, result.getPoint());
        verify(userPointTable, times(1)).selectById(1L);
        verify(userPointTable, times(1)).insertOrUpdate(1L, 700);
    }

    @Test
    void 포인트사용_실패_잔고부족() {
        UserPoint userPoint = new UserPoint(1L, 1000, System.currentTimeMillis());
        when(userPointTable.selectById(1L)).thenReturn(userPoint);

        assertThrows(InsufficientPointBalanceException.class, () -> pointService.usePoint(1L, 1500));
    }
}