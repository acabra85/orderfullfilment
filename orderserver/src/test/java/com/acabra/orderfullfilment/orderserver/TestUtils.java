package com.acabra.orderfullfilment.orderserver;

import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequestDTO;
import com.acabra.orderfullfilment.orderserver.event.CourierDispatchedEvent;
import com.acabra.orderfullfilment.orderserver.model.Dishes;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        Stream.Builder<DeliveryOrderRequestDTO> orderStreamBuilder = Stream.builder();
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

        ObjectMapper objectMapper = new ObjectMapper();
        try(final BufferedWriter bw = new BufferedWriter(
                new FileWriter(file))) {
            bw.write('[');
            orderStreamBuilder.build().forEach(order -> {
                try {
                    String orderStr = objectMapper.writeValueAsString(order);
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

        } catch (IOException e) {
            e.printStackTrace();
        }
        org.assertj.core.api.Assertions.assertThat(orderProcessedCount.get()).isEqualTo(TOTAL_ORDERS);
        logger.info("Success orders created: {}",orderProcessedCount.get());
    }

    private DeliveryOrderRequestDTO buildDish() {
        Dishes dish = Dishes.getRandomDish();
        return new DeliveryOrderRequestDTO(UUID.randomUUID().toString(), dish.description, dish.prepTime);
    }

    public static class DispatchMatch {

        private Integer courierId = null;
        private Long kitchenReservationId = null;

        private DispatchMatch() {}
        
        private void setKitchenReservationId(long kitchenReservationId) {
            this.kitchenReservationId = kitchenReservationId;
        }

        private void setCourierId(int courierId) {
            this.courierId = courierId;
        }
        
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private DispatchMatch dispatchMatch;

            private Builder() {
                this.dispatchMatch = new DispatchMatch();
            }
            
            public Builder withCourier(int courierId) {
                dispatchMatch.setCourierId(courierId);
                return this;
            }

            public Builder withOrder(long orderId) {
                dispatchMatch.setKitchenReservationId(orderId);
                return this;
            }
            
            public DispatchMatch build() {
                DispatchMatch dispatchMatch = this.dispatchMatch;
                this.dispatchMatch = null;
                return dispatchMatch;
            }
        }
    }

    public static CourierDispatchedEvent buildDispatchEvent(DispatchMatch match) {

        return CourierDispatchedEvent.of(1000, null, match.courierId, match.kitchenReservationId, 1000);
    }
}
