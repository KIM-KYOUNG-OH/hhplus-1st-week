package io.hhplus.tdd.point.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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
