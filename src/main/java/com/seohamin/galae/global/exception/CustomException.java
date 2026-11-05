package com.seohamin.galae.global.exception;

import com.seohamin.galae.global.exception.constants.ExceptionCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ExceptionCode exceptionCode;

    public CustomException(final ExceptionCode exceptionCode) {
        super("");
        this.exceptionCode = exceptionCode;
    }

    public CustomException(final ExceptionCode exceptionCode, final String message) {
        super(message);
        this.exceptionCode = exceptionCode;
    }
}
