package com.buildmat.ui;

import com.buildmat.dao.*;
import com.buildmat.model.*;
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

public class InvoicePanel {

    private final MainWindow mainWindow;
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final PaymentPanel paymentPanel = new PaymentPanel();
    private VBox view;
    private TableView<Invoice> table;
    private TextField searchField;

    public InvoicePanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        buildView();
    }

    public void refresh() { loadData(""); }

    private void buildView() {
        view = new VBox(16);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Invoices");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#1A2332"));

        searchField = new TextField();
        searchField.setPromptText("Search by invoice #, customer...");
        searchField.setPrefWidth(280);
        styleInput(searchField);
        searchField.textProperty().addListener((o, ov, nv) -> loadData(nv));

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newBtn = new Button("+ New Invoice");
        styleBtn(newBtn, "#2563EB");
        newBtn.setOnAction(e -> openInvoiceForm(null));

        Button exportExcelBtn = new Button("📊 Export Excel");
        exportExcelBtn.setStyle("-fx-background-color:#7C3AED;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 14;");
        exportExcelBtn.setOnAction(e -> exportData("excel"));

        Button exportPdfBtn = new Button("📄 Export PDF");
        exportPdfBtn.setStyle("-fx-background-color:#DC2626;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 14;");
        exportPdfBtn.setOnAction(e -> exportData("pdf"));

        header.getChildren().addAll(title, spacer, searchField, exportExcelBtn, exportPdfBtn, newBtn);
        view.getChildren().add(header);

        table = new TableView<>();
        table.setStyle("-fx-background-color:white;-fx-background-radius:12;");
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Invoice, String> numCol   = col("Invoice #", 130);
        numCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getInvoiceNumber()));

        TableColumn<Invoice, String> custCol  = col("Customer", 180);
        custCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCustomer() != null ? d.getValue().getCustomer().getName() : "—"));

        TableColumn<Invoice, String> dateCol  = col("Date", 100);
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getInvoiceDate().toString()));

        TableColumn<Invoice, String> subCol   = col("Subtotal", 110);
        subCol.setCellValueFactory(d -> new SimpleStringProperty(inr(d.getValue().getSubtotal())));

        TableColumn<Invoice, String> gstCol   = col("GST", 90);
        gstCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().isIncludeGst() ? inr(d.getValue().getTaxAmount()) : "—"));

        TableColumn<Invoice, String> totalCol = col("Total", 120);
        totalCol.setCellValueFactory(d -> new SimpleStringProperty(inr(d.getValue().getTotalAmount())));

        TableColumn<Invoice, String> paidCol  = col("Paid", 100);
        paidCol.setCellValueFactory(d -> new SimpleStringProperty(inr(d.getValue().getPaidAmount())));

        TableColumn<Invoice, String> balCol   = col("Balance", 100);
        balCol.setCellValueFactory(d -> new SimpleStringProperty(inr(d.getValue().getBalanceDue())));

        TableColumn<Invoice, String> statusCol = col("Status", 85);
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setCellFactory(c -> statusCell());

        TableColumn<Invoice, Void> actCol = new TableColumn<>("Actions");
        actCol.setPrefWidth(260);
        actCol.setCellFactory(c -> actionsCell());

        table.getColumns().addAll(numCol, custCol, dateCol, subCol, gstCol, totalCol, paidCol, balCol, statusCol, actCol);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        view.getChildren().add(table);
        loadData("");
    }

    private void loadData(String query) {
        try {
            List<Invoice> data = query.isBlank() ? invoiceDAO.getAll() : invoiceDAO.search(query);
            table.setItems(FXCollections.observableArrayList(data));
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    private void openInvoiceForm(Invoice existing) {
        Dialog<Invoice> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "New Invoice" : "Edit Invoice — " + existing.getInvoiceNumber());
        dialog.getDialogPane().setPrefSize(960, 720);

        Invoice invoice = existing == null ? new Invoice() : existing;
        if (existing != null) {
            try { invoice.setItems(invoiceDAO.getItems(existing.getId())); } catch (Exception ignored) {}
        } else {
            try { invoice.setInvoiceNumber(invoiceDAO.generateInvoiceNumber()); } catch (Exception ignored) {}
        }

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));

        // ── Top fields ────────────────────────────────────────────
        GridPane top = new GridPane();
        top.setHgap(16); top.setVgap(10);

        ComboBox<Customer> custBox = new ComboBox<>();
        custBox.setPromptText("Select Customer");
        custBox.setPrefWidth(240);
        try { custBox.setItems(FXCollections.observableArrayList(customerDAO.getAll())); } catch (Exception ignored) {}
        if (invoice.getCustomer() != null) custBox.setValue(invoice.getCustomer());

        TextField invNumField = new TextField(invoice.getInvoiceNumber());
        invNumField.setEditable(false);
        invNumField.setStyle("-fx-background-color:#F3F4F6;");

        DatePicker invDate  = new DatePicker(invoice.getInvoiceDate());
        DatePicker dueDate  = new DatePicker(invoice.getDueDate() != null ? invoice.getDueDate() : LocalDate.now().plusDays(30));

        // ── Include GST toggle ────────────────────────────────────
        CheckBox includeGstChk = new CheckBox("Include GST in invoice");
        includeGstChk.setSelected(invoice.isIncludeGst());
        includeGstChk.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        includeGstChk.setStyle("-fx-text-fill:#2563EB;");

        top.add(label("Customer *"),   0, 0); top.add(custBox, 1, 0);
        top.add(label("Invoice No."),  2, 0); top.add(invNumField, 3, 0);
        top.add(label("Invoice Date"), 0, 1); top.add(invDate, 1, 1);
        top.add(label("Due Date"),     2, 1); top.add(dueDate, 3, 1);
        top.add(includeGstChk,         0, 2, 4, 1);
        form.getChildren().add(top);

        // ── Line items table ──────────────────────────────────────
        Label itemsLbl = new Label("Line Items");
        itemsLbl.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        form.getChildren().add(itemsLbl);

        TableView<InvoiceItem> itemTable = new TableView<>();
        itemTable.setEditable(true);
        itemTable.setPrefHeight(220);
        itemTable.setItems(FXCollections.observableArrayList(invoice.getItems()));

        TableColumn<InvoiceItem, String> pNameCol = itemCol("Product / Description", 200);
        pNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProductName()));

        TableColumn<InvoiceItem, String> unitCol2 = itemCol("Unit", 70);
        unitCol2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUnit()));

        TableColumn<InvoiceItem, String> qtyCol = itemCol("Qty", 60);
        qtyCol.setCellValueFactory(d -> new SimpleStringProperty(formatNum(d.getValue().getQuantity())));

        TableColumn<InvoiceItem, String> priceCol = itemCol("Unit Price", 100);
        priceCol.setCellValueFactory(d -> new SimpleStringProperty(inr(d.getValue().getUnitPrice())));

        TableColumn<InvoiceItem, String> sgstCol = itemCol("SGST %", 65);
        sgstCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSgstPercent() + "%"));

        TableColumn<InvoiceItem, String> cgstCol = itemCol("CGST %", 65);
        cgstCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCgstPercent() + "%"));

        TableColumn<InvoiceItem, String> itemTotalCol = itemCol("Total", 100);
        itemTotalCol.setCellValueFactory(d -> new SimpleStringProperty(inr(d.getValue().getTotal())));

        // Totals labels — defined early so recalc can use them
        Label subtotalLbl   = new Label();
        Label sgstAmtLbl    = new Label();
        Label cgstAmtLbl    = new Label();
        Label gstTotalLbl   = new Label();
        Label grandTotalLbl = new Label();
        grandTotalLbl.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        TableColumn<InvoiceItem, Void> removeCol = new TableColumn<>("");
        removeCol.setPrefWidth(50);
        removeCol.setCellFactory(c -> new TableCell<>() {
            final Button btn = new Button("✕");
            { btn.setStyle("-fx-background-color:#FEE2E2;-fx-text-fill:#DC2626;-fx-background-radius:6;-fx-cursor:hand;");
              btn.setOnAction(e -> {
                  itemTable.getItems().remove(getIndex());
                  recalcTotals(itemTable, includeGstChk.isSelected(),
                          subtotalLbl, sgstAmtLbl, cgstAmtLbl, gstTotalLbl, grandTotalLbl);
              }); }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : btn); }
        });

        itemTable.getColumns().addAll(pNameCol, unitCol2, qtyCol, priceCol, sgstCol, cgstCol, itemTotalCol, removeCol);

        // ── Add item row ──────────────────────────────────────────
        HBox addItemRow = new HBox(8);
        addItemRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<Product> productBox = new ComboBox<>();
        productBox.setPromptText("Select product");
        productBox.setPrefWidth(210);
        try { productBox.setItems(FXCollections.observableArrayList(productDAO.getAll())); } catch (Exception ignored) {}

        TextField qtyInput   = new TextField("1");   qtyInput.setPrefWidth(60);
        TextField priceInput = new TextField();       priceInput.setPrefWidth(90); priceInput.setPromptText("Price");
        Label sgstHint = new Label("SGST: 0%");      sgstHint.setStyle("-fx-text-fill:#6B7280;-fx-font-size:12;");
        Label cgstHint = new Label("CGST: 0%");      cgstHint.setStyle("-fx-text-fill:#6B7280;-fx-font-size:12;");

        productBox.setOnAction(e -> {
            Product p = productBox.getValue();
            if (p != null) {
                priceInput.setText(String.valueOf(p.getPrice()));
                sgstHint.setText("SGST: " + p.getSgstPercent() + "%");
                cgstHint.setText("CGST: " + p.getCgstPercent() + "%");
            }
        });

        Button addItemBtn = new Button("Add Item");
        styleBtn(addItemBtn, "#16A34A");
        addItemBtn.setOnAction(e -> {
            InvoiceItem item = new InvoiceItem();
            Product p = productBox.getValue();
            if (p != null) {
                item.setProductId(p.getId());
                item.setProductName(p.getName());
                item.setUnit(p.getUnit());
                item.setSgstPercent(p.getSgstPercent());
                item.setCgstPercent(p.getCgstPercent());
            } else {
                item.setProductName("Custom Item");
            }
            try {
                item.setQuantity(Double.parseDouble(qtyInput.getText()));
                item.setUnitPrice(Double.parseDouble(priceInput.getText()));
                itemTable.getItems().add(item);
                recalcTotals(itemTable, includeGstChk.isSelected(),
                        subtotalLbl, sgstAmtLbl, cgstAmtLbl, gstTotalLbl, grandTotalLbl);
                productBox.setValue(null); qtyInput.setText("1"); priceInput.clear();
                sgstHint.setText("SGST: 0%"); cgstHint.setText("CGST: 0%");
            } catch (NumberFormatException ignored) {
                showAlert("Invalid Input", "Enter valid quantity and price.");
            }
        });

        addItemRow.getChildren().addAll(
                new Label("Product:"), productBox,
                new Label("Qty:"), qtyInput,
                new Label("Price:"), priceInput,
                sgstHint, cgstHint,
                addItemBtn);
        form.getChildren().addAll(itemTable, addItemRow);

        // ── Totals grid ───────────────────────────────────────────
        GridPane totalsGrid = new GridPane();
        totalsGrid.setHgap(20); totalsGrid.setVgap(6);
        totalsGrid.setAlignment(Pos.CENTER_RIGHT);
        totalsGrid.add(new Label("Subtotal:"),  0, 0); totalsGrid.add(subtotalLbl,  1, 0);
        totalsGrid.add(new Label("SGST:"),       0, 1); totalsGrid.add(sgstAmtLbl,   1, 1);
        totalsGrid.add(new Label("CGST:"),       0, 2); totalsGrid.add(cgstAmtLbl,   1, 2);
        totalsGrid.add(new Label("Total GST:"),  0, 3); totalsGrid.add(gstTotalLbl,  1, 3);
        Label grandLbl = new Label("Grand Total:"); grandLbl.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        totalsGrid.add(grandLbl,                 0, 4); totalsGrid.add(grandTotalLbl, 1, 4);

        // Hide/show GST rows based on toggle
        sgstAmtLbl.visibleProperty().bind(includeGstChk.selectedProperty());
        cgstAmtLbl.visibleProperty().bind(includeGstChk.selectedProperty());
        gstTotalLbl.visibleProperty().bind(includeGstChk.selectedProperty());
        totalsGrid.getChildren().stream()
            .filter(n -> GridPane.getRowIndex(n) != null &&
                         (GridPane.getRowIndex(n) == 1 || GridPane.getRowIndex(n) == 2 || GridPane.getRowIndex(n) == 3))
            .forEach(n -> n.visibleProperty().bind(includeGstChk.selectedProperty()));

        recalcTotals(itemTable, includeGstChk.isSelected(),
                subtotalLbl, sgstAmtLbl, cgstAmtLbl, gstTotalLbl, grandTotalLbl);
        includeGstChk.selectedProperty().addListener((o, ov, nv) ->
                recalcTotals(itemTable, nv, subtotalLbl, sgstAmtLbl, cgstAmtLbl, gstTotalLbl, grandTotalLbl));

        form.getChildren().add(totalsGrid);

        TextArea notes = new TextArea(invoice.getNotes());
        notes.setPromptText("Notes / remarks...");
        notes.setPrefRowCount(2);
        form.getChildren().add(notes);

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        dialog.getDialogPane().setContent(scroll);

        ButtonType saveType   = new ButtonType("Save Invoice", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = ButtonType.CANCEL;
        dialog.getDialogPane().getButtonTypes().addAll(saveType, cancelType);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                if (custBox.getValue() == null) { showAlert("Validation", "Please select a customer."); return null; }
                invoice.setCustomer(custBox.getValue());
                invoice.setInvoiceDate(invDate.getValue());
                invoice.setDueDate(dueDate.getValue());
                invoice.setIncludeGst(includeGstChk.isSelected());
                invoice.setNotes(notes.getText());
                invoice.setItems(itemTable.getItems());
                invoice.recalculate();
                return invoice;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(inv -> {
            try {
                invoiceDAO.save(inv);
                loadData(searchField.getText());
                showAlert("Success", "Invoice " + inv.getInvoiceNumber() + " saved.");
            } catch (Exception ex) { showAlert("Error", ex.getMessage()); }
        });
    }

    private void recalcTotals(TableView<InvoiceItem> t, boolean includeGst,
                               Label subtotalLbl, Label sgstLbl, Label cgstLbl,
                               Label gstTotalLbl, Label grandTotalLbl) {
        double sub  = t.getItems().stream().mapToDouble(InvoiceItem::getTotal).sum();
        double sgst = includeGst ? t.getItems().stream().mapToDouble(InvoiceItem::getSgstAmount).sum() : 0;
        double cgst = includeGst ? t.getItems().stream().mapToDouble(InvoiceItem::getCgstAmount).sum() : 0;
        subtotalLbl.setText(inr(sub));
        sgstLbl.setText(inr(sgst));
        cgstLbl.setText(inr(cgst));
        gstTotalLbl.setText(inr(sgst + cgst));
        grandTotalLbl.setText(inr(sub + sgst + cgst));
    }

    private TableColumn<Invoice, String> col(String name, int width) {
        TableColumn<Invoice, String> c = new TableColumn<>(name); c.setPrefWidth(width); return c;
    }

    private TableColumn<InvoiceItem, String> itemCol(String name, int width) {
        TableColumn<InvoiceItem, String> c = new TableColumn<>(name); c.setPrefWidth(width); return c;
    }

    private TableCell<Invoice, String> statusCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(s);
                badge.setPadding(new Insets(2, 10, 2, 10));
                badge.setFont(Font.font("Arial", FontWeight.BOLD, 11));
                String bg = switch (s) { case "PAID" -> "#D1FAE5"; case "PARTIAL" -> "#FEF3C7"; default -> "#FEE2E2"; };
                String fg = switch (s) { case "PAID" -> "#065F46"; case "PARTIAL" -> "#92400E"; default -> "#991B1B"; };
                badge.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:20;-fx-text-fill:" + fg + ";");
                setGraphic(badge); setText(null);
            }
        };
    }

    private TableCell<Invoice, Void> actionsCell() {
        return new TableCell<>() {
            final Button editBtn    = new Button("✏ Edit");
            final Button previewBtn = new Button("👁 View");
            final Button payBtn     = new Button("💰 Pay");
            final Button delBtn     = new Button("✕");
            final HBox box = new HBox(5, editBtn, previewBtn, payBtn, delBtn);
            {
                editBtn.setStyle("-fx-background-color:#DBEAFE;-fx-text-fill:#1D4ED8;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:11;-fx-padding:4 8;");
                previewBtn.setStyle("-fx-background-color:#EDE9FE;-fx-text-fill:#5B21B6;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:11;-fx-padding:4 8;");
                payBtn.setStyle("-fx-background-color:#D1FAE5;-fx-text-fill:#065F46;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:11;-fx-padding:4 8;");
                delBtn.setStyle("-fx-background-color:#FEE2E2;-fx-text-fill:#DC2626;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:11;-fx-padding:4 8;");

                editBtn.setOnAction(e -> openInvoiceForm(getTableView().getItems().get(getIndex())));
                previewBtn.setOnAction(e -> {
                    Invoice inv = getTableView().getItems().get(getIndex());
                    try {
                        Invoice full = invoiceDAO.getById(inv.getId());
                        if (full == null) full = inv;
                        InvoicePreviewWindow.show(full);
                    } catch (Exception ex) { showAlert("Error", "Could not load invoice: " + ex.getMessage()); }
                });
                payBtn.setOnAction(e -> {
                    Invoice inv = getTableView().getItems().get(getIndex());
                    if (inv.getStatus().equals("PAID")) { showAlert("Info", "Invoice is already fully paid."); return; }
                    paymentPanel.openForm(inv);
                    loadData(searchField.getText());
                });
                delBtn.setOnAction(e -> {
                    Invoice inv = getTableView().getItems().get(getIndex());
                    new Alert(Alert.AlertType.CONFIRMATION, "Delete invoice " + inv.getInvoiceNumber() + "?", ButtonType.YES, ButtonType.NO)
                        .showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
                            try { invoiceDAO.delete(inv.getId()); loadData(searchField.getText()); }
                            catch (Exception ex) { showAlert("Error", ex.getMessage()); }
                        });
                });
            }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : box); }
        };
    }

    private void exportData(String format) {
        javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
        dc.setTitle("Choose export folder");
        dc.setInitialDirectory(new java.io.File(System.getProperty("user.home")));
        java.io.File dir = dc.showDialog(null);
        if (dir == null) return;
        try {
            List<Invoice> data = invoiceDAO.getAll();
            String path = format.equals("excel")
                ? com.buildmat.util.DataExporter.exportInvoicesExcel(data, dir.getAbsolutePath())
                : com.buildmat.util.DataExporter.exportInvoicesPdf(data, dir.getAbsolutePath());
            showAlert("Export successful", "Saved to:\n" + path);
            try { java.awt.Desktop.getDesktop().open(new java.io.File(path)); } catch (Exception ignored) {}
        } catch (Exception ex) { showAlert("Export failed", ex.getMessage()); }
    }

    private Label label(String text) { Label l = new Label(text); l.setFont(Font.font("Arial", 13)); return l; }
    private void styleBtn(Button b, String color) { b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 16;"); }
    private void styleInput(TextField tf) { tf.setStyle("-fx-background-radius:8;-fx-border-color:#D1D5DB;-fx-border-radius:8;-fx-padding:8;"); }
    private String inr(double v) { return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(v); }
    private String formatNum(double v) { return v == (long) v ? String.valueOf((long) v) : String.valueOf(v); }
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
    private void showAlert(String msg) { showAlert("Info", msg); }
    public VBox getView() { return view; }
}
