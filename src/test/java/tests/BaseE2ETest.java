package tests;

import infrastructure.AppManager;
import org.example.dto.OrderRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class BaseE2ETest {

    protected static final Logger log = LoggerFactory.getLogger(BaseE2ETest.class);
    protected static final int WAIT_AFTER_SEND_SEC = 20;

    protected final AppManager appManager = new AppManager();
    protected final RestTemplate restTemplate = new RestTemplate();

    @BeforeEach
    void setUp() throws Exception {
        appManager.start();
    }

    @AfterEach
    void tearDown() {
        appManager.stop();
    }

    protected void sendOrder(OrderRequest request) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                appManager.getProducerUrl() + "/orders",
                request,
                String.class
        );
        assertEquals(200, response.getStatusCode().value());
        log.info("Request sent: {}", request);
    }

    protected void waitForProcessing() throws InterruptedException {
        Thread.sleep(WAIT_AFTER_SEND_SEC * 1000L);
    }

    protected void waitForProcessing(long seconds) throws InterruptedException {
        Thread.sleep(seconds * 1000L);
    }
}
