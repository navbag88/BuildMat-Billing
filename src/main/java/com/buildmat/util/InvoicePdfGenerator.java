package com.buildmat.util;

import com.buildmat.model.Invoice;
import com.buildmat.model.InvoiceItem;
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
import com.itextpdf.layout.properties.*;

import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;

public class InvoicePdfGenerator {

    private static final DeviceRgb BRAND_BLUE = new DeviceRgb(37, 99, 235);
    private static final DeviceRgb DARK = new DeviceRgb(26, 35, 50);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(243, 244, 246);
    private static final DeviceRgb MID_GRAY = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb GREEN = new DeviceRgb(22, 163, 74);
    private static final DeviceRgb ORANGE = new DeviceRgb(217, 119, 6);
    private static final DeviceRgb RED = new DeviceRgb(220, 38, 38);
    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public static String generate(Invoice invoice, String outputDir) throws Exception {
        String fileName = outputDir + File.separator + invoice.getInvoiceNumber().replace("/", "-") + ".pdf";
        PdfWriter writer = new PdfWriter(fileName);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(40, 50, 40, 50);

        PdfFont bold = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        // ─── Header ───────────────────────────────────────────────
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
        headerTable.setBorder(Border.NO_BORDER);

        Cell companyCell = new Cell().setBorder(Border.NO_BORDER);
        companyCell.add(new Paragraph("🏗 BuildMat Supplies").setFont(bold).setFontSize(22).setFontColor(BRAND_BLUE));
        companyCell.add(new Paragraph("Building Material Supplier").setFont(regular).setFontSize(11).setFontColor(MID_GRAY));
        companyCell.add(new Paragraph("GST No: 27ABCDE1234F1Z5").setFont(regular).setFontSize(10).setFontColor(MID_GRAY));
        companyCell.add(new Paragraph("Phone: +91 98765 43210").setFont(regular).setFontSize(10).setFontColor(MID_GRAY));
        headerTable.addCell(companyCell);

        Cell invoiceInfoCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        invoiceInfoCell.add(new Paragraph("TAX INVOICE").setFont(bold).setFontSize(18).setFontColor(DARK));
        invoiceInfoCell.add(new Paragraph(invoice.getInvoiceNumber()).setFont(bold).setFontSize(13).setFontColor(BRAND_BLUE));
        invoiceInfoCell.add(new Paragraph("Date: " + invoice.getInvoiceDate()).setFont(regular).setFontSize(10).setFontColor(MID_GRAY));
        if (invoice.getDueDate() != null)
            invoiceInfoCell.add(new Paragraph("Due: " + invoice.getDueDate()).setFont(regular).setFontSize(10).setFontColor(MID_GRAY));

        // Status badge color
        DeviceRgb statusColor = switch (invoice.getStatus()) {
            case "PAID" -> GREEN; case "PARTIAL" -> ORANGE; default -> RED;
        };
        invoiceInfoCell.add(new Paragraph(invoice.getStatus()).setFont(bold).setFontSize(11).setFontColor(statusColor));
        headerTable.addCell(invoiceInfoCell);

        doc.add(headerTable);
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f)).setMarginTop(8).setMarginBottom(12));

        // ─── Bill To ──────────────────────────────────────────────
        if (invoice.getCustomer() != null) {
            Table billTo = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
            billTo.setBorder(Border.NO_BORDER);

            Cell btCell = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(LIGHT_GRAY).setPadding(12).setBorderRadius(new BorderRadius(6));
            btCell.add(new Paragraph("Bill To").setFont(bold).setFontSize(10).setFontColor(MID_GRAY));
            btCell.add(new Paragraph(invoice.getCustomer().getName()).setFont(bold).setFontSize(13).setFontColor(DARK));
            if (invoice.getCustomer().getPhone() != null && !invoice.getCustomer().getPhone().isBlank())
                btCell.add(new Paragraph(invoice.getCustomer().getPhone()).setFont(regular).setFontSize(10).setFontColor(MID_GRAY));
            if (invoice.getCustomer().getEmail() != null && !invoice.getCustomer().getEmail().isBlank())
                btCell.add(new Paragraph(invoice.getCustomer().getEmail()).setFont(regular).setFontSize(10).setFontColor(MID_GRAY));
            if (invoice.getCustomer().getAddress() != null && !invoice.getCustomer().getAddress().isBlank())
                btCell.add(new Paragraph(invoice.getCustomer().getAddress()).setFont(regular).setFontSize(10).setFontColor(MID_GRAY));
            billTo.addCell(btCell);
            billTo.addCell(new Cell().setBorder(Border.NO_BORDER));
            doc.add(billTo);
            doc.add(new Paragraph(" "));
        }

        // ─── Items Table ──────────────────────────────────────────
        boolean showGst = invoice.isIncludeGst();
        float[] colWidths = showGst
            ? new float[]{4, 28, 9, 10, 12, 9, 9, 13, 6}
            : new float[]{5, 35, 12, 18, 15, 15};
        Table items = new Table(UnitValue.createPercentArray(colWidths)).useAllAvailableWidth();
        items.setBorder(new SolidBorder(new DeviceRgb(229, 231, 235), 1));

        String[] headers = showGst
            ? new String[]{"#", "Description", "Unit", "Qty", "Unit Price", "SGST%", "CGST%", "GST Amt", "Total"}
            : new String[]{"#", "Description", "Unit", "Qty", "Unit Price", "Total"};
        TextAlignment[] aligns = showGst
            ? new TextAlignment[]{TextAlignment.CENTER, TextAlignment.LEFT, TextAlignment.CENTER,
                                   TextAlignment.RIGHT, TextAlignment.RIGHT, TextAlignment.RIGHT,
                                   TextAlignment.RIGHT, TextAlignment.RIGHT, TextAlignment.RIGHT}
            : new TextAlignment[]{TextAlignment.CENTER, TextAlignment.LEFT, TextAlignment.CENTER,
                                   TextAlignment.RIGHT, TextAlignment.RIGHT, TextAlignment.RIGHT};

        for (int i = 0; i < headers.length; i++) {
            Cell h = new Cell().setBackgroundColor(BRAND_BLUE).setPadding(7).setBorder(Border.NO_BORDER);
            h.add(new Paragraph(headers[i]).setFont(bold).setFontSize(9).setFontColor(ColorConstants.WHITE).setTextAlignment(aligns[i]));
            items.addCell(h);
        }

        int rowNum = 1;
        for (InvoiceItem item : invoice.getItems()) {
            DeviceRgb rowBg = rowNum % 2 == 0 ? LIGHT_GRAY : new DeviceRgb(255, 255, 255);
            if (showGst) {
                addItemRow(items, rowBg, regular, aligns,
                        String.valueOf(rowNum++),
                        item.getProductName(),
                        item.getUnit() != null ? item.getUnit() : "",
                        formatNum(item.getQuantity()),
                        INR.format(item.getUnitPrice()),
                        item.getSgstPercent() + "%",
                        item.getCgstPercent() + "%",
                        INR.format(item.getTotalGstAmount()),
                        INR.format(item.getTotal()));
            } else {
                addItemRow(items, rowBg, regular, aligns,
                        String.valueOf(rowNum++),
                        item.getProductName(),
                        item.getUnit() != null ? item.getUnit() : "",
                        formatNum(item.getQuantity()),
                        INR.format(item.getUnitPrice()),
                        INR.format(item.getTotal()));
            }
        }
        doc.add(items);
        doc.add(new Paragraph(" "));

        // ─── Totals ───────────────────────────────────────────────
        Table totals = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
        totals.setBorder(Border.NO_BORDER);

        Cell emptyCell = new Cell().setBorder(Border.NO_BORDER);
        if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
            emptyCell.add(new Paragraph("Notes").setFont(bold).setFontSize(10).setFontColor(MID_GRAY));
            emptyCell.add(new Paragraph(invoice.getNotes()).setFont(regular).setFontSize(10).setFontColor(DARK));
        }
        totals.addCell(emptyCell);

        Table totalsInner = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        totalsInner.setBorder(new SolidBorder(new DeviceRgb(229, 231, 235), 1));
        addTotalRow(totalsInner, regular, bold, "Subtotal", INR.format(invoice.getSubtotal()), false);
        if (invoice.isIncludeGst()) {
            addTotalRow(totalsInner, regular, bold, "SGST", INR.format(invoice.getSgstAmount()), false);
            addTotalRow(totalsInner, regular, bold, "CGST", INR.format(invoice.getCgstAmount()), false);
            addTotalRow(totalsInner, regular, bold, "Total GST", INR.format(invoice.getTaxAmount()), false);
        }
        addTotalRow(totalsInner, regular, bold, "Grand Total", INR.format(invoice.getTotalAmount()), true);
        addTotalRow(totalsInner, regular, bold, "Paid", INR.format(invoice.getPaidAmount()), false);
        addTotalRow(totalsInner, regular, bold, "Balance Due", INR.format(invoice.getBalanceDue()), false);
        totals.addCell(new Cell().setBorder(Border.NO_BORDER).add(totalsInner));

        doc.add(totals);

        // ─── Footer ───────────────────────────────────────────────
        doc.add(new Paragraph(" "));
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f)).setMarginBottom(8));
        doc.add(new Paragraph("Thank you for your business! For queries, contact us at billing@buildmat.in")
                .setFont(regular).setFontSize(9).setFontColor(MID_GRAY).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("This is a computer-generated invoice and does not require a signature.")
                .setFont(regular).setFontSize(8).setFontColor(MID_GRAY).setTextAlignment(TextAlignment.CENTER));

        doc.close();
        return fileName;
    }

    private static void addItemRow(Table table, DeviceRgb bg, PdfFont font, TextAlignment[] aligns, String... cols) {
        for (int i = 0; i < cols.length; i++) {
            Cell cell = new Cell().setBackgroundColor(bg).setPadding(7).setBorder(Border.NO_BORDER);
            cell.add(new Paragraph(cols[i]).setFont(font).setFontSize(10).setFontColor(DARK).setTextAlignment(aligns[i]));
            table.addCell(cell);
        }
    }

    private static void addTotalRow(Table table, PdfFont regular, PdfFont bold, String label, String value, boolean highlight) {
        DeviceRgb bg = highlight ? BRAND_BLUE : new DeviceRgb(255, 255, 255);
        DeviceRgb fg = highlight ? new DeviceRgb(255, 255, 255) : DARK;
        PdfFont font = highlight ? bold : regular;
        float size = highlight ? 12 : 10;

        Cell lCell = new Cell().setBackgroundColor(bg).setPadding(7).setBorder(Border.NO_BORDER);
        lCell.add(new Paragraph(label).setFont(font).setFontSize(size).setFontColor(fg));
        Cell vCell = new Cell().setBackgroundColor(bg).setPadding(7).setBorder(Border.NO_BORDER);
        vCell.add(new Paragraph(value).setFont(font).setFontSize(size).setFontColor(fg).setTextAlignment(TextAlignment.RIGHT));
        table.addCell(lCell);
        table.addCell(vCell);
    }

    private static String formatNum(double v) {
        return v == (long) v ? String.valueOf((long) v) : String.valueOf(v);
    }
}
