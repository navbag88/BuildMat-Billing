package com.buildmat.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Invoice {
    private int id;
    private String invoiceNumber;
    private Customer customer;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private double subtotal;
    private double sgstAmount;
    private double cgstAmount;
    private double taxAmount;      // sgst + cgst combined
    private double totalAmount;
    private double paidAmount;
    private boolean includeGst;
    private String status;
    private String notes;
    private List<InvoiceItem> items = new ArrayList<>();

    public Invoice() {
        this.invoiceDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(30);
        this.includeGst = true;
        this.status = "UNPAID";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String n) { this.invoiceNumber = n; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer c) { this.customer = c; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate d) { this.invoiceDate = d; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate d) { this.dueDate = d; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double v) { this.subtotal = v; }
    public double getSgstAmount() { return sgstAmount; }
    public void setSgstAmount(double v) { this.sgstAmount = v; }
    public double getCgstAmount() { return cgstAmount; }
    public void setCgstAmount(double v) { this.cgstAmount = v; }
    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double v) { this.taxAmount = v; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double v) { this.totalAmount = v; }
    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double v) { this.paidAmount = v; }
    public boolean isIncludeGst() { return includeGst; }
    public void setIncludeGst(boolean v) { this.includeGst = v; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getNotes() { return notes; }
    public void setNotes(String n) { this.notes = n; }
    public List<InvoiceItem> getItems() { return items; }
    public void setItems(List<InvoiceItem> items) { this.items = items; }
    public double getBalanceDue() { return totalAmount - paidAmount; }

    // Kept for backward-compat with PaymentDAO
    public double getTaxPercent() {
        if (subtotal == 0) return 0;
        return (taxAmount / subtotal) * 100.0;
    }

    public void recalculate() {
        subtotal    = items.stream().mapToDouble(InvoiceItem::getTotal).sum();
        if (includeGst) {
            sgstAmount  = items.stream().mapToDouble(InvoiceItem::getSgstAmount).sum();
            cgstAmount  = items.stream().mapToDouble(InvoiceItem::getCgstAmount).sum();
            taxAmount   = sgstAmount + cgstAmount;
        } else {
            sgstAmount = 0; cgstAmount = 0; taxAmount = 0;
        }
        totalAmount = subtotal + taxAmount;
        if (paidAmount >= totalAmount) status = "PAID";
        else if (paidAmount > 0)       status = "PARTIAL";
        else                            status = "UNPAID";
    }
}
