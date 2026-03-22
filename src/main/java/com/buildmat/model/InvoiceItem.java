package com.buildmat.model;

public class InvoiceItem {
    private int id;
    private int invoiceId;
    private Integer productId;
    private String productName;
    private String unit;
    private double quantity;
    private double unitPrice;
    private double total;
    private double sgstPercent;
    private double cgstPercent;
    // Computed amounts (not stored separately, derived from total + percents)
    private double sgstAmount;
    private double cgstAmount;

    public InvoiceItem() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getInvoiceId() { return invoiceId; }
    public void setInvoiceId(int invoiceId) { this.invoiceId = invoiceId; }
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; recalculate(); }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; recalculate(); }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public double getSgstPercent() { return sgstPercent; }
    public void setSgstPercent(double sgstPercent) { this.sgstPercent = sgstPercent; recalculate(); }
    public double getCgstPercent() { return cgstPercent; }
    public void setCgstPercent(double cgstPercent) { this.cgstPercent = cgstPercent; recalculate(); }
    public double getSgstAmount() { return sgstAmount; }
    public double getCgstAmount() { return cgstAmount; }
    public double getTotalGstPercent() { return sgstPercent + cgstPercent; }
    public double getTotalGstAmount() { return sgstAmount + cgstAmount; }

    private void recalculate() {
        this.total = this.quantity * this.unitPrice;
        this.sgstAmount = this.total * this.sgstPercent / 100.0;
        this.cgstAmount = this.total * this.cgstPercent / 100.0;
    }
}
