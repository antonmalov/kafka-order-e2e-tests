package infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class DatabaseTestClient {

    private static final Logger log = LoggerFactory.getLogger(DatabaseTestClient.class);
    private static final String URL = "jdbc:postgresql://localhost:5432/orders_db";
    private static final String USER = "user";
    private static final String PASSWORD = "pass";

    public boolean isOrderSaved(String orderId) {
        String sql = "SELECT COUNT(*) FROM orders WHERE order_id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            log.error("Failed to check order in DB", e);
        }
        return false;
    }

    public long countOrders() {
        String sql = "SELECT COUNT(*) FROM orders";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.error("Failed to count orders", e);
        }
        return 0;
    }
}