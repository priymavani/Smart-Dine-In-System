package com.example.smartdinein.dao;

import com.example.smartdinein.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DineInSessionDAO {
    public int createSession(int customerId, int tableNumber) {
        String sql = "INSERT INTO dinein_sessions (customer_id, table_number, status) VALUES (?, ?, 'ACTIVE')";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerId);
            ps.setInt(2, tableNumber);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new RuntimeException("Session insert failed (no id).");
                return keys.getInt(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create dine-in session", e);
        }
    }

    public com.example.smartdinein.model.DineInSession findActiveSession(int customerId) {
        String sql = "SELECT * FROM dinein_sessions WHERE customer_id = ? AND status = 'ACTIVE' LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    com.example.smartdinein.model.DineInSession session = new com.example.smartdinein.model.DineInSession();
                    session.setId(rs.getInt("id"));
                    session.setCustomerId(rs.getInt("customer_id"));
                    session.setTableNumber(rs.getInt("table_number"));
                    session.setStatus(rs.getString("status"));
                    return session;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find active session", e);
        }
        return null;
    }

    public java.util.List<com.example.smartdinein.model.DineInSession> getCustomerHistory(int customerId, int daysLimit) {
        java.util.List<com.example.smartdinein.model.DineInSession> sessions = new java.util.ArrayList<>();
        String sql = "SELECT * FROM dinein_sessions WHERE customer_id = ? " +
                     "AND created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                     "ORDER BY created_at DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, daysLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.example.smartdinein.model.DineInSession session = new com.example.smartdinein.model.DineInSession();
                    session.setId(rs.getInt("id"));
                    session.setTableNumber(rs.getInt("table_number"));
                    session.setStatus(rs.getString("status"));
                    session.setDiscountAmount(rs.getBigDecimal("discount_amount"));
                    session.setFinalTotal(rs.getBigDecimal("final_total"));
                    session.setCreatedAt(rs.getTimestamp("created_at"));
                    session.setPaidAt(rs.getTimestamp("paid_at"));
                    sessions.add(session);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch customer history", e);
        }
        return sessions;
    }

    public com.example.smartdinein.model.DineInSession getSessionById(int sessionId, int customerId) {
        String sql = "SELECT * FROM dinein_sessions WHERE id = ? AND customer_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    com.example.smartdinein.model.DineInSession session = new com.example.smartdinein.model.DineInSession();
                    session.setId(rs.getInt("id"));
                    session.setTableNumber(rs.getInt("table_number"));
                    session.setStatus(rs.getString("status"));
                    session.setDiscountAmount(rs.getBigDecimal("discount_amount"));
                    session.setFinalTotal(rs.getBigDecimal("final_total"));
                    session.setCreatedAt(rs.getTimestamp("created_at"));
                    session.setPaidAt(rs.getTimestamp("paid_at"));
                    return session;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch session by id", e);
        }
        return null;
    }

    public void updateSessionStatus(int sessionId, String newStatus) {
        String sql = "UPDATE dinein_sessions SET status = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, sessionId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update session status", e);
        }
    }

    public java.util.List<com.example.smartdinein.model.DineInSession> getSessionsByStatus(String status) {
        java.util.List<com.example.smartdinein.model.DineInSession> sessions = new java.util.ArrayList<>();
        String sql = "SELECT * FROM dinein_sessions WHERE status = ? ORDER BY created_at ASC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.example.smartdinein.model.DineInSession session = new com.example.smartdinein.model.DineInSession();
                    session.setId(rs.getInt("id"));
                    session.setCustomerId(rs.getInt("customer_id"));
                    session.setTableNumber(rs.getInt("table_number"));
                    session.setStatus(rs.getString("status"));
                    session.setDiscountAmount(rs.getBigDecimal("discount_amount"));
                    session.setFinalTotal(rs.getBigDecimal("final_total"));
                    session.setCreatedAt(rs.getTimestamp("created_at"));
                    session.setPaidAt(rs.getTimestamp("paid_at"));
                    sessions.add(session);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch sessions by status", e);
        }
        return sessions;
    }

    public void applyDiscount(int sessionId, java.math.BigDecimal discountAmount, java.math.BigDecimal finalTotal) {
        String sql = "UPDATE dinein_sessions SET discount_amount = ?, final_total = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, discountAmount);
            ps.setBigDecimal(2, finalTotal);
            ps.setInt(3, sessionId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply discount", e);
        }
    }

    public void markPaid(int sessionId, java.math.BigDecimal finalTotal) {
        String sql = "UPDATE dinein_sessions SET status = 'PAID', paid_at = NOW(), final_total = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, finalTotal);
            ps.setInt(2, sessionId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to mark session as paid", e);
        }
    }
}

