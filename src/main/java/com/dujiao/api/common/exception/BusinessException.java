package com.dujiao.api.common.exception;

public class BusinessException extends RuntimeException {

    private final int statusCode;

    public BusinessException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
