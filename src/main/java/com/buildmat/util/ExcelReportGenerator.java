package com.buildmat.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExcelReportGenerator {

    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    // ── Style helpers ──────────────────────────────────────────────────────────
    private static CellStyle headerStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex()); f.setFontHeightInPoints((short)10);
        s.setFont(f); s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN); s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private static CellStyle titleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)16); f.setColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFont(f); return s;
    }

    private static CellStyle subTitleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setFontHeightInPoints((short)10); f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        s.setFont(f); return s;
    }

    private static CellStyle totalStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)10);
        s.setFont(f);
        s.setBorderTop(BorderStyle.MEDIUM);
        return s;
    }

    private static CellStyle altRowStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private static CellStyle amountStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        DataFormat df = wb.createDataFormat();
        s.setDataFormat(df.getFormat("#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private static CellStyle amountAltStyle(Workbook wb) {
        CellStyle s = amountStyle(wb);
        s.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private static CellStyle totalAmountStyle(Workbook wb) {
        CellStyle s = amountStyle(wb);
        s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)10);
        s.setFont(f); s.setBorderTop(BorderStyle.MEDIUM);
        return s;
    }

    private static void addTitle(Sheet sheet, Workbook wb, String title, String subtitle, int cols) {
        Row r1 = sheet.createRow(0);
        Cell c1 = r1.createCell(0); c1.setCellValue(title); c1.setCellStyle(titleStyle(wb));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, cols - 1));
        Row r2 = sheet.createRow(1);
        Cell c2 = r2.createCell(0); c2.setCellValue(subtitle); c2.setCellStyle(subTitleStyle(wb));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, cols - 1));
        sheet.createRow(2); // blank
    }

    private static Row addHeaderRow(Sheet sheet, Workbook wb, int rowNum, String... cols) {
        Row row = sheet.createRow(rowNum);
        CellStyle hs = headerStyle(wb);
        for (int i = 0; i < cols.length; i++) {
            Cell c = row.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(hs);
        }
        return row;
    }

    private static void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) sheet.autoSizeColumn(i);
    }

    private static double d(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number)o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }

    private static String s(Object o) { return o == null ? "" : o.toString(); }

    // ── 1. Sales Summary ──────────────────────────────────────────────────────
    public static String salesSummary(List<Map<String,Object>> rows, Map<String,Object> totals,
                                       String from, String to, String outputPath) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Sales Summary");
        sheet.setColumnWidth(0, 4000);

        String[] headers = {"Period","Invoices","Subtotal","SGST","CGST","Total GST","Total Amount","Paid","Outstanding"};
        addTitle(sheet, wb, "Sales Summary Report", "Period: " + from + " to " + to, headers.length);
        addHeaderRow(sheet, wb, 3, headers);

        CellStyle amt = amountStyle(wb); CellStyle amtAlt = amountAltStyle(wb);
        CellStyle def = wb.createCellStyle(); CellStyle alt = altRowStyle(wb);

        int r = 4;
        for (Map<String,Object> row : rows) {
            Row xr = sheet.createRow(r);
            boolean isAlt = (r % 2 == 0);
            setCell(xr, 0, s(row.get("period")),          isAlt ? alt : def);
            setCell(xr, 1, d(row.get("invoice_count")),   isAlt ? alt : def);
            setCell(xr, 2, d(row.get("subtotal")),         isAlt ? amtAlt : amt);
            setCell(xr, 3, d(row.get("sgst")),             isAlt ? amtAlt : amt);
            setCell(xr, 4, d(row.get("cgst")),             isAlt ? amtAlt : amt);
            setCell(xr, 5, d(row.get("total_gst")),        isAlt ? amtAlt : amt);
            setCell(xr, 6, d(row.get("total_amount")),     isAlt ? amtAlt : amt);
            setCell(xr, 7, d(row.get("paid_amount")),      isAlt ? amtAlt : amt);
            setCell(xr, 8, d(row.get("outstanding")),      isAlt ? amtAlt : amt);
            r++;
        }
        // Totals row
        CellStyle ts = totalStyle(wb); CellStyle ta = totalAmountStyle(wb);
        Row tr = sheet.createRow(r);
        setCell(tr, 0, "TOTAL", ts); setCell(tr, 1, d(totals.get("invoice_count")), ts);
        setCell(tr, 2, d(totals.get("subtotal")),   ta); setCell(tr, 3, d(totals.get("sgst")),      ta);
        setCell(tr, 4, d(totals.get("cgst")),       ta); setCell(tr, 5, d(totals.get("total_gst")), ta);
        setCell(tr, 6, d(totals.get("total_amount")),ta); setCell(tr, 7, d(totals.get("paid_amount")),ta);
        setCell(tr, 8, d(totals.get("outstanding")), ta);

        autoSize(sheet, headers.length);
        return save(wb, outputPath, "sales_summary_" + from + "_" + to + ".xlsx");
    }

    // ── 2. Outstanding Invoices ───────────────────────────────────────────────
    public static String outstandingInvoices(List<Map<String,Object>> rows, String asOf,
                                              String outputPath) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Outstanding Invoices");
        String[] headers = {"Invoice #","Customer","Phone","Invoice Date","Due Date","Total","Paid","Balance Due","Status","Flag","Days Overdue"};
        addTitle(sheet, wb, "Outstanding & Overdue Invoices", "As of: " + asOf, headers.length);
        addHeaderRow(sheet, wb, 3, headers);

        CellStyle amt = amountStyle(wb); CellStyle amtAlt = amountAltStyle(wb);
        CellStyle def = wb.createCellStyle(); CellStyle alt = altRowStyle(wb);
        CellStyle red = wb.createCellStyle();
        Font rf = wb.createFont(); rf.setColor(IndexedColors.RED.getIndex()); rf.setBold(true);
        red.setFont(rf);

        int r = 4;
        for (Map<String,Object> row : rows) {
            Row xr = sheet.createRow(r);
            boolean isAlt = r % 2 == 0;
            boolean overdue = "OVERDUE".equals(s(row.get("overdue_flag")));
            setCell(xr, 0,  s(row.get("invoice_number")),  overdue ? red : (isAlt ? alt : def));
            setCell(xr, 1,  s(row.get("customer_name")),   overdue ? red : (isAlt ? alt : def));
            setCell(xr, 2,  s(row.get("customer_phone")),  isAlt ? alt : def);
            setCell(xr, 3,  s(row.get("invoice_date")),    isAlt ? alt : def);
            setCell(xr, 4,  s(row.get("due_date")),        isAlt ? alt : def);
            setCell(xr, 5,  d(row.get("total_amount")),    isAlt ? amtAlt : amt);
            setCell(xr, 6,  d(row.get("paid_amount")),     isAlt ? amtAlt : amt);
            setCell(xr, 7,  d(row.get("balance_due")),     isAlt ? amtAlt : amt);
            setCell(xr, 8,  s(row.get("status")),          isAlt ? alt : def);
            setCell(xr, 9,  s(row.get("overdue_flag")),    overdue ? red : (isAlt ? alt : def));
            setCell(xr, 10, d(row.get("days_overdue")),    isAlt ? alt : def);
            r++;
        }
        autoSize(sheet, headers.length);
        return save(wb, outputPath, "outstanding_" + asOf + ".xlsx");
    }

    // ── 3. Customer-wise Sales ─────────────────────────────────────────────────
    public static String customerSales(List<Map<String,Object>> rows, String from, String to,
                                        String outputPath) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Customer Sales");
        String[] headers = {"Customer","Phone","Invoices","Subtotal","GST","Total Amount","Paid","Outstanding"};
        addTitle(sheet, wb, "Customer-wise Sales Report", "Period: " + from + " to " + to, headers.length);
        addHeaderRow(sheet, wb, 3, headers);

        CellStyle amt = amountStyle(wb); CellStyle amtAlt = amountAltStyle(wb);
        CellStyle def = wb.createCellStyle(); CellStyle alt = altRowStyle(wb);
        double[] totals = new double[5];

        int r = 4;
        for (Map<String,Object> row : rows) {
            Row xr = sheet.createRow(r);
            boolean isAlt = r % 2 == 0;
            setCell(xr, 0, s(row.get("customer_name")),  isAlt ? alt : def);
            setCell(xr, 1, s(row.get("customer_phone")), isAlt ? alt : def);
            setCell(xr, 2, d(row.get("invoice_count")),  isAlt ? alt : def);
            setCell(xr, 3, d(row.get("subtotal")),        isAlt ? amtAlt : amt);
            setCell(xr, 4, d(row.get("gst_amount")),      isAlt ? amtAlt : amt);
            setCell(xr, 5, d(row.get("total_amount")),    isAlt ? amtAlt : amt);
            setCell(xr, 6, d(row.get("paid_amount")),     isAlt ? amtAlt : amt);
            setCell(xr, 7, d(row.get("outstanding")),     isAlt ? amtAlt : amt);
            totals[0]+=d(row.get("subtotal")); totals[1]+=d(row.get("gst_amount"));
            totals[2]+=d(row.get("total_amount")); totals[3]+=d(row.get("paid_amount")); totals[4]+=d(row.get("outstanding"));
            r++;
        }
        CellStyle ts = totalStyle(wb); CellStyle ta = totalAmountStyle(wb);
        Row tr = sheet.createRow(r);
        setCell(tr, 0, "TOTAL", ts); setCell(tr, 1, "", ts); setCell(tr, 2, (double)rows.size(), ts);
        setCell(tr, 3, totals[0], ta); setCell(tr, 4, totals[1], ta);
        setCell(tr, 5, totals[2], ta); setCell(tr, 6, totals[3], ta); setCell(tr, 7, totals[4], ta);
        autoSize(sheet, headers.length);
        return save(wb, outputPath, "customer_sales_" + from + "_" + to + ".xlsx");
    }

    // ── 4. Product-wise Sales ──────────────────────────────────────────────────
    public static String productSales(List<Map<String,Object>> rows, String from, String to,
                                       String outputPath) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Product Sales");
        String[] headers = {"Product","Category","Unit","Total Qty","Avg Price","Total Sales","GST Collected","Invoices"};
        addTitle(sheet, wb, "Product-wise Sales Report", "Period: " + from + " to " + to, headers.length);
        addHeaderRow(sheet, wb, 3, headers);

        CellStyle amt = amountStyle(wb); CellStyle amtAlt = amountAltStyle(wb);
        CellStyle def = wb.createCellStyle(); CellStyle alt = altRowStyle(wb);
        double totalSales = 0, totalGst = 0;

        int r = 4;
        for (Map<String,Object> row : rows) {
            Row xr = sheet.createRow(r);
            boolean isAlt = r % 2 == 0;
            setCell(xr, 0, s(row.get("product_name")),  isAlt ? alt : def);
            setCell(xr, 1, s(row.get("category")),       isAlt ? alt : def);
            setCell(xr, 2, s(row.get("unit")),            isAlt ? alt : def);
            setCell(xr, 3, d(row.get("total_qty")),       isAlt ? amtAlt : amt);
            setCell(xr, 4, d(row.get("avg_price")),       isAlt ? amtAlt : amt);
            setCell(xr, 5, d(row.get("total_sales")),     isAlt ? amtAlt : amt);
            setCell(xr, 6, d(row.get("gst_collected")),   isAlt ? amtAlt : amt);
            setCell(xr, 7, d(row.get("invoice_count")),   isAlt ? alt : def);
            totalSales += d(row.get("total_sales")); totalGst += d(row.get("gst_collected"));
            r++;
        }
        CellStyle ts = totalStyle(wb); CellStyle ta = totalAmountStyle(wb);
        Row tr = sheet.createRow(r);
        setCell(tr, 0, "TOTAL", ts); setCell(tr, 1, "", ts); setCell(tr, 2, "", ts);
        setCell(tr, 3, "", ts); setCell(tr, 4, "", ts);
        setCell(tr, 5, totalSales, ta); setCell(tr, 6, totalGst, ta); setCell(tr, 7, "", ts);
        autoSize(sheet, headers.length);
        return save(wb, outputPath, "product_sales_" + from + "_" + to + ".xlsx");
    }

    // ── 5. GST Report ─────────────────────────────────────────────────────────
    public static String gstReport(List<Map<String,Object>> monthly, List<Map<String,Object>> byProduct,
                                    String from, String to, String outputPath) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();

        // Sheet 1: Monthly GST
        Sheet s1 = wb.createSheet("Monthly GST");
        String[] h1 = {"Period","Invoices","Taxable Value","SGST","CGST","Total GST","Total with GST"};
        addTitle(s1, wb, "GST Report — Monthly", "Period: " + from + " to " + to, h1.length);
        addHeaderRow(s1, wb, 3, h1);
        CellStyle amt = amountStyle(wb); CellStyle amtAlt = amountAltStyle(wb);
        CellStyle def = wb.createCellStyle(); CellStyle alt = altRowStyle(wb);
        double[] t1 = new double[5];
        int r = 4;
        for (Map<String,Object> row : monthly) {
            Row xr = s1.createRow(r); boolean isAlt = r % 2 == 0;
            setCell(xr,0,s(row.get("period")),isAlt?alt:def);    setCell(xr,1,d(row.get("invoice_count")),isAlt?alt:def);
            setCell(xr,2,d(row.get("taxable_value")),isAlt?amtAlt:amt); setCell(xr,3,d(row.get("sgst_amount")),isAlt?amtAlt:amt);
            setCell(xr,4,d(row.get("cgst_amount")),isAlt?amtAlt:amt);   setCell(xr,5,d(row.get("total_gst")),isAlt?amtAlt:amt);
            setCell(xr,6,d(row.get("total_with_gst")),isAlt?amtAlt:amt);
            t1[0]+=d(row.get("taxable_value")); t1[1]+=d(row.get("sgst_amount")); t1[2]+=d(row.get("cgst_amount"));
            t1[3]+=d(row.get("total_gst")); t1[4]+=d(row.get("total_with_gst")); r++;
        }
        CellStyle ts = totalStyle(wb); CellStyle ta = totalAmountStyle(wb);
        Row tr1 = s1.createRow(r);
        setCell(tr1,0,"TOTAL",ts); setCell(tr1,1,(double)monthly.size(),ts);
        setCell(tr1,2,t1[0],ta); setCell(tr1,3,t1[1],ta); setCell(tr1,4,t1[2],ta); setCell(tr1,5,t1[3],ta); setCell(tr1,6,t1[4],ta);
        autoSize(s1, h1.length);

        // Sheet 2: Product GST
        Sheet s2 = wb.createSheet("GST by Product");
        String[] h2 = {"Product","Category","Taxable Value","SGST %","CGST %","SGST Amount","CGST Amount","Total GST"};
        addTitle(s2, wb, "GST Report — Product-wise", "Period: " + from + " to " + to, h2.length);
        addHeaderRow(s2, wb, 3, h2);
        double[] t2 = new double[3];
        r = 4;
        for (Map<String,Object> row : byProduct) {
            Row xr = s2.createRow(r); boolean isAlt = r % 2 == 0;
            setCell(xr,0,s(row.get("product_name")),isAlt?alt:def); setCell(xr,1,s(row.get("category")),isAlt?alt:def);
            setCell(xr,2,d(row.get("taxable_value")),isAlt?amtAlt:amt); setCell(xr,3,d(row.get("sgst_percent")),isAlt?alt:def);
            setCell(xr,4,d(row.get("cgst_percent")),isAlt?alt:def); setCell(xr,5,d(row.get("sgst_amount")),isAlt?amtAlt:amt);
            setCell(xr,6,d(row.get("cgst_amount")),isAlt?amtAlt:amt); setCell(xr,7,d(row.get("total_gst")),isAlt?amtAlt:amt);
            t2[0]+=d(row.get("sgst_amount")); t2[1]+=d(row.get("cgst_amount")); t2[2]+=d(row.get("total_gst")); r++;
        }
        Row tr2 = s2.createRow(r);
        setCell(tr2,0,"TOTAL",ts); setCell(tr2,1,"",ts); setCell(tr2,2,"",ts); setCell(tr2,3,"",ts); setCell(tr2,4,"",ts);
        setCell(tr2,5,t2[0],ta); setCell(tr2,6,t2[1],ta); setCell(tr2,7,t2[2],ta);
        autoSize(s2, h2.length);

        return save(wb, outputPath, "gst_report_" + from + "_" + to + ".xlsx");
    }

    // ── 6. Payment Collection ─────────────────────────────────────────────────
    public static String paymentCollection(List<Map<String,Object>> rows, List<Map<String,Object>> methodSummary,
                                            String from, String to, String outputPath) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Payment Collection");
        String[] headers = {"Date","Invoice #","Customer","Amount","Method","Reference","Notes"};
        addTitle(sheet, wb, "Payment Collection Report", "Period: " + from + " to " + to, headers.length);
        addHeaderRow(sheet, wb, 3, headers);

        CellStyle amt = amountStyle(wb); CellStyle amtAlt = amountAltStyle(wb);
        CellStyle def = wb.createCellStyle(); CellStyle alt = altRowStyle(wb);
        double total = 0;

        int r = 4;
        for (Map<String,Object> row : rows) {
            Row xr = sheet.createRow(r); boolean isAlt = r % 2 == 0;
            setCell(xr,0,s(row.get("payment_date")),isAlt?alt:def);
            setCell(xr,1,s(row.get("invoice_number")),isAlt?alt:def);
            setCell(xr,2,s(row.get("customer_name")),isAlt?alt:def);
            setCell(xr,3,d(row.get("amount")),isAlt?amtAlt:amt);
            setCell(xr,4,s(row.get("method")),isAlt?alt:def);
            setCell(xr,5,s(row.get("reference")),isAlt?alt:def);
            setCell(xr,6,s(row.get("notes")),isAlt?alt:def);
            total += d(row.get("amount")); r++;
        }
        CellStyle ts = totalStyle(wb); CellStyle ta = totalAmountStyle(wb);
        Row tr = sheet.createRow(r);
        setCell(tr,0,"TOTAL",ts); setCell(tr,1,"",ts); setCell(tr,2,"",ts);
        setCell(tr,3,total,ta); setCell(tr,4,"",ts); setCell(tr,5,"",ts); setCell(tr,6,"",ts);

        // Method summary sheet
        Sheet s2 = wb.createSheet("By Method");
        addTitle(s2, wb, "Collection by Method", "Period: " + from + " to " + to, 3);
        addHeaderRow(s2, wb, 3, "Method", "Count", "Total Amount");
        r = 4;
        for (Map<String,Object> row : methodSummary) {
            Row xr = s2.createRow(r++);
            setCell(xr,0,s(row.get("method")),def);
            setCell(xr,1,d(row.get("count")),def);
            setCell(xr,2,d(row.get("total_amount")),amt);
        }
        autoSize(sheet, headers.length); autoSize(s2, 3);
        return save(wb, outputPath, "payment_collection_" + from + "_" + to + ".xlsx");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private static void setCell(Row row, int col, String val, CellStyle style) {
        Cell c = row.createCell(col); c.setCellValue(val == null ? "" : val); c.setCellStyle(style);
    }
    private static void setCell(Row row, int col, double val, CellStyle style) {
        Cell c = row.createCell(col); c.setCellValue(val); c.setCellStyle(style);
    }

    private static String save(Workbook wb, String dir, String filename) throws Exception {
        String path = dir + java.io.File.separator + filename;
        try (FileOutputStream fos = new FileOutputStream(path)) { wb.write(fos); }
        wb.close();
        return path;
    }
}
