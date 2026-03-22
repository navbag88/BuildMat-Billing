package com.buildmat.ui;

import com.buildmat.dao.InvoiceDAO;
import com.buildmat.dao.PaymentDAO;
import com.buildmat.model.Invoice;
import com.buildmat.model.Payment;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class PaymentPanel {

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private VBox view;
    private TableView<Payment> table;

    public PaymentPanel() { buildView(); }
    public void refresh() { loadData(); }

    private void buildView() {
        view = new VBox(16);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Payments");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#1A2332"));

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportExcelBtn = new Button("📊 Export Excel");
        exportExcelBtn.setStyle("-fx-background-color:#7C3AED;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 14;");
        exportExcelBtn.setOnAction(e -> exportData("excel"));

        Button exportPdfBtn = new Button("📄 Export PDF");
        exportPdfBtn.setStyle("-fx-background-color:#DC2626;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 14;");
        exportPdfBtn.setOnAction(e -> exportData("pdf"));

        Button addBtn = new Button("+ Record Payment");
        addBtn.setStyle("-fx-background-color:#16A34A;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 16;");
        addBtn.setOnAction(e -> openForm(null));
        header.getChildren().addAll(title, spacer, exportExcelBtn, exportPdfBtn, addBtn);
        view.getChildren().add(header);

        table = new TableView<>();
        table.setStyle("-fx-background-color:white;-fx-background-radius:12;");
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Payment, String> dateCol = tc("Date", 120);
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPaymentDate() != null ? d.getValue().getPaymentDate().toString() : ""));

        TableColumn<Payment, String> invCol = tc("Invoice #", 140);
        invCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getInvoiceNumber()));

        TableColumn<Payment, String> custCol = tc("Customer", 200);
        custCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCustomerName()));

        TableColumn<Payment, String> amtCol = tc("Amount", 130);
        amtCol.setCellValueFactory(d -> new SimpleStringProperty(inr(d.getValue().getAmount())));

        TableColumn<Payment, String> methodCol = tc("Method", 110);
        methodCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMethod()));
        methodCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(s);
                badge.setPadding(new Insets(2, 10, 2, 10));
                badge.setFont(Font.font("Arial", FontWeight.BOLD, 11));
                String bg = switch (s) {
                    case "CASH" -> "#D1FAE5"; case "CHEQUE" -> "#DBEAFE";
                    case "NEFT/RTGS" -> "#EDE9FE"; default -> "#F3F4F6";
                };
                String fg = switch (s) {
                    case "CASH" -> "#065F46"; case "CHEQUE" -> "#1D4ED8";
                    case "NEFT/RTGS" -> "#5B21B6"; default -> "#374151";
                };
                badge.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:20;-fx-text-fill:" + fg + ";");
                setGraphic(badge); setText(null);
            }
        });

        TableColumn<Payment, String> refCol = tc("Reference", 150);
        refCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getReference() != null ? d.getValue().getReference() : ""));

        TableColumn<Payment, String> notesCol = tc("Notes", 180);
        notesCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getNotes() != null ? d.getValue().getNotes() : ""));

        TableColumn<Payment, Void> actCol = new TableColumn<>("Action");
        actCol.setPrefWidth(90);
        actCol.setCellFactory(c -> new TableCell<>() {
            final Button del = new Button("Delete");
            { del.setStyle("-fx-background-color:#FEE2E2;-fx-text-fill:#DC2626;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12;");
              del.setOnAction(e -> {
                  Payment p = getTableView().getItems().get(getIndex());
                  new Alert(Alert.AlertType.CONFIRMATION, "Delete this payment of " + inr(p.getAmount()) + "?", ButtonType.YES, ButtonType.NO)
                      .showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
                          try { paymentDAO.delete(p.getId(), p.getInvoiceId(), p.getAmount()); loadData(); }
                          catch (Exception ex) { showAlert(ex.getMessage()); }
                      });
              }); }
            @Override protected void updateItem(Void v, boolean e) { super.updateItem(v, e); setGraphic(e ? null : del); }
        });

        table.getColumns().addAll(dateCol, invCol, custCol, amtCol, methodCol, refCol, notesCol, actCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        view.getChildren().add(table);
        loadData();
    }

    private void loadData() {
        try { table.setItems(FXCollections.observableArrayList(paymentDAO.getAll())); }
        catch (Exception e) { showAlert(e.getMessage()); }
    }

    public void openForm(Invoice preselectedInvoice) {
        Dialog<Payment> dialog = new Dialog<>();
        dialog.setTitle("Record Payment");
        dialog.getDialogPane().setPrefWidth(480);

        Payment payment = new Payment();

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(12); grid.setPadding(new Insets(24));

        // Invoice selector
        ComboBox<Invoice> invoiceBox = new ComboBox<>();
        invoiceBox.setPromptText("Select invoice...");
        invoiceBox.setPrefWidth(270);
        try {
            List<Invoice> unpaid = invoiceDAO.getAll().stream()
                .filter(i -> !i.getStatus().equals("PAID")).toList();
            invoiceBox.setItems(FXCollections.observableArrayList(unpaid));
            invoiceBox.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(Invoice i) {
                    if (i == null) return "";
                    String cname = i.getCustomer() != null ? i.getCustomer().getName() : "?";
                    return i.getInvoiceNumber() + " — " + cname + " (" + inr(i.getBalanceDue()) + " due)";
                }
                @Override public Invoice fromString(String s) { return null; }
            });
            if (preselectedInvoice != null) invoiceBox.setValue(preselectedInvoice);
        } catch (Exception ignored) {}

        // Balance due label
        Label balanceLabel = new Label("");
        balanceLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        balanceLabel.setTextFill(Color.web("#DC2626"));
        invoiceBox.setOnAction(e -> {
            Invoice sel = invoiceBox.getValue();
            if (sel != null) balanceLabel.setText("Balance due: " + inr(sel.getBalanceDue()));
        });

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");
        amountField.setPrefWidth(160);

        DatePicker datePicker = new DatePicker(LocalDate.now());

        ComboBox<String> methodBox = new ComboBox<>();
        methodBox.getItems().addAll("CASH", "CHEQUE", "NEFT/RTGS", "UPI", "CARD");
        methodBox.setValue("CASH");

        TextField refField = new TextField();
        refField.setPromptText("Cheque no. / UTR / UPI ref...");
        refField.setPrefWidth(270);

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Optional notes...");
        notesArea.setPrefRowCount(2);

        grid.add(lbl("Invoice *"), 0, 0); grid.add(invoiceBox, 1, 0);
        grid.add(new Label(""), 0, 1); grid.add(balanceLabel, 1, 1);
        grid.add(lbl("Amount *"), 0, 2); grid.add(amountField, 1, 2);
        grid.add(lbl("Date"), 0, 3); grid.add(datePicker, 1, 3);
        grid.add(lbl("Method"), 0, 4); grid.add(methodBox, 1, 4);
        grid.add(lbl("Reference"), 0, 5); grid.add(refField, 1, 5);
        grid.add(lbl("Notes"), 0, 6); grid.add(notesArea, 1, 6);

        dialog.getDialogPane().setContent(grid);
        ButtonType saveType = new ButtonType("Save Payment", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                if (invoiceBox.getValue() == null) { showAlert("Please select an invoice."); return null; }
                if (amountField.getText().isBlank()) { showAlert("Please enter an amount."); return null; }
                try {
                    double amt = Double.parseDouble(amountField.getText());
                    if (amt <= 0) { showAlert("Amount must be greater than zero."); return null; }
                    Invoice sel = invoiceBox.getValue();
                    if (amt > sel.getBalanceDue() + 0.01) {
                        showAlert("Amount (" + inr(amt) + ") exceeds balance due (" + inr(sel.getBalanceDue()) + ").");
                        return null;
                    }
                    payment.setInvoiceId(sel.getId());
                    payment.setAmount(amt);
                    payment.setPaymentDate(datePicker.getValue());
                    payment.setMethod(methodBox.getValue());
                    payment.setReference(refField.getText());
                    payment.setNotes(notesArea.getText());
                    return payment;
                } catch (NumberFormatException e) {
                    showAlert("Invalid amount. Please enter a number.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            try {
                paymentDAO.save(p);
                loadData();
                showAlert("Payment of " + inr(p.getAmount()) + " recorded successfully.");
            } catch (Exception e) { showAlert(e.getMessage()); }
        });
    }

    private void exportData(String format) {
        javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
        dc.setTitle("Choose export folder");
        dc.setInitialDirectory(new java.io.File(System.getProperty("user.home")));
        java.io.File dir = dc.showDialog(null);
        if (dir == null) return;
        try {
            List<com.buildmat.model.Payment> data = paymentDAO.getAll();
            String path = format.equals("excel")
                ? com.buildmat.util.DataExporter.exportPaymentsExcel(data, dir.getAbsolutePath())
                : com.buildmat.util.DataExporter.exportPaymentsPdf(data, dir.getAbsolutePath());
            showAlert("Export successful! Saved to:\n" + path);
            try { java.awt.Desktop.getDesktop().open(new java.io.File(path)); } catch (Exception ignored) {}
        } catch (Exception ex) { showAlert("Export failed: " + ex.getMessage()); }
    }

    private TableColumn<Payment, String> tc(String name, int w) {
        TableColumn<Payment, String> c = new TableColumn<>(name); c.setPrefWidth(w); return c;
    }
    private Label lbl(String t) { return new Label(t); }
    private String inr(double v) { return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(v); }
    private void showAlert(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    public VBox getView() { return view; }
}
