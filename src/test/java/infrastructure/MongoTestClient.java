package infrastructure;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class MongoTestClient {

    private static final String CONTAINER_NAME = "kafka-compose-mongodb-1";

    public boolean isOrderSaved(String orderId) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "exec", "-i", CONTAINER_NAME,
                    "mongosh", "-u", "user", "-p", "pass", "--authenticationDatabase", "admin",
                    "orders_db", "--quiet", "--eval",
                    String.format("db.orders.findOne({orderId:'%s'}) != null", orderId)
            );
            Process process = pb.start();
            String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
            process.waitFor();
            return "true".equals(output.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}