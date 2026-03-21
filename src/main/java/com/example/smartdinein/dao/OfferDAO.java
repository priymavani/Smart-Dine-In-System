package com.example.smartdinein.dao;

import com.example.smartdinein.model.Offer;
import com.example.smartdinein.model.Offer.OfferType;
import com.example.smartdinein.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Full CRUD data-access object for the {@code offers} table.
 */
public class OfferDAO {

    // ── Read ──────────────────────────────────────────────────────────────────

    /** All offers (admin view – includes inactive). */
    public List<Offer> findAll() throws Exception {
        List<Offer> list = new ArrayList<>();
        String sql = "SELECT * FROM offers ORDER BY id DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Offer offer = map(rs);
                offer.setEligibleItemIds(loadEligibleItemIds(con, offer.getId()));
                list.add(offer);
            }
        }
        return list;
    }

    /** Active offers only (customer / billing view). */
    public List<Offer> findAllActive() throws Exception {
        List<Offer> list = new ArrayList<>();
        String sql = "SELECT * FROM offers WHERE is_active = 1 ORDER BY id DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Offer offer = map(rs);
                offer.setEligibleItemIds(loadEligibleItemIds(con, offer.getId()));
                list.add(offer);
            }
        }
        return list;
    }

    /** Active offers not already claimed by this customer. */
    public List<Offer> findAllActiveForCustomer(int customerId) throws Exception {
        List<Offer> list = new ArrayList<>();
        String sql = "SELECT o.* FROM offers o " +
                     "WHERE o.is_active = 1 " +
                     "AND NOT EXISTS (SELECT 1 FROM customer_offer_claims c WHERE c.customer_id = ? AND c.offer_id = o.id) " +
                     "ORDER BY o.id DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Offer offer = map(rs);
                    offer.setEligibleItemIds(loadEligibleItemIds(con, offer.getId()));
                    list.add(offer);
                }
            }
        }
        return list;
    }

    /** Single offer by primary key. */
    public Offer findById(int id) throws Exception {
        String sql = "SELECT * FROM offers WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Offer offer = map(rs);
                    offer.setEligibleItemIds(loadEligibleItemIds(con, offer.getId()));
                    return offer;
                }
            }
        }
        return null;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public int insert(Offer o) throws Exception {
        String sql =
            "INSERT INTO offers " +
            "(title, description, offer_type, discount_value, min_order_amount, " +
            " buy_quantity, get_quantity, happy_start_time, happy_end_time, " +
            " is_active, valid_from, valid_until) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, o);
            ps.executeUpdate();

            int offerId = 0;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    offerId = keys.getInt(1);
                }
            }
            if (offerId <= 0) {
                throw new SQLException("Failed to create offer id");
            }

            replaceEligibleItems(con, offerId, o.getEligibleItemIds());
            return offerId;
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update(Offer o) throws Exception {
        String sql =
            "UPDATE offers SET " +
            "title=?, description=?, offer_type=?, discount_value=?, min_order_amount=?, " +
            "buy_quantity=?, get_quantity=?, happy_start_time=?, happy_end_time=?, " +
            "is_active=?, valid_from=?, valid_until=? " +
            "WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, o);
            ps.setInt(13, o.getId());
            ps.executeUpdate();
            replaceEligibleItems(con, o.getId(), o.getEligibleItemIds());
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(int id) throws Exception {
        String sql = "DELETE FROM offers WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public boolean hasCustomerClaimedOffer(int customerId, int offerId) {
        String sql = "SELECT 1 FROM customer_offer_claims WHERE customer_id = ? AND offer_id = ? LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, offerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate customer offer claim", e);
        }
    }

    public boolean customerHasAnyPreviousOrder(int customerId) {
        String sql = "SELECT 1 FROM orders o JOIN dinein_sessions s ON o.session_id = s.id WHERE s.customer_id = ? LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate customer order history", e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Bind all columns (in INSERT/UPDATE order). Index 13 is omitted (used for WHERE id). */
    private void bind(PreparedStatement ps, Offer o) throws SQLException {
        ps.setString(1, o.getTitle());
        ps.setString(2, o.getDescription());
        ps.setString(3, o.getOfferType().name());
        ps.setBigDecimal(4, o.getDiscountValue());
        ps.setBigDecimal(5, o.getMinOrderAmount() != null ? o.getMinOrderAmount() : BigDecimal.ZERO);
        ps.setInt(6, o.getBuyQuantity());
        ps.setInt(7, o.getGetQuantity());

        if (o.getHappyStartTime() != null) {
            ps.setTime(8, Time.valueOf(o.getHappyStartTime()));
        } else {
            ps.setNull(8, Types.TIME);
        }
        if (o.getHappyEndTime() != null) {
            ps.setTime(9, Time.valueOf(o.getHappyEndTime()));
        } else {
            ps.setNull(9, Types.TIME);
        }

        ps.setBoolean(10, o.isActive());
        ps.setDate(11, o.getValidFrom()  != null ? Date.valueOf(o.getValidFrom())  : null);
        ps.setDate(12, o.getValidUntil() != null ? Date.valueOf(o.getValidUntil()) : null);
    }

    /** Map a ResultSet row to an Offer object. */
    private Offer map(ResultSet rs) throws SQLException {
        Offer o = new Offer();
        o.setId(rs.getInt("id"));
        o.setTitle(rs.getString("title"));
        o.setDescription(rs.getString("description"));
        o.setOfferType(OfferType.valueOf(rs.getString("offer_type")));
        o.setDiscountValue(rs.getBigDecimal("discount_value"));
        o.setMinOrderAmount(rs.getBigDecimal("min_order_amount"));
        o.setBuyQuantity(rs.getInt("buy_quantity"));
        o.setGetQuantity(rs.getInt("get_quantity"));

        Time start = rs.getTime("happy_start_time");
        if (start != null) o.setHappyStartTime(start.toLocalTime());

        Time end = rs.getTime("happy_end_time");
        if (end != null) o.setHappyEndTime(end.toLocalTime());

        o.setActive(rs.getBoolean("is_active"));

        Date from = rs.getDate("valid_from");
        if (from != null) o.setValidFrom(from.toLocalDate());

        Date until = rs.getDate("valid_until");
        if (until != null) o.setValidUntil(until.toLocalDate());

        return o;
    }

    private List<Integer> loadEligibleItemIds(Connection con, int offerId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT menu_item_id FROM offer_eligible_items WHERE offer_id = ? ORDER BY menu_item_id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, offerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    private void replaceEligibleItems(Connection con, int offerId, List<Integer> menuItemIds) throws SQLException {
        try (PreparedStatement del = con.prepareStatement("DELETE FROM offer_eligible_items WHERE offer_id = ?")) {
            del.setInt(1, offerId);
            del.executeUpdate();
        }

        if (menuItemIds == null || menuItemIds.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO offer_eligible_items (offer_id, menu_item_id) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Integer menuItemId : menuItemIds) {
                if (menuItemId == null || menuItemId <= 0) continue;
                ps.setInt(1, offerId);
                ps.setInt(2, menuItemId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
