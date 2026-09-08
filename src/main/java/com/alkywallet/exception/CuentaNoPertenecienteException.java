package com.alkywallet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class CuentaNoPertenecienteException extends RuntimeException {
    public CuentaNoPertenecienteException(String message) {
        super(message);
    }
}
