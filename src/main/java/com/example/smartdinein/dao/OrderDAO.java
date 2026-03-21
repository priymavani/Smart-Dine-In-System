package com.example.smartdinein.dao;

import com.example.smartdinein.model.OrderItem;
import com.example.smartdinein.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

public class OrderDAO {
    // Overloaded for backwards compatibility during transition
    public int placeOrder(int tableNumber, Integer sessionId, List<OrderItem> orderItems) {
        return placeOrder(tableNumber, sessionId, null, orderItems, null, BigDecimal.ZERO);
    }

    public int placeOrder(int tableNumber, Integer sessionId, List<OrderItem> orderItems, Integer appliedOfferId, BigDecimal discountAmount) {
        return placeOrder(tableNumber, sessionId, null, orderItems, appliedOfferId, discountAmount);
    }

    public int placeOrder(int tableNumber,
                          Integer sessionId,
                          Integer customerId,
                          List<OrderItem> orderItems,
                          Integer appliedOfferId,
                          BigDecimal discountAmount) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("Please select at least 1 item.");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : orderItems) {
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(lineTotal);
        }
        
        // Subtract discount from total
        if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            total = total.subtract(discountAmount);
            if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;
        }

        String insertOrderSql = "INSERT INTO orders (table_number, session_id, total_bill, status, applied_offer_id, discount_amount) VALUES (?, ?, ?, 'Preparing', ?, ?)";
        String insertDetailSql = "INSERT INTO order_details (order_id, menu_item_id, quantity) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            int orderId;
            try (PreparedStatement ps = con.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, tableNumber);
                if (sessionId != null) {
                    ps.setInt(2, sessionId);
                } else {
                    ps.setNull(2, java.sql.Types.INTEGER);
                }
                ps.setBigDecimal(3, total);
                
                if (appliedOfferId != null && appliedOfferId > 0) {
                    ps.setInt(4, appliedOfferId);
                } else {
                    ps.setNull(4, java.sql.Types.INTEGER);
                }
                
                if (discountAmount != null) {
                    ps.setBigDecimal(5, discountAmount);
                } else {
                    ps.setBigDecimal(5, BigDecimal.ZERO);
                }
                
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new RuntimeException("Failed to create order (no generated id).");
                    }
                    orderId = keys.getInt(1);
                }
            }

            try (PreparedStatement ps = con.prepareStatement(insertDetailSql)) {
                for (OrderItem item : orderItems) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, item.getMenuItemId());
                    ps.setInt(3, item.getQuantity());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            if (customerId != null && appliedOfferId != null && appliedOfferId > 0
                    && discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                String claimSql = "INSERT INTO customer_offer_claims (customer_id, offer_id, order_id, claimed_discount) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(claimSql)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, appliedOfferId);
                    ps.setInt(3, orderId);
                    ps.setBigDecimal(4, discountAmount);
                    ps.executeUpdate();
                }
            }

            con.commit();
            return orderId;
        } catch (Exception e) {
            throw new RuntimeException("Failed to place order", e);
        }
    }

    public BigDecimal getSessionSubtotal(int sessionId) {
        String sql = "SELECT SUM(total_bill) FROM orders WHERE session_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal sum = rs.getBigDecimal(1);
                    return sum != null ? sum : BigDecimal.ZERO;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get session subtotal", e);
        }
        return BigDecimal.ZERO;
    }

    public List<com.example.smartdinein.model.OrderSummary> getOrdersBySession(int sessionId) {
        List<com.example.smartdinein.model.OrderSummary> orders = new java.util.ArrayList<>();
        String sql = "SELECT o.id, o.table_number, o.total_bill, o.status, o.created_at, " +
                     "od.quantity, m.name, m.price " +
                     "FROM orders o " +
                     "LEFT JOIN order_details od ON o.id = od.order_id " +
                     "LEFT JOIN menu_items m ON od.menu_item_id = m.id " +
                     "WHERE o.session_id = ? " +
                     "ORDER BY o.created_at DESC, o.id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                com.example.smartdinein.model.OrderSummary currentOrder = null;
                while (rs.next()) {
                    int orderId = rs.getInt("id");
                    if (currentOrder == null || currentOrder.getOrderId() != orderId) {
                        currentOrder = new com.example.smartdinein.model.OrderSummary(
                                orderId,
                                rs.getInt("table_number"),
                                rs.getBigDecimal("total_bill"),
                                rs.getString("status"),
                                rs.getTimestamp("created_at")
                        );
                        orders.add(currentOrder);
                    }
                    String itemName = rs.getString("name");
                    if (itemName != null) {
                        int qty = rs.getInt("quantity");
                        BigDecimal price = rs.getBigDecimal("price");
                        currentOrder.getLines().add(new com.example.smartdinein.model.OrderLine(itemName, qty, price));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch session orders", e);
        }
        return orders;
    }
}

