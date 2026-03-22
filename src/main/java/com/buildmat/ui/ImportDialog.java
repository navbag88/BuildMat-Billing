package com.buildmat.ui;

import com.buildmat.dao.CustomerDAO;
import com.buildmat.dao.ProductDAO;
import com.buildmat.model.Customer;
import com.buildmat.model.Product;
import com.buildmat.util.ExcelImporter;
import com.buildmat.util.ExcelImporter.ImportResult;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class ImportDialog {

    public enum ImportType { CUSTOMERS, PRODUCTS }

    private final ImportType type;
    private final Runnable onSuccess;
    private Stage stage;

    // Preview state
    private List<?> previewItems;
    private VBox previewBox;
    private Label statusLabel;
    private Button importBtn;
    private Label fileLabel;
    private String selectedFilePath;

    public ImportDialog(ImportType type, Runnable onSuccess) {
        this.type = type;
        this.onSuccess = onSuccess;
    }

    public void show() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle((type == ImportType.CUSTOMERS ? "Import Customers" : "Import Products") + " from Excel");
        stage.setWidth(820);
        stage.setHeight(680);
        stage.setMinWidth(700);
        stage.setMinHeight(500);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#F0F2F5;");

        // ── Top bar ───────────────────────────────────────────────
        HBox topBar = new HBox(14);
        topBar.setPadding(new Insets(18, 24, 18, 24));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color:#1A2332;");

        String icon = type == ImportType.CUSTOMERS ? "👥" : "📦";
        Label titleLbl = new Label(icon + "  " + (type == ImportType.CUSTOMERS ? "Import Customers" : "Import Products"));
        titleLbl.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleLbl.setTextFill(Color.WHITE);
        topBar.getChildren().add(titleLbl);
        root.getChildren().add(topBar);

        // ── Main content ──────────────────────────────────────────
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        VBox.setVgrow(content, Priority.ALWAYS);

        // Step 1: Download template
        VBox step1 = buildStepCard("1", "Download Template",
            "Download the Excel template, fill in your data, then upload it back.",
            "#2563EB");

        Button templateBtn = stepBtn("⬇  Download Template", "#2563EB");
        templateBtn.setOnAction(e -> downloadTemplate());
        step1.getChildren().add(templateBtn);
        content.getChildren().add(step1);

        // Step 2: Upload file
        VBox step2 = buildStepCard("2", "Select Your Excel File",
            "Select the filled Excel file (.xlsx). Columns will be auto-detected.", "#16A34A");

        HBox fileRow = new HBox(10);
        fileRow.setAlignment(Pos.CENTER_LEFT);
        fileLabel = new Label("No file selected");
        fileLabel.setFont(Font.font("Arial", 13));
        fileLabel.setTextFill(Color.web("#6B7280"));
        fileLabel.setMaxWidth(400);

        Button browseBtn = stepBtn("📂  Browse File", "#16A34A");
        browseBtn.setOnAction(e -> browseFile());
        fileRow.getChildren().addAll(browseBtn, fileLabel);
        step2.getChildren().add(fileRow);
        content.getChildren().add(step2);

        // Step 3: Preview & import
        VBox step3 = buildStepCard("3", "Preview & Import",
            "Review the data before importing. Rows with errors will be skipped.", "#7C3AED");

        statusLabel = new Label("Upload a file to see a preview.");
        statusLabel.setFont(Font.font("Arial", 12));
        statusLabel.setTextFill(Color.web("#6B7280"));
        statusLabel.setWrapText(true);

        previewBox = new VBox(8);
        previewBox.setVisible(false);

        importBtn = stepBtn("✓  Import Now", "#7C3AED");
        importBtn.setDisable(true);
        importBtn.setOnAction(e -> doImport());

        step3.getChildren().addAll(statusLabel, previewBox, importBtn);
        content.getChildren().add(step3);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#F0F2F5; -fx-background:#F0F2F5;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().add(scroll);

        // ── Bottom bar ────────────────────────────────────────────
        HBox bottomBar = new HBox();
        bottomBar.setPadding(new Insets(12, 20, 12, 20));
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setStyle("-fx-background-color:white;-fx-border-color:#E5E7EB;-fx-border-width:1 0 0 0;");
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color:#F3F4F6;-fx-text-fill:#374151;-fx-background-radius:8;-fx-cursor:hand;-fx-font-size:13;-fx-padding:8 20;");
        closeBtn.setOnAction(e -> stage.close());
        bottomBar.getChildren().add(closeBtn);
        root.getChildren().add(bottomBar);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    // ── Step 1: Download template ──────────────────────────────────────────────
    private void downloadTemplate() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choose folder to save template");
        dc.setInitialDirectory(new File(System.getProperty("user.home")));
        File dir = dc.showDialog(stage);
        if (dir == null) return;
        try {
            String path = type == ImportType.CUSTOMERS
                ? ExcelImporter.generateCustomerTemplate(dir.getAbsolutePath())
                : ExcelImporter.generateProductTemplate(dir.getAbsolutePath());
            java.awt.Desktop.getDesktop().open(new File(path));
            alert(Alert.AlertType.INFORMATION, "Template Downloaded",
                "Template saved and opened:\n" + path + "\n\nFill in your data and upload it in Step 2.");
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Error", "Failed to create template:\n" + ex.getMessage());
        }
    }

    // ── Step 2: Browse file ────────────────────────────────────────────────────
    private void browseFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Excel File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", "*.xlsx"));
        fc.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        selectedFilePath = file.getAbsolutePath();
        fileLabel.setText(file.getName());
        fileLabel.setTextFill(Color.web("#374151"));

        // Auto-parse and preview
        parseAndPreview();
    }

    // ── Step 3: Parse file and show preview ────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void parseAndPreview() {
        previewBox.getChildren().clear();
        previewBox.setVisible(false);
        importBtn.setDisable(true);
        previewItems = null;

        try {
            if (type == ImportType.CUSTOMERS) {
                ImportResult<Customer> result = ExcelImporter.importCustomers(selectedFilePath);
                previewItems = result.imported;
                showCustomerPreview(result);
            } else {
                ImportResult<Product> result = ExcelImporter.importProducts(selectedFilePath);
                previewItems = result.imported;
                showProductPreview(result);
            }
        } catch (Exception ex) {
            statusLabel.setText("❌ Failed to read file: " + ex.getMessage());
            statusLabel.setTextFill(Color.web("#DC2626"));
        }
    }

    private void showCustomerPreview(ImportResult<Customer> result) {
        renderStatus(result);
        if (result.imported.isEmpty()) return;

        TableView<Customer> table = new TableView<>();
        table.setPrefHeight(Math.min(280, 40 + result.imported.size() * 36));
        table.setStyle("-fx-background-color:white;-fx-background-radius:8;");

        TableColumn<Customer, String> nameCol = tcStr("Name", 180);
        nameCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getName()));
        TableColumn<Customer, String> phoneCol = tcStr("Phone", 130);
        phoneCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getPhone()));
        TableColumn<Customer, String> emailCol = tcStr("Email", 200);
        emailCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getEmail()));
        TableColumn<Customer, String> addrCol = tcStr("Address", 240);
        addrCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getAddress()));

        table.getColumns().addAll(nameCol, phoneCol, emailCol, addrCol);
        table.getItems().addAll(result.imported);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        previewBox.getChildren().add(table);

        renderErrors(result);
        previewBox.setVisible(true);
        importBtn.setDisable(false);
    }

    private void showProductPreview(ImportResult<Product> result) {
        renderStatus(result);
        if (result.imported.isEmpty()) return;

        TableView<Product> table = new TableView<>();
        table.setPrefHeight(Math.min(280, 40 + result.imported.size() * 36));
        table.setStyle("-fx-background-color:white;-fx-background-radius:8;");

        TableColumn<Product, String> nameCol = tcStr("Name", 160);
        nameCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getName()));
        TableColumn<Product, String> catCol = tcStr("Category", 100);
        catCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getCategory()));
        TableColumn<Product, String> unitCol = tcStr("Unit", 70);
        unitCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getUnit()));
        TableColumn<Product, String> priceCol = tcStr("Price", 90);
        priceCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("en","IN")).format(d.getValue().getPrice())));
        TableColumn<Product, String> stockCol = tcStr("Stock", 70);
        stockCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(String.valueOf(d.getValue().getStockQty())));
        TableColumn<Product, String> sgstCol = tcStr("SGST%", 65);
        sgstCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getSgstPercent() + "%"));
        TableColumn<Product, String> cgstCol = tcStr("CGST%", 65);
        cgstCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getCgstPercent() + "%"));

        table.getColumns().addAll(nameCol, catCol, unitCol, priceCol, stockCol, sgstCol, cgstCol);
        table.getItems().addAll(result.imported);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        previewBox.getChildren().add(table);

        renderErrors(result);
        previewBox.setVisible(true);
        importBtn.setDisable(false);
    }

    private void renderStatus(ImportResult<?> result) {
        if (result.imported.isEmpty() && result.errors.isEmpty()) {
            statusLabel.setText("⚠ No valid data rows found in the file. Please check your data.");
            statusLabel.setTextFill(Color.web("#D97706"));
            return;
        }
        String msg = "✓ Found " + result.imported.size() + " valid record(s) ready to import.";
        if (result.skipped > 0) msg += "  |  Skipped " + result.skipped + " empty row(s).";
        if (!result.errors.isEmpty()) msg += "  |  " + result.errors.size() + " error(s) (shown below).";
        statusLabel.setText(msg);
        statusLabel.setTextFill(result.errors.isEmpty() ? Color.web("#16A34A") : Color.web("#D97706"));
    }

    private void renderErrors(ImportResult<?> result) {
        if (result.errors.isEmpty()) return;
        VBox errBox = new VBox(4);
        errBox.setPadding(new Insets(10));
        errBox.setStyle("-fx-background-color:#FEF2F2;-fx-background-radius:8;-fx-border-color:#FCA5A5;-fx-border-radius:8;-fx-border-width:1;");
        Label errTitle = new Label("⚠ Rows with errors (will be skipped):");
        errTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        errTitle.setTextFill(Color.web("#DC2626"));
        errBox.getChildren().add(errTitle);
        for (String err : result.errors) {
            Label l = new Label("• " + err);
            l.setFont(Font.font("Arial", 11));
            l.setTextFill(Color.web("#991B1B"));
            l.setWrapText(true);
            errBox.getChildren().add(l);
        }
        previewBox.getChildren().add(errBox);
    }

    // ── Do the actual import ───────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void doImport() {
        if (previewItems == null || previewItems.isEmpty()) return;

        importBtn.setDisable(true);
        importBtn.setText("Importing...");

        try {
            int count = 0;
            if (type == ImportType.CUSTOMERS) {
                CustomerDAO dao = new CustomerDAO();
                for (Customer c : (List<Customer>) previewItems) {
                    dao.save(c); count++;
                }
            } else {
                ProductDAO dao = new ProductDAO();
                for (Product p : (List<Product>) previewItems) {
                    dao.save(p); count++;
                }
            }

            alert(Alert.AlertType.INFORMATION, "Import Successful",
                "✓ Successfully imported " + count + " " +
                (type == ImportType.CUSTOMERS ? "customer(s)" : "product(s)") + ".");

            if (onSuccess != null) onSuccess.run();
            stage.close();

        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Import Failed", "Error during import:\n" + ex.getMessage());
            importBtn.setDisable(false);
            importBtn.setText("✓  Import Now");
        }
    }

    // ── UI helpers ─────────────────────────────────────────────────────────────
    private VBox buildStepCard(String stepNum, String title, String desc, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color:white;-fx-background-radius:12;" +
                      "-fx-border-color:#E5E7EB;-fx-border-radius:12;-fx-border-width:1;");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label numBadge = new Label(stepNum);
        numBadge.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;" +
                          "-fx-font-weight:bold;-fx-font-size:12;-fx-padding:3 9;-fx-background-radius:20;");
        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        titleLbl.setTextFill(Color.web("#1A2332"));
        titleRow.getChildren().addAll(numBadge, titleLbl);

        Label descLbl = new Label(desc);
        descLbl.setFont(Font.font("Arial", 12));
        descLbl.setTextFill(Color.web("#6B7280"));
        descLbl.setWrapText(true);

        card.getChildren().addAll(titleRow, descLbl);
        return card;
    }

    private Button stepBtn(String label, String color) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;" +
                   "-fx-background-radius:8;-fx-cursor:hand;-fx-font-size:13;-fx-padding:9 20;");
        b.setOnMouseEntered(e -> b.setOpacity(0.88));
        b.setOnMouseExited(e -> b.setOpacity(1.0));
        return b;
    }

    private <T> TableColumn<T, String> tcStr(String name, int width) {
        TableColumn<T, String> c = new TableColumn<>(name);
        c.setPrefWidth(width);
        return c;
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.initOwner(stage); a.showAndWait();
    }
}
