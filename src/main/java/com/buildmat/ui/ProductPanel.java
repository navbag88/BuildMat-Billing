package com.buildmat.ui;

import com.buildmat.dao.ProductDAO;
import com.buildmat.model.Product;
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
import java.util.List;
import java.util.Locale;

public class ProductPanel {

    private final ProductDAO productDAO = new ProductDAO();
    private VBox view;
    private TableView<Product> table;
    private TextField searchField;

    public ProductPanel() { buildView(); }
    public void refresh() { loadData(""); }

    private void buildView() {
        view = new VBox(16);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Products & Inventory");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#1A2332"));

        searchField = new TextField();
        searchField.setPromptText("Search by name or category...");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((o, ov, nv) -> loadData(nv));

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button importBtn = new Button("⬆ Import Excel");
        importBtn.setStyle("-fx-background-color:#16A34A;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 14;");
        importBtn.setOnAction(e -> new ImportDialog(ImportDialog.ImportType.PRODUCTS, () -> loadData("")).show());

        Button exportExcelBtn = new Button("📊 Export Excel");
        exportExcelBtn.setStyle("-fx-background-color:#7C3AED;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 14;");
        exportExcelBtn.setOnAction(e -> exportData("excel"));

        Button exportPdfBtn = new Button("📄 Export PDF");
        exportPdfBtn.setStyle("-fx-background-color:#DC2626;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 14;");
        exportPdfBtn.setOnAction(e -> exportData("pdf"));

        Button addBtn = new Button("+ Add Product");
        addBtn.setStyle("-fx-background-color:#2563EB;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 16;");
        addBtn.setOnAction(e -> openForm(null));
        header.getChildren().addAll(title, spacer, searchField, importBtn, exportExcelBtn, exportPdfBtn, addBtn);
        view.getChildren().add(header);

        table = new TableView<>();
        table.setStyle("-fx-background-color:white;-fx-background-radius:12;");
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Product, String> nameCol = tc("Product Name", 200);
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));

        TableColumn<Product, String> catCol = tc("Category", 120);
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));

        TableColumn<Product, String> unitCol = tc("Unit", 70);
        unitCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUnit()));

        TableColumn<Product, String> priceCol = tc("Price / Unit", 120);
        priceCol.setCellValueFactory(d -> new SimpleStringProperty(
                NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(d.getValue().getPrice())));

        TableColumn<Product, String> sgstCol = tc("SGST %", 70);
        sgstCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSgstPercent() + "%"));

        TableColumn<Product, String> cgstCol = tc("CGST %", 70);
        cgstCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCgstPercent() + "%"));

        TableColumn<Product, String> gstCol = tc("Total GST %", 90);
        gstCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTotalGstPercent() + "%"));

        TableColumn<Product, String> stockCol = tc("Stock", 90);
        stockCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getStockQty())));
        stockCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                double qty = Double.parseDouble(s);
                Label badge = new Label(s);
                badge.setPadding(new Insets(2, 10, 2, 10));
                badge.setFont(Font.font("Arial", FontWeight.BOLD, 11));
                String bg = qty > 50 ? "#D1FAE5" : qty > 10 ? "#FEF3C7" : "#FEE2E2";
                String fg = qty > 50 ? "#065F46" : qty > 10 ? "#92400E" : "#991B1B";
                badge.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:20;-fx-text-fill:" + fg + ";");
                setGraphic(badge); setText(null);
            }
        });

        TableColumn<Product, Void> actCol = new TableColumn<>("Actions");
        actCol.setPrefWidth(140);
        actCol.setCellFactory(c -> new TableCell<>() {
            final Button edit = new Button("Edit"), del = new Button("Delete");
            final HBox box = new HBox(6, edit, del);
            { edit.setStyle("-fx-background-color:#DBEAFE;-fx-text-fill:#1D4ED8;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12;");
              del.setStyle("-fx-background-color:#FEE2E2;-fx-text-fill:#DC2626;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12;");
              edit.setOnAction(e -> openForm(getTableView().getItems().get(getIndex())));
              del.setOnAction(e -> {
                  Product p = getTableView().getItems().get(getIndex());
                  new Alert(Alert.AlertType.CONFIRMATION, "Delete product " + p.getName() + "?", ButtonType.YES, ButtonType.NO)
                      .showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
                          try { productDAO.delete(p.getId()); loadData(searchField.getText()); } catch (Exception ex) { showAlert(ex.getMessage()); }
                      });
              }); }
            @Override protected void updateItem(Void v, boolean e) { super.updateItem(v, e); setGraphic(e ? null : box); }
        });

        table.getColumns().addAll(nameCol, catCol, unitCol, priceCol, sgstCol, cgstCol, gstCol, stockCol, actCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        view.getChildren().add(table);
        loadData("");
    }

    private void loadData(String q) {
        try {
            List<Product> data = q.isBlank() ? productDAO.getAll() : productDAO.search(q);
            table.setItems(FXCollections.observableArrayList(data));
        } catch (Exception e) { showAlert(e.getMessage()); }
    }

    private void openForm(Product existing) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Product" : "Edit Product");
        dialog.getDialogPane().setPrefWidth(460);

        Product p = existing == null ? new Product() : existing;
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));

        TextField nameF  = field(p.getName());
        TextField catF   = field(p.getCategory());
        TextField unitF  = field(p.getUnit());
        TextField priceF = field(p.getPrice() == 0 ? "" : String.valueOf(p.getPrice()));
        TextField stockF = field(p.getStockQty() == 0 ? "" : String.valueOf(p.getStockQty()));
        TextField sgstF  = field(p.getSgstPercent() == 0 ? "0" : String.valueOf(p.getSgstPercent()));
        TextField cgstF  = field(p.getCgstPercent() == 0 ? "0" : String.valueOf(p.getCgstPercent()));
        Label totalGstLbl = new Label("0%");
        totalGstLbl.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        totalGstLbl.setTextFill(Color.web("#2563EB"));

        // Live total GST label
        Runnable updateTotal = () -> {
            try {
                double s = Double.parseDouble(sgstF.getText());
                double c = Double.parseDouble(cgstF.getText());
                totalGstLbl.setText("Total GST: " + (s + c) + "%");
            } catch (Exception ignored) { totalGstLbl.setText("Total GST: ?"); }
        };
        sgstF.textProperty().addListener((o, ov, nv) -> updateTotal.run());
        cgstF.textProperty().addListener((o, ov, nv) -> updateTotal.run());
        updateTotal.run();

        // GST section separator
        Separator sep = new Separator();
        Label gstSectionLbl = new Label("GST Configuration");
        gstSectionLbl.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        gstSectionLbl.setTextFill(Color.web("#2563EB"));

        grid.add(lbl("Product Name *"), 0, 0);  grid.add(nameF, 1, 0);
        grid.add(lbl("Category"),        0, 1);  grid.add(catF, 1, 1);
        grid.add(lbl("Unit (Bag/Kg...)"), 0, 2); grid.add(unitF, 1, 2);
        grid.add(lbl("Price per Unit *"), 0, 3); grid.add(priceF, 1, 3);
        grid.add(lbl("Stock Quantity"),   0, 4); grid.add(stockF, 1, 4);
        grid.add(sep,                     0, 5, 2, 1);
        grid.add(gstSectionLbl,           0, 6, 2, 1);
        grid.add(lbl("SGST %"),           0, 7); grid.add(sgstF, 1, 7);
        grid.add(lbl("CGST %"),           0, 8); grid.add(cgstF, 1, 8);
        grid.add(new Label(""),           0, 9); grid.add(totalGstLbl, 1, 9);

        dialog.getDialogPane().setContent(grid);
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == save) {
                if (nameF.getText().isBlank() || priceF.getText().isBlank()) { showAlert("Name and price are required."); return null; }
                p.setName(nameF.getText()); p.setCategory(catF.getText()); p.setUnit(unitF.getText());
                try { p.setPrice(Double.parseDouble(priceF.getText())); } catch (Exception e) { showAlert("Invalid price."); return null; }
                try { p.setStockQty(Double.parseDouble(stockF.getText())); } catch (Exception ignored) {}
                try { p.setSgstPercent(Double.parseDouble(sgstF.getText())); } catch (Exception ignored) { p.setSgstPercent(0); }
                try { p.setCgstPercent(Double.parseDouble(cgstF.getText())); } catch (Exception ignored) { p.setCgstPercent(0); }
                return p;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(saved -> {
            try { productDAO.save(saved); loadData(searchField.getText()); } catch (Exception e) { showAlert(e.getMessage()); }
        });
    }

    private void exportData(String format) {
        javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
        dc.setTitle("Choose export folder");
        dc.setInitialDirectory(new java.io.File(System.getProperty("user.home")));
        java.io.File dir = dc.showDialog(null);
        if (dir == null) return;
        try {
            List<Product> data = productDAO.getAll();
            String path = format.equals("excel")
                ? com.buildmat.util.DataExporter.exportProductsExcel(data, dir.getAbsolutePath())
                : com.buildmat.util.DataExporter.exportProductsPdf(data, dir.getAbsolutePath());
            showAlert("Export successful! Saved to:\n" + path);
            try { java.awt.Desktop.getDesktop().open(new java.io.File(path)); } catch (Exception ignored) {}
        } catch (Exception ex) { showAlert("Export failed: " + ex.getMessage()); }
    }

    private TableColumn<Product, String> tc(String name, int w) {
        TableColumn<Product, String> c = new TableColumn<>(name); c.setPrefWidth(w); return c;
    }
    private TextField field(String val) { TextField tf = new TextField(val == null ? "" : val); tf.setPrefWidth(230); return tf; }
    private Label lbl(String t) { return new Label(t); }
    private void showAlert(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    public VBox getView() { return view; }
}
