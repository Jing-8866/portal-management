package com.portal.common.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResult<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "success", data);
    }
    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(200, message, data);
    }
    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, message, null);
    }
    public static <T> ApiResult<T> error(String message) {
        return new ApiResult<>(500, message, null);
    }
}
