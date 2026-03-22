package com.buildmat.util;

import com.buildmat.model.Invoice;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfReportGenerator {

    private static final DeviceRgb BRAND     = new DeviceRgb(37, 99, 235);
    private static final DeviceRgb DARK      = new DeviceRgb(26, 35, 50);
    private static final DeviceRgb LIGHT_BG  = new DeviceRgb(243, 244, 246);
    private static final DeviceRgb ALT_BG    = new DeviceRgb(239, 246, 255);
    private static final DeviceRgb MID_GRAY  = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb RED       = new DeviceRgb(220, 38, 38);
    private static final DeviceRgb GREEN     = new DeviceRgb(22, 163, 74);
    private static final DeviceRgb TOTAL_BG  = new DeviceRgb(219, 234, 254);
    private static final NumberFormat INR    = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    // ── Shared helpers ─────────────────────────────────────────────────────────
    private static PdfFont bold() throws Exception {
        return PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
    }
    private static PdfFont regular() throws Exception {
        return PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
    }

    private static Document openDoc(String path, boolean landscape) throws Exception {
        PdfDocument pdf = new PdfDocument(new PdfWriter(path));
        PageSize ps = landscape ? PageSize.A4.rotate() : PageSize.A4;
        Document doc = new Document(pdf, ps);
        doc.setMargins(36, 40, 36, 40);
        return doc;
    }

    private static void addReportHeader(Document doc, PdfFont bold, PdfFont reg,
                                         String title, String subtitle) throws Exception {
        Table ht = new Table(UnitValue.createPercentArray(new float[]{60,40})).useAllAvailableWidth();
        ht.setBorder(Border.NO_BORDER);
        Cell lc = new Cell().setBorder(Border.NO_BORDER);
        lc.add(new Paragraph("BuildMat Supplies").setFont(bold).setFontSize(16).setFontColor(BRAND));
        lc.add(new Paragraph("Building Material Supplier").setFont(reg).setFontSize(9).setFontColor(MID_GRAY));
        Cell rc = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        rc.add(new Paragraph(title).setFont(bold).setFontSize(14).setFontColor(DARK));
        rc.add(new Paragraph(subtitle).setFont(reg).setFontSize(9).setFontColor(MID_GRAY));
        ht.addCell(lc); ht.addCell(rc);
        doc.add(ht);
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1.5f))
                .setMarginTop(6).setMarginBottom(10));
    }

    private static Cell hCell(String text, PdfFont bold, TextAlignment align) {
        Cell c = new Cell().setBackgroundColor(BRAND).setPadding(6).setBorder(Border.NO_BORDER);
        c.add(new Paragraph(text).setFont(bold).setFontSize(8)
                .setFontColor(ColorConstants.WHITE).setTextAlignment(align));
        return c;
    }

    private static Cell dCell(String text, PdfFont font, boolean alt, TextAlignment align) {
        Cell c = new Cell().setBackgroundColor(alt ? ALT_BG : ColorConstants.WHITE).setPadding(5).setBorder(Border.NO_BORDER);
        c.add(new Paragraph(text == null ? "" : text).setFont(font).setFontSize(8)
                .setFontColor(DARK).setTextAlignment(align));
        return c;
    }

    private static Cell tCell(String text, PdfFont bold, TextAlignment align) {
        Cell c = new Cell().setBackgroundColor(TOTAL_BG).setPadding(6).setBorder(new SolidBorder(BRAND, 1f));
        c.add(new Paragraph(text).setFont(bold).setFontSize(8).setFontColor(BRAND).setTextAlignment(align));
        return c;
    }

    private static String fmt(Object o) { return o == null ? "—" : o.toString(); }
    private static String fmtAmt(Object o) {
        if (o == null) return "—";
        try { return INR.format(((Number)o).doubleValue()); } catch (Exception e) { return o.toString(); }
    }
    private static double d(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number)o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }

    private static void addFooter(Document doc, PdfFont reg) {
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f)).setMarginTop(8).setMarginBottom(4));
        doc.add(new Paragraph("This is a computer-generated report. BuildMat Billing System.")
                .setFont(reg).setFontSize(7).setFontColor(MID_GRAY).setTextAlignment(TextAlignment.CENTER));
    }

    // ── 1. Sales Summary ──────────────────────────────────────────────────────
    public static String salesSummary(List<Map<String,Object>> rows, Map<String,Object> totals,
                                       String from, String to, String outputDir) throws Exception {
        String path = outputDir + File.separator + "sales_summary_" + from + "_" + to + ".pdf";
        Document doc = openDoc(path, true);
        PdfFont bold = bold(), reg = regular();
        addReportHeader(doc, bold, reg, "Sales Summary Report", "Period: " + from + " to " + to);

        float[] widths = {12,7,14,10,10,10,14,12,11};
        Table t = new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth();
        t.setBorder(Border.NO_BORDER);
        for (String h : new String[]{"Period","Invoices","Subtotal","SGST","CGST","Total GST","Total Amount","Paid","Outstanding"})
            t.addHeaderCell(hCell(h, bold, h.equals("Period")||h.equals("Invoices") ? TextAlignment.LEFT : TextAlignment.RIGHT));

        int i = 0;
        for (Map<String,Object> row : rows) {
            boolean alt = i++ % 2 == 0;
            t.addCell(dCell(fmt(row.get("period")),reg,alt,TextAlignment.LEFT));
            t.addCell(dCell(fmt(row.get("invoice_count")),reg,alt,TextAlignment.CENTER));
            t.addCell(dCell(fmtAmt(row.get("subtotal")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmtAmt(row.get("sgst")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmtAmt(row.get("cgst")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmtAmt(row.get("total_gst")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmtAmt(row.get("total_amount")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmtAmt(row.get("paid_amount")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmtAmt(row.get("outstanding")),reg,alt,TextAlignment.RIGHT));
        }
        // Totals
        t.addCell(tCell("TOTAL",bold,TextAlignment.LEFT));
        t.addCell(tCell(fmt(totals.get("invoice_count")),bold,TextAlignment.CENTER));
        t.addCell(tCell(fmtAmt(totals.get("subtotal")),bold,TextAlignment.RIGHT));
        t.addCell(tCell(fmtAmt(totals.get("sgst")),bold,TextAlignment.RIGHT));
        t.addCell(tCell(fmtAmt(totals.get("cgst")),bold,TextAlignment.RIGHT));
        t.addCell(tCell(fmtAmt(totals.get("total_gst")),bold,TextAlignment.RIGHT));
        t.addCell(tCell(fmtAmt(totals.get("total_amount")),bold,TextAlignment.RIGHT));
        t.addCell(tCell(fmtAmt(totals.get("paid_amount")),bold,TextAlignment.RIGHT));
        t.addCell(tCell(fmtAmt(totals.get("outstanding")),bold,TextAlignment.RIGHT));
        doc.add(t); addFooter(doc, reg); doc.close(); return path;
    }

    // ── 2. Outstanding ────────────────────────────────────────────────────────
    public static String outstandingInvoices(List<Map<String,Object>> rows, String asOf,
                                              String outputDir) throws Exception {
        String path = outputDir + File.separator + "outstanding_" + asOf + ".pdf";
        Document doc = openDoc(path, true);
        PdfFont bold = bold(), reg = regular();
        addReportHeader(doc, bold, reg, "Outstanding & Overdue Invoices", "As of: " + asOf);

        float[] widths = {14,16,12,10,10,11,11,11,8,9};
        Table t = new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth();
        t.setBorder(Border.NO_BORDER);
        for (String h : new String[]{"Invoice #","Customer","Phone","Inv Date","Due Date","Total","Paid","Balance Due","Status","Days Due"})
            t.addHeaderCell(hCell(h, bold, TextAlignment.LEFT));

        int i = 0;
        for (Map<String,Object> row : rows) {
            boolean alt = i++ % 2 == 0;
            boolean overdue = "OVERDUE".equals(fmt(row.get("overdue_flag")));
            DeviceRgb fg = overdue ? RED : DARK;
            t.addCell(dCell(fmt(row.get("invoice_number")),reg,alt,TextAlignment.LEFT));
            t.addCell(dCell(fmt(row.get("customer_name")),reg,alt,TextAlignment.LEFT));
            t.addCell(dCell(fmt(row.get("customer_phone")),reg,alt,TextAlignment.LEFT));
            t.addCell(dCell(fmt(row.get("invoice_date")),reg,alt,TextAlignment.LEFT));
            t.addCell(dCell(fmt(row.get("due_date")),reg,alt,TextAlignment.LEFT));
            t.addCell(dCell(fmtAmt(row.get("total_amount")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmtAmt(row.get("paid_amount")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmtAmt(row.get("balance_due")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmt(row.get("status")),reg,alt,TextAlignment.CENTER));
            Cell dc = dCell(fmt(row.get("days_overdue")),reg,alt,TextAlignment.CENTER);
            if (overdue && d(row.get("days_overdue")) > 0) dc.setFontColor(RED);
            t.addCell(dc);
        }
        doc.add(t); addFooter(doc, reg); doc.close(); return path;
    }

    // ── 3. Customer Sales ──────────────────────────────────────────────────────
    public static String customerSales(List<Map<String,Object>> rows, String from, String to,
                                        String outputDir) throws Exception {
        String path = outputDir + File.separator + "customer_sales_" + from + "_" + to + ".pdf";
        Document doc = openDoc(path, true);
        PdfFont bold = bold(), reg = regular();
        addReportHeader(doc, bold, reg, "Customer-wise Sales Report", "Period: " + from + " to " + to);

        float[] widths = {22,14,8,14,11,14,12,13};
        Table t = new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth();
        t.setBorder(Border.NO_BORDER);
        for (String h : new String[]{"Customer","Phone","Invoices","Subtotal","GST","Total Amount","Paid","Outstanding"})
            t.addHeaderCell(hCell(h, bold, h.equals("Customer")||h.equals("Phone") ? TextAlignment.LEFT : TextAlignment.RIGHT));

        double[] tots = new double[5]; int i = 0;
        for (Map<String,Object> row : rows) {
            boolean alt = i++ % 2 == 0;
            t.addCell(dCell(fmt(row.get("customer_name")),reg,alt,TextAlignment.LEFT));
            t.addCell(dCell(fmt(row.get("customer_phone")),reg,alt,TextAlignment.LEFT));
            t.addCell(dCell(fmt(row.get("invoice_count")),reg,alt,TextAlignment.CENTER));
            t.addCell(dCell(fmtAmt(row.get("subtotal")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmtAmt(row.get("gst_amount")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmtAmt(row.get("total_amount")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmtAmt(row.get("paid_amount")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmtAmt(row.get("outstanding")),reg,alt,TextAlignment.RIGHT));
            tots[0]+=d(row.get("subtotal")); tots[1]+=d(row.get("gst_amount"));
            tots[2]+=d(row.get("total_amount")); tots[3]+=d(row.get("paid_amount")); tots[4]+=d(row.get("outstanding"));
        }
        t.addCell(tCell("TOTAL",bold,TextAlignment.LEFT)); t.addCell(tCell("",bold,TextAlignment.LEFT));
        t.addCell(tCell(String.valueOf(rows.size()),bold,TextAlignment.CENTER));
        for (double tot : tots) t.addCell(tCell(INR.format(tot),bold,TextAlignment.RIGHT));
        doc.add(t); addFooter(doc, reg); doc.close(); return path;
    }

    // ── 4. Product Sales ──────────────────────────────────────────────────────
    public static String productSales(List<Map<String,Object>> rows, String from, String to,
                                       String outputDir) throws Exception {
        String path = outputDir + File.separator + "product_sales_" + from + "_" + to + ".pdf";
        Document doc = openDoc(path, true);
        PdfFont bold = bold(), reg = regular();
        addReportHeader(doc, bold, reg, "Product-wise Sales Report", "Period: " + from + " to " + to);

        float[] widths = {25,14,8,10,12,17,14,10};
        Table t = new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth();
        t.setBorder(Border.NO_BORDER);
        for (String h : new String[]{"Product","Category","Unit","Total Qty","Avg Price","Total Sales","GST Collected","Invoices"})
            t.addHeaderCell(hCell(h, bold, h.equals("Product")||h.equals("Category") ? TextAlignment.LEFT : TextAlignment.RIGHT));

        double totalSales = 0, totalGst = 0; int i = 0;
        for (Map<String,Object> row : rows) {
            boolean alt = i++ % 2 == 0;
            t.addCell(dCell(fmt(row.get("product_name")),reg,alt,TextAlignment.LEFT));
            t.addCell(dCell(fmt(row.get("category")),reg,alt,TextAlignment.LEFT));
            t.addCell(dCell(fmt(row.get("unit")),reg,alt,TextAlignment.CENTER));
            t.addCell(dCell(fmt(row.get("total_qty")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmtAmt(row.get("avg_price")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmtAmt(row.get("total_sales")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmtAmt(row.get("gst_collected")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmt(row.get("invoice_count")),reg,alt,TextAlignment.CENTER));
            totalSales += d(row.get("total_sales")); totalGst += d(row.get("gst_collected"));
        }
        t.addCell(tCell("TOTAL",bold,TextAlignment.LEFT));
        for (int k=0;k<5;k++) t.addCell(tCell("",bold,TextAlignment.LEFT));
        t.addCell(tCell(INR.format(totalSales),bold,TextAlignment.RIGHT));
        t.addCell(tCell(INR.format(totalGst),bold,TextAlignment.RIGHT));
        t.addCell(tCell("",bold,TextAlignment.LEFT));
        doc.add(t); addFooter(doc, reg); doc.close(); return path;
    }

    // ── 5. GST Report ──────────────────────────────────────────────────────────
    public static String gstReport(List<Map<String,Object>> monthly, List<Map<String,Object>> byProduct,
                                    String from, String to, String outputDir) throws Exception {
        String path = outputDir + File.separator + "gst_report_" + from + "_" + to + ".pdf";
        Document doc = openDoc(path, true);
        PdfFont bold = bold(), reg = regular();
        addReportHeader(doc, bold, reg, "GST Report", "Period: " + from + " to " + to);

        // Monthly summary
        doc.add(new Paragraph("Monthly GST Summary").setFont(bold).setFontSize(10).setFontColor(BRAND).setMarginBottom(4));
        float[] w1 = {14,9,18,14,14,14,17};
        Table t1 = new Table(UnitValue.createPercentArray(w1)).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        for (String h : new String[]{"Period","Invoices","Taxable Value","SGST","CGST","Total GST","Total with GST"})
            t1.addHeaderCell(hCell(h,bold,h.equals("Period")||h.equals("Invoices")?TextAlignment.LEFT:TextAlignment.RIGHT));
        double[] t = new double[5]; int i = 0;
        for (Map<String,Object> row : monthly) {
            boolean alt = i++ % 2 == 0;
            t1.addCell(dCell(fmt(row.get("period")),reg,alt,TextAlignment.LEFT));
            t1.addCell(dCell(fmt(row.get("invoice_count")),reg,alt,TextAlignment.CENTER));
            t1.addCell(dCell(fmtAmt(row.get("taxable_value")),reg,alt,TextAlignment.RIGHT));
            t1.addCell(dCell(fmtAmt(row.get("sgst_amount")),reg,alt,TextAlignment.RIGHT));
            t1.addCell(dCell(fmtAmt(row.get("cgst_amount")),reg,alt,TextAlignment.RIGHT));
            t1.addCell(dCell(fmtAmt(row.get("total_gst")),reg,alt,TextAlignment.RIGHT));
            t1.addCell(dCell(fmtAmt(row.get("total_with_gst")),reg,alt,TextAlignment.RIGHT));
            t[0]+=d(row.get("taxable_value")); t[1]+=d(row.get("sgst_amount")); t[2]+=d(row.get("cgst_amount"));
            t[3]+=d(row.get("total_gst")); t[4]+=d(row.get("total_with_gst"));
        }
        t1.addCell(tCell("TOTAL",bold,TextAlignment.LEFT)); t1.addCell(tCell(String.valueOf(monthly.size()),bold,TextAlignment.CENTER));
        for (double v : t) t1.addCell(tCell(INR.format(v),bold,TextAlignment.RIGHT));
        doc.add(t1);

        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("GST by Product").setFont(bold).setFontSize(10).setFontColor(BRAND).setMarginBottom(4));
        float[] w2 = {24,14,16,9,9,14,14,14};
        Table t2 = new Table(UnitValue.createPercentArray(w2)).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        for (String h : new String[]{"Product","Category","Taxable Value","SGST%","CGST%","SGST Amt","CGST Amt","Total GST"})
            t2.addHeaderCell(hCell(h,bold,h.equals("Product")||h.equals("Category")?TextAlignment.LEFT:TextAlignment.RIGHT));
        double sg=0,cg=0,tg=0; i=0;
        for (Map<String,Object> row : byProduct) {
            boolean alt = i++ % 2 == 0;
            t2.addCell(dCell(fmt(row.get("product_name")),reg,alt,TextAlignment.LEFT));
            t2.addCell(dCell(fmt(row.get("category")),reg,alt,TextAlignment.LEFT));
            t2.addCell(dCell(fmtAmt(row.get("taxable_value")),reg,alt,TextAlignment.RIGHT));
            t2.addCell(dCell(fmt(row.get("sgst_percent"))+"%",reg,alt,TextAlignment.CENTER));
            t2.addCell(dCell(fmt(row.get("cgst_percent"))+"%",reg,alt,TextAlignment.CENTER));
            t2.addCell(dCell(fmtAmt(row.get("sgst_amount")),reg,alt,TextAlignment.RIGHT));
            t2.addCell(dCell(fmtAmt(row.get("cgst_amount")),reg,alt,TextAlignment.RIGHT));
            t2.addCell(dCell(fmtAmt(row.get("total_gst")),reg,alt,TextAlignment.RIGHT));
            sg+=d(row.get("sgst_amount")); cg+=d(row.get("cgst_amount")); tg+=d(row.get("total_gst"));
        }
        t2.addCell(tCell("TOTAL",bold,TextAlignment.LEFT));
        for(int k=0;k<4;k++) t2.addCell(tCell("",bold,TextAlignment.LEFT));
        t2.addCell(tCell(INR.format(sg),bold,TextAlignment.RIGHT));
        t2.addCell(tCell(INR.format(cg),bold,TextAlignment.RIGHT));
        t2.addCell(tCell(INR.format(tg),bold,TextAlignment.RIGHT));
        doc.add(t2); addFooter(doc, reg); doc.close(); return path;
    }

    // ── 6. Payment Collection ─────────────────────────────────────────────────
    public static String paymentCollection(List<Map<String,Object>> rows, List<Map<String,Object>> methodSummary,
                                            String from, String to, String outputDir) throws Exception {
        String path = outputDir + File.separator + "payment_collection_" + from + "_" + to + ".pdf";
        Document doc = openDoc(path, true);
        PdfFont bold = bold(), reg = regular();
        addReportHeader(doc, bold, reg, "Payment Collection Report", "Period: " + from + " to " + to);

        // Method summary box
        Table msTable = new Table(UnitValue.createPercentArray(new float[]{25,25,25,25})).useAllAvailableWidth();
        msTable.setBorder(Border.NO_BORDER);
        for (Map<String,Object> m : methodSummary) {
            Cell mc = new Cell().setBackgroundColor(LIGHT_BG).setPadding(8).setBorder(Border.NO_BORDER).setBorderRadius(new BorderRadius(4));
            mc.add(new Paragraph(fmt(m.get("method"))).setFont(bold).setFontSize(10).setFontColor(BRAND));
            mc.add(new Paragraph(INR.format(d(m.get("total_amount")))).setFont(bold).setFontSize(12).setFontColor(DARK));
            mc.add(new Paragraph(fmt(m.get("count")) + " payment(s)").setFont(reg).setFontSize(8).setFontColor(MID_GRAY));
            msTable.addCell(mc);
        }
        doc.add(msTable);
        doc.add(new Paragraph(" "));

        float[] widths = {11,16,20,13,10,15,15};
        Table t = new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        for (String h : new String[]{"Date","Invoice #","Customer","Amount","Method","Reference","Notes"})
            t.addHeaderCell(hCell(h,bold,h.equals("Amount")?TextAlignment.RIGHT:TextAlignment.LEFT));

        double total = 0; int i = 0;
        for (Map<String,Object> row : rows) {
            boolean alt = i++ % 2 == 0;
            t.addCell(dCell(fmt(row.get("payment_date")),reg,alt,TextAlignment.LEFT));
            t.addCell(dCell(fmt(row.get("invoice_number")),reg,alt,TextAlignment.LEFT));
            t.addCell(dCell(fmt(row.get("customer_name")),reg,alt,TextAlignment.LEFT));
            t.addCell(dCell(fmtAmt(row.get("amount")),reg,alt,TextAlignment.RIGHT));
            t.addCell(dCell(fmt(row.get("method")),reg,alt,TextAlignment.LEFT));
            t.addCell(dCell(fmt(row.get("reference")),reg,alt,TextAlignment.LEFT));
            t.addCell(dCell(fmt(row.get("notes")),reg,alt,TextAlignment.LEFT));
            total += d(row.get("amount"));
        }
        t.addCell(tCell("TOTAL",bold,TextAlignment.LEFT));
        for(int k=0;k<2;k++) t.addCell(tCell("",bold,TextAlignment.LEFT));
        t.addCell(tCell(INR.format(total),bold,TextAlignment.RIGHT));
        for(int k=0;k<3;k++) t.addCell(tCell("",bold,TextAlignment.LEFT));
        doc.add(t); addFooter(doc, reg); doc.close(); return path;
    }
}
