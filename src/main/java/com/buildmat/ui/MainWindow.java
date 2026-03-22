package com.buildmat.ui;

import com.buildmat.dao.UserDAO;
import com.buildmat.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class MainWindow {

    private BorderPane root;
    private StackPane contentArea;
    private DashboardPanel dashboardPanel;
    private InvoicePanel invoicePanel;
    private CustomerPanel customerPanel;
    private ProductPanel productPanel;
    private PaymentPanel paymentPanel;
    private UserManagementPanel userPanel;
    private ReportsPanel reportsPanel;

    public void show(Stage stage) {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #F0F2F5;");
        root.setLeft(buildSidebar(stage));

        contentArea = new StackPane();
        contentArea.setPadding(new Insets(24));
        root.setCenter(contentArea);

        dashboardPanel  = new DashboardPanel(this);
        invoicePanel    = new InvoicePanel(this);
        customerPanel   = new CustomerPanel();
        productPanel    = new ProductPanel();
        paymentPanel    = new PaymentPanel();
        userPanel       = new UserManagementPanel();
        reportsPanel    = new ReportsPanel();
        reportsPanel    = new ReportsPanel();

        showDashboard();

        Scene scene = new Scene(root, 1280, 800);
        stage.setTitle("BuildMat Billing — " + SessionManager.getCurrentUser().getFullName());
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();
    }

    private VBox buildSidebar(Stage stage) {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(228);
        sidebar.setStyle("-fx-background-color: #1A2332;");

        // ── Logo header ───────────────────────────────────────────
        VBox header = new VBox(4);
        header.setPadding(new Insets(24, 20, 20, 20));
        header.setStyle("-fx-background-color: #141C28;");
        Label logo = new Label("🏗 BuildMat");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        logo.setTextFill(Color.WHITE);
        Label sub = new Label("Billing System");
        sub.setFont(Font.font("Arial", 12));
        sub.setTextFill(Color.rgb(150, 170, 200));
        header.getChildren().addAll(logo, sub);
        sidebar.getChildren().add(header);

        // ── Nav items ─────────────────────────────────────────────
        VBox nav = new VBox(4);
        nav.setPadding(new Insets(16, 12, 8, 12));
        VBox.setVgrow(nav, Priority.ALWAYS);

        Label mainMenu = new Label("MAIN MENU");
        mainMenu.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        mainMenu.setTextFill(Color.rgb(100, 120, 150));
        mainMenu.setPadding(new Insets(4, 8, 4, 8));
        nav.getChildren().add(mainMenu);

        String[][] items = {
            {"📊", "Dashboard",  "dashboard"},
            {"🧾", "Invoices",   "invoices"},
            {"👥", "Customers",  "customers"},
            {"📦", "Products",   "products"},
            {"💰", "Payments",   "payments"},
            {"📋", "MIS Reports","reports"}
        };
        for (String[] item : items) {
            nav.getChildren().add(createNavButton(item[0], item[1], item[2]));
        }

        // Admin-only Users section
        if (SessionManager.isAdmin()) {
            Label adminMenu = new Label("ADMINISTRATION");
            adminMenu.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            adminMenu.setTextFill(Color.rgb(100, 120, 150));
            adminMenu.setPadding(new Insets(12, 8, 4, 8));
            nav.getChildren().add(adminMenu);
            nav.getChildren().add(createNavButton("🔐", "Users", "users"));
        }

        VBox.setVgrow(nav, Priority.ALWAYS);
        sidebar.getChildren().add(nav);

        // ── User info + Logout ────────────────────────────────────
        VBox footer = new VBox(10);
        footer.setPadding(new Insets(14, 16, 16, 16));
        footer.setStyle("-fx-background-color: #141C28;");

        // Logged-in user info
        HBox userInfo = new HBox(10);
        userInfo.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label(getInitials(SessionManager.getCurrentUser().getFullName()));
        avatar.setStyle("-fx-background-color:#2563EB;-fx-background-radius:20;-fx-text-fill:white;-fx-font-size:12;-fx-font-weight:bold;-fx-padding:6 9;");

        VBox userDetails = new VBox(2);
        Label userName = new Label(SessionManager.getCurrentUser().getFullName());
        userName.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        userName.setTextFill(Color.WHITE);
        Label userRole = new Label(SessionManager.getCurrentUser().getRole());
        userRole.setFont(Font.font("Arial", 11));
        userRole.setTextFill(Color.rgb(100, 130, 170));
        userDetails.getChildren().addAll(userName, userRole);

        userInfo.getChildren().addAll(avatar, userDetails);

        // Change password link
        Button changePwdBtn = new Button("Change Password");
        changePwdBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#6B9FD4;-fx-cursor:hand;-fx-font-size:11;-fx-padding:2 0;");
        changePwdBtn.setOnAction(e -> openChangeOwnPassword());

        // Logout button
        Button logoutBtn = new Button("⎋  Sign Out");
        logoutBtn.setPrefWidth(Double.MAX_VALUE);
        logoutBtn.setStyle("-fx-background-color:#DC2626;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-font-size:13;-fx-padding:8 0;");
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle("-fx-background-color:#B91C1C;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-font-size:13;-fx-padding:8 0;"));
        logoutBtn.setOnMouseExited(e -> logoutBtn.setStyle("-fx-background-color:#DC2626;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-font-size:13;-fx-padding:8 0;"));
        logoutBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Sign out of BuildMat Billing?", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Sign Out");
            confirm.setHeaderText(null);
            confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
                SessionManager.logout();
                LoginScreen loginScreen = new LoginScreen();
                loginScreen.show(stage);
            });
        });

        footer.getChildren().addAll(userInfo, changePwdBtn, logoutBtn);
        sidebar.getChildren().add(footer);

        return sidebar;
    }

    private Button createNavButton(String icon, String label, String id) {
        Button btn = new Button(icon + "  " + label);
        btn.setPrefWidth(204);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10, 16, 10, 16));
        btn.setFont(Font.font("Arial", 14));
        btn.setTextFill(Color.rgb(180, 200, 220));
        btn.setStyle("-fx-background-color:transparent;-fx-background-radius:8;-fx-cursor:hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color:#243447;-fx-background-radius:8;-fx-cursor:hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color:transparent;-fx-background-radius:8;-fx-cursor:hand;"));
        btn.setOnAction(e -> navigate(id));
        return btn;
    }

    public void navigate(String id) {
        contentArea.getChildren().clear();
        switch (id) {
            case "dashboard" -> { dashboardPanel.refresh(); contentArea.getChildren().add(dashboardPanel.getView()); }
            case "invoices"  -> { invoicePanel.refresh();   contentArea.getChildren().add(invoicePanel.getView()); }
            case "customers" -> { customerPanel.refresh();  contentArea.getChildren().add(customerPanel.getView()); }
            case "products"  -> { productPanel.refresh();   contentArea.getChildren().add(productPanel.getView()); }
            case "payments"  -> { paymentPanel.refresh();   contentArea.getChildren().add(paymentPanel.getView()); }
            case "reports"   -> { reportsPanel.refresh();   contentArea.getChildren().add(reportsPanel.getView()); }
            case "users"     -> {
                if (SessionManager.isAdmin()) { userPanel.refresh(); contentArea.getChildren().add(userPanel.getView()); }
            }
        }
    }

    private void openChangeOwnPassword() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Change Your Password");
        dialog.getDialogPane().setPrefWidth(380);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));

        PasswordField currentPwd = new PasswordField(); currentPwd.setPromptText("Current password"); currentPwd.setPrefWidth(220);
        PasswordField newPwd     = new PasswordField(); newPwd.setPromptText("New password");         newPwd.setPrefWidth(220);
        PasswordField confPwd    = new PasswordField(); confPwd.setPromptText("Confirm new password"); confPwd.setPrefWidth(220);

        grid.add(new Label("Current Password"), 0, 0); grid.add(currentPwd, 1, 0);
        grid.add(new Label("New Password"),     0, 1); grid.add(newPwd,     1, 1);
        grid.add(new Label("Confirm"),          0, 2); grid.add(confPwd,    1, 2);

        dialog.getDialogPane().setContent(grid);
        ButtonType save = new ButtonType("Change", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        UserDAO userDAO = new UserDAO();
        dialog.setResultConverter(bt -> {
            if (bt == save) {
                try {
                    com.buildmat.model.User me = SessionManager.getCurrentUser();
                    com.buildmat.model.User verified = userDAO.authenticate(me.getUsername(), currentPwd.getText());
                    if (verified == null) { new Alert(Alert.AlertType.ERROR, "Current password is incorrect.", ButtonType.OK).showAndWait(); return null; }
                    if (newPwd.getText().length() < 4) { new Alert(Alert.AlertType.ERROR, "Password must be at least 4 characters.", ButtonType.OK).showAndWait(); return null; }
                    if (!newPwd.getText().equals(confPwd.getText())) { new Alert(Alert.AlertType.ERROR, "Passwords do not match.", ButtonType.OK).showAndWait(); return null; }
                    return newPwd.getText();
                } catch (Exception e) { return null; }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(pwd -> {
            try {
                userDAO.changePassword(SessionManager.getCurrentUser().getId(), pwd);
                new Alert(Alert.AlertType.INFORMATION, "Password changed successfully.", ButtonType.OK).showAndWait();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait();
            }
        });
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    public void showDashboard() { navigate("dashboard"); }
    public void showInvoices()  { navigate("invoices"); }
}
