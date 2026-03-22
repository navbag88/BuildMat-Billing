package com.buildmat.ui;

import com.buildmat.model.Invoice;
import com.buildmat.model.InvoiceItem;
import com.buildmat.util.InvoicePdfGenerator;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Scale;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;

public class InvoicePreviewWindow {

    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public static void show(Invoice invoice) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Invoice Preview — " + invoice.getInvoiceNumber());
        stage.setWidth(860);
        stage.setHeight(900);

        // ── Top toolbar ──────────────────────────────────────────
        HBox toolbar = new HBox(12);
        toolbar.setPadding(new Insets(12, 20, 12, 20));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #1A2332;");

        Label titleLbl = new Label("Invoice Preview");
        titleLbl.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        titleLbl.setTextFill(Color.WHITE);

        Label invNum = new Label(invoice.getInvoiceNumber());
        invNum.setFont(Font.font("Arial", 13));
        invNum.setTextFill(Color.rgb(150, 180, 220));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button printBtn     = toolbarBtn("🖨  Print",        "#374151");
        Button downloadBtn  = toolbarBtn("⬇  Download PDF",  "#2563EB");

        toolbar.getChildren().addAll(titleLbl, invNum, spacer, printBtn, downloadBtn);

        // ── WebView ───────────────────────────────────────────────
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        VBox.setVgrow(webView, Priority.ALWAYS);

