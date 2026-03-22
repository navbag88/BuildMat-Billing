package com.buildmat.dao;

import com.buildmat.model.*;
import com.buildmat.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAO {

    public List<Invoice> getAll() throws SQLException {
        List<Invoice> list = new ArrayList<>();
        String sql = """
            SELECT i.*, c.name as cname, c.phone as cphone, c.email as cemail, c.address as caddress
            FROM invoices i LEFT JOIN customers c ON i.customer_id = c.id
            ORDER BY i.invoice_date DESC, i.id DESC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapInvoice(rs));
        }
        return list;
    }

    public List<Invoice> search(String query) throws SQLException {
        List<Invoice> list = new ArrayList<>();
        String sql = """
            SELECT i.*, c.name as cname, c.phone as cphone, c.email as cemail, c.address as caddress
            FROM invoices i LEFT JOIN customers c ON i.customer_id = c.id
            WHERE i.invoice_number LIKE ? OR c.name LIKE ? OR i.status LIKE ?
            ORDER BY i.invoice_date DESC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + query + "%");
            ps.setString(2, "%" + query + "%");
            ps.setString(3, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapInvoice(rs));
            }
        }
        return list;
    }

    public Invoice getById(int id) throws SQLException {
        String sql = """
            SELECT i.*, c.name as cname, c.phone as cphone, c.email as cemail, c.address as caddress
            FROM invoices i LEFT JOIN customers c ON i.customer_id = c.id WHERE i.id=?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Invoice inv = mapInvoice(rs);
                    inv.setItems(getItems(id));
                    return inv;
                }
            }
        }
        return null;
    }

    public List<InvoiceItem> getItems(int invoiceId) throws SQLException {
        List<InvoiceItem> items = new ArrayList<>();
        String sql = "SELECT * FROM invoice_items WHERE invoice_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InvoiceItem item = new InvoiceItem();
                    item.setId(rs.getInt("id"));
                    item.setInvoiceId(rs.getInt("invoice_id"));
                    item.setProductId(rs.getObject("product_id") != null ? rs.getInt("product_id") : null);
                    item.setProductName(rs.getString("product_name"));
                    item.setUnit(rs.getString("unit"));
                    item.setQuantity(rs.getDouble("quantity"));
                    item.setUnitPrice(rs.getDouble("unit_price"));
                    item.setTotal(rs.getDouble("total"));
                    item.setSgstPercent(rs.getDouble("sgst_percent"));
                    item.setCgstPercent(rs.getDouble("cgst_percent"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    public String generateInvoiceNumber() {
        String prefix = "INV-" + LocalDate.now().getYear() + "-";
        try {
            String sql = "SELECT invoice_number FROM invoices WHERE invoice_number LIKE ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, prefix + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    int max = 0;
                    while (rs.next()) {
                        String num = rs.getString(1);
                        if (num != null && num.startsWith(prefix)) {
                            try {
                                int n = Integer.parseInt(num.substring(prefix.length()));
                                if (n > max) max = n;
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    return prefix + String.format("%04d", max + 1);
                }
            }
        } catch (Exception e) {
            return prefix + String.format("%04d", 1);
        }
    }

    public void save(Invoice invoice) throws SQLException {
        if (invoice.getId() == 0) insert(invoice);
        else update(invoice);
    }

    private void insert(Invoice inv) throws SQLException {
        String sql = """
            INSERT INTO invoices (invoice_number, customer_id, invoice_date, due_date,
            subtotal, sgst_amount, cgst_amount, tax_amount, total_amount, paid_amount,
            include_gst, status, notes)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, inv.getInvoiceNumber());
            ps.setObject(2, inv.getCustomer() != null ? inv.getCustomer().getId() : null);
            ps.setString(3, inv.getInvoiceDate().toString());
            ps.setString(4, inv.getDueDate() != null ? inv.getDueDate().toString() : null);
            ps.setDouble(5, inv.getSubtotal());
            ps.setDouble(6, inv.getSgstAmount());
            ps.setDouble(7, inv.getCgstAmount());
            ps.setDouble(8, inv.getTaxAmount());
            ps.setDouble(9, inv.getTotalAmount());
            ps.setDouble(10, inv.getPaidAmount());
            ps.setInt(11, inv.isIncludeGst() ? 1 : 0);
            ps.setString(12, inv.getStatus());
            ps.setString(13, inv.getNotes());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) inv.setId(rs.getInt(1));
            }
        }
        saveItems(inv);
    }

    private void update(Invoice inv) throws SQLException {
        String sql = """
            UPDATE invoices SET customer_id=?, invoice_date=?, due_date=?,
            subtotal=?, sgst_amount=?, cgst_amount=?, tax_amount=?, total_amount=?,
            paid_amount=?, include_gst=?, status=?, notes=?
            WHERE id=?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, inv.getCustomer() != null ? inv.getCustomer().getId() : null);
            ps.setString(2, inv.getInvoiceDate().toString());
            ps.setString(3, inv.getDueDate() != null ? inv.getDueDate().toString() : null);
            ps.setDouble(4, inv.getSubtotal());
            ps.setDouble(5, inv.getSgstAmount());
            ps.setDouble(6, inv.getCgstAmount());
            ps.setDouble(7, inv.getTaxAmount());
            ps.setDouble(8, inv.getTotalAmount());
            ps.setDouble(9, inv.getPaidAmount());
            ps.setInt(10, inv.isIncludeGst() ? 1 : 0);
            ps.setString(11, inv.getStatus());
            ps.setString(12, inv.getNotes());
            ps.setInt(13, inv.getId());
            ps.executeUpdate();
        }
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM invoice_items WHERE invoice_id=?")) {
            ps.setInt(1, inv.getId()); ps.executeUpdate();
        }
        saveItems(inv);
    }

    private void saveItems(Invoice inv) throws SQLException {
        String sql = "INSERT INTO invoice_items (invoice_id, product_id, product_name, unit, quantity, unit_price, total, sgst_percent, cgst_percent) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (InvoiceItem item : inv.getItems()) {
                ps.setInt(1, inv.getId());
                ps.setObject(2, item.getProductId());
                ps.setString(3, item.getProductName());
                ps.setString(4, item.getUnit());
                ps.setDouble(5, item.getQuantity());
                ps.setDouble(6, item.getUnitPrice());
                ps.setDouble(7, item.getTotal());
                ps.setDouble(8, item.getSgstPercent());
                ps.setDouble(9, item.getCgstPercent());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM invoices WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    public double getTotalRevenue() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(paid_amount), 0) FROM invoices");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    public double getTotalOutstanding() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(total_amount - paid_amount), 0) FROM invoices WHERE status != 'PAID'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    public int getCountByStatus(String status) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM invoices WHERE status=?")) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private Invoice mapInvoice(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setId(rs.getInt("id"));
        inv.setInvoiceNumber(rs.getString("invoice_number"));
        String invDate = rs.getString("invoice_date");
        if (invDate != null) inv.setInvoiceDate(LocalDate.parse(invDate));
        String dueDate = rs.getString("due_date");
        if (dueDate != null) inv.setDueDate(LocalDate.parse(dueDate));
        inv.setSubtotal(rs.getDouble("subtotal"));
        inv.setSgstAmount(rs.getDouble("sgst_amount"));
        inv.setCgstAmount(rs.getDouble("cgst_amount"));
        inv.setTaxAmount(rs.getDouble("tax_amount"));
        inv.setTotalAmount(rs.getDouble("total_amount"));
        inv.setPaidAmount(rs.getDouble("paid_amount"));
        inv.setIncludeGst(rs.getInt("include_gst") == 1);
        inv.setStatus(rs.getString("status"));
        inv.setNotes(rs.getString("notes"));
        String cname = rs.getString("cname");
        if (cname != null) {
            Customer c = new Customer();
            c.setId(rs.getInt("customer_id"));
            c.setName(cname);
            c.setPhone(rs.getString("cphone"));
            c.setEmail(rs.getString("cemail"));
            c.setAddress(rs.getString("caddress"));
            inv.setCustomer(c);
        }
        return inv;
    }
}
