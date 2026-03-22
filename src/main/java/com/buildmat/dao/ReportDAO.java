package com.buildmat.dao;

import com.buildmat.util.DatabaseManager;
import java.sql.*;
import java.util.*;

public class ReportDAO {

    // ── Sales Summary ──────────────────────────────────────────────────────────
    public List<Map<String,Object>> getSalesSummary(String fromDate, String toDate) throws SQLException {
        String sql = """
            SELECT
                strftime('%Y-%m', invoice_date) AS period,
                COUNT(*) AS invoice_count,
                SUM(subtotal)    AS subtotal,
                SUM(CASE WHEN include_gst=1 THEN sgst_amount ELSE 0 END) AS sgst,
                SUM(CASE WHEN include_gst=1 THEN cgst_amount ELSE 0 END) AS cgst,
                SUM(CASE WHEN include_gst=1 THEN tax_amount  ELSE 0 END) AS total_gst,
                SUM(total_amount)  AS total_amount,
                SUM(paid_amount)   AS paid_amount,
                SUM(total_amount - paid_amount) AS outstanding
            FROM invoices
            WHERE invoice_date BETWEEN ? AND ?
            GROUP BY period
            ORDER BY period
        """;
        return query(sql, fromDate, toDate);
    }

    public Map<String,Object> getSalesSummaryTotals(String fromDate, String toDate) throws SQLException {
        String sql = """
            SELECT
                COUNT(*) AS invoice_count,
                SUM(subtotal)    AS subtotal,
                SUM(CASE WHEN include_gst=1 THEN sgst_amount ELSE 0 END) AS sgst,
                SUM(CASE WHEN include_gst=1 THEN cgst_amount ELSE 0 END) AS cgst,
                SUM(CASE WHEN include_gst=1 THEN tax_amount  ELSE 0 END) AS total_gst,
                SUM(total_amount)  AS total_amount,
                SUM(paid_amount)   AS paid_amount,
                SUM(total_amount - paid_amount) AS outstanding
            FROM invoices
            WHERE invoice_date BETWEEN ? AND ?
        """;
        List<Map<String,Object>> rows = query(sql, fromDate, toDate);
        return rows.isEmpty() ? new LinkedHashMap<>() : rows.get(0);
    }

