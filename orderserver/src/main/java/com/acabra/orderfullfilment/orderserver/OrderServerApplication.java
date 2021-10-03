package com.acabra.orderfullfilment.orderserver;

import com.acabra.orderfullfilment.orderserver.config.CourierConfig;
import com.acabra.orderfullfilment.orderserver.config.OrderServerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({CourierConfig.class, OrderServerConfig.class})
public class OrderServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServerApplication.class, args);
    }

}
