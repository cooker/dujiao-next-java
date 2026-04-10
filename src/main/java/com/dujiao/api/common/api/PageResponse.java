package com.dujiao.api.common.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PageResponse<T> {

    @JsonProperty("status_code")
    private final int statusCode;

    private final String msg;

    private final T data;

    private final PaginationDto pagination;

    public PageResponse(int statusCode, String msg, T data, PaginationDto pagination) {
        this.statusCode = statusCode;
        this.msg = msg;
        this.data = data;
        this.pagination = pagination;
    }

    public static <T> PageResponse<T> success(T data, PaginationDto pagination) {
        return new PageResponse<>(ResponseCodes.OK, "success", data, pagination);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

    public PaginationDto getPagination() {
        return pagination;
    }
}
