package com.seohamin.galae.global.exception.handler;

import com.seohamin.galae.global.exception.CustomException;
import com.seohamin.galae.global.exception.response.ExceptionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class CustomizedResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    // 커스텀으로 만든 예외들 처리
    @ExceptionHandler(CustomException.class)
    public final ResponseEntity<ExceptionResponse> handleException(final CustomException ex) {

        final String message;
        if (ex.getMessage() == null || ex.getMessage().isEmpty()) {
            message = ex.getExceptionCode().getMessage();
        } else {
            message = ex.getMessage();
        }

        final ExceptionResponse exceptionResponse = new ExceptionResponse(
                ex.getExceptionCode().getStatus(),
                ex.getExceptionCode().name(),
                message
        );

        return new ResponseEntity<>(exceptionResponse, ex.getExceptionCode().getStatus());
    }

    // @Valid 검증 실패 처리
    // 어떤 필드가 잘못되었는지 메시지에 담아 준다
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex,
            final HttpHeaders headers,
            final HttpStatusCode status,
            final WebRequest request
    ) {
        final String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("요청 정보가 완전하지 않습니다.");

        final ExceptionResponse exceptionResponse = new ExceptionResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                message
        );

        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    // JSON 형식이 잘못된 경우
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            final HttpMessageNotReadableException ex,
            final HttpHeaders headers,
            final HttpStatusCode status,
            final WebRequest request
    ) {
        final ExceptionResponse exceptionResponse = new ExceptionResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "요청 본문을 읽을 수 없습니다."
        );

        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    // 경로·쿼리 파라미터 타입이 잘못된 경우
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public final ResponseEntity<ExceptionResponse> handleTypeMismatch(final MethodArgumentTypeMismatchException ex) {
        final ExceptionResponse exceptionResponse = new ExceptionResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                ex.getName() + " 값이 올바르지 않습니다."
        );

        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    // 유니크 제약 위반 (같은 요청이 동시에 들어온 경우 등)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public final ResponseEntity<ExceptionResponse> handleDataIntegrity(final DataIntegrityViolationException ex) {
        log.warn("DataIntegrityViolationException: {}", ex.getMostSpecificCause().getMessage());

        final ExceptionResponse exceptionResponse = new ExceptionResponse(
                HttpStatus.CONFLICT,
                "CONFLICT",
                "요청이 현재 데이터와 충돌합니다."
        );

        return new ResponseEntity<>(exceptionResponse, HttpStatus.CONFLICT);
    }

    // 나머지 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ExceptionResponse> handleAllException(final Exception ex) {
        final ExceptionResponse exceptionResponse = new ExceptionResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Internal Server Error"
        );

        log.error("CustomizedResponseEntityExceptionHandler.handleAllExceptions message:{}", ex.getMessage(), ex);

        return new ResponseEntity<>(exceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