        // Load content and wait for it to finish before enabling print
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                printBtn.setDisable(false);
            }
        });
        printBtn.setDisable(true);
        engine.loadContent(buildHtml(invoice));

        // ── Print — PrinterJob on the WebView node ────────────────
        // window.print() does not work in JavaFX WebView.
        // The correct approach is PrinterJob.printPage(webView).
        printBtn.setOnAction(e -> {
            javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
            if (job == null) {
                showAlert(stage, "Print Error",
                    "No printer found. Please install a printer or use Download PDF.",
                    Alert.AlertType.ERROR);
                return;
            }
            // Show the system print dialog
            boolean proceed = job.showPrintDialog(stage);
            if (!proceed) { job.cancelJob(); return; }

            // Scale WebView to fit the page
            javafx.print.PageLayout pageLayout = job.getJobSettings().getPageLayout();
            double pageW = pageLayout.getPrintableWidth();
            double pageH = pageLayout.getPrintableHeight();

            // Temporarily scale WebView to page size for printing
            double scaleX = pageW / webView.getBoundsInParent().getWidth();
            webView.getTransforms().add(new javafx.scene.transform.Scale(scaleX, scaleX));

            boolean printed = job.printPage(webView);

            // Remove scale transform after printing
            webView.getTransforms().clear();

            if (printed) {
                job.endJob();
            } else {
                job.cancelJob();
                showAlert(stage, "Print Failed",
                    "Printing failed. Try Download PDF instead.",
                    Alert.AlertType.WARNING);
            }
        });

        // ── Download PDF ──────────────────────────────────────────
        downloadBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Choose folder to save PDF");
            dc.setInitialDirectory(new File(System.getProperty("user.home")));
            File dir = dc.showDialog(stage);
            if (dir != null) {
                try {
                    String path = InvoicePdfGenerator.generate(invoice, dir.getAbsolutePath());
                    showAlert(stage, "Success", "PDF saved to:\n" + path, Alert.AlertType.INFORMATION);
                    try { java.awt.Desktop.getDesktop().open(new File(path)); } catch (Exception ignored) {}
                } catch (Exception ex) {
                    showAlert(stage, "Error", "Failed to generate PDF:\n" + ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });

        VBox root = new VBox(toolbar, webView);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    // ── HTML invoice template ─────────────────────────────────────────────────
    private static String buildHtml(Invoice invoice) {
        String customerName  = invoice.getCustomer() != null ? invoice.getCustomer().getName() : "—";
        String customerPhone = invoice.getCustomer() != null && invoice.getCustomer().getPhone() != null ? invoice.getCustomer().getPhone() : "";
        String customerEmail = invoice.getCustomer() != null && invoice.getCustomer().getEmail() != null ? invoice.getCustomer().getEmail() : "";
        String customerAddr  = invoice.getCustomer() != null && invoice.getCustomer().getAddress() != null ? invoice.getCustomer().getAddress() : "";

        String statusColor = switch (invoice.getStatus()) {
            case "PAID" -> "#16A34A"; case "PARTIAL" -> "#D97706"; default -> "#DC2626";
        };
        String statusBg = switch (invoice.getStatus()) {
            case "PAID" -> "#DCFCE7"; case "PARTIAL" -> "#FEF3C7"; default -> "#FEE2E2";
        };

        StringBuilder itemRows = new StringBuilder();
        int i = 1;
        for (InvoiceItem item : invoice.getItems()) {
            String rowBg = i % 2 == 0 ? "#F9FAFB" : "#FFFFFF";
            itemRows.append(String.format("""
                <tr style="background:%s;">
                    <td style="padding:10px 12px;text-align:center;color:#6B7280;border-bottom:1px solid #F3F4F6;">%d</td>
                    <td style="padding:10px 12px;font-weight:500;color:#111827;border-bottom:1px solid #F3F4F6;">%s</td>
                    <td style="padding:10px 12px;text-align:center;color:#6B7280;border-bottom:1px solid #F3F4F6;">%s</td>
                    <td style="padding:10px 12px;text-align:right;color:#374151;border-bottom:1px solid #F3F4F6;">%s</td>
                    <td style="padding:10px 12px;text-align:right;color:#374151;border-bottom:1px solid #F3F4F6;">%s</td>
                    <td style="padding:10px 12px;text-align:right;font-weight:700;color:#111827;border-bottom:1px solid #F3F4F6;">%s</td>
                </tr>
            """, rowBg, i++,
                escHtml(item.getProductName()),
                escHtml(item.getUnit() != null ? item.getUnit() : ""),
                formatNum(item.getQuantity()),
                INR.format(item.getUnitPrice()),
                INR.format(item.getTotal())
            ));
        }

        String notes = (invoice.getNotes() != null && !invoice.getNotes().isBlank())
            ? "<div style='margin-top:20px;padding:12px 14px;border-left:4px solid #2563EB;background:#EFF6FF;border-radius:0 6px 6px 0;'>"
              + "<div style='font-size:11px;font-weight:700;color:#2563EB;letter-spacing:1px;margin-bottom:4px;'>NOTES</div>"
              + "<div style='font-size:13px;color:#374151;'>" + escHtml(invoice.getNotes()) + "</div></div>"
            : "";

        return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="UTF-8"/>
        <style>
            * { margin:0; padding:0; box-sizing:border-box; }
            body { font-family: Arial, sans-serif; background:#F3F4F6; padding:28px; }
            .page {
                background:white; max-width:740px; margin:0 auto;
                padding:44px; box-shadow:0 4px 24px rgba(0,0,0,0.10); border-radius:8px;
            }
            @media print {
                html, body { margin:0; padding:0; background:white; }
                body { padding:0; }
                .page {
                    box-shadow:none; border-radius:0; max-width:100%%;
                    padding:16px; margin:0; width:100%%;
                }
                @page { size: A4; margin: 15mm; }
            }
        </style>
        </head>
        <body><div class="page">

        <!-- HEADER -->
        <table width="100%%" style="border-collapse:collapse;margin-bottom:28px;">
        <tr>
            <td style="vertical-align:top;width:55%%;">
                <div style="font-size:24px;font-weight:800;color:#2563EB;letter-spacing:-0.5px;">BuildMat Supplies</div>
                <div style="font-size:12px;color:#6B7280;margin-top:3px;">Building Material Supplier</div>
                <div style="font-size:11px;color:#9CA3AF;margin-top:8px;line-height:1.9;">
                    GST No: 27ABCDE1234F1Z5<br>
                    Phone: +91 98765 43210<br>
                    Email: billing@buildmat.in
                </div>
            </td>
            <td style="vertical-align:top;text-align:right;width:45%%;">
                <div style="font-size:26px;font-weight:800;color:#111827;letter-spacing:2px;">TAX INVOICE</div>
                <div style="font-size:15px;font-weight:700;color:#2563EB;margin-top:6px;">%s</div>
                <div style="font-size:11px;color:#6B7280;margin-top:6px;line-height:1.9;">
                    <b>Date:</b> %s<br>%s
                </div>
                <div style="display:inline-block;margin-top:8px;padding:3px 14px;
                            background:%s;color:%s;border-radius:20px;
                            font-size:11px;font-weight:700;letter-spacing:1px;">
                    %s
                </div>
            </td>
        </tr>
        </table>

        <!-- DIVIDER -->
        <div style="border-top:2px solid #2563EB;margin-bottom:24px;"></div>

        <!-- BILL TO + META -->
        <table width="100%%" style="border-collapse:collapse;margin-bottom:24px;">
        <tr>
            <td style="vertical-align:top;width:50%%;">
                <div style="background:#EFF6FF;border-left:4px solid #2563EB;padding:14px 16px;border-radius:0 6px 6px 0;">
                    <div style="font-size:10px;font-weight:700;color:#2563EB;letter-spacing:1px;margin-bottom:6px;">BILL TO</div>
                    <div style="font-size:15px;font-weight:700;color:#111827;margin-bottom:4px;">%s</div>
                    %s%s%s
                </div>
            </td>
            <td style="vertical-align:top;width:50%%;padding-left:20px;">
                <table width="100%%" style="border-collapse:collapse;font-size:12px;">
                    <tr><td style="padding:7px 0;color:#6B7280;border-bottom:1px solid #F3F4F6;">Invoice No.</td>
                        <td style="padding:7px 0;font-weight:700;color:#111827;text-align:right;border-bottom:1px solid #F3F4F6;">%s</td></tr>
                    <tr><td style="padding:7px 0;color:#6B7280;border-bottom:1px solid #F3F4F6;">Invoice Date</td>
                        <td style="padding:7px 0;color:#374151;text-align:right;border-bottom:1px solid #F3F4F6;">%s</td></tr>
                    <tr><td style="padding:7px 0;color:#6B7280;border-bottom:1px solid #F3F4F6;">Due Date</td>
                        <td style="padding:7px 0;color:#374151;text-align:right;border-bottom:1px solid #F3F4F6;">%s</td></tr>
                    <tr><td style="padding:7px 0;color:#6B7280;">GST %%</td>
                        <td style="padding:7px 0;color:#374151;text-align:right;">%.0f%%</td></tr>
                </table>
            </td>
        </tr>
        </table>

        <!-- ITEMS TABLE -->
        <table width="100%%" style="border-collapse:collapse;border:1px solid #E5E7EB;border-radius:8px;overflow:hidden;margin-bottom:20px;">
        <thead>
            <tr style="background:#2563EB;">
                <th style="padding:11px 12px;text-align:center;color:white;font-size:11px;width:5%%;">#</th>
                <th style="padding:11px 12px;text-align:left;color:white;font-size:11px;width:35%%;">DESCRIPTION</th>
                <th style="padding:11px 12px;text-align:center;color:white;font-size:11px;width:10%%;">UNIT</th>
                <th style="padding:11px 12px;text-align:right;color:white;font-size:11px;width:13%%;">QTY</th>
                <th style="padding:11px 12px;text-align:right;color:white;font-size:11px;width:18%%;">UNIT PRICE</th>
                <th style="padding:11px 12px;text-align:right;color:white;font-size:11px;width:19%%;">TOTAL</th>
            </tr>
        </thead>
        <tbody>%s</tbody>
        </table>

        <!-- TOTALS + NOTES -->
        <table width="100%%" style="border-collapse:collapse;margin-bottom:28px;">
        <tr>
            <td style="vertical-align:top;width:48%%;">%s</td>
            <td style="vertical-align:top;width:52%%;padding-left:20px;">
                <table width="100%%" style="border-collapse:collapse;border:1px solid #E5E7EB;border-radius:8px;overflow:hidden;">
                    <tr style="background:#F9FAFB;">
                        <td style="padding:9px 14px;font-size:12px;color:#6B7280;">Subtotal</td>
                        <td style="padding:9px 14px;font-size:12px;color:#374151;text-align:right;">%s</td>
                    </tr>
                    %s
                    <tr style="background:#2563EB;">
                        <td style="padding:12px 14px;font-size:14px;font-weight:700;color:white;">Grand Total</td>
                        <td style="padding:12px 14px;font-size:14px;font-weight:700;color:white;text-align:right;">%s</td>
                    </tr>
                    <tr style="background:#F0FDF4;">
                        <td style="padding:9px 14px;font-size:12px;color:#16A34A;border-top:1px solid #DCFCE7;">Paid</td>
                        <td style="padding:9px 14px;font-size:12px;color:#16A34A;font-weight:600;text-align:right;border-top:1px solid #DCFCE7;">%s</td>
                    </tr>
                    <tr style="background:#FFF7ED;">
                        <td style="padding:9px 14px;font-size:12px;font-weight:700;color:#C2410C;border-top:1px solid #FFEDD5;">Balance Due</td>
                        <td style="padding:9px 14px;font-size:12px;font-weight:700;color:#C2410C;text-align:right;border-top:1px solid #FFEDD5;">%s</td>
                    </tr>
                </table>
            </td>
        </tr>
        </table>

        <!-- FOOTER -->
        <div style="border-top:1px solid #E5E7EB;padding-top:14px;text-align:center;">
            <p style="font-size:13px;color:#374151;font-weight:500;margin-bottom:4px;">Thank you for your business!</p>
            <p style="font-size:10px;color:#9CA3AF;">
                This is a computer-generated invoice and does not require a physical signature.<br>
                For queries: billing@buildmat.in | +91 98765 43210
            </p>
        </div>

        </div></body></html>
        """.formatted(
            // Header right
            escHtml(invoice.getInvoiceNumber()),
            invoice.getInvoiceDate().toString(),
            invoice.getDueDate() != null ? "<b>Due:</b> " + invoice.getDueDate() : "",
            statusBg, statusColor, invoice.getStatus(),
            // Bill to
            escHtml(customerName),
            customerPhone.isBlank() ? "" : "<div style='font-size:11px;color:#6B7280;margin-top:3px;'>Ph: " + escHtml(customerPhone) + "</div>",
            customerEmail.isBlank() ? "" : "<div style='font-size:11px;color:#6B7280;margin-top:2px;'>" + escHtml(customerEmail) + "</div>",
            customerAddr.isBlank()  ? "" : "<div style='font-size:11px;color:#9CA3AF;margin-top:4px;line-height:1.5;'>" + escHtml(customerAddr).replace("\n","<br>") + "</div>",
            // Meta right
            escHtml(invoice.getInvoiceNumber()),
            invoice.getInvoiceDate().toString(),
            invoice.getDueDate() != null ? invoice.getDueDate().toString() : "—",
            invoice.getTaxPercent(),
            // Items
            itemRows.toString(),
            // Notes
            notes,
            // Totals
            INR.format(invoice.getSubtotal()),
            buildGstRows(invoice),
            INR.format(invoice.getTotalAmount()),
            INR.format(invoice.getPaidAmount()),
            INR.format(invoice.getBalanceDue())
        );
    }

    private static Button toolbarBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;" +
                     "-fx-background-radius:8;-fx-font-size:13;-fx-cursor:hand;-fx-padding:8 18;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    private static String buildGstRows(Invoice invoice) {
        if (!invoice.isIncludeGst()) return "";
        return """
            <tr>
                <td style="padding:9px 14px;font-size:12px;color:#6B7280;border-top:1px solid #F3F4F6;">SGST</td>
                <td style="padding:9px 14px;font-size:12px;color:#374151;text-align:right;border-top:1px solid #F3F4F6;">%s</td>
            </tr>
            <tr>
                <td style="padding:9px 14px;font-size:12px;color:#6B7280;border-top:1px solid #F3F4F6;">CGST</td>
                <td style="padding:9px 14px;font-size:12px;color:#374151;text-align:right;border-top:1px solid #F3F4F6;">%s</td>
            </tr>
            <tr style="background:#EFF6FF;">
                <td style="padding:9px 14px;font-size:12px;font-weight:700;color:#1D4ED8;border-top:1px solid #DBEAFE;">Total GST</td>
                <td style="padding:9px 14px;font-size:12px;font-weight:700;color:#1D4ED8;text-align:right;border-top:1px solid #DBEAFE;">%s</td>
            </tr>
        """.formatted(
            INR.format(invoice.getSgstAmount()),
            INR.format(invoice.getCgstAmount()),
            INR.format(invoice.getTaxAmount())
        );
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    private static String formatNum(double v) {
        return v == (long) v ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static void showAlert(Stage owner, String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.initOwner(owner);
        a.showAndWait();
    }
}
