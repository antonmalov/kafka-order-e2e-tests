package infrastructure;

import utils.HealthChecker;

import java.io.File;
import java.io.FileNotFoundException;

public class AppManager {

    private Process producerProcess;
    private Process consumerProcess;

    private static final String PRODUCER_JAR = "apps/producer-service.jar";
    private static final String CONSUMER_JAR = "apps/notification-service.jar";
    private static final String KAFKA_BOOTSTRAP = "localhost:9092";
    private static final int PRODUCER_PORT = 8081;
    private static final int CONSUMER_PORT = 8080;

    public void start() throws Exception {
        producerProcess = startApp(PRODUCER_JAR, PRODUCER_PORT);
        consumerProcess = startApp(CONSUMER_JAR, CONSUMER_PORT);
        waitForStartup();
    }

    public void stop() {
        if (producerProcess != null) producerProcess.destroy();
        if (consumerProcess != null) consumerProcess.destroy();
    }

    public String getProducerUrl() {
        return "http://localhost:" + PRODUCER_PORT;
    }

    private Process startApp(String jarPath, int port) throws Exception {
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            throw new FileNotFoundException("JAR not found: " + jarFile.getAbsolutePath());
        }

        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-jar",
                jarFile.getAbsolutePath(),
                "--server.port=" + port,
                "--spring.kafka.bootstrap-servers=" + KAFKA_BOOTSTRAP
        );
        pb.inheritIO();
        return pb.start();
    }

    private void waitForStartup() throws InterruptedException {
        HealthChecker healthChecker = new HealthChecker();
        healthChecker.waitForReady("http://localhost:" + PRODUCER_PORT + "/actuator/health");
        healthChecker.waitForReady("http://localhost:" + CONSUMER_PORT + "/actuator/health");
    }
}