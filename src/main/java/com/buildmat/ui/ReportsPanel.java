package com.buildmat.ui;

import com.buildmat.dao.ReportDAO;
import com.buildmat.util.ExcelReportGenerator;
import com.buildmat.util.PdfReportGenerator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReportsPanel {

    private final ReportDAO reportDAO = new ReportDAO();
    private VBox view;
    private DatePicker fromPicker, toPicker;
    private ComboBox<String> reportSelector;
    private VBox previewArea;
    private Label statusLabel;

    public ReportsPanel() { buildView(); }
    public void refresh() {}

    private void buildView() {
        view = new VBox(16);
        view.setPadding(new Insets(0));

        // ── Header ────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("MIS Reports");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#1A2332"));
        header.getChildren().add(title);
        view.getChildren().add(header);

        // ── Controls card ─────────────────────────────────────────
        VBox controls = new VBox(14);
        controls.setPadding(new Insets(20));
        controls.setStyle("-fx-background-color:white;-fx-background-radius:12;-fx-border-color:#E5E7EB;-fx-border-radius:12;-fx-border-width:1;");

        Label ctrlTitle = new Label("Select Report & Date Range");
        ctrlTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        ctrlTitle.setTextFill(Color.web("#374151"));
        controls.getChildren().add(ctrlTitle);

        // Report selector
        reportSelector = new ComboBox<>();
        reportSelector.getItems().addAll(
            "📈  Sales Summary",
            "⚠️  Outstanding & Overdue Invoices",
            "👥  Customer-wise Sales",
            "📦  Product-wise Sales",
            "🧾  GST Report (SGST / CGST)",
            "💰  Payment Collection"
        );
        reportSelector.setValue("📈  Sales Summary");
        reportSelector.setPrefWidth(320);
        reportSelector.setStyle("-fx-font-size:13;");

        // Date range
        fromPicker = new DatePicker(LocalDate.now().withDayOfMonth(1));
        toPicker   = new DatePicker(LocalDate.now());
        fromPicker.setPrefWidth(150); toPicker.setPrefWidth(150);

        // Quick range buttons
        Button thisMonth = quickBtn("This Month");
        Button lastMonth = quickBtn("Last Month");
        Button thisYear  = quickBtn("This Year");
        Button lastYear  = quickBtn("Last Year");

        thisMonth.setOnAction(e -> {
            fromPicker.setValue(LocalDate.now().withDayOfMonth(1));
            toPicker.setValue(LocalDate.now());
        });
        lastMonth.setOnAction(e -> {
            LocalDate first = LocalDate.now().minusMonths(1).withDayOfMonth(1);
            fromPicker.setValue(first);
            toPicker.setValue(first.withDayOfMonth(first.lengthOfMonth()));
        });
        thisYear.setOnAction(e -> {
            fromPicker.setValue(LocalDate.now().withDayOfYear(1));
            toPicker.setValue(LocalDate.now());
        });
        lastYear.setOnAction(e -> {
            int yr = LocalDate.now().getYear() - 1;
            fromPicker.setValue(LocalDate.of(yr, 1, 1));
            toPicker.setValue(LocalDate.of(yr, 12, 31));
        });

        HBox quickBtns = new HBox(8, thisMonth, lastMonth, thisYear, lastYear);

        HBox dateRow = new HBox(12);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        dateRow.getChildren().addAll(
            new Label("Report:"), reportSelector,
            new Label("From:"), fromPicker,
            new Label("To:"), toPicker
        );

        // Action buttons
        Button viewBtn    = actionBtn("👁  View Report",    "#2563EB");
        Button pdfBtn     = actionBtn("⬇  Download PDF",   "#7C3AED");
        Button excelBtn   = actionBtn("📊  Download Excel", "#16A34A");

        statusLabel = new Label("");
        statusLabel.setFont(Font.font("Arial", 12));
        statusLabel.setTextFill(Color.web("#2563EB"));

        HBox actionRow = new HBox(10, viewBtn, pdfBtn, excelBtn, statusLabel);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        viewBtn.setOnAction(e  -> runReport(false, false));
        pdfBtn.setOnAction(e   -> runReport(true, false));
        excelBtn.setOnAction(e -> runReport(false, true));

        controls.getChildren().addAll(dateRow, quickBtns, actionRow);
        view.getChildren().add(controls);

        // ── Preview area ──────────────────────────────────────────
        previewArea = new VBox(12);
        previewArea.setPadding(new Insets(20));
        previewArea.setStyle("-fx-background-color:white;-fx-background-radius:12;-fx-border-color:#E5E7EB;-fx-border-radius:12;-fx-border-width:1;");
        VBox.setVgrow(previewArea, Priority.ALWAYS);

        Label placeholder = new Label("Select a report and click  👁 View Report  to preview data here.");
        placeholder.setFont(Font.font("Arial", 14));
        placeholder.setTextFill(Color.web("#9CA3AF"));
        previewArea.getChildren().add(placeholder);

        view.getChildren().add(previewArea);
    }

    private void runReport(boolean exportPdf, boolean exportExcel) {
        String report = reportSelector.getValue();
        String from = fromPicker.getValue().toString();
        String to   = toPicker.getValue().toString();

        statusLabel.setText("Loading...");
        statusLabel.setTextFill(Color.web("#2563EB"));
        previewArea.getChildren().clear();

        try {
            if (report.contains("Sales Summary")) {
                List<Map<String,Object>> rows = reportDAO.getSalesSummary(from, to);
                Map<String,Object> totals = reportDAO.getSalesSummaryTotals(from, to);
                if (!exportPdf && !exportExcel) { showSalesSummary(rows, totals, from, to); statusLabel.setText(""); }
                else if (exportPdf)   { String path = pickDir("Save Sales Summary PDF");   if (path != null) { PdfReportGenerator.salesSummary(rows, totals, from, to, path);   openFile(path, "sales_summary_"+from+"_"+to+".pdf");  } }
                else                  { String path = pickDir("Save Sales Summary Excel"); if (path != null) { ExcelReportGenerator.salesSummary(rows, totals, from, to, path);   openFile(path, "sales_summary_"+from+"_"+to+".xlsx"); } }
            } else if (report.contains("Outstanding")) {
                List<Map<String,Object>> rows = reportDAO.getOutstandingInvoices(to);
                if (!exportPdf && !exportExcel) { showOutstanding(rows, to); statusLabel.setText(""); }
                else if (exportPdf)   { String path = pickDir("Save Outstanding PDF");   if (path != null) { PdfReportGenerator.outstandingInvoices(rows, to, path);   openFile(path, "outstanding_"+to+".pdf");  } }
                else                  { String path = pickDir("Save Outstanding Excel"); if (path != null) { ExcelReportGenerator.outstandingInvoices(rows, to, path);   openFile(path, "outstanding_"+to+".xlsx"); } }
            } else if (report.contains("Customer")) {
                List<Map<String,Object>> rows = reportDAO.getCustomerSales(from, to);
                if (!exportPdf && !exportExcel) { showCustomerSales(rows, from, to); statusLabel.setText(""); }
                else if (exportPdf)   { String path = pickDir("Save Customer Sales PDF");   if (path != null) { PdfReportGenerator.customerSales(rows, from, to, path);   openFile(path, "customer_sales_"+from+"_"+to+".pdf");  } }
                else                  { String path = pickDir("Save Customer Sales Excel"); if (path != null) { ExcelReportGenerator.customerSales(rows, from, to, path);   openFile(path, "customer_sales_"+from+"_"+to+".xlsx"); } }
            } else if (report.contains("Product")) {
                List<Map<String,Object>> rows = reportDAO.getProductSales(from, to);
                if (!exportPdf && !exportExcel) { showProductSales(rows, from, to); statusLabel.setText(""); }
                else if (exportPdf)   { String path = pickDir("Save Product Sales PDF");   if (path != null) { PdfReportGenerator.productSales(rows, from, to, path);   openFile(path, "product_sales_"+from+"_"+to+".pdf");  } }
                else                  { String path = pickDir("Save Product Sales Excel"); if (path != null) { ExcelReportGenerator.productSales(rows, from, to, path);   openFile(path, "product_sales_"+from+"_"+to+".xlsx"); } }
            } else if (report.contains("GST")) {
                List<Map<String,Object>> monthly   = reportDAO.getGstReport(from, to);
                List<Map<String,Object>> byProduct = reportDAO.getGstByHsn(from, to);
                if (!exportPdf && !exportExcel) { showGstReport(monthly, byProduct, from, to); statusLabel.setText(""); }
                else if (exportPdf)   { String path = pickDir("Save GST Report PDF");   if (path != null) { PdfReportGenerator.gstReport(monthly, byProduct, from, to, path);   openFile(path, "gst_report_"+from+"_"+to+".pdf");  } }
                else                  { String path = pickDir("Save GST Report Excel"); if (path != null) { ExcelReportGenerator.gstReport(monthly, byProduct, from, to, path);   openFile(path, "gst_report_"+from+"_"+to+".xlsx"); } }
            } else if (report.contains("Payment")) {
                List<Map<String,Object>> rows   = reportDAO.getPaymentCollection(from, to);
                List<Map<String,Object>> method = reportDAO.getPaymentMethodSummary(from, to);
                if (!exportPdf && !exportExcel) { showPaymentCollection(rows, method, from, to); statusLabel.setText(""); }
                else if (exportPdf)   { String path = pickDir("Save Payment PDF");   if (path != null) { PdfReportGenerator.paymentCollection(rows, method, from, to, path);   openFile(path, "payment_collection_"+from+"_"+to+".pdf");  } }
                else                  { String path = pickDir("Save Payment Excel"); if (path != null) { ExcelReportGenerator.paymentCollection(rows, method, from, to, path);   openFile(path, "payment_collection_"+from+"_"+to+".xlsx"); } }
            }
            if (exportPdf || exportExcel) { statusLabel.setText("✓ File saved successfully."); statusLabel.setTextFill(Color.web("#16A34A")); }
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
            statusLabel.setTextFill(Color.web("#DC2626"));
            ex.printStackTrace();
        }
    }

    // ── Preview renderers ──────────────────────────────────────────────────────
    private void showSalesSummary(List<Map<String,Object>> rows, Map<String,Object> totals, String from, String to) {
        previewArea.getChildren().clear();
        previewArea.getChildren().add(reportTitle("📈 Sales Summary", from + " → " + to));
        // Summary cards
        HBox cards = new HBox(12);
        cards.getChildren().addAll(
            summaryCard("Total Revenue",    fmtAmt(totals.get("total_amount")), "#2563EB", "#EFF6FF"),
            summaryCard("Total Collected",  fmtAmt(totals.get("paid_amount")),  "#16A34A", "#F0FDF4"),
            summaryCard("Outstanding",      fmtAmt(totals.get("outstanding")),  "#DC2626", "#FEF2F2"),
            summaryCard("Total GST",        fmtAmt(totals.get("total_gst")),    "#7C3AED", "#F5F3FF"),
            summaryCard("Invoices",         fmtN(totals.get("invoice_count")),  "#374151", "#F9FAFB")
        );
        previewArea.getChildren().add(cards);
        String[] cols = {"Period","Invoices","Subtotal","SGST","CGST","Total GST","Total Amount","Paid","Outstanding"};
        String[] keys = {"period","invoice_count","subtotal","sgst","cgst","total_gst","total_amount","paid_amount","outstanding"};
        boolean[] isAmt = {false,false,true,true,true,true,true,true,true};
        previewArea.getChildren().add(buildTable(cols, keys, isAmt, rows));
    }

    private void showOutstanding(List<Map<String,Object>> rows, String asOf) {
        previewArea.getChildren().clear();
        previewArea.getChildren().add(reportTitle("⚠️ Outstanding & Overdue", "As of " + asOf));
        long overdue = rows.stream().filter(r -> "OVERDUE".equals(r.get("overdue_flag"))).count();
        double totalBal = rows.stream().mapToDouble(r -> d(r.get("balance_due"))).sum();
        HBox cards = new HBox(12);
        cards.getChildren().addAll(
            summaryCard("Total Outstanding", fmtAmtD(totalBal), "#DC2626", "#FEF2F2"),
            summaryCard("Unpaid Invoices",   String.valueOf(rows.size()), "#D97706", "#FFFBEB"),
            summaryCard("Overdue Invoices",  String.valueOf(overdue), "#DC2626", "#FEF2F2")
        );
        previewArea.getChildren().add(cards);
        String[] cols = {"Invoice #","Customer","Invoice Date","Due Date","Total","Paid","Balance Due","Status","Days Overdue"};
        String[] keys = {"invoice_number","customer_name","invoice_date","due_date","total_amount","paid_amount","balance_due","status","days_overdue"};
        boolean[] isAmt = {false,false,false,false,true,true,true,false,false};
        previewArea.getChildren().add(buildTable(cols, keys, isAmt, rows));
    }

    private void showCustomerSales(List<Map<String,Object>> rows, String from, String to) {
        previewArea.getChildren().clear();
        previewArea.getChildren().add(reportTitle("👥 Customer-wise Sales", from + " → " + to));
        String[] cols = {"Customer","Phone","Invoices","Subtotal","GST","Total Amount","Paid","Outstanding"};
        String[] keys = {"customer_name","customer_phone","invoice_count","subtotal","gst_amount","total_amount","paid_amount","outstanding"};
        boolean[] isAmt = {false,false,false,true,true,true,true,true};
        previewArea.getChildren().add(buildTable(cols, keys, isAmt, rows));
    }

    private void showProductSales(List<Map<String,Object>> rows, String from, String to) {
        previewArea.getChildren().clear();
        previewArea.getChildren().add(reportTitle("📦 Product-wise Sales", from + " → " + to));
        String[] cols = {"Product","Category","Unit","Total Qty","Avg Price","Total Sales","GST Collected","Invoices"};
        String[] keys = {"product_name","category","unit","total_qty","avg_price","total_sales","gst_collected","invoice_count"};
        boolean[] isAmt = {false,false,false,false,true,true,true,false};
        previewArea.getChildren().add(buildTable(cols, keys, isAmt, rows));
    }

    private void showGstReport(List<Map<String,Object>> monthly, List<Map<String,Object>> byProduct, String from, String to) {
        previewArea.getChildren().clear();
        previewArea.getChildren().add(reportTitle("🧾 GST Report", from + " → " + to));
        Label m = new Label("Monthly Summary"); m.setFont(Font.font("Arial", FontWeight.BOLD, 13)); m.setTextFill(Color.web("#2563EB"));
        previewArea.getChildren().add(m);
        String[] c1 = {"Period","Invoices","Taxable Value","SGST","CGST","Total GST","Total with GST"};
        String[] k1 = {"period","invoice_count","taxable_value","sgst_amount","cgst_amount","total_gst","total_with_gst"};
        boolean[] a1 = {false,false,true,true,true,true,true};
        previewArea.getChildren().add(buildTable(c1, k1, a1, monthly));
        Label p = new Label("By Product"); p.setFont(Font.font("Arial", FontWeight.BOLD, 13)); p.setTextFill(Color.web("#2563EB"));
        previewArea.getChildren().add(p);
        String[] c2 = {"Product","Category","Taxable Value","SGST %","CGST %","SGST Amt","CGST Amt","Total GST"};
        String[] k2 = {"product_name","category","taxable_value","sgst_percent","cgst_percent","sgst_amount","cgst_amount","total_gst"};
        boolean[] a2 = {false,false,true,false,false,true,true,true};
        previewArea.getChildren().add(buildTable(c2, k2, a2, byProduct));
    }

    private void showPaymentCollection(List<Map<String,Object>> rows, List<Map<String,Object>> method, String from, String to) {
        previewArea.getChildren().clear();
        previewArea.getChildren().add(reportTitle("💰 Payment Collection", from + " → " + to));
        HBox cards = new HBox(12);
        double total = rows.stream().mapToDouble(r -> d(r.get("amount"))).sum();
        cards.getChildren().add(summaryCard("Total Collected", fmtAmtD(total), "#16A34A", "#F0FDF4"));
        for (Map<String,Object> m : method)
            cards.getChildren().add(summaryCard(m.get("method")+" Payments", fmtAmt(m.get("total_amount")), "#2563EB", "#EFF6FF"));
        previewArea.getChildren().add(cards);
        String[] cols = {"Date","Invoice #","Customer","Amount","Method","Reference"};
        String[] keys = {"payment_date","invoice_number","customer_name","amount","method","reference"};
        boolean[] isAmt = {false,false,false,true,false,false};
        previewArea.getChildren().add(buildTable(cols, keys, isAmt, rows));
    }

    // ── UI helpers ─────────────────────────────────────────────────────────────
    private VBox summaryCard(String label, String value, String color, String bg) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:10;-fx-border-color:" + color + "22;-fx-border-radius:10;-fx-border-width:1;");
        Label lbl = new Label(label); lbl.setFont(Font.font("Arial", 11)); lbl.setTextFill(Color.web("#6B7280"));
        Label val = new Label(value); val.setFont(Font.font("Arial", FontWeight.BOLD, 18)); val.setTextFill(Color.web(color));
        card.getChildren().addAll(lbl, val);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox reportTitle(String title, String subtitle) {
        HBox box = new HBox(12); box.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(title); t.setFont(Font.font("Arial", FontWeight.BOLD, 18)); t.setTextFill(Color.web("#1A2332"));
        Label s = new Label(subtitle); s.setFont(Font.font("Arial", 13)); s.setTextFill(Color.web("#6B7280"));
        box.getChildren().addAll(t, s);
        return box;
    }

    private ScrollPane buildTable(String[] cols, String[] keys, boolean[] isAmt, List<Map<String,Object>> rows) {
        TableView<Map<String,Object>> table = new TableView<>();
        table.setStyle("-fx-background-color:white;");
        table.setPrefHeight(Math.min(400, 60 + rows.size() * 36));

        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            final boolean amt = isAmt[i];
            TableColumn<Map<String,Object>, String> col = new TableColumn<>(cols[i]);
            col.setCellValueFactory(d -> {
                Object val = d.getValue().get(keys[idx]);
                String text = amt ? fmtAmt(val) : (val == null ? "—" : val.toString());
                return new javafx.beans.property.SimpleStringProperty(text);
            });
            if (amt) col.setStyle("-fx-alignment: CENTER-RIGHT;");
            col.setPrefWidth(amt ? 120 : 140);
            table.getColumns().add(col);
        }
        table.getItems().addAll(rows);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        ScrollPane sp = new ScrollPane(table);
        sp.setFitToWidth(true);
        sp.setPrefHeight(table.getPrefHeight() + 20);
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    private String pickDir(String title) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle(title);
        dc.setInitialDirectory(new File(System.getProperty("user.home")));
        File dir = dc.showDialog(null);
        return dir != null ? dir.getAbsolutePath() : null;
    }

    private void openFile(String dir, String filename) {
        try { java.awt.Desktop.getDesktop().open(new File(dir + File.separator + filename)); } catch (Exception ignored) {}
    }

    private Button quickBtn(String label) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color:#F3F4F6;-fx-text-fill:#374151;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12;-fx-padding:5 12;");
        return b;
    }

    private Button actionBtn(String label, String color) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-font-size:13;-fx-padding:9 18;");
        return b;
    }

    private String fmtAmt(Object o) {
        if (o == null) return "—";
        try { return java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("en","IN")).format(((Number)o).doubleValue()); }
        catch (Exception e) { return o.toString(); }
    }
    private String fmtAmtD(double v) { return java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("en","IN")).format(v); }
    private String fmtN(Object o) { return o == null ? "0" : String.valueOf(((Number)o).longValue()); }
    private double d(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number)o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }

    public VBox getView() { return view; }
}
