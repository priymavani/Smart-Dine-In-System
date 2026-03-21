package com.example.smartdinein.dao;

import com.example.smartdinein.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportsDAO {

    public static class TopItem {
        public String name;
        public int totalQuantity;
        public BigDecimal revenue;

        public TopItem(String name, int totalQuantity, BigDecimal revenue) {
            this.name = name;
            this.totalQuantity = totalQuantity;
            this.revenue = revenue;
        }
    }

    public Map<String, BigDecimal> getDashboardMetrics(int daysLimit) {
        Map<String, BigDecimal> metrics = new HashMap<>();
        metrics.put("totalRevenue", BigDecimal.ZERO);
        metrics.put("totalDiscounts", BigDecimal.ZERO);
        metrics.put("totalSessions", BigDecimal.ZERO);

        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(id) AS total_sessions, " +
                "SUM(final_total) AS total_revenue, " +
                "SUM(discount_amount) AS total_discounts " +
                "FROM dinein_sessions WHERE status = 'PAID'");

        if (daysLimit > 0) {
            sql.append(" AND paid_at >= DATE_SUB(NOW(), INTERVAL ? DAY)");
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
             
            if (daysLimit > 0) {
                ps.setInt(1, daysLimit);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    metrics.put("totalSessions", new BigDecimal(rs.getInt("total_sessions")));
                    metrics.put("totalRevenue", rs.getBigDecimal("total_revenue") != null ? rs.getBigDecimal("total_revenue") : BigDecimal.ZERO);
                    metrics.put("totalDiscounts", rs.getBigDecimal("total_discounts") != null ? rs.getBigDecimal("total_discounts") : BigDecimal.ZERO);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch dashboard metrics", e);
        }

        return metrics;
    }

    public List<TopItem> getTopSellingItems(int daysLimit) {
        List<TopItem> items = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT m.name, SUM(od.quantity) as total_qty, SUM(od.quantity * m.price) as item_revenue " +
                "FROM order_details od " +
                "JOIN menu_items m ON od.menu_item_id = m.id " +
                "JOIN orders o ON od.order_id = o.id " +
                "JOIN dinein_sessions s ON o.session_id = s.id " +
                "WHERE s.status = 'PAID'");

        if (daysLimit > 0) {
            sql.append(" AND s.paid_at >= DATE_SUB(NOW(), INTERVAL ? DAY)");
        }
        
        sql.append(" GROUP BY m.id, m.name ORDER BY total_qty DESC LIMIT 10");

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
             
            if (daysLimit > 0) {
                ps.setInt(1, daysLimit);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new TopItem(
                            rs.getString("name"),
                            rs.getInt("total_qty"),
                            rs.getBigDecimal("item_revenue")
                    ));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch top selling items", e);
        }

        return items;
    }
    public static class OfferAnalytics {
        public String title;
        public String type;
        public int timesUsed;
        public BigDecimal totalDiscountGiven;

        public OfferAnalytics(String title, String type, int timesUsed, BigDecimal totalDiscountGiven) {
            this.title = title;
            this.type = type;
            this.timesUsed = timesUsed;
            this.totalDiscountGiven = totalDiscountGiven;
        }
    }

    public List<OfferAnalytics> getOfferAnalytics(int daysLimit) {
        List<OfferAnalytics> analytics = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT off.title, off.offer_type, COUNT(o.id) as times_used, SUM(o.discount_amount) as total_discount " +
                "FROM offers off " +
                "JOIN orders o ON off.id = o.applied_offer_id " +
                "JOIN dinein_sessions s ON o.session_id = s.id " +
                "WHERE s.status = 'PAID' AND o.discount_amount > 0");

        if (daysLimit > 0) {
            sql.append(" AND s.paid_at >= DATE_SUB(NOW(), INTERVAL ? DAY)");
        }
        
        sql.append(" GROUP BY off.id, off.title, off.offer_type ORDER BY times_used DESC");

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
             
            if (daysLimit > 0) {
                ps.setInt(1, daysLimit);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    analytics.add(new OfferAnalytics(
                            rs.getString("title"),
                            rs.getString("offer_type"),
                            rs.getInt("times_used"),
                            rs.getBigDecimal("total_discount")
                    ));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch offer analytics", e);
        }

        return analytics;
    }
}
