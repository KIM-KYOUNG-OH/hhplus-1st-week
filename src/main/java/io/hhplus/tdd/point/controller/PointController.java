package io.hhplus.tdd.point.controller;

import io.hhplus.tdd.point.model.ChargeRequest;
import io.hhplus.tdd.point.model.PointHistory;
import io.hhplus.tdd.point.model.UserPoint;
import io.hhplus.tdd.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final PointService pointService;

    /**
     * 특정 유저의 포인트를 조회
     */
    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable(name = "id") long id
    ) {
        return pointService.findUserPointBy(id);
    }

    /**
     * 특정 유저의 포인트 충전/이용 내역을 조회
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable(name = "id") long id
    ) {
        return pointService.findPointHistoriesBy(id);
    }

    /**
     * 특정 유저의 포인트를 충전
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable(name = "id") long id,
            @RequestBody ChargeRequest chargeRequest
    ) {
        return pointService.chargeUserPoint(id, chargeRequest.getAmount());
    }

    /**
     * 특정 유저의 포인트를 사용
     */
    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable(name = "id") long id,
            @RequestBody ChargeRequest chargeRequest
    ) {
        return pointService.usePoint(id, chargeRequest.getAmount());
    }
}
