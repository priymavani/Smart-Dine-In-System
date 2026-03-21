package com.example.smartdinein.model;

import java.math.BigDecimal;

public class MenuItem {
    private int id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal rating;

    // Phase M1 additions
    private String imageUrl;
    private String cloudinaryId;
    private String category;
    private int prepTimeMin;
    private boolean isSpecial;
    private boolean isVisible;

    // ---- Existing fields ----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    // ---- New fields ----

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCloudinaryId() { return cloudinaryId; }
    public void setCloudinaryId(String cloudinaryId) { this.cloudinaryId = cloudinaryId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getPrepTimeMin() { return prepTimeMin; }
    public void setPrepTimeMin(int prepTimeMin) { this.prepTimeMin = prepTimeMin; }

    public boolean isSpecial() { return isSpecial; }
    public void setSpecial(boolean special) { isSpecial = special; }

    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { isVisible = visible; }
}
