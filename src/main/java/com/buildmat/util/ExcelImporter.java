package com.buildmat.util;

import com.buildmat.model.Customer;
import com.buildmat.model.Product;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelImporter {

    // ── Result wrapper ─────────────────────────────────────────────────────────
    public static class ImportResult<T> {
        public final List<T> imported = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
        public int skipped = 0;

        public boolean hasErrors() { return !errors.isEmpty(); }
        public String summary() {
            return "Imported: " + imported.size()
                + (skipped > 0 ? "  |  Skipped: " + skipped : "")
                + (errors.isEmpty() ? "" : "  |  Errors: " + errors.size());
        }
    }

    // ── Customer import ────────────────────────────────────────────────────────
    // Expected columns: Name*, Phone, Email, Address
    public static ImportResult<Customer> importCustomers(String filePath) throws Exception {
        ImportResult<Customer> result = new ImportResult<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheetAt(0);
            int[] colIdx = findCustomerColumns(sheet.getRow(0));

            if (colIdx[0] == -1) {
                result.errors.add("Column 'Name' not found. Please use the provided template.");
                return result;
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) { result.skipped++; continue; }

                String name = cellStr(row, colIdx[0]).trim();
                if (name.isEmpty()) {
                    result.errors.add("Row " + (r + 1) + ": Name is required — skipped.");
                    result.skipped++; continue;
                }

                Customer c = new Customer();
                c.setName(name);
                c.setPhone(colIdx[1] >= 0 ? cellStr(row, colIdx[1]) : "");
                c.setEmail(colIdx[2] >= 0 ? cellStr(row, colIdx[2]) : "");
                c.setAddress(colIdx[3] >= 0 ? cellStr(row, colIdx[3]) : "");
                result.imported.add(c);
            }
        }
        return result;
    }

    // ── Product import ─────────────────────────────────────────────────────────
    // Expected columns: Name*, Category, Unit*, Price*, Stock Qty, SGST %, CGST %
    public static ImportResult<Product> importProducts(String filePath) throws Exception {
        ImportResult<Product> result = new ImportResult<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheetAt(0);
            int[] colIdx = findProductColumns(sheet.getRow(0));

            if (colIdx[0] == -1) {
                result.errors.add("Column 'Name' not found. Please use the provided template.");
                return result;
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) { result.skipped++; continue; }

                String name = cellStr(row, colIdx[0]).trim();
                if (name.isEmpty()) {
                    result.errors.add("Row " + (r + 1) + ": Product Name is required — skipped.");
                    result.skipped++; continue;
                }

                double price = colIdx[3] >= 0 ? cellNum(row, colIdx[3]) : 0;
                if (price <= 0) {
                    result.errors.add("Row " + (r + 1) + " (" + name + "): Price must be > 0 — skipped.");
                    result.skipped++; continue;
                }

                Product p = new Product();
                p.setName(name);
                p.setCategory(colIdx[1] >= 0 ? cellStr(row, colIdx[1]) : "");
                p.setUnit(colIdx[2] >= 0 ? cellStr(row, colIdx[2]) : "Unit");
                p.setPrice(price);
                p.setStockQty(colIdx[4] >= 0 ? cellNum(row, colIdx[4]) : 0);
                p.setSgstPercent(colIdx[5] >= 0 ? cellNum(row, colIdx[5]) : 0);
                p.setCgstPercent(colIdx[6] >= 0 ? cellNum(row, colIdx[6]) : 0);
                result.imported.add(p);
            }
        }
        return result;
    }

    // ── Template generators ────────────────────────────────────────────────────
    public static String generateCustomerTemplate(String outputDir) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Customers");

            // Header style
            CellStyle headerStyle = headerStyle(wb);
            CellStyle instrStyle  = instrStyle(wb);
            CellStyle sampleStyle = sampleStyle(wb);
            CellStyle reqStyle    = reqStyle(wb);

            // Instructions row
            Row instr = sheet.createRow(0);
            Cell instrCell = instr.createCell(0);
            instrCell.setCellValue("INSTRUCTIONS: Fill customer details below. Required fields are marked with *. Do not change column headers. Row 3 onwards = your data.");
            instrCell.setCellStyle(instrStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));
            instr.setHeightInPoints(28);

            // Header row
            Row header = sheet.createRow(1);
            String[] cols    = {"Name *",  "Phone",        "Email",          "Address"};
            boolean[] req    = {true,       false,          false,            false};
            int[] widths     = {7000,       4000,           6000,             8000};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(req[i] ? reqStyle : headerStyle);
                sheet.setColumnWidth(i, widths[i]);
            }
            header.setHeightInPoints(22);

            // Sample rows
            String[][] samples = {
                {"Ravi Kumar",     "9876543210", "ravi@example.com",  "MG Road, Bengaluru"},
                {"Meena Builders", "9123456780", "meena@builders.in", "Whitefield, Bengaluru"},
                {"Shree Constructions", "",      "",                  "HSR Layout, Bengaluru"}
            };
            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 2);
                for (int c = 0; c < samples[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(samples[r][c]);
                    cell.setCellStyle(sampleStyle);
                }
            }

            String path = outputDir + java.io.File.separator + "customer_import_template.xlsx";
            try (FileOutputStream fos = new FileOutputStream(path)) { wb.write(fos); }
            return path;
        }
    }

    public static String generateProductTemplate(String outputDir) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Products");

            CellStyle headerStyle = headerStyle(wb);
            CellStyle instrStyle  = instrStyle(wb);
            CellStyle sampleStyle = sampleStyle(wb);
            CellStyle reqStyle    = reqStyle(wb);
            CellStyle numStyle    = numStyle(wb);

            // Instructions
            Row instr = sheet.createRow(0);
            Cell instrCell = instr.createCell(0);
            instrCell.setCellValue("INSTRUCTIONS: Fill product details below. Required fields (*) must not be empty. SGST% and CGST% are separate (e.g. 9 and 9 for 18% GST). Price must be > 0.");
            instrCell.setCellStyle(instrStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 6));
            instr.setHeightInPoints(28);

            // Header row
            String[] cols  = {"Name *",              "Category",    "Unit *",  "Price *",  "Stock Qty", "SGST %",   "CGST %"};
            boolean[] req  = {true,                   false,         true,      true,       false,       false,      false};
            int[] widths   = {8000,                   4000,          3000,      3500,       3500,        3000,       3000};
            Row header = sheet.createRow(1);
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(req[i] ? reqStyle : headerStyle);
                sheet.setColumnWidth(i, widths[i]);
            }
            header.setHeightInPoints(22);

            // Sample rows
            Object[][] samples = {
                {"OPC Cement 53 Grade", "Cement",    "Bag",    380.00, 500.0, 9.0, 9.0},
                {"TMT Steel Bar 10mm",  "Steel",     "Kg",      68.00, 2000.0,9.0, 9.0},
                {"Red Bricks",          "Bricks",    "Piece",    8.50, 10000.0,6.0, 6.0},
                {"River Sand",          "Sand",      "CFT",     45.00, 800.0, 6.0, 6.0},
                {"PVC Pipe 4 inch",     "Plumbing",  "Piece",  320.00, 150.0, 9.0, 9.0}
            };
            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 2);
                for (int c = 0; c < samples[r].length; c++) {
                    Cell cell = row.createCell(c);
                    if (samples[r][c] instanceof Number) {
                        cell.setCellValue(((Number) samples[r][c]).doubleValue());
                        cell.setCellStyle(numStyle);
                    } else {
                        cell.setCellValue(samples[r][c].toString());
                        cell.setCellStyle(sampleStyle);
                    }
                }
            }

            // Unit dropdown via data validation
            DataValidationHelper dvh = sheet.getDataValidationHelper();
            DataValidationConstraint dvc = dvh.createExplicitListConstraint(
                new String[]{"Bag","Kg","Piece","CFT","Sq.Ft","Meter","Liter","Box","Roll","Ton","Set"});
            DataValidation dv = dvh.createValidation(dvc,
                new org.apache.poi.ss.util.CellRangeAddressList(2, 500, 2, 2));
            dv.setShowErrorBox(true);
            dv.setErrorStyle(DataValidation.ErrorStyle.STOP);
            dv.createErrorBox("Invalid Unit", "Please select from the dropdown or type a valid unit.");
            sheet.addValidationData(dv);

            String path = outputDir + java.io.File.separator + "product_import_template.xlsx";
            try (FileOutputStream fos = new FileOutputStream(path)) { wb.write(fos); }
            return path;
        }
    }

    // ── Column detection (flexible — works even if columns are reordered) ──────
    private static int[] findCustomerColumns(Row headerRow) {
        // [name, phone, email, address]
        int[] idx = {-1, -1, -1, -1};
        if (headerRow == null) return idx;
        for (Cell c : headerRow) {
            String h = cellStr(c).toLowerCase().replaceAll("[^a-z]", "");
            if (h.contains("name"))    idx[0] = c.getColumnIndex();
            else if (h.contains("phone") || h.contains("mobile")) idx[1] = c.getColumnIndex();
            else if (h.contains("email")) idx[2] = c.getColumnIndex();
            else if (h.contains("address")) idx[3] = c.getColumnIndex();
        }
        return idx;
    }

    private static int[] findProductColumns(Row headerRow) {
        // [name, category, unit, price, stock, sgst, cgst]
        int[] idx = {-1, -1, -1, -1, -1, -1, -1};
        if (headerRow == null) return idx;
        for (Cell c : headerRow) {
            String h = cellStr(c).toLowerCase().replaceAll("[^a-z]", "");
            if (h.contains("name") && !h.contains("cat")) idx[0] = c.getColumnIndex();
            else if (h.contains("category") || h.contains("cat")) idx[1] = c.getColumnIndex();
            else if (h.contains("unit"))  idx[2] = c.getColumnIndex();
            else if (h.contains("price")) idx[3] = c.getColumnIndex();
            else if (h.contains("stock") || h.contains("qty")) idx[4] = c.getColumnIndex();
            else if (h.contains("sgst"))  idx[5] = c.getColumnIndex();
            else if (h.contains("cgst"))  idx[6] = c.getColumnIndex();
        }
        return idx;
    }

    // ── Style factories ────────────────────────────────────────────────────────
    private static CellStyle headerStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex()); f.setFontHeightInPoints((short)11);
        s.setFont(f); s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN); s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private static CellStyle reqStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex()); f.setFontHeightInPoints((short)11);
        s.setFont(f); s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.MEDIUM); s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN); s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private static CellStyle instrStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont(); f.setItalic(true); f.setFontHeightInPoints((short)10);
        f.setColor(IndexedColors.DARK_RED.getIndex());
        s.setFont(f); s.setWrapText(true);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private static CellStyle sampleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont(); f.setColor(IndexedColors.GREY_50_PERCENT.getIndex()); f.setFontHeightInPoints((short)10);
        s.setFont(f);
        s.setBorderBottom(BorderStyle.HAIR); s.setBorderRight(BorderStyle.HAIR);
        return s;
    }

    private static CellStyle numStyle(Workbook wb) {
        CellStyle s = sampleStyle(wb);
        DataFormat df = wb.createDataFormat();
        s.setDataFormat(df.getFormat("#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    // ── Cell readers ───────────────────────────────────────────────────────────
    private static String cellStr(Row row, int col) {
        if (col < 0) return "";
        Cell c = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return c == null ? "" : cellStr(c);
    }

    private static String cellStr(Cell c) {
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING  -> c.getStringCellValue().trim();
            case NUMERIC -> {
                double v = c.getNumericCellValue();
                yield v == (long) v ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            case FORMULA -> {
                try { yield c.getStringCellValue().trim(); }
                catch (Exception e) { yield String.valueOf(c.getNumericCellValue()); }
            }
            default -> "";
        };
    }

    private static double cellNum(Row row, int col) {
        if (col < 0) return 0;
        Cell c = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return 0;
        return switch (c.getCellType()) {
            case NUMERIC -> c.getNumericCellValue();
            case STRING  -> { try { yield Double.parseDouble(c.getStringCellValue().trim()); } catch (Exception e) { yield 0; } }
            case FORMULA -> { try { yield c.getNumericCellValue(); } catch (Exception e) { yield 0; } }
            default -> 0;
        };
    }

    private static boolean isRowEmpty(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell c = row.getCell(i);
            if (c != null && c.getCellType() != CellType.BLANK && !cellStr(c).isEmpty()) return false;
        }
        return true;
    }
}
