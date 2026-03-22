package com.buildmat.util;

import com.buildmat.model.User;

public class SessionManager {
    private static User currentUser = null;

    public static void login(User user) { currentUser = user; }
    public static void logout() { currentUser = null; }
    public static User getCurrentUser() { return currentUser; }
    public static boolean isLoggedIn() { return currentUser != null; }
    public static boolean isAdmin() { return currentUser != null && "ADMIN".equals(currentUser.getRole()); }
}
