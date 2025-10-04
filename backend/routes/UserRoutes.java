package backend.routes;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import backend.db.DBConnection;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRoutes implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Handle CORS preflight requests for browser compatibility
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptionsRequest(exchange);
            return;
        }

        String response = "{\"message\":\"Invalid request\"}";
        int statusCode = 400;

        try {
            if ("POST".equals(exchange.getRequestMethod())) {
                String path = exchange.getRequestURI().getPath();
                JsonObject requestBody = new Gson().fromJson(new InputStreamReader(exchange.getRequestBody()), JsonObject.class);

                if ("/api/users/signup".equals(path)) {
                    String name = requestBody.get("name").getAsString();
                    String password = requestBody.get("password").getAsString();
                    
                    if (signupUser(name, password)) {
                        response = "{\"success\":true, \"message\":\"User created successfully\"}";
                        statusCode = 201; // HTTP 201 Created
                    } else {
                        response = "{\"success\":false, \"message\":\"Username already exists\"}";
                        statusCode = 409; // HTTP 409 Conflict
                    }

                } else if ("/api/users/login".equals(path)) {
                    String name = requestBody.get("name").getAsString();
                    String password = requestBody.get("password").getAsString();
                    
                    JsonObject loginResult = loginUser(name, password);
                    if (loginResult.get("success").getAsBoolean()) {
                        response = new Gson().toJson(loginResult);
                        statusCode = 200; // HTTP 200 OK
                    } else {
                        response = new Gson().toJson(loginResult);
                        statusCode = 401; // HTTP 401 Unauthorized
                    }
                }
            }
        } catch (Exception e) {
            response = "{\"success\":false, \"message\":\"Server error: " + e.getMessage() + "\"}";
            statusCode = 500; // HTTP 500 Internal Server Error
            e.printStackTrace();
        }

        sendResponse(exchange, response, statusCode);
    }

    private boolean signupUser(String name, String password) throws SQLException {
        // First, check if the username is already taken
        String checkUserSql = "SELECT id FROM users WHERE name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt_check = conn.prepareStatement(checkUserSql)) {
            
            pstmt_check.setString(1, name);
            ResultSet rs = pstmt_check.executeQuery();
            if (rs.next()) {
                return false; // User already exists
            }
        }

        // If the username is available, insert the new user
        String insertSql = "INSERT INTO users (name, password, role) VALUES (?, ?, 'student')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt_insert = conn.prepareStatement(insertSql)) {
            
            pstmt_insert.setString(1, name);
            // In a real application, you would hash the password here before saving
            pstmt_insert.setString(2, password);
            
            int rowsAffected = pstmt_insert.executeUpdate();
            return rowsAffected > 0;
        }
    }

    // This is the final, clean version of the login method
    private JsonObject loginUser(String name, String password) throws SQLException {
        JsonObject result = new JsonObject();
        String userSql = "SELECT id, name, role FROM users WHERE name = ? AND password = ?";
        Connection conn = null;

        try {
            conn = DBConnection.getConnection();
            try (PreparedStatement userPstmt = conn.prepareStatement(userSql)) {
                userPstmt.setString(1, name);
                userPstmt.setString(2, password);

                ResultSet rs = userPstmt.executeQuery();
                if (rs.next()) {
                    result.addProperty("success", true);
                    int userId = rs.getInt("id");
                    result.addProperty("id", userId);
                    result.addProperty("name", rs.getString("name"));
                    
                    String roleFromDb = rs.getString("role");
                    String cleanRole = "student"; // Default role
                    if (roleFromDb != null) {
                        cleanRole = roleFromDb.trim().toLowerCase();
                    }
                    result.addProperty("role", cleanRole);
                    
                    // If the user is a vendor, run a second query to get their vendorId
                    if ("vendor".equals(cleanRole)) {
                        String vendorSql = "SELECT id FROM vendors WHERE user_id = ?";
                        try (PreparedStatement vendorPstmt = conn.prepareStatement(vendorSql)) {
                            vendorPstmt.setInt(1, userId);
                            ResultSet vendorRs = vendorPstmt.executeQuery();
                            if (vendorRs.next()) {
                                result.addProperty("vendorId", vendorRs.getInt("id"));
                            }
                        }
                    }

                } else {
                    result.addProperty("success", false);
                    result.addProperty("message", "Invalid username or password");
                }
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        return result;
    }

    // --- Helper methods for handling HTTP responses ---

    private void handleOptionsRequest(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-control-allow-origin", "*");
        exchange.getResponseHeaders().set("Access-control-allow-methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-control-allow-headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
    }

    private void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-control-allow-origin", "*");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}

