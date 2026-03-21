package com.example.smartdinein.model;

import java.math.BigDecimal;

public class OfferValidationResult {
    private final boolean valid;
    private final BigDecimal discountAmount;
    private final String message;

    private OfferValidationResult(boolean valid, BigDecimal discountAmount, String message) {
        this.valid = valid;
        this.discountAmount = discountAmount;
        this.message = message;
    }

    public static OfferValidationResult ok(BigDecimal discountAmount) {
        return new OfferValidationResult(true, discountAmount, null);
    }

    public static OfferValidationResult fail(String message) {
        return new OfferValidationResult(false, BigDecimal.ZERO, message);
    }

    public boolean isValid() {
        return valid;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public String getMessage() {
        return message;
    }
}
