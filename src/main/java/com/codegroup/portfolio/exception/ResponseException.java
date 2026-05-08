package com.codegroup.portfolio.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ResponseException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String messageKey;
    private final String[] params;

    public ResponseException(String messageKey, HttpStatus httpStatus, String... params) {
        super(messageKey);
        this.httpStatus = httpStatus;
        this.messageKey = messageKey;
        this.params = params;
    }
}
