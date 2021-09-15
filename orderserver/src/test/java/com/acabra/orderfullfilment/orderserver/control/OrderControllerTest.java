package com.acabra.orderfullfilment.orderserver.control;

import com.acabra.orderfullfilment.orderserver.core.OrderRequestHandler;
import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequestDTO;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class OrderControllerTest {

    private OrderController underTest;


    Consumer<DeliveryOrderRequestDTO> mockConsumer = Mockito.mock(OrderRequestHandler.class);

    @BeforeEach
    public void setup() {
        underTest = new OrderController(mockConsumer);
    }

    @Test
    public void mustAcceptValidRequestDTO() {
        //given
        DeliveryOrderRequestDTO validDTO = new DeliveryOrderRequestDTO("ssds", "asdesa", 1);
        Mockito.doNothing().when(mockConsumer).accept(validDTO);
        //then

        //when
        ResponseEntity<String> response = underTest.acceptOrder(validDTO);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        //verify
        Mockito.verify(mockConsumer, Mockito.times(1)).accept(validDTO);
    }

    @Test
    public void mustRejectInvalidRequestsDTO() {
        //given
        List<DeliveryOrderRequestDTO> invalidDTOS = List.of(new DeliveryOrderRequestDTO("", "", -12),
                new DeliveryOrderRequestDTO("312", "", 1), new DeliveryOrderRequestDTO("", "321", 1));

        //when
        List<ResponseEntity<String>> collect = invalidDTOS.stream()
                .map(dto -> underTest.acceptOrder(dto))
                .collect(Collectors.toList());

        //then
        boolean actual = collect.stream().allMatch(r -> r.getStatusCode() == HttpStatus.BAD_REQUEST);

        //verify
        Assertions.assertThat(actual).isTrue();
        Mockito.verifyNoInteractions(mockConsumer);
    }


}