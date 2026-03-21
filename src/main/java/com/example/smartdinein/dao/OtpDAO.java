package com.example.smartdinein.dao;

import com.example.smartdinein.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class OtpDAO {
    public void createOtp(String email, String otpCode, Timestamp expiresAt) {
        String sql = "INSERT INTO email_otps (email, otp_code, expires_at) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, otpCode);
            ps.setTimestamp(3, expiresAt);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save OTP", e);
        }
    }

    public boolean verifyAndUseOtp(String email, String otpCode) {
        String selectSql = ""
                + "SELECT id, expires_at, used_at, attempts "
                + "FROM email_otps "
                + "WHERE email = ? "
                + "ORDER BY created_at DESC "
                + "LIMIT 1";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            int id;
            Timestamp expiresAt;
            Timestamp usedAt;
            int attempts;

            try (PreparedStatement ps = con.prepareStatement(selectSql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                    id = rs.getInt("id");
                    expiresAt = rs.getTimestamp("expires_at");
                    usedAt = rs.getTimestamp("used_at");
                    attempts = rs.getInt("attempts");
                }
            }

            if (usedAt != null) {
                con.rollback();
                return false;
            }

            if (expiresAt == null || expiresAt.before(new Timestamp(System.currentTimeMillis()))) {
                con.rollback();
                return false;
            }

            if (attempts >= 5) {
                con.rollback();
                return false;
            }

            // Check OTP code by querying latest OTP row by id
            String checkSql = "SELECT otp_code FROM email_otps WHERE id = ?";
            String storedCode;
            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    storedCode = rs.getString("otp_code");
                }
            }

            boolean ok = otpCode != null && otpCode.equals(storedCode);

            if (ok) {
                String useSql = "UPDATE email_otps SET used_at = ? WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(useSql)) {
                    ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }
            } else {
                String incSql = "UPDATE email_otps SET attempts = attempts + 1 WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(incSql)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
            }

            con.commit();
            return ok;
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify OTP", e);
        }
    }
}

