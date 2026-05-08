package com.codegroup.portfolio.exception;

import org.springframework.http.HttpStatus;

public class Response400Exception extends ResponseException {
    public Response400Exception(String messageKey, String... params) {
        super(messageKey, HttpStatus.BAD_REQUEST, params);
    }
}
