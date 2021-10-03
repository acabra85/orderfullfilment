package com.acabra.orderfullfilment.orderserver.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class DeliveryOrderTest {

    @Test
    public void mustReturnFalseForDifferentOrders() {
        DeliveryOrder firstOrder = DeliveryOrder.of("something", "banana-split", 1);
        DeliveryOrder other = DeliveryOrder.of("other", "banana-split", 1);
        Object object = new Object();
        Assertions.assertThat(firstOrder).isNotEqualTo(other);
        Assertions.assertThat(firstOrder.equals(null)).isFalse();
        Assertions.assertThat(firstOrder.equals(object)).isFalse();
        Assertions.assertThat(firstOrder.hashCode()).isNotEqualTo(other.hashCode());
    }

    @Test
    public void mustReturnTrueForOrdersWithEqualIds() {
        String id = "id-uuid-for-order";
        DeliveryOrder firstOrder = DeliveryOrder.of(id, "banana-split", 2);
        DeliveryOrder other = DeliveryOrder.of(id, "sundae", 2);
        Assertions.assertThat(firstOrder).isEqualTo(other);
        Assertions.assertThat(firstOrder.hashCode()).isEqualTo(other.hashCode());
    }
}