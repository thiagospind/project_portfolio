package com.codegroup.portfolio.exception;

import org.springframework.http.HttpStatus;

public class Response500Exception extends ResponseException {

    public Response500Exception(String messageKey, String... params) {
        super(messageKey, HttpStatus.INTERNAL_SERVER_ERROR, params);
    }

}
