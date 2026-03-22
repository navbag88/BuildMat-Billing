package com.buildmat.ui;

import com.buildmat.dao.UserDAO;
import com.buildmat.model.User;
import com.buildmat.util.PasswordUtil;
import com.buildmat.util.SessionManager;
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

public class UserManagementPanel {

    private final UserDAO userDAO = new UserDAO();
    private VBox view;
    private TableView<User> table;

    public UserManagementPanel() { buildView(); }
    public void refresh() { loadData(); }

    private void buildView() {
        view = new VBox(16);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("User Management");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        title.setTextFill(Color.web("#1A2332"));

        Label adminBadge = new Label("ADMIN ONLY");
        adminBadge.setStyle("-fx-background-color:#FEF3C7;-fx-text-fill:#92400E;-fx-font-size:11;-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:20;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+ Add User");
        addBtn.setStyle("-fx-background-color:#2563EB;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 16;");
        addBtn.setOnAction(e -> openForm(null));
        header.getChildren().addAll(title, adminBadge, spacer, addBtn);
        view.getChildren().add(header);

        // Info box
        Label info = new Label("ℹ  Manage who can access BuildMat Billing. Only Admins can add/edit users.");
        info.setStyle("-fx-background-color:#EFF6FF;-fx-text-fill:#1D4ED8;-fx-padding:10 16;-fx-background-radius:8;-fx-font-size:12;");
        info.setMaxWidth(Double.MAX_VALUE);
        view.getChildren().add(info);

        table = new TableView<>();
        table.setStyle("-fx-background-color:white;-fx-background-radius:12;");
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<User, String> userCol = tc("Username", 140);
        userCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));

        TableColumn<User, String> nameCol = tc("Full Name", 200);
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));

        TableColumn<User, String> roleCol = tc("Role", 90);
        roleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole()));
        roleCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(s);
                badge.setPadding(new Insets(2, 10, 2, 10));
                badge.setFont(Font.font("Arial", FontWeight.BOLD, 11));
                String bg = "ADMIN".equals(s) ? "#EDE9FE" : "#DBEAFE";
                String fg = "ADMIN".equals(s) ? "#5B21B6" : "#1D4ED8";
                badge.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:20;-fx-text-fill:" + fg + ";");
                setGraphic(badge); setText(null);
            }
        });

        TableColumn<User, String> statusCol = tc("Status", 90);
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActive() ? "Active" : "Inactive"));
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(s);
                badge.setPadding(new Insets(2, 10, 2, 10));
                badge.setFont(Font.font("Arial", FontWeight.BOLD, 11));
                boolean active = "Active".equals(s);
                badge.setStyle("-fx-background-color:" + (active ? "#D1FAE5" : "#FEE2E2") + ";-fx-background-radius:20;-fx-text-fill:" + (active ? "#065F46" : "#991B1B") + ";");
                setGraphic(badge); setText(null);
            }
        });

        TableColumn<User, Void> actCol = new TableColumn<>("Actions");
        actCol.setPrefWidth(220);
        actCol.setCellFactory(c -> new TableCell<>() {
            final Button editBtn   = new Button("Edit");
            final Button pwdBtn    = new Button("Change Password");
            final Button delBtn    = new Button("Delete");
            final HBox box = new HBox(6, editBtn, pwdBtn, delBtn);
            {
                editBtn.setStyle("-fx-background-color:#DBEAFE;-fx-text-fill:#1D4ED8;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:11;");
                pwdBtn.setStyle("-fx-background-color:#FEF3C7;-fx-text-fill:#92400E;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:11;");
                delBtn.setStyle("-fx-background-color:#FEE2E2;-fx-text-fill:#DC2626;-fx-background-radius:6;-fx-cursor:hand;-fx-font-size:11;");
                editBtn.setOnAction(e -> openForm(getTableView().getItems().get(getIndex())));
                pwdBtn.setOnAction(e -> openChangePassword(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u.getUsername().equals(SessionManager.getCurrentUser().getUsername())) {
                        alert("Cannot delete your own account."); return;
                    }
                    new Alert(Alert.AlertType.CONFIRMATION, "Delete user '" + u.getUsername() + "'?", ButtonType.YES, ButtonType.NO)
                        .showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
                            try { userDAO.delete(u.getId()); loadData(); } catch (Exception ex) { alert(ex.getMessage()); }
                        });
                });
            }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : box); }
        });

        table.getColumns().addAll(userCol, nameCol, roleCol, statusCol, actCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        view.getChildren().add(table);
        loadData();
    }

    private void loadData() {
        try { table.setItems(FXCollections.observableArrayList(userDAO.getAll())); }
        catch (Exception e) { alert(e.getMessage()); }
    }

    private void openForm(User existing) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add User" : "Edit User");
        dialog.getDialogPane().setPrefWidth(420);

        User u = existing == null ? new User() : existing;

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));

        TextField fullNameF   = field(u.getFullName());
        TextField usernameF   = field(u.getUsername());
        usernameF.setEditable(existing == null); // can't change username
        if (existing != null) usernameF.setStyle("-fx-background-color:#F3F4F6;");

        PasswordField passwordF = new PasswordField();
        passwordF.setPrefWidth(250);
        passwordF.setPromptText(existing == null ? "Set password" : "Leave blank to keep current");

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("ADMIN", "USER");
        roleBox.setValue(u.getRole() != null ? u.getRole() : "USER");

        CheckBox activeChk = new CheckBox("Active");
        activeChk.setSelected(existing == null || u.isActive());

        grid.add(new Label("Full Name *"), 0, 0); grid.add(fullNameF, 1, 0);
        grid.add(new Label("Username *"),  0, 1); grid.add(usernameF, 1, 1);
        if (existing == null) {
            grid.add(new Label("Password *"), 0, 2); grid.add(passwordF, 1, 2);
            grid.add(new Label("Role"),        0, 3); grid.add(roleBox, 1, 3);
            grid.add(activeChk,                1, 4);
        } else {
            grid.add(new Label("Role"),   0, 2); grid.add(roleBox, 1, 2);
            grid.add(activeChk,           1, 3);
        }

        dialog.getDialogPane().setContent(grid);
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == save) {
                if (fullNameF.getText().isBlank()) { alert("Full name is required."); return null; }
                if (existing == null) {
                    if (usernameF.getText().isBlank()) { alert("Username is required."); return null; }
                    if (passwordF.getText().isBlank()) { alert("Password is required."); return null; }
                    try {
                        if (userDAO.usernameExists(usernameF.getText())) { alert("Username already exists."); return null; }
                    } catch (Exception ignored) {}
                    u.setUsername(usernameF.getText().trim().toLowerCase());
                    u.setPasswordHash(PasswordUtil.createStoredPassword(passwordF.getText()));
                }
                u.setFullName(fullNameF.getText());
                u.setRole(roleBox.getValue());
                u.setActive(activeChk.isSelected());
                return u;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(saved -> {
            try { userDAO.save(saved); loadData(); } catch (Exception e) { alert(e.getMessage()); }
        });
    }

    private void openChangePassword(User u) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Change Password — " + u.getUsername());
        dialog.getDialogPane().setPrefWidth(380);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));

        PasswordField newPwd  = new PasswordField(); newPwd.setPromptText("New password"); newPwd.setPrefWidth(220);
        PasswordField confPwd = new PasswordField(); confPwd.setPromptText("Confirm password"); confPwd.setPrefWidth(220);

        grid.add(new Label("New Password"),     0, 0); grid.add(newPwd,  1, 0);
        grid.add(new Label("Confirm Password"), 0, 1); grid.add(confPwd, 1, 1);

        dialog.getDialogPane().setContent(grid);
        ButtonType save = new ButtonType("Change", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == save) {
                if (newPwd.getText().length() < 4) { alert("Password must be at least 4 characters."); return null; }
                if (!newPwd.getText().equals(confPwd.getText())) { alert("Passwords do not match."); return null; }
                return newPwd.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(pwd -> {
            try { userDAO.changePassword(u.getId(), pwd); alert("Password changed successfully."); }
            catch (Exception e) { alert(e.getMessage()); }
        });
    }

    private TableColumn<User, String> tc(String name, int w) {
        TableColumn<User, String> c = new TableColumn<>(name); c.setPrefWidth(w); return c;
    }
    private TextField field(String val) { TextField tf = new TextField(val == null ? "" : val); tf.setPrefWidth(250); return tf; }
    private void alert(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    public VBox getView() { return view; }
}
