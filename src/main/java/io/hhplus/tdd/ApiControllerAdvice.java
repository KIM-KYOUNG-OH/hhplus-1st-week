package io.hhplus.tdd;

import io.hhplus.tdd.point.controller.PointController;
import io.hhplus.tdd.point.exception.InsufficientPointBalanceException;
import io.hhplus.tdd.point.exception.MaximumPointExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiControllerAdvice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);

    @ExceptionHandler(value = MaximumPointExceededException.class)
    public ResponseEntity<ErrorResponse> handleException(MaximumPointExceededException e) {
        return ResponseEntity.status(400).body(new ErrorResponse("400", e.getMessage()));
    }

    @ExceptionHandler(value = InsufficientPointBalanceException.class)
    public ResponseEntity<ErrorResponse> handleException(InsufficientPointBalanceException e) {
        return ResponseEntity.status(400).body(new ErrorResponse("400", e.getMessage()));
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("error occurred : ", e);
        return ResponseEntity.status(500).body(new ErrorResponse("500", "에러가 발생했습니다."));
    }
}
