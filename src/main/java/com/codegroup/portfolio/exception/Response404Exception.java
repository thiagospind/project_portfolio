package com.codegroup.portfolio.exception;

import org.springframework.http.HttpStatus;

public class Response404Exception extends ResponseException {

    public Response404Exception(String messageKey, String... params) {
        super(messageKey, HttpStatus.NOT_FOUND, params);
    }

}
