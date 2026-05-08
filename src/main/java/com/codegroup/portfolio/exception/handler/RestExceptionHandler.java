package com.codegroup.portfolio.exception.handler;

import com.codegroup.portfolio.common.AppResourceBundle;
import com.codegroup.portfolio.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
public class RestExceptionHandler {

    private final AppResourceBundle bundle;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> genericException(Exception ex) {
        ApiError apiError = ApiError
                .builder()
                .timestamp(LocalDateTime.now())
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .errors(List.of(bundle.getMessage("error.unexpected", ex.getMessage())))
                .build();
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({
            Response400Exception.class,
            Response404Exception.class,
            Response422Exception.class,
            Response500Exception.class,
            ResponseException.class,
    })
    public ResponseEntity<ApiError> responseException(ResponseException ex) {
        String message = bundle.getMessage(ex.getMessageKey(), ex.getParams());
        ApiError apiError = ApiError
                .builder()
                .timestamp(LocalDateTime.now())
                .code(ex.getHttpStatus().value())
                .status(ex.getHttpStatus().name())
                .errors(List.of(message))
                .build();
        return new ResponseEntity<>(apiError, ex.getHttpStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> argumentNotValidException(MethodArgumentNotValidException ex) {
        List<String> errorList = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage).toList();
        ApiError apiError = ApiError
                .builder()
                .timestamp(LocalDateTime.now())
                .code(HttpStatus.BAD_REQUEST.value())
                .status(HttpStatus.BAD_REQUEST.name())
                .errors(errorList)
                .build();
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }
}
