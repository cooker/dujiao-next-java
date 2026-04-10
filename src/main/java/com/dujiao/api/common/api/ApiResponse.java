package com.dujiao.api.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @JsonProperty("status_code")
    private final int statusCode;

    private final String msg;

    private final T data;

    public ApiResponse(int statusCode, String msg, T data) {
        this.statusCode = statusCode;
        this.msg = msg;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseCodes.OK, "success", data);
    }

    public static <T> ApiResponse<T> error(int code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }

    public static <T> ApiResponse<T> error(int code, String msg, T data) {
        return new ApiResponse<>(code, msg, data);
    }

    /** 与 Go 版一致：业务成功、无载荷时用 0 + success + null。 */
    public static <T> ApiResponse<T> empty() {
        return new ApiResponse<>(ResponseCodes.OK, "success", null);
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
}
