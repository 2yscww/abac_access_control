package com.xie.platform.common;

import lombok.Data;

@Data
public class Response<T> {
    private int code;
    private String msg;
    private T data;

    public Response() {
    }

    public Response(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // ? 静态方法构造

    // * 示范构造
    // public static <T> Response<T> Success(T data, String msg) {
    //     return new Response<>(200, msg, data);
    // }

}
