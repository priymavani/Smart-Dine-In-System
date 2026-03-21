package com.example.smartdinein.dao;

import com.example.smartdinein.model.Customer;
import com.example.smartdinein.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class CustomerDAO {
    public Customer findByEmail(String email) {
        String sql = "SELECT id, name, email FROM customers WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Customer c = new Customer();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setEmail(rs.getString("email"));
                return c;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load customer", e);
        }
    }

    public Customer create(String name, String email) {
        String sql = "INSERT INTO customers (name, email) VALUES (?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new RuntimeException("Customer insert failed (no id).");
                int id = keys.getInt(1);
                Customer c = new Customer();
                c.setId(id);
                c.setName(name);
                c.setEmail(email);
                return c;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create customer", e);
        }
    }

    public Customer findOrCreate(String name, String email) {
        Customer existing = findByEmail(email);
        if (existing != null) return existing;
        return create(name, email);
    }
}

