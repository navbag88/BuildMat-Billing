package com.buildmat.util;

import com.buildmat.model.*;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class DataExporter {

    private static final DeviceRgb BRAND    = new DeviceRgb(37, 99, 235);
    private static final DeviceRgb DARK     = new DeviceRgb(26, 35, 50);
    private static final DeviceRgb LIGHTBG  = new DeviceRgb(243, 244, 246);
    private static final DeviceRgb ALTBG    = new DeviceRgb(239, 246, 255);
    private static final DeviceRgb MIDGRAY  = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb TOTALBG  = new DeviceRgb(219, 234, 254);
    private static final DeviceRgb GREEN    = new DeviceRgb(22, 163, 74);
    private static final DeviceRgb RED      = new DeviceRgb(220, 38, 38);
    private static final DeviceRgb ORANGE   = new DeviceRgb(217, 119, 6);
    private static final NumberFormat INR   = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    // ═══════════════════════════════════════════════════════════════════════════
    // EXCEL EXPORTS
    // ═══════════════════════════════════════════════════════════════════════════

    // ── Customers Excel ────────────────────────────────────────────────────────
    public static String exportCustomersExcel(List<Customer> customers, String dir) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Customers");
            CellStyle hdr = excelHeader(wb), alt = excelAlt(wb), def = wb.createCellStyle(), tot = excelTotal(wb);

            addExcelTitle(sheet, wb, "Customers List", "Total: " + customers.size(), 5);
            Row h = sheet.createRow(2);
            excelHdrRow(h, hdr, "ID", "Name", "Phone", "Email", "Address");
            setColWidths(sheet, 2000, 7000, 4500, 6000, 9000);

            for (int i = 0; i < customers.size(); i++) {
                Customer c = customers.get(i);
                Row r = sheet.createRow(i + 3);
                CellStyle s = i % 2 == 0 ? alt : def;
                excelRow(r, s, String.valueOf(c.getId()), c.getName(),
                    nvl(c.getPhone()), nvl(c.getEmail()), nvl(c.getAddress()));
            }
            Row totRow = sheet.createRow(customers.size() + 3);
            excelRow(totRow, tot, "Total", String.valueOf(customers.size()), "", "", "");

            String path = dir + File.separator + "customers_export.xlsx";
            try (FileOutputStream fos = new FileOutputStream(path)) { wb.write(fos); }
            return path;
        }
    }

    // ── Products Excel ─────────────────────────────────────────────────────────
    public static String exportProductsExcel(List<Product> products, String dir) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Products");
            CellStyle hdr = excelHeader(wb), alt = excelAlt(wb), def = wb.createCellStyle();
            CellStyle num = excelNum(wb), numAlt = excelNumAlt(wb), tot = excelTotal(wb);

            addExcelTitle(sheet, wb, "Products Catalog", "Total: " + products.size(), 8);
            Row h = sheet.createRow(2);
            excelHdrRow(h, hdr, "ID", "Name", "Category", "Unit", "Price", "Stock", "SGST%", "CGST%");
            setColWidths(sheet, 2000, 7000, 4000, 3000, 4000, 3500, 3000, 3000);

            double totalValue = 0;
            for (int i = 0; i < products.size(); i++) {
                Product p = products.get(i);
                Row r = sheet.createRow(i + 3);
                CellStyle s = i % 2 == 0 ? alt : def;
                CellStyle ns = i % 2 == 0 ? numAlt : num;
                setCells(r, s, ns, String.valueOf(p.getId()), p.getName(),
                    nvl(p.getCategory()), nvl(p.getUnit()));
                setNumCells(r, ns, 4, p.getPrice(), p.getStockQty(), p.getSgstPercent(), p.getCgstPercent());
                totalValue += p.getPrice() * p.getStockQty();
            }
            Row totRow = sheet.createRow(products.size() + 3);
            excelRow(totRow, tot, "Total", String.valueOf(products.size()), "", "", "");

            String path = dir + File.separator + "products_export.xlsx";
            try (FileOutputStream fos = new FileOutputStream(path)) { wb.write(fos); }
            return path;
        }
    }

    // ── Invoices Excel ─────────────────────────────────────────────────────────
    public static String exportInvoicesExcel(List<Invoice> invoices, String dir) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Invoices");
            CellStyle hdr = excelHeader(wb), alt = excelAlt(wb), def = wb.createCellStyle();
            CellStyle num = excelNum(wb), numAlt = excelNumAlt(wb), tot = excelTotal(wb), totNum = excelTotalNum(wb);

            addExcelTitle(sheet, wb, "Invoices List", "Total: " + invoices.size(), 10);
            Row h = sheet.createRow(2);
            excelHdrRow(h, hdr, "Invoice #", "Customer", "Date", "Due Date", "Subtotal", "GST", "Total", "Paid", "Balance", "Status");
            setColWidths(sheet, 4500, 6000, 3500, 3500, 4000, 4000, 4500, 4000, 4000, 3500);

            double subTot=0, gstTot=0, totAmt=0, paidTot=0, balTot=0;
            for (int i = 0; i < invoices.size(); i++) {
                Invoice inv = invoices.get(i);
                Row r = sheet.createRow(i + 3);
                CellStyle s = i % 2 == 0 ? alt : def;
                CellStyle ns = i % 2 == 0 ? numAlt : num;
                setCells(r, s, ns,
                    inv.getInvoiceNumber(),
                    inv.getCustomer() != null ? inv.getCustomer().getName() : "",
                    inv.getInvoiceDate().toString(),
                    inv.getDueDate() != null ? inv.getDueDate().toString() : "");
                setNumCells(r, ns, 4, inv.getSubtotal(),
                    inv.isIncludeGst() ? inv.getTaxAmount() : 0,
                    inv.getTotalAmount(), inv.getPaidAmount(), inv.getBalanceDue());
                setCellStr(r, 9, inv.getStatus(), s);
                subTot+=inv.getSubtotal(); gstTot+=inv.isIncludeGst()?inv.getTaxAmount():0;
                totAmt+=inv.getTotalAmount(); paidTot+=inv.getPaidAmount(); balTot+=inv.getBalanceDue();
            }
            Row totRow = sheet.createRow(invoices.size() + 3);
            excelRow(totRow, tot, "TOTAL", String.valueOf(invoices.size()), "", "");
            setNumCells(totRow, totNum, 4, subTot, gstTot, totAmt, paidTot, balTot);

            String path = dir + File.separator + "invoices_export.xlsx";
            try (FileOutputStream fos = new FileOutputStream(path)) { wb.write(fos); }
            return path;
        }
    }

    // ── Payments Excel ─────────────────────────────────────────────────────────
    public static String exportPaymentsExcel(List<Payment> payments, String dir) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Payments");
            CellStyle hdr = excelHeader(wb), alt = excelAlt(wb), def = wb.createCellStyle();
            CellStyle num = excelNum(wb), numAlt = excelNumAlt(wb), tot = excelTotal(wb), totNum = excelTotalNum(wb);

            addExcelTitle(sheet, wb, "Payments List", "Total: " + payments.size(), 7);
            Row h = sheet.createRow(2);
            excelHdrRow(h, hdr, "Date", "Invoice #", "Customer", "Amount", "Method", "Reference", "Notes");
            setColWidths(sheet, 3500, 4000, 6000, 4500, 3500, 5000, 5000);

            double total = 0;
            for (int i = 0; i < payments.size(); i++) {
                Payment p = payments.get(i);
                Row r = sheet.createRow(i + 3);
                CellStyle s = i % 2 == 0 ? alt : def;
                CellStyle ns = i % 2 == 0 ? numAlt : num;
                setCells(r, s, ns, p.getPaymentDate().toString(),
                    nvl(p.getInvoiceNumber()), nvl(p.getCustomerName()));
                setNumCells(r, ns, 3, p.getAmount());
                setCells4(r, s, 4, nvl(p.getMethod()), nvl(p.getReference()), nvl(p.getNotes()));
                total += p.getAmount();
            }
            Row totRow = sheet.createRow(payments.size() + 3);
            excelRow(totRow, tot, "TOTAL", "", "");
            setNumCells(totRow, totNum, 3, total);

            String path = dir + File.separator + "payments_export.xlsx";
            try (FileOutputStream fos = new FileOutputStream(path)) { wb.write(fos); }
            return path;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PDF EXPORTS
    // ═══════════════════════════════════════════════════════════════════════════

    // ── Customers PDF ──────────────────────────────────────────────────────────
    public static String exportCustomersPdf(List<Customer> customers, String dir) throws Exception {
        String path = dir + File.separator + "customers_export.pdf";
        PdfFont bold = bold(), reg = reg();
        Document doc = openDoc(path, false);
        addPdfHeader(doc, bold, reg, "Customers List", "Total: " + customers.size());

        float[] w = {8, 28, 16, 24, 24};
        Table t = new Table(UnitValue.createPercentArray(w)).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        for (String h : new String[]{"#", "Name", "Phone", "Email", "Address"})
            t.addHeaderCell(pHdr(h, bold, TextAlignment.LEFT));

        for (int i = 0; i < customers.size(); i++) {
            Customer c = customers.get(i);
            boolean alt = i % 2 == 0;
            t.addCell(pCell(String.valueOf(i+1), reg, alt, TextAlignment.CENTER));
            t.addCell(pCell(c.getName(), bold, alt, TextAlignment.LEFT));
            t.addCell(pCell(nvl(c.getPhone()), reg, alt, TextAlignment.LEFT));
            t.addCell(pCell(nvl(c.getEmail()), reg, alt, TextAlignment.LEFT));
            t.addCell(pCell(nvl(c.getAddress()), reg, alt, TextAlignment.LEFT));
        }
        addPdfTotal(t, bold, 5, "Total: " + customers.size() + " customers");
        doc.add(t); addPdfFooter(doc, reg); doc.close(); return path;
    }

    // ── Products PDF ───────────────────────────────────────────────────────────
    public static String exportProductsPdf(List<Product> products, String dir) throws Exception {
        String path = dir + File.separator + "products_export.pdf";
        PdfFont bold = bold(), reg = reg();
        Document doc = openDoc(path, true);
        addPdfHeader(doc, bold, reg, "Products Catalog", "Total: " + products.size());

        float[] w = {6, 24, 12, 8, 12, 10, 7, 7, 14};
        Table t = new Table(UnitValue.createPercentArray(w)).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        for (String h : new String[]{"#", "Name", "Category", "Unit", "Price", "Stock", "SGST%", "CGST%", "Stock Value"})
            t.addHeaderCell(pHdr(h, bold, h.equals("Price")||h.contains("Value")||h.equals("Stock") ? TextAlignment.RIGHT : TextAlignment.LEFT));

        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            boolean alt = i % 2 == 0;
            double stockVal = p.getPrice() * p.getStockQty();
            t.addCell(pCell(String.valueOf(i+1), reg, alt, TextAlignment.CENTER));
            t.addCell(pCell(p.getName(), bold, alt, TextAlignment.LEFT));
            t.addCell(pCell(nvl(p.getCategory()), reg, alt, TextAlignment.LEFT));
            t.addCell(pCell(nvl(p.getUnit()), reg, alt, TextAlignment.CENTER));
            t.addCell(pCell(INR.format(p.getPrice()), reg, alt, TextAlignment.RIGHT));
            t.addCell(pCell(String.valueOf(p.getStockQty()), reg, alt, TextAlignment.RIGHT));
            t.addCell(pCell(p.getSgstPercent()+"%", reg, alt, TextAlignment.CENTER));
            t.addCell(pCell(p.getCgstPercent()+"%", reg, alt, TextAlignment.CENTER));
            t.addCell(pCell(INR.format(stockVal), reg, alt, TextAlignment.RIGHT));
        }
        addPdfTotal(t, bold, 9, "Total: " + products.size() + " products");
        doc.add(t); addPdfFooter(doc, reg); doc.close(); return path;
    }

    // ── Invoices PDF ───────────────────────────────────────────────────────────
    public static String exportInvoicesPdf(List<Invoice> invoices, String dir) throws Exception {
        String path = dir + File.separator + "invoices_export.pdf";
        PdfFont bold = bold(), reg = reg();
        Document doc = openDoc(path, true);
        addPdfHeader(doc, bold, reg, "Invoices List", "Total: " + invoices.size());

        float[] w = {13, 17, 9, 9, 11, 9, 11, 10, 10, 9};
        Table t = new Table(UnitValue.createPercentArray(w)).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        for (String h : new String[]{"Invoice #","Customer","Date","Due","Subtotal","GST","Total","Paid","Balance","Status"})
            t.addHeaderCell(pHdr(h, bold, TextAlignment.LEFT));

        double subTot=0, gstTot=0, totAmt=0, paidTot=0, balTot=0;
        for (int i = 0; i < invoices.size(); i++) {
            Invoice inv = invoices.get(i);
            boolean alt = i % 2 == 0;
            double gst = inv.isIncludeGst() ? inv.getTaxAmount() : 0;
            DeviceRgb stColor = switch(inv.getStatus()) {
                case "PAID" -> GREEN; case "PARTIAL" -> ORANGE; default -> RED;
            };
            t.addCell(pCell(inv.getInvoiceNumber(), bold, alt, TextAlignment.LEFT));
            t.addCell(pCell(inv.getCustomer()!=null?inv.getCustomer().getName():"", reg, alt, TextAlignment.LEFT));
            t.addCell(pCell(inv.getInvoiceDate().toString(), reg, alt, TextAlignment.LEFT));
            t.addCell(pCell(inv.getDueDate()!=null?inv.getDueDate().toString():"—", reg, alt, TextAlignment.LEFT));
            t.addCell(pCell(INR.format(inv.getSubtotal()), reg, alt, TextAlignment.RIGHT));
            t.addCell(pCell(INR.format(gst), reg, alt, TextAlignment.RIGHT));
            t.addCell(pCell(INR.format(inv.getTotalAmount()), bold, alt, TextAlignment.RIGHT));
            t.addCell(pCell(INR.format(inv.getPaidAmount()), reg, alt, TextAlignment.RIGHT));
            t.addCell(pCell(INR.format(inv.getBalanceDue()), reg, alt, TextAlignment.RIGHT));
            Cell sc = pCell(inv.getStatus(), bold, alt, TextAlignment.CENTER);
            sc.setFontColor(stColor);
            t.addCell(sc);
            subTot+=inv.getSubtotal(); gstTot+=gst;
            totAmt+=inv.getTotalAmount(); paidTot+=inv.getPaidAmount(); balTot+=inv.getBalanceDue();
        }
        // Totals row
        for (int k=0; k<4; k++) t.addCell(pTotCell(k==0?"TOTAL ("+invoices.size()+")":"", bold, TextAlignment.LEFT));
        t.addCell(pTotCell(INR.format(subTot), bold, TextAlignment.RIGHT));
        t.addCell(pTotCell(INR.format(gstTot), bold, TextAlignment.RIGHT));
        t.addCell(pTotCell(INR.format(totAmt), bold, TextAlignment.RIGHT));
        t.addCell(pTotCell(INR.format(paidTot), bold, TextAlignment.RIGHT));
        t.addCell(pTotCell(INR.format(balTot), bold, TextAlignment.RIGHT));
        t.addCell(pTotCell("", bold, TextAlignment.LEFT));
        doc.add(t); addPdfFooter(doc, reg); doc.close(); return path;
    }

    // ── Payments PDF ───────────────────────────────────────────────────────────
    public static String exportPaymentsPdf(List<Payment> payments, String dir) throws Exception {
        String path = dir + File.separator + "payments_export.pdf";
        PdfFont bold = bold(), reg = reg();
        Document doc = openDoc(path, true);
        addPdfHeader(doc, bold, reg, "Payments List", "Total: " + payments.size());

        float[] w = {11, 14, 20, 14, 11, 16, 14};
        Table t = new Table(UnitValue.createPercentArray(w)).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        for (String h : new String[]{"Date","Invoice #","Customer","Amount","Method","Reference","Notes"})
            t.addHeaderCell(pHdr(h, bold, h.equals("Amount") ? TextAlignment.RIGHT : TextAlignment.LEFT));

        double total = 0;
        for (int i = 0; i < payments.size(); i++) {
            Payment p = payments.get(i);
            boolean alt = i % 2 == 0;
            t.addCell(pCell(p.getPaymentDate().toString(), reg, alt, TextAlignment.LEFT));
            t.addCell(pCell(nvl(p.getInvoiceNumber()), reg, alt, TextAlignment.LEFT));
            t.addCell(pCell(nvl(p.getCustomerName()), reg, alt, TextAlignment.LEFT));
            t.addCell(pCell(INR.format(p.getAmount()), bold, alt, TextAlignment.RIGHT));
            t.addCell(pCell(nvl(p.getMethod()), reg, alt, TextAlignment.LEFT));
            t.addCell(pCell(nvl(p.getReference()), reg, alt, TextAlignment.LEFT));
            t.addCell(pCell(nvl(p.getNotes()), reg, alt, TextAlignment.LEFT));
            total += p.getAmount();
        }
        for (int k=0; k<3; k++) t.addCell(pTotCell(k==0?"TOTAL ("+payments.size()+")":"", bold, TextAlignment.LEFT));
        t.addCell(pTotCell(INR.format(total), bold, TextAlignment.RIGHT));
        for (int k=0; k<3; k++) t.addCell(pTotCell("", bold, TextAlignment.LEFT));
        doc.add(t); addPdfFooter(doc, reg); doc.close(); return path;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHARED PDF HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    private static Document openDoc(String path, boolean landscape) throws Exception {
        PdfDocument pdf = new PdfDocument(new PdfWriter(path));
        PageSize ps = landscape ? PageSize.A4.rotate() : PageSize.A4;
        Document doc = new Document(pdf, ps);
        doc.setMargins(36, 40, 36, 40);
        return doc;
    }

    private static void addPdfHeader(Document doc, PdfFont bold, PdfFont reg, String title, String subtitle) throws Exception {
        Table ht = new Table(UnitValue.createPercentArray(new float[]{60,40})).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        Cell lc = new Cell().setBorder(Border.NO_BORDER);
        lc.add(new Paragraph("BuildMat Supplies").setFont(bold).setFontSize(16).setFontColor(BRAND));
        lc.add(new Paragraph("Building Material Supplier").setFont(reg).setFontSize(9).setFontColor(MIDGRAY));
        Cell rc = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        rc.add(new Paragraph(title).setFont(bold).setFontSize(15).setFontColor(DARK));
        rc.add(new Paragraph(subtitle).setFont(reg).setFontSize(9).setFontColor(MIDGRAY));
        rc.add(new Paragraph("Generated: " + java.time.LocalDate.now()).setFont(reg).setFontSize(9).setFontColor(MIDGRAY));
        ht.addCell(lc); ht.addCell(rc);
        doc.add(ht);
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1.5f)).setMarginTop(6).setMarginBottom(10));
    }

    private static void addPdfFooter(Document doc, PdfFont reg) {
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f)).setMarginTop(8).setMarginBottom(4));
        doc.add(new Paragraph("BuildMat Billing System — Computer generated export")
                .setFont(reg).setFontSize(7).setFontColor(MIDGRAY).setTextAlignment(TextAlignment.CENTER));
    }

    private static Cell pHdr(String text, PdfFont bold, TextAlignment align) {
        Cell c = new Cell().setBackgroundColor(BRAND).setPadding(6).setBorder(Border.NO_BORDER);
        c.add(new Paragraph(text).setFont(bold).setFontSize(9).setFontColor(ColorConstants.WHITE).setTextAlignment(align));
        return c;
    }

    private static Cell pCell(String text, PdfFont font, boolean alt, TextAlignment align) {
        Cell c = new Cell().setBackgroundColor(alt ? ALTBG : ColorConstants.WHITE).setPadding(5).setBorder(Border.NO_BORDER);
        c.add(new Paragraph(text == null ? "" : text).setFont(font).setFontSize(8).setFontColor(DARK).setTextAlignment(align));
        return c;
    }

    private static Cell pTotCell(String text, PdfFont bold, TextAlignment align) {
        Cell c = new Cell().setBackgroundColor(TOTALBG).setPadding(6).setBorder(new SolidBorder(BRAND, 1f));
        c.add(new Paragraph(text).setFont(bold).setFontSize(8).setFontColor(BRAND).setTextAlignment(align));
        return c;
    }

    private static void addPdfTotal(Table t, PdfFont bold, int cols, String label) {
        for (int k = 0; k < cols; k++) t.addCell(pTotCell(k==0?label:"", bold, TextAlignment.LEFT));
    }

    private static PdfFont bold() throws Exception {
        return PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
    }
    private static PdfFont reg() throws Exception {
        return PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHARED EXCEL HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    private static void addExcelTitle(Sheet sheet, Workbook wb, String title, String sub, int cols) {
        Row r0 = sheet.createRow(0);
        CellStyle ts = wb.createCellStyle();
        Font tf = wb.createFont(); tf.setBold(true); tf.setFontHeightInPoints((short)14);
        tf.setColor(IndexedColors.DARK_BLUE.getIndex()); ts.setFont(tf);
        org.apache.poi.ss.usermodel.Cell  tc = r0.createCell(0); tc.setCellValue(title + "  —  " + sub); tc.setCellStyle(ts);
        sheet.addMergedRegion(new CellRangeAddress(0,0,0,cols-1));
        sheet.createRow(1); // blank
    }

    private static CellStyle excelHeader(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex()); f.setFontHeightInPoints((short)10);
        s.setFont(f); s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN); s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private static CellStyle excelAlt(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private static CellStyle excelTotal(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont(); f.setBold(true); s.setFont(f);
        s.setBorderTop(BorderStyle.MEDIUM);
        return s;
    }

    private static CellStyle excelNum(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        DataFormat df = wb.createDataFormat(); s.setDataFormat(df.getFormat("#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT); return s;
    }

    private static CellStyle excelNumAlt(Workbook wb) {
        CellStyle s = excelNum(wb);
        s.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND); return s;
    }

    private static CellStyle excelTotalNum(Workbook wb) {
        CellStyle s = excelNum(wb);
        s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont(); f.setBold(true); s.setFont(f);
        s.setBorderTop(BorderStyle.MEDIUM); return s;
    }

    private static void excelHdrRow(Row row, CellStyle style, String... values) {
        for (int i = 0; i < values.length; i++) {
            org.apache.poi.ss.usermodel.Cell c = row.createCell(i); c.setCellValue(values[i]); c.setCellStyle(style);
        }
        row.setHeightInPoints(20);
    }

    private static void excelRow(Row row, CellStyle style, String... values) {
        for (int i = 0; i < values.length; i++) {
            org.apache.poi.ss.usermodel.Cell c = row.createCell(i); c.setCellValue(values[i]); c.setCellStyle(style);
        }
    }

    private static void setCells(Row row, CellStyle strStyle, CellStyle numStyle, String... vals) {
        for (int i = 0; i < vals.length; i++) {
            org.apache.poi.ss.usermodel.Cell c = row.createCell(i); c.setCellValue(vals[i]); c.setCellStyle(strStyle);
        }
    }

    private static void setCells4(Row row, CellStyle style, int startCol, String... vals) {
        for (int i = 0; i < vals.length; i++) {
            org.apache.poi.ss.usermodel.Cell c = row.createCell(startCol + i); c.setCellValue(vals[i]); c.setCellStyle(style);
        }
    }

    private static void setNumCells(Row row, CellStyle numStyle, int startCol, double... vals) {
        for (int i = 0; i < vals.length; i++) {
            org.apache.poi.ss.usermodel.Cell c = row.createCell(startCol + i); c.setCellValue(vals[i]); c.setCellStyle(numStyle);
        }
    }

    private static void setCellStr(Row row, int col, String val, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell c = row.createCell(col); c.setCellValue(val); c.setCellStyle(style);
    }

    private static void setColWidths(Sheet sheet, int... widths) {
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i]);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
