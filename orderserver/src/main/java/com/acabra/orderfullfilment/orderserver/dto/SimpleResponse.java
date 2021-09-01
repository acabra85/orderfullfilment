package com.acabra.orderfullfilment.orderserver.dto;

public abstract class SimpleResponse<T> {
    final String message;
    final T body;

    protected SimpleResponse(String message, T body) {
        this.message = message;
        this.body = body;
    }

    public String getMessage() {
        return message;
    }

    public T getBody() {
        return body;
    }
}
