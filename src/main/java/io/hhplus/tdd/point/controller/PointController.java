package io.hhplus.tdd.point.controller;

import io.hhplus.tdd.point.entity.PointHistory;
import io.hhplus.tdd.point.entity.UserPoint;
import io.hhplus.tdd.point.model.*;
import io.hhplus.tdd.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;
    private final ModelMapper modelMapper = new ModelMapper();

    /**
     * 특정 유저의 포인트를 조회
     */
    @GetMapping("{id}")
    public PointDto.Response point(
            @PathVariable(name = "id") long id
    ) {
        UserPoint userPoint = pointService.findUserPointBy(id);
        return modelMapper.map(userPoint, PointDto.Response.class);
    }

    /**
     * 특정 유저의 포인트 충전/이용 내역을 조회
     */
    @GetMapping("{id}/histories")
    public List<PointHistoryDto.Response> history(
            @PathVariable(name = "id") long id
    ) {
        List<PointHistory> pointHistories = pointService.findPointHistoriesBy(id);
        return pointHistories.stream()
                .map(h -> modelMapper.map(h, PointHistoryDto.Response.class))
                .collect(Collectors.toList());
    }

    /**
     * 특정 유저의 포인트를 충전
     */
    @PatchMapping("{id}/charge")
    public ChargeDto.Response charge(
            @PathVariable(name = "id") long id,
            @RequestBody ChargeDto.Request request
    ) {
        UserPoint userPoint = pointService.chargeUserPoint(id, request.getAmount());
        return modelMapper.map(userPoint, ChargeDto.Response.class);
    }

    /**
     * 특정 유저의 포인트를 사용
     */
    @PatchMapping("{id}/use")
    public UserDto.Response use(
            @PathVariable(name = "id") long id,
            @RequestBody UserDto.Request request
    ) {
        UserPoint userPoint = pointService.usePoint(id, request.getAmount());
        return modelMapper.map(userPoint, UserDto.Response.class);
    }
}
