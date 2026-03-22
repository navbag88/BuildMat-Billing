package com.buildmat.model;

public class Product {
    private int id;
    private String name;
    private String category;
    private String unit;
    private double price;
    private double stockQty;
    private double sgstPercent;
    private double cgstPercent;

    public Product() {}

    public Product(int id, String name, String category, String unit, double price, double stockQty) {
        this.id = id; this.name = name; this.category = category;
        this.unit = unit; this.price = price; this.stockQty = stockQty;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getStockQty() { return stockQty; }
    public void setStockQty(double stockQty) { this.stockQty = stockQty; }
    public double getSgstPercent() { return sgstPercent; }
    public void setSgstPercent(double sgstPercent) { this.sgstPercent = sgstPercent; }
    public double getCgstPercent() { return cgstPercent; }
    public void setCgstPercent(double cgstPercent) { this.cgstPercent = cgstPercent; }
    public double getTotalGstPercent() { return sgstPercent + cgstPercent; }

    @Override public String toString() { return name + " (" + unit + ")"; }
}
