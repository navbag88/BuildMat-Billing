package com.buildmat.dao;

import com.buildmat.model.Product;
import com.buildmat.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public List<Product> getAll() throws SQLException {
        List<Product> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM products ORDER BY category, name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Product> search(String query) throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE name LIKE ? OR category LIKE ? ORDER BY name";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + query + "%");
            ps.setString(2, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public void save(Product product) throws SQLException {
        if (product.getId() == 0) insert(product);
        else update(product);
    }

    private void insert(Product p) throws SQLException {
        String sql = "INSERT INTO products (name, category, unit, price, stock_qty, sgst_percent, cgst_percent) VALUES (?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName()); ps.setString(2, p.getCategory());
            ps.setString(3, p.getUnit()); ps.setDouble(4, p.getPrice());
            ps.setDouble(5, p.getStockQty());
            ps.setDouble(6, p.getSgstPercent()); ps.setDouble(7, p.getCgstPercent());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) p.setId(rs.getInt(1));
            }
        }
    }

    private void update(Product p) throws SQLException {
        String sql = "UPDATE products SET name=?, category=?, unit=?, price=?, stock_qty=?, sgst_percent=?, cgst_percent=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getName()); ps.setString(2, p.getCategory());
            ps.setString(3, p.getUnit()); ps.setDouble(4, p.getPrice());
            ps.setDouble(5, p.getStockQty());
            ps.setDouble(6, p.getSgstPercent()); ps.setDouble(7, p.getCgstPercent());
            ps.setInt(8, p.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    public void updateStock(int id, double qty) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE products SET stock_qty=stock_qty-? WHERE id=?")) {
            ps.setDouble(1, qty); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product(rs.getInt("id"), rs.getString("name"), rs.getString("category"),
                rs.getString("unit"), rs.getDouble("price"), rs.getDouble("stock_qty"));
        p.setSgstPercent(rs.getDouble("sgst_percent"));
        p.setCgstPercent(rs.getDouble("cgst_percent"));
        return p;
    }
}
