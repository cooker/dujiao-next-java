package com.dujiao.api.common.api;

public final class ResponseCodes {

    public static final int OK = 0;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int TOO_MANY_REQUESTS = 429;
    public static final int INTERNAL = 500;
    public static final int NOT_IMPLEMENTED = 501;

    private ResponseCodes() {}
}
