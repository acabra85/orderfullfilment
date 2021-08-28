package com.acabra.orderfullfilment.orderserver.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
public class CourierConfig {

    @NonNull
    @Value("${courier.min-eta}")
    private Integer minEta;

    @NonNull
    @Value("${courier.max-eta}")
    private Integer maxEta;

    @NonNull
    public Integer getMinEta() {
        return minEta;
    }

    @NonNull
    public Integer getMaxEta() {
        return maxEta;
    }
}