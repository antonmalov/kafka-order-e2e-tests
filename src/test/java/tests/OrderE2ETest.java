package tests;

import infrastructure.AppManager;
import infrastructure.KafkaTestClient;
import org.example.dto.OrderRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderE2ETest {

    private static final Logger log = LoggerFactory.getLogger(OrderE2ETest.class);
    private static final int WAIT_AFTER_SEND_SEC = 10;

    private final AppManager appManager = new AppManager();
    private final KafkaTestClient kafkaClient = new KafkaTestClient();
    private final RestTemplate restTemplate = new RestTemplate();

    @BeforeEach
    void setUp() throws Exception {
        appManager.start();
    }

    @AfterEach
    void tearDown() {
        appManager.stop();
    }

    @Test
    void positiveScenario_orderProcessedSuccessfully() throws Exception {
        String uniqueId = "pos-" + UUID.randomUUID();
        OrderRequest request = new OrderRequest(uniqueId, "book", 2);

        sendOrder(request);

        Thread.sleep(WAIT_AFTER_SEND_SEC * 1000L);

        long countInMain = kafkaClient.countMessages("orders-v2", "\"orderId\":\"" + uniqueId + "\"", 5);
        long countInDlt = kafkaClient.countMessages("orders-v2.DLT", "\"orderId\":\"" + uniqueId + "\"", 5);

        assertEquals(1, countInMain, "Message should be in main topic");
        assertEquals(0, countInDlt, "Message should NOT be in DLT");
    }

    @Test
    void negativeScenario_orderGoesToDlt() throws Exception {
        String uniqueId = "neg-" + UUID.randomUUID();
        OrderRequest request = new OrderRequest(uniqueId, "fail", 1);

        long startTime = System.currentTimeMillis();
        sendOrder(request);
        Thread.sleep(WAIT_AFTER_SEND_SEC * 1000L);

        long countInDlt = kafkaClient.countMessages("orders-v2.DLT", "\"orderId\":\"" + uniqueId + "\"", 15);
        long elapsed = System.currentTimeMillis() - startTime;

        assertEquals(1, countInDlt, "Message should be in DLT");
        assertTrue(elapsed >= 2000, "Expected at least 2000 ms for retries, but was " + elapsed + " ms");
        log.info("Time to DLT: {} ms", elapsed);
    }

    @Test
    void partialFailure_retryThenSuccess() throws Exception {
        String uniqueId = "partial-" + UUID.randomUUID();
        OrderRequest request = new OrderRequest(uniqueId, "someProduct", 1);

        long startTime = System.currentTimeMillis();
        sendOrder(request);
        Thread.sleep(WAIT_AFTER_SEND_SEC * 2000L);

        long countInMain = kafkaClient.countMessages("orders-v2", "\"orderId\":\"" + uniqueId + "\"", 15);
        long countInDlt = kafkaClient.countMessages("orders-v2.DLT", "\"orderId\":\"" + uniqueId + "\"", 5);

        assertEquals(1, countInMain, "Message should be in main topic");
        assertEquals(0, countInDlt, "Message should NOT be in DLT");
        log.info("Partial failure test completed in {} ms", System.currentTimeMillis() - startTime);
    }

    private void sendOrder(OrderRequest request) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                appManager.getProducerUrl() + "/orders",
                request,
                String.class
        );
        assertEquals(200, response.getStatusCode().value());
        log.info("Request sent: {}", request);
    }
}