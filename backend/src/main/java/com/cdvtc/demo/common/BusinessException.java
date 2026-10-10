package com.cdvtc.demo.common;

/**
 * 业务异常。code 用于前端区分错误类型：
 *   400-参数/业务规则错误  403-无权限  404-资源不存在  409-状态冲突/重复提交
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
