package com.acabra.orderfullfilment.orderserver.dto;

public class SimpleResponseImpl<T> extends SimpleResponse<T> {
    public SimpleResponseImpl(String message, T body) {
        super(message, body);
    }
}
