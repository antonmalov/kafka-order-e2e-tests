package utils;

import config.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class HealthChecker {

    private static final Logger log = LoggerFactory.getLogger(HealthChecker.class);
    private final RestTemplate restTemplate = new RestTemplate();

    public void waitForReady(String url) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < TestConfig.HEALTH_CHECK_TIMEOUT_SEC * 1000L) {
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
}