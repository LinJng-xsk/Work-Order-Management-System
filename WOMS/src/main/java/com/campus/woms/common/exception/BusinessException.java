package com.campus.woms.common.exception;

import lombok.Getter;

/** 业务异常：Service 层抛出，后续由 GlobalExceptionHandler 统一处理 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
