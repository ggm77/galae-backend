package com.seohamin.galae.global.exception.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
public class ExceptionResponse {
    private final LocalDateTime timestamp;
    private final HttpStatus status;
    private final String code;
    private final String message;

    public ExceptionResponse(
            final HttpStatus status,
            final String code,
            final String message
    ) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
