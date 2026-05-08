package com.codegroup.portfolio.common;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AppResourceBundle {

    private final MessageSource messageSource;

    public String getMessage(String key, String... params) {
        return messageSource.getMessage(key, params, Locale.getDefault());
    }
}
