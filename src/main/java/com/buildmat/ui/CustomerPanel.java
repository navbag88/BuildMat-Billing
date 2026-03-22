package com.buildmat.ui;

import com.buildmat.dao.CustomerDAO;
import com.buildmat.model.Customer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class CustomerPanel {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private VBox view;
    private TableView<Customer> table;
    private TextField searchField;

    public CustomerPanel() { buildView(); }
    public void refresh() { loadData(""); }

    private void buildView() {
        view = new VBox(16);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Customers");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#1A2332"));

        searchField = new TextField();
        searchField.setPromptText("Search by name or phone...");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((o, ov, nv) -> loadData(nv));

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button importBtn = new Button("⬆ Import Excel");
        importBtn.setStyle("-fx-background-color:#16A34A;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 14;");
        importBtn.setOnAction(e -> new ImportDialog(ImportDialog.ImportType.CUSTOMERS, () -> loadData("")).show());

        Button exportExcelBtn = new Button("📊 Export Excel");
        exportExcelBtn.setStyle("-fx-background-color:#7C3AED;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 14;");
        exportExcelBtn.setOnAction(e -> exportData("excel"));

        Button exportPdfBtn = new Button("📄 Export PDF");
        exportPdfBtn.setStyle("-fx-background-color:#DC2626;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 14;");
        exportPdfBtn.setOnAction(e -> exportData("pdf"));

        Button addBtn = new Button("+ Add Customer");
        addBtn.setStyle("-fx-background-color:#2563EB;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 16;");
        addBtn.setOnAction(e -> openForm(null));
        header.getChildren().addAll(title, spacer, searchField, importBtn, exportExcelBtn, exportPdfBtn, addBtn);
        view.getChildren().add(header);

        table = new TableView<>();
        table.setStyle("-fx-background-color:white;-fx-background-radius:12;");
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Customer, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<Customer, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPhone()));
        phoneCol.setPrefWidth(150);

        TableColumn<Customer, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        emailCol.setPrefWidth(200);

        TableColumn<Customer, String> addrCol = new TableColumn<>("Address");
        addrCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAddress()));
        addrCol.setPrefWidth(300);

        TableColumn<Customer, Void> actCol = new TableColumn<>("Actions");
        actCol.setPrefWidth(140);
        actCol.setCellFactory(c -> new TableCell<>() {
            final Button edit = new Button("Edit"), del = new Button("Delete");
            final HBox box = new HBox(6, edit, del);
            { edit.setStyle("-fx-background-color:#DBEAFE;-fx-text-fill:#1D4ED8;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12;");
              del.setStyle("-fx-background-color:#FEE2E2;-fx-text-fill:#DC2626;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:12;");
              edit.setOnAction(e -> openForm(getTableView().getItems().get(getIndex())));
              del.setOnAction(e -> {
                  Customer c2 = getTableView().getItems().get(getIndex());
                  new Alert(Alert.AlertType.CONFIRMATION, "Delete customer " + c2.getName() + "?", ButtonType.YES, ButtonType.NO)
                      .showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
                          try { customerDAO.delete(c2.getId()); loadData(searchField.getText()); } catch (Exception ex) { showAlert(ex.getMessage()); }
                      });
              }); }
            @Override protected void updateItem(Void v, boolean e) { super.updateItem(v, e); setGraphic(e ? null : box); }
        });

        table.getColumns().addAll(nameCol, phoneCol, emailCol, addrCol, actCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        view.getChildren().add(table);
        loadData("");
    }

    private void loadData(String q) {
        try {
            List<Customer> data = q.isBlank() ? customerDAO.getAll() : customerDAO.search(q);
            table.setItems(FXCollections.observableArrayList(data));
        } catch (Exception e) { showAlert(e.getMessage()); }
    }

    private void openForm(Customer existing) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Customer" : "Edit Customer");
        dialog.getDialogPane().setPrefWidth(450);

        Customer c = existing == null ? new Customer() : existing;
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));

        TextField nameF = field(c.getName()), phoneF = field(c.getPhone()), emailF = field(c.getEmail());
        TextArea addrF = new TextArea(c.getAddress()); addrF.setPrefRowCount(3);

        grid.add(lbl("Name *"), 0, 0); grid.add(nameF, 1, 0);
        grid.add(lbl("Phone"), 0, 1); grid.add(phoneF, 1, 1);
        grid.add(lbl("Email"), 0, 2); grid.add(emailF, 1, 2);
        grid.add(lbl("Address"), 0, 3); grid.add(addrF, 1, 3);

        dialog.getDialogPane().setContent(grid);
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == save) {
                if (nameF.getText().isBlank()) { showAlert("Name is required."); return null; }
                c.setName(nameF.getText()); c.setPhone(phoneF.getText());
                c.setEmail(emailF.getText()); c.setAddress(addrF.getText());
                return c;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(saved -> {
            try { customerDAO.save(saved); loadData(searchField.getText()); } catch (Exception e) { showAlert(e.getMessage()); }
        });
    }

    private void exportData(String format) {
        javafx.stage.DirectoryChooser dc = new javafx.stage.DirectoryChooser();
        dc.setTitle("Choose export folder");
        dc.setInitialDirectory(new java.io.File(System.getProperty("user.home")));
        java.io.File dir = dc.showDialog(null);
        if (dir == null) return;
        try {
            List<Customer> data = customerDAO.getAll();
            String path = format.equals("excel")
                ? com.buildmat.util.DataExporter.exportCustomersExcel(data, dir.getAbsolutePath())
                : com.buildmat.util.DataExporter.exportCustomersPdf(data, dir.getAbsolutePath());
            showAlert("Export successful! Saved to:\n" + path);
            try { java.awt.Desktop.getDesktop().open(new java.io.File(path)); } catch (Exception ignored) {}
        } catch (Exception ex) { showAlert("Export failed: " + ex.getMessage()); }
    }

    private TextField field(String val) { TextField tf = new TextField(val == null ? "" : val); tf.setPrefWidth(250); return tf; }
    private Label lbl(String t) { return new Label(t); }
    private void showAlert(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    public VBox getView() { return view; }
}
