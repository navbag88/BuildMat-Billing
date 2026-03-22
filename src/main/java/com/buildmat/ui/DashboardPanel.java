package com.buildmat.ui;

import com.buildmat.dao.CustomerDAO;
import com.buildmat.dao.InvoiceDAO;
import com.buildmat.dao.ProductDAO;
import com.buildmat.model.Invoice;
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

public class DashboardPanel {

    private final MainWindow mainWindow;
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private VBox view;

    public DashboardPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        buildView();
    }

    public void refresh() { buildView(); }

    private void buildView() {
        view = new VBox(20);
        view.setPadding(new Insets(0));

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Dashboard");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#1A2332"));
        Button newInvoiceBtn = new Button("+ New Invoice");
        styleActionButton(newInvoiceBtn, "#2563EB");
        newInvoiceBtn.setOnAction(e -> mainWindow.showInvoices());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, newInvoiceBtn);
        view.getChildren().add(header);

        try {
            double revenue = invoiceDAO.getTotalRevenue();
            double outstanding = invoiceDAO.getTotalOutstanding();
            int unpaid = invoiceDAO.getCountByStatus("UNPAID");
            int partial = invoiceDAO.getCountByStatus("PARTIAL");
            int paid = invoiceDAO.getCountByStatus("PAID");
            int customers = customerDAO.getAll().size();
            int products = productDAO.getAll().size();

            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

            // Stats cards
            GridPane cards = new GridPane();
            cards.setHgap(16);
            cards.setVgap(16);
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25);
            cards.getColumnConstraints().addAll(col, col, col, col);

            cards.add(statCard("Total Revenue", fmt.format(revenue), "#2563EB", "#EFF6FF"), 0, 0);
            cards.add(statCard("Outstanding", fmt.format(outstanding), "#DC2626", "#FEF2F2"), 1, 0);
            cards.add(statCard("Paid Invoices", String.valueOf(paid), "#16A34A", "#F0FDF4"), 2, 0);
            cards.add(statCard("Unpaid / Partial", unpaid + " / " + partial, "#D97706", "#FFFBEB"), 3, 0);
            view.getChildren().add(cards);

            // Second row
            GridPane row2 = new GridPane();
            row2.setHgap(16);
            row2.setVgap(16);
            ColumnConstraints half = new ColumnConstraints();
            half.setPercentWidth(50);
            row2.getColumnConstraints().addAll(half, half);

            row2.add(statCard("Total Customers", String.valueOf(customers), "#7C3AED", "#F5F3FF"), 0, 0);
            row2.add(statCard("Products in Catalog", String.valueOf(products), "#0891B2", "#ECFEFF"), 1, 0);
            view.getChildren().add(row2);

            // Recent invoices
            Label recentTitle = new Label("Recent Invoices");
            recentTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            recentTitle.setTextFill(Color.web("#1A2332"));
            view.getChildren().add(recentTitle);

            List<Invoice> recent = invoiceDAO.getAll();
            if (recent.size() > 8) recent = recent.subList(0, 8);

            TableView<Invoice> table = buildRecentInvoiceTable(recent);
            VBox.setVgrow(table, Priority.ALWAYS);
            view.getChildren().add(table);

        } catch (Exception ex) {
            view.getChildren().add(new Label("Error loading dashboard: " + ex.getMessage()));
        }
    }

    private VBox statCard(String label, String value, String accentColor, String bgColor) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12; -fx-border-color: #E5E7EB; -fx-border-radius: 12; -fx-border-width: 1;");

        Label lbl = new Label(label);
        lbl.setFont(Font.font("Arial", 13));
        lbl.setTextFill(Color.web("#6B7280"));

        Label val = new Label(value);
        val.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        val.setTextFill(Color.web(accentColor));

        card.getChildren().addAll(lbl, val);
        return card;
    }

    private TableView<Invoice> buildRecentInvoiceTable(List<Invoice> invoices) {
        TableView<Invoice> table = new TableView<>();
        table.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        table.setMaxHeight(280);

        TableColumn<Invoice, String> numCol = new TableColumn<>("Invoice #");
        numCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getInvoiceNumber()));
        numCol.setPrefWidth(140);

        TableColumn<Invoice, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getCustomer() != null ? d.getValue().getCustomer().getName() : "—"));
        custCol.setPrefWidth(200);

        TableColumn<Invoice, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getInvoiceDate().toString()));
        dateCol.setPrefWidth(120);

        TableColumn<Invoice, String> amtCol = new TableColumn<>("Amount");
        amtCol.setCellValueFactory(d -> {
            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            return new javafx.beans.property.SimpleStringProperty(fmt.format(d.getValue().getTotalAmount()));
        });
        amtCol.setPrefWidth(140);

        TableColumn<Invoice, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setGraphic(null); return; }
                Label badge = new Label(s);
                badge.setPadding(new Insets(2, 10, 2, 10));
                badge.setFont(Font.font("Arial", FontWeight.BOLD, 11));
                String bg = switch (s) {
                    case "PAID" -> "#D1FAE5"; case "PARTIAL" -> "#FEF3C7"; default -> "#FEE2E2";
                };
                String fg = switch (s) {
                    case "PAID" -> "#065F46"; case "PARTIAL" -> "#92400E"; default -> "#991B1B";
                };
                badge.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 20; -fx-text-fill: " + fg + ";");
                setGraphic(badge); setText(null);
            }
        });
        statusCol.setPrefWidth(100);

        table.getColumns().addAll(numCol, custCol, dateCol, amtCol, statusCol);
        table.getItems().addAll(invoices);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    private void styleActionButton(Button btn, String color) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 14; -fx-cursor: hand; -fx-padding: 10 20;");
    }

    public VBox getView() { return view; }
}