    // ── Outstanding & Overdue ──────────────────────────────────────────────────
    public List<Map<String,Object>> getOutstandingInvoices(String asOfDate) throws SQLException {
        String sql = """
            SELECT
                i.invoice_number,
                c.name            AS customer_name,
                c.phone           AS customer_phone,
                i.invoice_date,
                i.due_date,
                i.total_amount,
                i.paid_amount,
                (i.total_amount - i.paid_amount) AS balance_due,
                i.status,
                CASE
                    WHEN i.due_date < ? AND i.status != 'PAID' THEN 'OVERDUE'
                    ELSE 'CURRENT'
                END AS overdue_flag,
                CASE
                    WHEN i.due_date < ? AND i.status != 'PAID'
                    THEN CAST(julianday(?) - julianday(i.due_date) AS INTEGER)
                    ELSE 0
                END AS days_overdue
            FROM invoices i
            LEFT JOIN customers c ON i.customer_id = c.id
            WHERE i.status != 'PAID'
            ORDER BY overdue_flag DESC, days_overdue DESC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, asOfDate);
            ps.setString(2, asOfDate);
            ps.setString(3, asOfDate);
            return toList(ps.executeQuery());
        }
    }

    // ── Customer-wise Sales ────────────────────────────────────────────────────
    public List<Map<String,Object>> getCustomerSales(String fromDate, String toDate) throws SQLException {
        String sql = """
            SELECT
                c.name            AS customer_name,
                c.phone           AS customer_phone,
                COUNT(i.id)       AS invoice_count,
                SUM(i.subtotal)   AS subtotal,
                SUM(CASE WHEN i.include_gst=1 THEN i.tax_amount ELSE 0 END) AS gst_amount,
                SUM(i.total_amount) AS total_amount,
                SUM(i.paid_amount)  AS paid_amount,
                SUM(i.total_amount - i.paid_amount) AS outstanding
            FROM invoices i
            LEFT JOIN customers c ON i.customer_id = c.id
            WHERE i.invoice_date BETWEEN ? AND ?
            GROUP BY i.customer_id
            ORDER BY total_amount DESC
        """;
        return query(sql, fromDate, toDate);
    }

    // ── Product-wise Sales ─────────────────────────────────────────────────────
    public List<Map<String,Object>> getProductSales(String fromDate, String toDate) throws SQLException {
        String sql = """
            SELECT
                ii.product_name,
                p.category,
                ii.unit,
                SUM(ii.quantity)   AS total_qty,
                AVG(ii.unit_price) AS avg_price,
                SUM(ii.total)      AS total_sales,
                SUM(CASE WHEN inv.include_gst=1 THEN ii.total*(ii.sgst_percent+ii.cgst_percent)/100.0 ELSE 0 END) AS gst_collected,
                COUNT(DISTINCT inv.id) AS invoice_count
            FROM invoice_items ii
            JOIN invoices inv ON ii.invoice_id = inv.id
            LEFT JOIN products p ON ii.product_id = p.id
            WHERE inv.invoice_date BETWEEN ? AND ?
            GROUP BY ii.product_name
            ORDER BY total_sales DESC
        """;
        return query(sql, fromDate, toDate);
    }

    // ── GST Report ─────────────────────────────────────────────────────────────
    public List<Map<String,Object>> getGstReport(String fromDate, String toDate) throws SQLException {
        String sql = """
            SELECT
                strftime('%Y-%m', i.invoice_date) AS period,
                COUNT(*) AS invoice_count,
                SUM(i.subtotal)    AS taxable_value,
                SUM(i.sgst_amount) AS sgst_amount,
                SUM(i.cgst_amount) AS cgst_amount,
                SUM(i.tax_amount)  AS total_gst,
                SUM(i.total_amount) AS total_with_gst
            FROM invoices i
            WHERE i.invoice_date BETWEEN ? AND ?
              AND i.include_gst = 1
            GROUP BY period
            ORDER BY period
        """;
        return query(sql, fromDate, toDate);
    }

    public List<Map<String,Object>> getGstByHsn(String fromDate, String toDate) throws SQLException {
        String sql = """
            SELECT
                ii.product_name,
                p.category,
                SUM(ii.total)      AS taxable_value,
                ii.sgst_percent,
                ii.cgst_percent,
                SUM(ii.total * ii.sgst_percent / 100.0) AS sgst_amount,
                SUM(ii.total * ii.cgst_percent / 100.0) AS cgst_amount,
                SUM(ii.total * (ii.sgst_percent + ii.cgst_percent) / 100.0) AS total_gst
            FROM invoice_items ii
            JOIN invoices inv ON ii.invoice_id = inv.id AND inv.include_gst = 1
            LEFT JOIN products p ON ii.product_id = p.id
            WHERE inv.invoice_date BETWEEN ? AND ?
            GROUP BY ii.product_name, ii.sgst_percent, ii.cgst_percent
            ORDER BY taxable_value DESC
        """;
        return query(sql, fromDate, toDate);
    }

    // ── Payment Collection ─────────────────────────────────────────────────────
    public List<Map<String,Object>> getPaymentCollection(String fromDate, String toDate) throws SQLException {
        String sql = """
            SELECT
                p.payment_date,
                i.invoice_number,
                c.name   AS customer_name,
                p.amount,
                p.method,
                p.reference,
                p.notes
            FROM payments p
            JOIN invoices i  ON p.invoice_id = i.id
            LEFT JOIN customers c ON i.customer_id = c.id
            WHERE p.payment_date BETWEEN ? AND ?
            ORDER BY p.payment_date DESC
        """;
        return query(sql, fromDate, toDate);
    }

    public List<Map<String,Object>> getPaymentMethodSummary(String fromDate, String toDate) throws SQLException {
        String sql = """
            SELECT
                method,
                COUNT(*) AS count,
                SUM(amount) AS total_amount
            FROM payments
            WHERE payment_date BETWEEN ? AND ?
            GROUP BY method
            ORDER BY total_amount DESC
        """;
        return query(sql, fromDate, toDate);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private List<Map<String,Object>> query(String sql, String... params) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
            return toList(ps.executeQuery());
        }
    }

    private List<Map<String,Object>> toList(ResultSet rs) throws SQLException {
        List<Map<String,Object>> list = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        while (rs.next()) {
            Map<String,Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= cols; i++) row.put(meta.getColumnLabel(i), rs.getObject(i));
            list.add(row);
        }
        return list;
    }
}
