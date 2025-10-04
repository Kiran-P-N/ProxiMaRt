package backend.routes;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
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

public class OrderRoutes implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptionsRequest(exchange);
            return;
        }

        String response = "{\"message\":\"Invalid request\"}";
        int statusCode = 400;

        try {
            if ("POST".equals(exchange.getRequestMethod())) {
                JsonObject requestBody = new Gson().fromJson(new InputStreamReader(exchange.getRequestBody()), JsonObject.class);
                
                int customerId = requestBody.get("customerId").getAsInt();
                JsonArray cartItems = requestBody.get("items").getAsJsonArray();
                
                if (createOrder(customerId, cartItems)) {
                    response = "{\"success\":true, \"message\":\"Order placed successfully!\"}";
                    statusCode = 201;
                } else {
                    response = "{\"success\":false, \"message\":\"Failed to place order.\"}";
                    statusCode = 500;
                }
            }
        } catch (Exception e) {
            response = "{\"success\":false, \"message\":\"Server error: " + e.getMessage() + "\"}";
            statusCode = 500;
            e.printStackTrace();
        }

        sendResponse(exchange, response, statusCode);
    }

    private boolean createOrder(int customerId, JsonArray cartItems) {
        Connection conn = null;
        double totalAmount = 0;

        // First, calculate the total amount
        for (int i = 0; i < cartItems.size(); i++) {
            JsonObject item = cartItems.get(i).getAsJsonObject();
            totalAmount += item.get("price").getAsDouble() * item.get("quantity").getAsInt();
        }
        totalAmount += 50; // Add delivery fee

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Step 1: Insert into the 'orders' table
            String orderSql = "INSERT INTO orders (customer_id, total_amount) VALUES (?, ?)";
            int newOrderId = -1;
            try (PreparedStatement pstmtOrder = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmtOrder.setInt(1, customerId);
                pstmtOrder.setDouble(2, totalAmount);
                pstmtOrder.executeUpdate();
                try (ResultSet generatedKeys = pstmtOrder.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newOrderId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating order failed, no ID obtained.");
                    }
                }
            }

            // Step 2: Insert each item into the 'order_items' table
            String itemSql = "INSERT INTO order_items (order_id, product_id, quantity, price_per_item) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmtItem = conn.prepareStatement(itemSql)) {
                for (int i = 0; i < cartItems.size(); i++) {
                    JsonObject item = cartItems.get(i).getAsJsonObject();
                    pstmtItem.setInt(1, newOrderId);
                    pstmtItem.setInt(2, item.get("id").getAsInt());
                    pstmtItem.setInt(3, item.get("quantity").getAsInt());
                    pstmtItem.setDouble(4, item.get("price").getAsDouble());
                    pstmtItem.addBatch(); // Add this INSERT statement to a batch
                }
                pstmtItem.executeBatch(); // Execute all statements in the batch
            }
            
            conn.commit(); // Commit transaction
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback transaction on error
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleOptionsRequest(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
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
