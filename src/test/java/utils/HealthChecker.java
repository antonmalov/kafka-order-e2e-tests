package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class HealthChecker {

    private static final Logger log = LoggerFactory.getLogger(HealthChecker.class);
    private static final int HEALTH_CHECK_TIMEOUT_SEC = 30;
    private final RestTemplate restTemplate = new RestTemplate();

    public void waitForReady(String url) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < HEALTH_CHECK_TIMEOUT_SEC * 1000L) {
            try {
                ResponseEntity<String> health = restTemplate.getForEntity(url, String.class);
                if (health.getStatusCode().is2xxSuccessful()) {
                    log.info("Service ready: {}", url);
                    return;
                }
            } catch (Exception e) {
                // ignore, continue waiting
            }
            Thread.sleep(500);
        }
        throw new RuntimeException("Health check timeout for " + url);
    }
}