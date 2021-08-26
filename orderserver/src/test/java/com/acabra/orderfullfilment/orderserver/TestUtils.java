package com.acabra.orderfullfilment.orderserver;

import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequest;
import com.acabra.orderfullfilment.orderserver.model.Dishes;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.FileSystems;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TestUtils {
    // 18 hrs a day (assuming the system receives orders from 6:00 - 22:00)
    private static final int SECONDS_IN_WORKING_DAY = 18 * 60 * 60;
    private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    @Test
    public void generateDayOrders() {
        logger.info("Generating random daily orders ....");
        Stream.Builder<DeliveryOrderRequest> orderStreamBuilder = Stream.builder();
        AtomicInteger orderProcessedCount = new AtomicInteger(0);
        int TOTAL_ORDERS = SECONDS_IN_WORKING_DAY * 2;
        logger.info("Total orders to be created: {}", TOTAL_ORDERS);
        IntStream.range(0, SECONDS_IN_WORKING_DAY)
                .forEach(i -> {
                    orderStreamBuilder.accept(buildDish());
                    orderStreamBuilder.accept(buildDish());
                });

        File file = new File(String.format("target%sorders-%s.json",
                FileSystems.getDefault().getSeparator(),
                LocalDateTime.now().format(FORMATTER)));
        logger.info("File result: {}", file.getAbsolutePath());

        try(final BufferedWriter bw = new BufferedWriter(
                new FileWriter(file))) {
            bw.write('[');
            orderStreamBuilder.build().forEach(order -> {
                String orderStr = order.toString();
                try {
                    bw.write(orderStr);
                    if(orderProcessedCount.getAndIncrement() < TOTAL_ORDERS - 1) {
                        bw.write(',');
                        bw.write('\n');
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            bw.write(']');

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        org.assertj.core.api.Assertions.assertThat(orderProcessedCount.get()).isEqualTo(TOTAL_ORDERS);
        logger.info("Success orders created: {}",orderProcessedCount.get());
    }

    private DeliveryOrderRequest buildDish() {
        Dishes dish = Dishes.getRandomDish();
        return new DeliveryOrderRequest(UUID.randomUUID().toString(), dish.description, dish.prepTime);
    }
}
