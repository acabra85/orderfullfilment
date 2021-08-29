package com.acabra.orderfullfilment.orderserver.utils;

import com.acabra.orderfullfilment.orderserver.config.CourierConfig;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class EtaEstimator {

    private final int ceilingEta;
    private final Integer floorEta;
    private final static Random etaGenerate = new Random();

    public EtaEstimator(CourierConfig config) {
        this.ceilingEta = config.getMaxEta() - config.getMinEta() + 1;
        this.floorEta = config.getMinEta();
    }

    public int estimateCourierTravelTime() {
        return Math.abs(floorEta + etaGenerate.nextInt(ceilingEta));
    }

}