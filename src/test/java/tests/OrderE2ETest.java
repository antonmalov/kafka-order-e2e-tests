package tests;

import config.TestConfig;
import infrastructure.KafkaTestClient;
import io.qameta.allure.*;
import org.example.dto.OrderRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Epic("Kafka Order Processing")
@Feature("E2E Tests")
class OrderE2ETest extends BaseE2ETest {

    private static final Logger log = LoggerFactory.getLogger(OrderE2ETest.class);
    private final KafkaTestClient kafkaClient = new KafkaTestClient();

    @Test
    @Story("Positive scenario")
    @Description("Order with product='book' should be processed successfully and not go to DLT")
    @Severity(SeverityLevel.CRITICAL)
    void positiveScenario_orderProcessedSuccessfully() throws InterruptedException {
        String uniqueId = "pos-" + UUID.randomUUID();
        OrderRequest request = new OrderRequest(uniqueId, "book", 2);
        sendOrder(request);

        waitForProcessing();

        long countInMain = kafkaClient.countMessages(TestConfig.MAIN_TOPIC, "\"orderId\":\"" + uniqueId + "\"", TestConfig.COUNT_TIMEOUT_SEC);
        long countInDlt = kafkaClient.countMessages(TestConfig.DLT_TOPIC, "\"orderId\":\"" + uniqueId + "\"", TestConfig.COUNT_TIMEOUT_SEC);

        assertEquals(1, countInMain, "Message should be in main topic");
        assertEquals(0, countInDlt, "Message should NOT be in DLT");
    }

    @Test
    @Story("Negative scenario")
    @Description("Order with product='fail' should cause retries and end up in DLT")
    @Severity(SeverityLevel.CRITICAL)
    void negativeScenario_orderGoesToDlt() throws InterruptedException {
        String uniqueId = "neg-" + UUID.randomUUID();
        OrderRequest request = new OrderRequest(uniqueId, "fail", 1);

        long startTime = System.currentTimeMillis();
        sendOrder(request);

        waitForProcessing();

        long countInDlt = kafkaClient.countMessages(TestConfig.DLT_TOPIC, "\"orderId\":\"" + uniqueId + "\"", TestConfig.COUNT_TIMEOUT_SEC);
        long elapsed = System.currentTimeMillis() - startTime;

        assertEquals(1, countInDlt, "Message should be in DLT");
        assertTrue(elapsed >= 2000, "Expected at least 2000 ms for retries, but was " + elapsed + " ms");
        log.info("Time to DLT: {} ms", elapsed);
    }

    @Test
    @Story("Partial failure scenario")
    @Description("Order with orderId starting with 'partial-' should fail twice, then succeed")
    @Severity(SeverityLevel.NORMAL)
    void partialFailure_retryThenSuccess() throws InterruptedException {
        String uniqueId = "partial-" + UUID.randomUUID();
        OrderRequest request = new OrderRequest(uniqueId, "someProduct", 1);

        long startTime = System.currentTimeMillis();
        sendOrder(request);

        waitForProcessing(WAIT_AFTER_SEND_SEC * 2);

        long countInMain = kafkaClient.countMessages(TestConfig.MAIN_TOPIC, "\"orderId\":\"" + uniqueId + "\"", TestConfig.PARTIAL_COUNT_TIMEOUT_SEC);
        long countInDlt = kafkaClient.countMessages(TestConfig.DLT_TOPIC, "\"orderId\":\"" + uniqueId + "\"", TestConfig.COUNT_TIMEOUT_SEC);

        assertEquals(1, countInMain, "Message should be in main topic");
        assertEquals(0, countInDlt, "Message should NOT be in DLT");
        log.info("Partial failure test completed in {} ms", System.currentTimeMillis() - startTime);
    }
}