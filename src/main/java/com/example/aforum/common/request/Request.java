package com.example.aforum.common.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一请求封装
 */
@Data
public class Request<T> implements Serializable {

    /**
     * 请求数据
     */
    private T data;

    /**
     * 请求令牌（登录凭证）
     */
    private String token;

    /**
     * 请求链路追踪 ID
     */
    private String traceId;

    public Request() {
    }

    public Request(T data) {
        this.data = data;
    }

    public static <T> Request<T> of(T data) {
        return new Request<>(data);
    }
}
