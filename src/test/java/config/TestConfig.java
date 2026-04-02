package config;

public final class TestConfig {

    private TestConfig() {}

    public static final String KAFKA_BOOTSTRAP = "localhost:9092";
    public static final String MAIN_TOPIC = "orders-v2";
    public static final String DLT_TOPIC = "orders-v2.DLT";

    public static final String PRODUCER_JAR = "apps/producer-service.jar";
    public static final String CONSUMER_JAR = "apps/notification-service.jar";
    public static final int PRODUCER_PORT = 8081;
    public static final int CONSUMER_PORT = 8080;

    public static final int HEALTH_CHECK_TIMEOUT_SEC = Integer.parseInt(System.getenv().getOrDefault("HEALTH_CHECK_TIMEOUT_SEC", "30"));

    public static final int COUNT_TIMEOUT_SEC = 15;
    public static final int PARTIAL_COUNT_TIMEOUT_SEC = 15;
}