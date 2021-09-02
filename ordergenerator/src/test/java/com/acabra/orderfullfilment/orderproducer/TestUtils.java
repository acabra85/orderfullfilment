package com.acabra.orderfullfilment.orderproducer;

import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequestDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestUtils {
    public static List<DeliveryOrderRequestDTO> buildOrderListOfSize(int size) {
        return getOrdersWithSigPillAtPos(size, -1);
    }

    public static List<DeliveryOrderRequestDTO> getOrdersWithSigPillAtPos(int size, int pos) {
        if(size<=0) return Collections.emptyList();
        List<DeliveryOrderRequestDTO> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if(i == pos) {
                list.add(DeliveryOrderRequestDTO.ofSigPill());
            } else {
                list.add(new DeliveryOrderRequestDTO("1", "1", 1));
            }
        }
        return list;
    }
}
