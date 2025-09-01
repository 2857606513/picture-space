package com.gzx.gzxpicturebackend.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    ACCOUNT_LENGTH_ERROR(40102, "账号长度不能小于4"),
    PASSWORD_LENGTH_ERROR(40103, "密码长度不能小于8"),
    OPERATION_ERROR(50001, "操作失败");


    private final int code;


    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}