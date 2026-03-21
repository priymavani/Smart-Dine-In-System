package com.example.smartdinein.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a promotional offer / campaign.
 *
 * Supported offer types:
 *  FLAT_OFF      – fixed rupee discount when order >= minOrderAmount
 *  PERCENTAGE    – percentage off the total bill
 *  BUY_X_GET_Y   – buy buyQty items, get getQty free
 *  FIRST_ORDER   – special discount for the customer's first order of the session
 *  COMBO         – get a free item when a specific quantity is ordered
 *  HAPPY_HOURS   – discount only between happyStartTime and happyEndTime
 */
public class Offer {

    public enum OfferType {
        FLAT_OFF, PERCENTAGE, BUY_X_GET_Y, FIRST_ORDER, COMBO, HAPPY_HOURS
    }

    private int       id;
    private String    title;
    private String    description;
    private OfferType offerType;

    /** Amount (₹) for FLAT_OFF; percentage (0-100) for PERCENTAGE, HAPPY_HOURS, FIRST_ORDER */
    private BigDecimal discountValue;

    /** Minimum cart total for FLAT_OFF to apply */
    private BigDecimal minOrderAmount;

    /** For BUY_X_GET_Y / COMBO: buy this many */
    private int buyQuantity;

    /** For BUY_X_GET_Y / COMBO: get this many free */
    private int getQuantity;

    /** For HAPPY_HOURS: window start (nullable for other types) */
    private LocalTime happyStartTime;

    /** For HAPPY_HOURS: window end (nullable for other types) */
    private LocalTime happyEndTime;

    private boolean   active;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private List<Integer> eligibleItemIds = new ArrayList<>();

    // ── Constructors ─────────────────────────────────────────────────────────

    public Offer() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int getId()                      { return id; }
    public void setId(int id)               { this.id = id; }

    public String getTitle()                { return title; }
    public void setTitle(String title)      { this.title = title; }

    public String getDescription()                   { return description; }
    public void setDescription(String description)   { this.description = description; }

    public OfferType getOfferType()                  { return offerType; }
    public void setOfferType(OfferType offerType)    { this.offerType = offerType; }

    public BigDecimal getDiscountValue()                      { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue)    { this.discountValue = discountValue; }

    public BigDecimal getMinOrderAmount()                       { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount)   { this.minOrderAmount = minOrderAmount; }

    public int getBuyQuantity()                  { return buyQuantity; }
    public void setBuyQuantity(int buyQuantity)  { this.buyQuantity = buyQuantity; }

    public int getGetQuantity()                  { return getQuantity; }
    public void setGetQuantity(int getQuantity)  { this.getQuantity = getQuantity; }

    public LocalTime getHappyStartTime()                       { return happyStartTime; }
    public void setHappyStartTime(LocalTime happyStartTime)   { this.happyStartTime = happyStartTime; }

    public LocalTime getHappyEndTime()                       { return happyEndTime; }
    public void setHappyEndTime(LocalTime happyEndTime)      { this.happyEndTime = happyEndTime; }

    public boolean isActive()                { return active; }
    public void setActive(boolean active)   { this.active = active; }

    public LocalDate getValidFrom()                  { return validFrom; }
    public void setValidFrom(LocalDate validFrom)    { this.validFrom = validFrom; }

    public LocalDate getValidUntil()                   { return validUntil; }
    public void setValidUntil(LocalDate validUntil)   { this.validUntil = validUntil; }

    public List<Integer> getEligibleItemIds() { return eligibleItemIds; }
    public void setEligibleItemIds(List<Integer> eligibleItemIds) {
        this.eligibleItemIds = (eligibleItemIds == null) ? new ArrayList<>() : new ArrayList<>(eligibleItemIds);
    }

    public String getEligibleItemIdsCsv() {
        if (eligibleItemIds == null || eligibleItemIds.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < eligibleItemIds.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(eligibleItemIds.get(i));
        }
        return sb.toString();
    }

    /** Human-readable label for the offer type. */
    public String getOfferTypeLabel() {
        if (offerType == null) return "";
        switch (offerType) {
            case FLAT_OFF:    return "Flat Off (₹)";
            case PERCENTAGE:  return "Percentage (%)";
            case BUY_X_GET_Y: return "Buy X Get Y Free";
            case FIRST_ORDER: return "First Order Discount";
            case COMBO:       return "Combo Deal";
            case HAPPY_HOURS: return "Happy Hours";
            default:          return offerType.name();
        }
    }
}
