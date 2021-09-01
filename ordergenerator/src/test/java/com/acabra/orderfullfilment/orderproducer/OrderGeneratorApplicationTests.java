package com.acabra.orderfullfilment.orderproducer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderGeneratorApplicationTests {
    //Ensures the context application of spring loads correctly

    @Test
    public void mustRunApplicationContext() { }

    @Test
    public void mustProvideDefault_givenNonExistentFile() {
        OrderGeneratorApp.main(new String[]{"my_non_existent_file"});
    }

    @Test
    public void mustExecuteSmallFile_givenSmallParameter() {
        OrderGeneratorApp.main(new String[]{"small"});
    }

}
