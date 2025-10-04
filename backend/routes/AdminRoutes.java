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
import java.sql.Statement;

// This class "implements HttpHandler" to act as a web route
public class AdminRoutes implements HttpHandler {

    // The @Override annotation and exact method signature are crucial
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
            // Ensure the request is a POST request
            if ("POST".equals(exchange.getRequestMethod())) {
                JsonObject requestBody = new Gson().fromJson(new InputStreamReader(exchange.getRequestBody()), JsonObject.class);

                // Extract all data from the frontend admin form
                String shopName = requestBody.get("name").getAsString();
                String location = requestBody.get("location").getAsString();
                String specialties = requestBody.get("specialties").getAsString();
                String username = requestBody.get("username").getAsString();
                String password = requestBody.get("password").getAsString();

                if (createVendorAndUser(shopName, location, specialties, username, password)) {
                    response = "{\"success\":true, \"message\":\"Vendor created successfully\"}";
                    statusCode = 201; // HTTP 201 Created
                } else {
                    response = "{\"success\":false, \"message\":\"Username already exists or a database error occurred\"}";
                    statusCode = 409; // HTTP 409 Conflict (e.g., username taken)
                }
            }
        } catch (Exception e) {
            response = "{\"success\":false, \"message\":\"A server error occurred: " + e.getMessage() + "\"}";
            statusCode = 500; // HTTP 500 Internal Server Error
            e.printStackTrace();
        }

        sendResponse(exchange, response, statusCode);
    }
    
    // This method uses a database transaction to ensure data integrity
    private boolean createVendorAndUser(String shopName, String location, String specialties, String username, String password) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            // Start a transaction by disabling auto-commit
            conn.setAutoCommit(false);

            // Step 1: Create the user with the 'vendor' role
            String userSql = "INSERT INTO users (name, password, role) VALUES (?, ?, 'vendor')";
            int newUserId = -1;
            
            try (PreparedStatement pstmtUser = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmtUser.setString(1, username);
                // In a real application, you MUST hash the password before storing it
                pstmtUser.setString(2, password);
                pstmtUser.executeUpdate();

                // Get the ID of the user we just created to link it to the vendor
                try (ResultSet generatedKeys = pstmtUser.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newUserId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            }

            // Step 2: Create the vendor profile linked to the new user ID
            String vendorSql = "INSERT INTO vendors (user_id, name, location, specialties) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmtVendor = conn.prepareStatement(vendorSql)) {
                pstmtVendor.setInt(1, newUserId);
                pstmtVendor.setString(2, shopName);
                pstmtVendor.setString(3, location);
                pstmtVendor.setString(4, specialties);
                pstmtVendor.executeUpdate();
            }

            // If both operations were successful, commit the transaction to the database
            conn.commit();
            return true;

        } catch (SQLException e) {
            // If any error occurs, rollback the entire transaction so no partial data is saved
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            // Always close the connection
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // --- Helper methods for handling HTTP responses ---

    private void handleOptionsRequest(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
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
}
