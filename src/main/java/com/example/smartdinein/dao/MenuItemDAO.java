package com.example.smartdinein.dao;

import com.example.smartdinein.model.MenuItem;
import com.example.smartdinein.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MenuItemDAO {

    private static final String COLS =
            "id, name, description, price, rating, image_url, cloudinary_id, " +
            "category, prep_time_min, is_special, is_visible";

    // ───────── Map a ResultSet row to a MenuItem ─────────
    private MenuItem map(ResultSet rs) throws SQLException {
        MenuItem item = new MenuItem();
        item.setId(rs.getInt("id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setPrice(rs.getBigDecimal("price"));
        item.setRating(rs.getBigDecimal("rating"));
        item.setImageUrl(rs.getString("image_url"));
        item.setCloudinaryId(rs.getString("cloudinary_id"));
        item.setCategory(rs.getString("category"));
        item.setPrepTimeMin(rs.getInt("prep_time_min"));
        item.setSpecial(rs.getBoolean("is_special"));
        item.setVisible(rs.getBoolean("is_visible"));
        return item;
    }

    // ───────── Find all VISIBLE items (for customers) ─────────
    public List<MenuItem> findAll() {
        String sql = "SELECT " + COLS + " FROM menu_items WHERE is_visible = 1 ORDER BY category, name";
        List<MenuItem> items = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) items.add(map(rs));
            return items;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load menu items", e);
        }
    }

    // ───────── Find ALL items including hidden (for admin) ─────────
    public List<MenuItem> findAllForAdmin() {
        String sql = "SELECT " + COLS + " FROM menu_items ORDER BY category, name";
        List<MenuItem> items = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) items.add(map(rs));
            return items;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load all menu items", e);
        }
    }

    // ───────── Find by ID ─────────
    public MenuItem findById(int id) {
        String sql = "SELECT " + COLS + " FROM menu_items WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find menu item by id: " + id, e);
        }
    }

    // ───────── Insert new item ─────────
    public void insert(MenuItem item) {
        String sql = "INSERT INTO menu_items " +
                "(name, description, price, category, prep_time_min, is_special, is_visible, image_url, cloudinary_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setBigDecimal(3, item.getPrice());
            ps.setString(4, item.getCategory());
            ps.setInt(5, item.getPrepTimeMin());
            ps.setBoolean(6, item.isSpecial());
            ps.setBoolean(7, item.isVisible());
            ps.setString(8, item.getImageUrl());
            ps.setString(9, item.getCloudinaryId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert menu item", e);
        }
    }

    // ───────── Update existing item ─────────
    public void update(MenuItem item) {
        // If a new image was uploaded, also update image columns; otherwise leave them
        boolean hasNewImage = item.getImageUrl() != null && !item.getImageUrl().isBlank();

        String sql = hasNewImage
                ? "UPDATE menu_items SET name=?, description=?, price=?, category=?, " +
                  "prep_time_min=?, is_special=?, is_visible=?, image_url=?, cloudinary_id=? WHERE id=?"
                : "UPDATE menu_items SET name=?, description=?, price=?, category=?, " +
                  "prep_time_min=?, is_special=?, is_visible=? WHERE id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setBigDecimal(3, item.getPrice());
            ps.setString(4, item.getCategory());
            ps.setInt(5, item.getPrepTimeMin());
            ps.setBoolean(6, item.isSpecial());
            ps.setBoolean(7, item.isVisible());
            if (hasNewImage) {
                ps.setString(8, item.getImageUrl());
                ps.setString(9, item.getCloudinaryId());
                ps.setInt(10, item.getId());
            } else {
                ps.setInt(8, item.getId());
            }
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update menu item id=" + item.getId(), e);
        }
    }

    // ───────── Delete item ─────────
    public void delete(int id) {
        String sql = "DELETE FROM menu_items WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete menu item id=" + id, e);
        }
    }

    // ───────── Update rating (existing) ─────────
    public void updateRating(int menuItemId, int rating) {
        String sql = "UPDATE menu_items SET rating = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, java.math.BigDecimal.valueOf(rating));
            ps.setInt(2, menuItemId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update rating for item id=" + menuItemId, e);
        }
    }
}
