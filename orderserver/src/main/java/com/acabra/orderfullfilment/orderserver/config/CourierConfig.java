package com.acabra.orderfullfilment.orderserver.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

@ConstructorBinding
@ConfigurationProperties(prefix = "courier")
@Validated
public class CourierConfig {

    private final Integer minEta;
    private final Integer maxEta;

    public CourierConfig(@NonNull @Value("${courier.min-eta}")Integer minEta, @NonNull @Value("${courier.max-eta}")Integer maxEta) {
        this.minEta = minEta;
        this.maxEta = maxEta;
    }

    @NonNull
    public Integer getMinEta() {
        return minEta;
    }

    @NonNull
    public Integer getMaxEta() {
        return maxEta;
    }
}