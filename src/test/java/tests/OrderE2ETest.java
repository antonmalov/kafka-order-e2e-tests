package tests;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.dto.OrderRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderE2ETest {

    private static final Logger log = LoggerFactory.getLogger(OrderE2ETest.class);

    private static final String KAFKA_BOOTSTRAP = "localhost:9092";
    private static final String PRODUCER_JAR = "apps/producer-service.jar";
    private static final String CONSUMER_JAR = "apps/notification-service.jar";
    private static final int PRODUCER_PORT = 8081;
    private static final int CONSUMER_PORT = 8080;
    private static final int HEALTH_CHECK_TIMEOUT_SEC = 30;
    private static final int WAIT_AFTER_SEND_SEC = 10;

    private Process producerProcess;
    private Process consumerProcess;
    private RestTemplate restTemplate = new RestTemplate();

    @BeforeEach
    void startApps() throws Exception {
        producerProcess = startApp(PRODUCER_JAR, String.valueOf(PRODUCER_PORT), KAFKA_BOOTSTRAP);
        consumerProcess = startApp(CONSUMER_JAR, String.valueOf(CONSUMER_PORT), KAFKA_BOOTSTRAP);
        waitForHealth("http://localhost:" + PRODUCER_PORT + "/actuator/health");
        waitForHealth("http://localhost:" + CONSUMER_PORT + "/actuator/health");
    }

    @AfterEach
    void stopApps() {
        if (producerProcess != null) producerProcess.destroy();
        if (consumerProcess != null) consumerProcess.destroy();
    }

    @Test
    void testPositiveScenario_OrderProcessedSuccessfully() throws Exception {
        String uniqueId = "pos-" + UUID.randomUUID();
        OrderRequest request = new OrderRequest(uniqueId, "book", 2);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + PRODUCER_PORT + "/orders",
                request,
                String.class
        );
        assertEquals(200, response.getStatusCode().value());
        log.info("Positive request sent: {}", request);

        Thread.sleep(WAIT_AFTER_SEND_SEC * 1000L);

        long countInMain = countMessagesInTopic("orders-v2", "\"orderId\":\"" + uniqueId + "\"", 5);
        assertEquals(1, countInMain, "Exactly one message with orderId=" + uniqueId + " should be in main topic");

        long countInDlt = countMessagesInTopic("orders-v2.DLT", "\"orderId\":\"" + uniqueId + "\"", 5);
        assertEquals(0, countInDlt, "No message with orderId=" + uniqueId + " should be in DLT");

        log.info("Positive scenario passed: main count={}, dlt count={}", countInMain, countInDlt);
    }

    @Test
    void testNegativeScenario_OrderGoesToDlt() throws Exception {
        String uniqueId = "neg-" + UUID.randomUUID();
        OrderRequest request = new OrderRequest(uniqueId, "fail", 1);

        long startTime = System.currentTimeMillis();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + PRODUCER_PORT + "/orders",
                request,
                String.class
        );
        assertEquals(200, response.getStatusCode().value());
        log.info("Negative request sent: {}", request);

        Thread.sleep(WAIT_AFTER_SEND_SEC * 1000L);

        long countInDlt = countMessagesInTopic("orders-v2.DLT", "\"orderId\":\"" + uniqueId + "\"", 15);
        assertEquals(1, countInDlt, "Exactly one message with orderId=" + uniqueId + " should be in DLT");

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        log.info("Time to DLT: {} ms", elapsed);

        assertTrue(elapsed >= 2000, "Expected at least 2000 ms for retries, but was " + elapsed + " ms");
    }

    @Test
    void testPartialFailure_RetryThenSuccess() throws Exception {
        String uniqueId = "partial-" + UUID.randomUUID();
        OrderRequest request = new OrderRequest(uniqueId, "someProduct", 1);

        long startTime = System.currentTimeMillis();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + PRODUCER_PORT + "/orders",
                request,
                String.class
        );
        assertEquals(200, response.getStatusCode().value());
        log.info("Partial failure request sent: {}", request);

        Thread.sleep(WAIT_AFTER_SEND_SEC * 2000L);

        long countInMain = countMessagesInTopic("orders-v2", "\"orderId\":\"" + uniqueId + "\"", 15);
        assertEquals(1, countInMain, "Exactly one message with orderId=" + uniqueId + " should be in main topic");

        long countInDlt = countMessagesInTopic("orders-v2.DLT", "\"orderId\":\"" + uniqueId + "\"", 5);
        assertEquals(0, countInDlt, "No message with orderId=" + uniqueId + " should be in DLT");

        long endTime = System.currentTimeMillis();
        log.info("Partial failure test completed in {} ms", endTime - startTime);
    }

    private Process startApp(String jarPath, String port, String bootstrapServers) throws Exception {
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            throw new FileNotFoundException("JAR not found: " + jarFile.getAbsolutePath());
        }

        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-jar",
                jarFile.getAbsolutePath(),
                "--server.port=" + port,
                "--spring.kafka.bootstrap-servers=" + bootstrapServers
        );
        pb.inheritIO();
        return pb.start();
    }

    private void waitForHealth(String url) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < HEALTH_CHECK_TIMEOUT_SEC * 1000L) {
            try {
                ResponseEntity<String> health = restTemplate.getForEntity(url, String.class);
                if (health.getStatusCode().is2xxSuccessful()) {
                    log.info("Service ready: {}", url);
                    return;
                }
            } catch (Exception e) {

            }
            Thread.sleep(500);
        }
        throw new RuntimeException("Health check timeout for " + url);
    }

    private long countMessagesInTopic(String topic, String expectedSubstring, int timeoutSeconds) {
        Properties props = createConsumerProperties();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));
            long start = System.currentTimeMillis();
            long count = 0;
            while (System.currentTimeMillis() - start < timeoutSeconds * 1000L) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (var record : records) {
                    if (record.value().contains(expectedSubstring)) {
                        count++;
                    }
                }
            }
            return count;
        }
    }

    private Properties createConsumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }
}