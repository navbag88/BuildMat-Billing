package com.buildmat.ui;

import com.buildmat.dao.UserDAO;
import com.buildmat.model.User;
import com.buildmat.util.SessionManager;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginScreen {

    private final UserDAO userDAO = new UserDAO();
    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;
    private Button loginBtn;

    public void show(Stage stage) {
        // ── Root layout: left brand panel + right login card ──────
        HBox root = new HBox();
        root.setPrefSize(900, 580);

        // ── Left brand panel ──────────────────────────────────────
        StackPane brandPanel = new StackPane();
        brandPanel.setPrefWidth(380);
        brandPanel.setStyle("-fx-background-color: #1A2332;");

        VBox brandContent = new VBox(16);
        brandContent.setAlignment(Pos.CENTER_LEFT);
        brandContent.setPadding(new Insets(60));

        Label appIcon = new Label("🏗");
        appIcon.setFont(Font.font("Arial", 56));

        Label appName = new Label("BuildMat");
        appName.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        appName.setTextFill(Color.WHITE);

        Label appSub = new Label("Billing System");
        appSub.setFont(Font.font("Arial", 18));
        appSub.setTextFill(Color.rgb(150, 180, 220));

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#2563EB; -fx-pref-height:2;");
        sep.setPrefWidth(60);

        Label tagline = new Label("Complete billing solution\nfor building material suppliers.");
        tagline.setFont(Font.font("Arial", 13));
        tagline.setTextFill(Color.rgb(120, 150, 190));
        tagline.setWrapText(true);
        tagline.setTextAlignment(TextAlignment.LEFT);

        // Feature bullets
        VBox features = new VBox(8);
        features.setPadding(new Insets(20, 0, 0, 0));
        for (String f : new String[]{"✓  Invoice & GST Management", "✓  Customer & Product Catalog", "✓  Payment Tracking", "✓  PDF Export & Print"}) {
            Label lbl = new Label(f);
            lbl.setFont(Font.font("Arial", 12));
            lbl.setTextFill(Color.rgb(100, 140, 180));
            features.getChildren().add(lbl);
        }

        brandContent.getChildren().addAll(appIcon, appName, appSub, sep, tagline, features);
        brandPanel.getChildren().add(brandContent);

        // ── Right login card ──────────────────────────────────────
        StackPane rightPanel = new StackPane();
        rightPanel.setStyle("-fx-background-color: #F0F2F5;");
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(48));
        card.setMaxWidth(380);
        card.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 16;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 20, 0, 0, 4);
        """);

        Label welcomeLabel = new Label("Welcome back");
        welcomeLabel.setFont(Font.font("Arial", 14));
        welcomeLabel.setTextFill(Color.web("#6B7280"));

        Label signInLabel = new Label("Sign in to continue");
        signInLabel.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        signInLabel.setTextFill(Color.web("#1A2332"));

        // Username field
        VBox usernameBox = new VBox(6);
        Label userLbl = new Label("Username");
        userLbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        userLbl.setTextFill(Color.web("#374151"));
        usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setPrefHeight(44);
        styleField(usernameField);
        usernameBox.getChildren().addAll(userLbl, usernameField);

        // Password field
        VBox passwordBox = new VBox(6);
        Label passLbl = new Label("Password");
        passLbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        passLbl.setTextFill(Color.web("#374151"));
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefHeight(44);
        styleField(passwordField);
        passwordBox.getChildren().addAll(passLbl, passwordField);

        // Error label
        errorLabel = new Label("");
        errorLabel.setFont(Font.font("Arial", 12));
        errorLabel.setTextFill(Color.web("#DC2626"));
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);

        // Login button
        loginBtn = new Button("Sign In");
        loginBtn.setPrefWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(46);
        loginBtn.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        loginBtn.setStyle("""
            -fx-background-color: #2563EB;
            -fx-text-fill: white;
            -fx-background-radius: 10;
            -fx-cursor: hand;
        """);
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle("""
            -fx-background-color: #1D4ED8;
            -fx-text-fill: white;
            -fx-background-radius: 10;
            -fx-cursor: hand;
        """));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle("""
            -fx-background-color: #2563EB;
            -fx-text-fill: white;
            -fx-background-radius: 10;
            -fx-cursor: hand;
        """));

        loginBtn.setOnAction(e -> attemptLogin(stage));
        passwordField.setOnAction(e -> attemptLogin(stage));
        usernameField.setOnAction(e -> passwordField.requestFocus());

        // Default credentials hint
        Label hint = new Label("Default: admin / admin123");
        hint.setFont(Font.font("Arial", 11));
        hint.setTextFill(Color.web("#9CA3AF"));

        card.getChildren().addAll(welcomeLabel, signInLabel, usernameBox, passwordBox, errorLabel, loginBtn, hint);
        rightPanel.getChildren().add(card);

        root.getChildren().addAll(brandPanel, rightPanel);

        // Entrance animation on the card
        card.setOpacity(0);
        card.setTranslateY(20);
        FadeTransition ft = new FadeTransition(Duration.millis(500), card);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), card);
        tt.setToY(0);
        ft.play(); tt.play();

        Scene scene = new Scene(root);
        stage.setTitle("BuildMat Billing — Sign In");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void attemptLogin(Stage stage) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Signing in...");

        try {
            User user = userDAO.authenticate(username, password);
            if (user != null) {
                SessionManager.login(user);
                MainWindow mainWindow = new MainWindow();
                mainWindow.show(stage);
            } else {
                showError("Invalid username or password.");
                passwordField.clear();
                loginBtn.setDisable(false);
                loginBtn.setText("Sign In");
                // Shake animation on error
                TranslateTransition shake = new TranslateTransition(Duration.millis(60), passwordField);
                shake.setFromX(0); shake.setByX(10); shake.setCycleCount(4); shake.setAutoReverse(true);
                shake.play();
            }
        } catch (Exception ex) {
            showError("Login error: " + ex.getMessage());
            loginBtn.setDisable(false);
            loginBtn.setText("Sign In");
        }
    }

    private void showError(String msg) {
        errorLabel.setText("⚠ " + msg);
        errorLabel.setVisible(true);
    }

    private void styleField(TextField tf) {
        tf.setStyle("""
            -fx-background-color: #F9FAFB;
            -fx-border-color: #D1D5DB;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
            -fx-padding: 8 12;
            -fx-font-size: 14;
        """);
        tf.focusedProperty().addListener((o, ov, nv) -> {
            if (nv) tf.setStyle("""
                -fx-background-color: #EFF6FF;
                -fx-border-color: #2563EB;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-padding: 8 12;
                -fx-font-size: 14;
            """);
            else tf.setStyle("""
                -fx-background-color: #F9FAFB;
                -fx-border-color: #D1D5DB;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-padding: 8 12;
                -fx-font-size: 14;
            """);
        });
    }
}
