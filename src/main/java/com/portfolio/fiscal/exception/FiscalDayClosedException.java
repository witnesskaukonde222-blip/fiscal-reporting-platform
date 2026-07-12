package com.portfolio.fiscal.exception;

public class FiscalDayClosedException extends RuntimeException {
    public FiscalDayClosedException(String message) {
        super(message);
    }
}
