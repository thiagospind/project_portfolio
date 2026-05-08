package com.codegroup.portfolio.exception;

import org.springframework.http.HttpStatus;

public class Response422Exception extends ResponseException {

    public Response422Exception(String messageKey, String... params) {
        super(messageKey, HttpStatus.UNPROCESSABLE_ENTITY, params);
    }

}
