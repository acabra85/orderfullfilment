package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.courier.model.Courier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Configuration
public class CourierLoader {

    @Bean
    public List<Courier> readFromConfig() throws IOException {
        URL resource = Objects.requireNonNull(CourierLoader.class.getClassLoader().getResource("couriers.json"));
        File src = new File(resource.getFile());
        if(src.exists() && src.isFile()) {
            return new ObjectMapper().readValue(src, new TypeReference<ArrayList<Courier>>() {});
        }
        return Collections.emptyList();
    }
}
