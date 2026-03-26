package infrastructure;

import config.TestConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

public class KafkaTestClient {

    private static final Logger log = LoggerFactory.getLogger(KafkaTestClient.class);

    public long countMessages(String topic, String expectedSubstring, int timeoutSeconds) {
        try (KafkaConsumer<String, String> consumer = createConsumer()) {
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

    private KafkaConsumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, TestConfig.KAFKA_BOOTSTRAP);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }
}