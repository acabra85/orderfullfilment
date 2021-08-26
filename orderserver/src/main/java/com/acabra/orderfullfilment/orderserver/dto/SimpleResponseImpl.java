package com.acabra.orderfullfilment.orderserver.dto;

public class SimpleResponseImpl<T> extends SimpleResponse<T> {
    public SimpleResponseImpl(int statusCode, String message, T body) {
        super(statusCode, message, body);
    }
}
