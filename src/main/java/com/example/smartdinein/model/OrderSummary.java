package com.example.smartdinein.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class OrderSummary {
    private final int orderId;
    private final int tableNumber;
    private final BigDecimal totalBill;
    private final String status;
    private final Timestamp createdAt;
    private final List<OrderLine> lines = new ArrayList<>();

    public OrderSummary(int orderId, int tableNumber, BigDecimal totalBill, String status, Timestamp createdAt) {
        this.orderId = orderId;
        this.tableNumber = tableNumber;
        this.totalBill = totalBill;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public BigDecimal getTotalBill() {
        return totalBill;
    }

    public String getStatus() {
        return status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public List<OrderLine> getLines() {
        return lines;
    }
}

