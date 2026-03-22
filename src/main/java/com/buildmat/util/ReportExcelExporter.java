package com.buildmat.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportExcelExporter {

    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public static String export(String reportTitle, String period,
                                 List<String> headers, List<List<Object>> rows,
                                 String outputDir) throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet(reportTitle.length() > 31 ? reportTitle.substring(0, 31) : reportTitle);

        // ── Styles ────────────────────────────────────────────────
        CellStyle titleStyle = wb.createCellStyle();
        XSSFFont titleFont = wb.createFont();
        titleFont.setBold(true); titleFont.setFontHeightInPoints((short)16);
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleStyle.setFont(titleFont);
        titleStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)26, (byte)35, (byte)50}, null));
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.LEFT);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle subStyle = wb.createCellStyle();
        XSSFFont subFont = wb.createFont();
        subFont.setItalic(true); subFont.setFontHeightInPoints((short)10);
        subFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        subStyle.setFont(subFont);

        CellStyle headerStyle = wb.createCellStyle();
        XSSFFont headerFont = wb.createFont();
        headerFont.setBold(true); headerFont.setFontHeightInPoints((short)10);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)37, (byte)99, (byte)235}, null));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBottomBorderColor(IndexedColors.WHITE.getIndex());

        CellStyle evenStyle = wb.createCellStyle();
        evenStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)243, (byte)244, (byte)246}, null));
        evenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        evenStyle.setBorderBottom(BorderStyle.HAIR);
        evenStyle.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());

        CellStyle oddStyle = wb.createCellStyle();
        oddStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null));
        oddStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        oddStyle.setBorderBottom(BorderStyle.HAIR);
        oddStyle.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());

        CellStyle totalStyle = wb.createCellStyle();
        XSSFFont totalFont = wb.createFont();
        totalFont.setBold(true); totalFont.setFontHeightInPoints((short)10);
        totalStyle.setFont(totalFont);
        totalStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)219, (byte)234, (byte)254}, null));
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalStyle.setBorderTop(BorderStyle.MEDIUM);
        totalStyle.setTopBorderColor(IndexedColors.BLUE.getIndex());

        CellStyle currencyStyle = wb.createCellStyle();
        currencyStyle.cloneStyleFrom(oddStyle);
        DataFormat fmt = wb.createDataFormat();
        currencyStyle.setDataFormat(fmt.getFormat("\"₹\"#,##0.00"));
        currencyStyle.setAlignment(HorizontalAlignment.RIGHT);

        CellStyle currencyEvenStyle = wb.createCellStyle();
        currencyEvenStyle.cloneStyleFrom(evenStyle);
        currencyEvenStyle.setDataFormat(fmt.getFormat("\"₹\"#,##0.00"));
        currencyEvenStyle.setAlignment(HorizontalAlignment.RIGHT);

        CellStyle currencyTotalStyle = wb.createCellStyle();
        currencyTotalStyle.cloneStyleFrom(totalStyle);
        currencyTotalStyle.setDataFormat(fmt.getFormat("\"₹\"#,##0.00"));
        currencyTotalStyle.setAlignment(HorizontalAlignment.RIGHT);

        // ── Title row ─────────────────────────────────────────────
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(30);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BuildMat Supplies — " + reportTitle);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.size() - 1));

        Row periodRow = sheet.createRow(1);
        Cell periodCell = periodRow.createCell(0);
        periodCell.setCellValue("Period: " + period + "   Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        periodCell.setCellStyle(subStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, headers.size() - 1));

        sheet.createRow(2); // blank row

        // ── Header row ────────────────────────────────────────────
        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(20);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }

        // ── Data rows ─────────────────────────────────────────────
        Set<Integer> currencyCols = detectCurrencyColumns(headers);
        Set<Integer> numericCols  = detectNumericColumns(headers);
        double[] colTotals = new double[headers.size()];

        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 4);
            List<Object> data = rows.get(r);
            boolean even = r % 2 == 0;
            for (int c = 0; c < data.size() && c < headers.size(); c++) {
                Cell cell = row.createCell(c);
                Object val = data.get(c);
                if (val == null) {
                    cell.setCellValue("—");
                    cell.setCellStyle(even ? evenStyle : oddStyle);
                } else if (currencyCols.contains(c)) {
                    double d = toDouble(val);
                    cell.setCellValue(d);
                    cell.setCellStyle(even ? currencyEvenStyle : currencyStyle);
                    colTotals[c] += d;
                } else if (numericCols.contains(c)) {
                    double d = toDouble(val);
                    cell.setCellValue(d);
                    cell.setCellStyle(even ? evenStyle : oddStyle);
                    colTotals[c] += d;
                } else {
                    cell.setCellValue(String.valueOf(val));
                    cell.setCellStyle(even ? evenStyle : oddStyle);
                }
            }
        }

        // ── Totals row ────────────────────────────────────────────
        if (!rows.isEmpty()) {
            Row totalRow = sheet.createRow(rows.size() + 4);
            totalRow.setHeightInPoints(18);
            Cell labelCell = totalRow.createCell(0);
            labelCell.setCellValue("TOTAL");
            labelCell.setCellStyle(totalStyle);
            for (int c = 1; c < headers.size(); c++) {
                Cell cell = totalRow.createCell(c);
                if (currencyCols.contains(c)) {
                    cell.setCellValue(colTotals[c]);
                    cell.setCellStyle(currencyTotalStyle);
                } else if (numericCols.contains(c)) {
                    cell.setCellValue(colTotals[c]);
                    cell.setCellStyle(totalStyle);
                } else {
                    cell.setCellStyle(totalStyle);
                }
            }
        }

        // ── Auto-size columns ─────────────────────────────────────
        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
            int w = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(w + 512, 8000));
        }

        // ── Save ──────────────────────────────────────────────────
        String safe = reportTitle.replaceAll("[^a-zA-Z0-9_\\- ]", "").replace(" ", "_");
        String path = outputDir + File.separator + safe + "_" + LocalDate.now() + ".xlsx";
        try (FileOutputStream fos = new FileOutputStream(path)) { wb.write(fos); }
        wb.close();
        return path;
    }

    private static Set<Integer> detectCurrencyColumns(List<String> headers) {
        Set<Integer> cols = new HashSet<>();
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i).toLowerCase();
            if (h.contains("amount") || h.contains("total") || h.contains("subtotal") ||
                h.contains("balance") || h.contains("paid") || h.contains("outstanding") ||
                h.contains("gst") || h.contains("sgst") || h.contains("cgst") ||
                h.contains("price") || h.contains("collected") || h.contains("revenue") ||
                h.contains("value") || h.contains("invoice total"))
                cols.add(i);
        }
        return cols;
    }

    private static Set<Integer> detectNumericColumns(List<String> headers) {
        Set<Integer> cols = new HashSet<>();
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i).toLowerCase();
            if (h.contains("count") || h.contains("qty") || h.contains("quantity"))
                cols.add(i);
        }
        return cols;
    }

    private static double toDouble(Object v) {
        if (v == null) return 0;
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0; }
    }
}
