package com.acabra.orderfullfilment.orderserver.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class DeliveryOrderTest {

    @Test
    public void mustReturnFalseForDifferentOrders() {
        Assertions.assertThat(new DeliveryOrder("something", 2, 1))
                .isNotEqualTo(new DeliveryOrder("other", 2, 1));
    }

    @Test
    public void mustReturnTrueForOrdersWithEqualIds() {
        String id = "id-uuid-for-order";
        Assertions.assertThat(new DeliveryOrder(id, "something", 2, 2))
                .isEqualTo(new DeliveryOrder(id, "other", 2, 2));
    }
}