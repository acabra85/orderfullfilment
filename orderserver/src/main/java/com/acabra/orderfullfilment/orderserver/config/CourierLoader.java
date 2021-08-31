package com.acabra.orderfullfilment.orderserver.config;

import com.acabra.orderfullfilment.orderserver.courier.model.Courier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Configuration
@Slf4j
public class CourierLoader {

    @Bean
    public List<Courier> readFromConfig(ResourceLoader resourceLoader) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = resourceLoader.getResource("classpath:couriers.json").getInputStream();
        } catch (Exception e) {
            log.error("[SYSTEM] error loading from resource "+ e.getMessage(), e);
            try {
                inputStream = new FileInputStream("target/couriers.json");
            } catch (FileNotFoundException ex) {
                log.error("[SYSTEM] No Couriers loaded:" +ex.getMessage(), ex);
                return Collections.emptyList();
            }
        }
        ArrayList<Courier> couriers = new ObjectMapper().readValue(inputStream, new TypeReference<ArrayList<Courier>>() {
        });
        log.info("[SYSTEM] {} Couriers loaded", couriers.size());
        return couriers;
    }

}
