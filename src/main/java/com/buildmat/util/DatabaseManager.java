package com.buildmat.util;

import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL  = "jdbc:mysql://caboose.proxy.rlwy.net:16788/railway"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "DvDGiSAweBNIPTSDjNebiaSxwYEuRBgQ";

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        }
        return connection;
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // ── Users ─────────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id            INT PRIMARY KEY AUTO_INCREMENT,
                    username      VARCHAR(100) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    full_name     VARCHAR(255),
                    role          VARCHAR(50)  DEFAULT 'USER',
                    active        TINYINT(1)   DEFAULT 1,
                    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Seed default admin if no users exist
            ResultSet userCount = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (userCount.next() && userCount.getInt(1) == 0) {
                String stored = PasswordUtil.createStoredPassword("admin123");
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users (username, password_hash, full_name, role, active) VALUES (?,?,?,?,?)")) {
                    ps.setString(1, "admin");
                    ps.setString(2, stored);
                    ps.setString(3, "Administrator");
                    ps.setString(4, "ADMIN");
                    ps.setInt(5, 1);
                    ps.executeUpdate();
                }
                System.out.println("Default admin created: username=admin, password=admin123");
            }

            // ── Customers ─────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    id         INT PRIMARY KEY AUTO_INCREMENT,
                    name       VARCHAR(255) NOT NULL,
                    phone      VARCHAR(50),
                    email      VARCHAR(255),
                    address    TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // ── Products ──────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id           INT PRIMARY KEY AUTO_INCREMENT,
                    name         VARCHAR(255) NOT NULL,
                    category     VARCHAR(100),
                    unit         VARCHAR(50)  NOT NULL,
                    price        DOUBLE       NOT NULL,
                    stock_qty    DOUBLE       DEFAULT 0,
                    sgst_percent DOUBLE       DEFAULT 0,
                    cgst_percent DOUBLE       DEFAULT 0,
                    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
                )
            """);
            try { stmt.execute("ALTER TABLE products ADD COLUMN sgst_percent DOUBLE DEFAULT 0"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN cgst_percent DOUBLE DEFAULT 0"); } catch (Exception ignored) {}

            // ── Invoices ──────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS invoices (
                    id             INT PRIMARY KEY AUTO_INCREMENT,
                    invoice_number VARCHAR(100) UNIQUE NOT NULL,
                    customer_id    INT,
                    invoice_date   DATE         NOT NULL,
                    due_date       DATE,
                    subtotal       DOUBLE       DEFAULT 0,
                    sgst_amount    DOUBLE       DEFAULT 0,
                    cgst_amount    DOUBLE       DEFAULT 0,
                    tax_amount     DOUBLE       DEFAULT 0,
                    total_amount   DOUBLE       DEFAULT 0,
                    paid_amount    DOUBLE       DEFAULT 0,
                    include_gst    TINYINT(1)   DEFAULT 1,
                    status         VARCHAR(50)  DEFAULT 'UNPAID',
                    notes          TEXT,
                    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (customer_id) REFERENCES customers(id)
                )
            """);
            try { stmt.execute("ALTER TABLE invoices ADD COLUMN sgst_amount DOUBLE DEFAULT 0"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE invoices ADD COLUMN cgst_amount DOUBLE DEFAULT 0"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE invoices ADD COLUMN include_gst TINYINT(1) DEFAULT 1"); } catch (Exception ignored) {}

            // ── Invoice Items ──────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS invoice_items (
                    id           INT PRIMARY KEY AUTO_INCREMENT,
                    invoice_id   INT          NOT NULL,
                    product_id   INT,
                    product_name VARCHAR(255) NOT NULL,
                    unit         VARCHAR(50),
                    quantity     DOUBLE       NOT NULL,
                    unit_price   DOUBLE       NOT NULL,
                    total        DOUBLE       NOT NULL,
                    sgst_percent DOUBLE       DEFAULT 0,
                    cgst_percent DOUBLE       DEFAULT 0,
                    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
                    FOREIGN KEY (product_id) REFERENCES products(id)
                )
            """);
            try { stmt.execute("ALTER TABLE invoice_items ADD COLUMN sgst_percent DOUBLE DEFAULT 0"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE invoice_items ADD COLUMN cgst_percent DOUBLE DEFAULT 0"); } catch (Exception ignored) {}

            // ── Payments ──────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payments (
                    id           INT PRIMARY KEY AUTO_INCREMENT,
                    invoice_id   INT         NOT NULL,
                    amount       DOUBLE      NOT NULL,
                    payment_date DATE        NOT NULL,
                    method       VARCHAR(50) DEFAULT 'CASH',
                    reference    VARCHAR(255),
                    notes        TEXT,
                    created_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (invoice_id) REFERENCES invoices(id)
                )
            """);

            // ── Sample products ────────────────────────────────────
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("""
                    INSERT INTO products (name, category, unit, price, stock_qty, sgst_percent, cgst_percent) VALUES
                    ('OPC Cement 53 Grade',   'Cement',    'Bag',    380.00, 500,   9, 9),
                    ('TMT Steel Bar 10mm',    'Steel',     'Kg',      68.00, 2000,  9, 9),
                    ('TMT Steel Bar 12mm',    'Steel',     'Kg',      68.00, 1500,  9, 9),
                    ('Red Bricks',            'Bricks',    'Piece',    8.50, 10000, 6, 6),
                    ('River Sand',            'Sand',      'CFT',     45.00, 800,   6, 6),
                    ('M-Sand',                'Sand',      'CFT',     38.00, 600,   6, 6),
                    ('20mm Aggregate',        'Aggregate', 'CFT',     42.00, 700,   6, 6),
                    ('AAC Block 600x200x150', 'Blocks',    'Piece',   55.00, 2000,  9, 9),
                    ('Ceramic Floor Tile 2x2','Tiles',     'Sq.Ft',   35.00, 1200,  9, 9),
                    ('PVC Pipe 4 inch',       'Plumbing',  'Piece',  320.00, 150,   9, 9)
                """);
            }

            System.out.println("Database initialized successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }
    }
}
