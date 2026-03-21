package com.example.smartdinein.model;

import java.math.BigDecimal;

public class OrderItem {
    private int menuItemId;
    private int quantity;
    private BigDecimal unitPrice;

    public OrderItem(int menuItemId, int quantity, BigDecimal unitPrice) {
        this.menuItemId = menuItemId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getMenuItemId() {
        return menuItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}

