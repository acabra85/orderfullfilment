package com.acabra.orderfullfilment.orderserver.dto;

public abstract class SimpleResponse<T> {
    final int statusCode;
    final String message;
    final T body;

    protected SimpleResponse(int statusCode, String message, T body) {
        this.statusCode = statusCode;
        this.message = message;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public T getBody() {
        return body;
    }
}
