package com.example.smartdinein.dao;

import com.example.smartdinein.model.MenuItem;
import com.example.smartdinein.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FeedbackDAO {

    public FeedbackDAO() {
        createFeedbackTableIfNotExists();
    }

    private void createFeedbackTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS feedback (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "session_id INT NOT NULL, " +
                "menu_item_id INT, " +
                "rating INT, " +
                "comment TEXT, " +
                "skipped_flag BOOLEAN DEFAULT FALSE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT fk_feedback_session FOREIGN KEY (session_id) REFERENCES dinein_sessions(id) ON DELETE CASCADE, " +
                "CONSTRAINT fk_feedback_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE" +
                ")";
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create feedback table", e);
        }
    }

    public List<MenuItem> getEligibleItemsForFeedback(int sessionId) {
        List<MenuItem> items = new ArrayList<>();
        String sql = "SELECT DISTINCT m.id, m.name, m.price, m.description " +
                     "FROM menu_items m " +
                     "JOIN order_details od ON m.id = od.menu_item_id " +
                     "JOIN orders o ON od.order_id = o.id " +
                     "WHERE o.session_id = ?";
                     
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MenuItem item = new MenuItem();
                    item.setId(rs.getInt("id"));
                    item.setName(rs.getString("name"));
                    item.setPrice(rs.getBigDecimal("price"));
                    item.setDescription(rs.getString("description"));
                    items.add(item);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch eligible items for feedback", e);
        }
        return items;
    }

    public boolean hasProvidedFeedbackOrSkipped(int sessionId) {
        String sql = "SELECT COUNT(*) FROM feedback WHERE session_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to check existing feedback", e);
        }
        return false;
    }

    public void saveFeedback(int sessionId, int menuItemId, int rating, String comment) {
        String sql = "INSERT INTO feedback (session_id, menu_item_id, rating, comment, skipped_flag) VALUES (?, ?, ?, ?, FALSE)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, menuItemId);
            ps.setInt(3, rating);
            ps.setString(4, comment);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save feedback", e);
        }
    }

    public void saveSkippedFeedback(int sessionId) {
        String sql = "INSERT INTO feedback (session_id, skipped_flag) VALUES (?, TRUE)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save skipped feedback", e);
        }
    }
}
