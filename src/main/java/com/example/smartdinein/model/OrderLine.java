package com.example.smartdinein.model;

import java.math.BigDecimal;

public class OrderLine {
    private final String itemName;
    private final int quantity;
    private final BigDecimal unitPrice;

    public OrderLine(String itemName, int quantity, BigDecimal unitPrice) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}

