package com.extractor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "ai.api-key=test-key",
        "ai.model=test-model"
})
class AiInvoiceExtractorApplicationTests {

    @Test
    void contextLoads() {
    }
}
