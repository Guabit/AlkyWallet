package com.alkywallet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MonedaIncompatibleException extends RuntimeException {
    public MonedaIncompatibleException(String message) {
        super(message);
    }
}
