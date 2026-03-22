package com.buildmat.dao;

import com.buildmat.model.Payment;
import com.buildmat.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    public List<Payment> getAll() throws SQLException {
        List<Payment> list = new ArrayList<>();
        String sql = """
            SELECT p.*, i.invoice_number, c.name as customer_name
            FROM payments p
            JOIN invoices i ON p.invoice_id = i.id
            LEFT JOIN customers c ON i.customer_id = c.id
            ORDER BY p.payment_date DESC, p.id DESC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Payment> getByInvoice(int invoiceId) throws SQLException {
        List<Payment> list = new ArrayList<>();
        String sql = """
            SELECT p.*, i.invoice_number, c.name as customer_name
            FROM payments p
            JOIN invoices i ON p.invoice_id = i.id
            LEFT JOIN customers c ON i.customer_id = c.id
            WHERE p.invoice_id=? ORDER BY p.payment_date DESC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public void save(Payment payment) throws SQLException {
        String sql = "INSERT INTO payments (invoice_id, amount, payment_date, method, reference, notes) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, payment.getInvoiceId());
            ps.setDouble(2, payment.getAmount());
            ps.setString(3, payment.getPaymentDate().toString());
            ps.setString(4, payment.getMethod());
            ps.setString(5, payment.getReference());
            ps.setString(6, payment.getNotes());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) payment.setId(rs.getInt(1));
            }
        }
        // Update invoice paid_amount
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE invoices SET paid_amount = paid_amount + ?, status = CASE WHEN paid_amount + ? >= total_amount THEN 'PAID' WHEN paid_amount + ? > 0 THEN 'PARTIAL' ELSE 'UNPAID' END WHERE id=?")) {
            ps.setDouble(1, payment.getAmount());
            ps.setDouble(2, payment.getAmount());
            ps.setDouble(3, payment.getAmount());
            ps.setInt(4, payment.getInvoiceId());
            ps.executeUpdate();
        }
    }

    public void delete(int id, int invoiceId, double amount) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM payments WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE invoices SET paid_amount = MAX(0, paid_amount - ?), status = CASE WHEN paid_amount - ? <= 0 THEN 'UNPAID' WHEN paid_amount - ? >= total_amount THEN 'PAID' ELSE 'PARTIAL' END WHERE id=?")) {
            ps.setDouble(1, amount); ps.setDouble(2, amount);
            ps.setDouble(3, amount); ps.setInt(4, invoiceId);
            ps.executeUpdate();
        }
    }

    private Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getInt("id"));
        p.setInvoiceId(rs.getInt("invoice_id"));
        p.setInvoiceNumber(rs.getString("invoice_number"));
        p.setCustomerName(rs.getString("customer_name"));
        p.setAmount(rs.getDouble("amount"));
        String date = rs.getString("payment_date");
        if (date != null) p.setPaymentDate(LocalDate.parse(date));
        p.setMethod(rs.getString("method"));
        p.setReference(rs.getString("reference"));
        p.setNotes(rs.getString("notes"));
        return p;
    }
}
