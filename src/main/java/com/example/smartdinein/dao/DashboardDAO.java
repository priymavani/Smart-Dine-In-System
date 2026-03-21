package com.example.smartdinein.dao;

import com.example.smartdinein.model.OrderLine;
import com.example.smartdinein.model.OrderSummary;
import com.example.smartdinein.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardDAO {
    public List<OrderSummary> getPreparingOrders() {
        String sql = ""
                + "SELECT "
                + "  o.id AS order_id, o.table_number, o.total_bill, o.status, o.created_at, "
                + "  mi.name AS item_name, mi.price AS item_price, od.quantity "
                + "FROM orders o "
                + "JOIN order_details od ON od.order_id = o.id "
                + "JOIN menu_items mi ON mi.id = od.menu_item_id "
                + "WHERE o.status = 'Preparing' "
                + "ORDER BY o.created_at DESC, o.id DESC, od.id ASC";

        Map<Integer, OrderSummary> byOrderId = new LinkedHashMap<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int orderId = rs.getInt("order_id");
                OrderSummary order = byOrderId.get(orderId);
                if (order == null) {
                    order = new OrderSummary(
                            orderId,
                            rs.getInt("table_number"),
                            rs.getBigDecimal("total_bill"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at")
                    );
                    byOrderId.put(orderId, order);
                }

                order.getLines().add(new OrderLine(
                        rs.getString("item_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("item_price")
                ));
            }

            return new ArrayList<>(byOrderId.values());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load preparing orders", e);
        }
    }

    public void markOrderServed(int orderId) {
        String sql = "UPDATE orders SET status = 'Served' WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update order status", e);
        }
    }
}

