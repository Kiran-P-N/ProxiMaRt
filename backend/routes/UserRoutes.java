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
        // --- CORS Preflight Request Handling ---
        // This block handles the browser's preliminary "OPTIONS" request.
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
                        statusCode = 201;
                    } else {
                        response = "{\"success\":false, \"message\":\"Username already exists\"}";
                        statusCode = 409;
                    }

                } else if ("/api/users/login".equals(path)) {
                    String name = requestBody.get("name").getAsString();
                    String password = requestBody.get("password").getAsString();
                    
                    JsonObject loginResult = loginUser(name, password);
                    if (loginResult.get("success").getAsBoolean()) {
                        response = new Gson().toJson(loginResult);
                        statusCode = 200;
                    } else {
                        response = new Gson().toJson(loginResult);
                        statusCode = 401;
                    }
                }
            }
        } catch (Exception e) {
            response = "{\"success\":false, \"message\":\"Server error: " + e.getMessage() + "\"}";
            statusCode = 500;
            e.printStackTrace();
        }

        sendResponse(exchange, response, statusCode);
    }

    private void handleOptionsRequest(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1); // 204 No Content
    }

    private void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    // --- Database Logic Methods ---

    private boolean signupUser(String name, String password) throws SQLException {
        String checkUserSql = "SELECT id FROM users WHERE name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt_check = conn.prepareStatement(checkUserSql)) {
            
            pstmt_check.setString(1, name);
            ResultSet rs = pstmt_check.executeQuery();
            if (rs.next()) {
                return false; // User already exists
            }
        }

        String insertSql = "INSERT INTO users (name, password, role) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt_insert = conn.prepareStatement(insertSql)) {
            
            pstmt_insert.setString(1, name);
            pstmt_insert.setString(2, password);
            pstmt_insert.setString(3, "student"); 
            
            int rowsAffected = pstmt_insert.executeUpdate();
            return rowsAffected > 0;
        }
    }

    private JsonObject loginUser(String name, String password) throws SQLException {
        JsonObject result = new JsonObject();
        String sql = "SELECT id, name, role FROM users WHERE name = ? AND password = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                result.addProperty("success", true);
                result.addProperty("id", rs.getInt("id"));
                result.addProperty("name", rs.getString("name"));
                result.addProperty("role", rs.getString("role"));
            } else {
                result.addProperty("success", false);
                result.addProperty("message", "Invalid username or password");
            }
        }
        return result;
    }
}
