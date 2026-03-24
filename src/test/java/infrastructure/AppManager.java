package infrastructure;

import config.TestConfig;
import utils.HealthChecker;

import java.io.File;
import java.io.FileNotFoundException;

public class AppManager {

    private Process producerProcess;
    private Process consumerProcess;

    public void start() throws Exception {
        producerProcess = startApp(TestConfig.PRODUCER_JAR, TestConfig.PRODUCER_PORT);
        consumerProcess = startApp(TestConfig.CONSUMER_JAR, TestConfig.CONSUMER_PORT);
        waitForStartup();
    }

    public void stop() {
        if (producerProcess != null) producerProcess.destroy();
        if (consumerProcess != null) consumerProcess.destroy();
    }

    public String getProducerUrl() {
        return "http://localhost:" + TestConfig.PRODUCER_PORT;
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
                "--spring.kafka.bootstrap-servers=" + TestConfig.KAFKA_BOOTSTRAP
        );
        pb.inheritIO();
        return pb.start();
    }

    private void waitForStartup() throws InterruptedException {
        HealthChecker healthChecker = new HealthChecker();
        healthChecker.waitForReady("http://localhost:" + TestConfig.PRODUCER_PORT + "/actuator/health");
        healthChecker.waitForReady("http://localhost:" + TestConfig.CONSUMER_PORT + "/actuator/health");
    }
}