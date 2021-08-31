package com.acabra.orderfullfilment.orderserver.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class DeliveryOrderTest {

    @Test
    public void mustReturnFalseForDifferentOrders() {
        Assertions.assertThat(DeliveryOrder.of("something", "banana-split", 1))
                .isNotEqualTo(DeliveryOrder.of("other", "banana-split", 1));
    }

    @Test
    public void mustReturnTrueForOrdersWithEqualIds() {
        String id = "id-uuid-for-order";
        Assertions.assertThat(DeliveryOrder.of(id, "banana-split", 2))
                .isEqualTo(DeliveryOrder.of(id, "sundae", 2));
    }
}