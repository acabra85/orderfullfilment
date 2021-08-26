package com.acabra.orderfullfilment.orderproducer;

import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestUtils {
    public static List<DeliveryOrderRequest> getOrders(int size) {
        return getOrdersWithSigPillAtPos(size, -1);
    }

    public static List<DeliveryOrderRequest> getOrdersWithSigPillAtPos(int size, int pos) {
        if(size<=0) return Collections.emptyList();
        List<DeliveryOrderRequest> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if(i == pos) {
                list.add(DeliveryOrderRequest.ofSigPill());
            } else {
                list.add(new DeliveryOrderRequest("1", "1", 1));
            }
        }
        return list;
    }
}
