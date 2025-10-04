package backend.routes;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import backend.db.DBConnection;
import backend.models.Product;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProductRoutes implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Handle CORS preflight requests
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptionsRequest(exchange);
            return;
        }

        String response = "{\"message\":\"Invalid request\"}";
        int statusCode = 400;

        try {
            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> queryParams = parseQuery(query);
                
                if (queryParams.containsKey("vendorId")) {
                    int vendorId = Integer.parseInt(queryParams.get("vendorId"));
                    List<Product> products = getProductsByVendorId(vendorId);
                    response = new Gson().toJson(products);
                    statusCode = 200;
                } else {
                    response = "{\"message\":\"Missing vendorId parameter\"}";
                }

            } else if ("POST".equals(method)) {
                JsonObject requestBody = new Gson().fromJson(new InputStreamReader(exchange.getRequestBody()), JsonObject.class);
                
                String name = requestBody.get("name").getAsString();
                String category = requestBody.get("category").getAsString();
                double price = requestBody.get("price").getAsDouble();
                int vendorId = requestBody.get("vendorId").getAsInt();
                
                if (addProduct(name, category, price, vendorId)) {
                    response = "{\"success\":true, \"message\":\"Product added successfully\"}";
                    statusCode = 201;
                } else {
                    response = "{\"success\":false, \"message\":\"Failed to add product\"}";
                    statusCode = 500;
                }
            } 
            // 3. ADDED LOGIC TO HANDLE DELETE REQUESTS
            else if ("DELETE".equals(method)) {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> queryParams = parseQuery(query);

                if (queryParams.containsKey("id")) {
                    int productId = Integer.parseInt(queryParams.get("id"));
                    if (deleteProduct(productId)) {
                        response = "{\"success\":true, \"message\":\"Product deleted successfully\"}";
                        statusCode = 200;
                    } else {
                        response = "{\"success\":false, \"message\":\"Failed to delete product\"}";
                        statusCode = 500;
                    }
                } else {
                    response = "{\"message\":\"Missing product id parameter\"}";
                }
            }

        } catch (Exception e) {
            response = "{\"success\":false, \"message\":\"Server error: " + e.getMessage() + "\"}";
            statusCode = 500;
            e.printStackTrace();
        }

        sendResponse(exchange, response, statusCode);
    }

    // --- Database Logic Methods ---
    private List<Product> getProductsByVendorId(int vendorId) throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE vendor_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, vendorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(new Product(
                        rs.getInt("id"),
                        rs.getInt("vendor_id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price")
                    ));
                }
            }
        }
        return products;
    }

    private boolean addProduct(String name, String category, double price, int vendorId) throws SQLException {
        String sql = "INSERT INTO products (name, category, price, vendor_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            pstmt.setString(2, category);
            pstmt.setDouble(3, price);
            pstmt.setInt(4, vendorId);
            
            return pstmt.executeUpdate() > 0;
        }
    }

    // 4. ADDED THE METHOD TO DELETE A PRODUCT FROM THE DATABASE
    private boolean deleteProduct(int productId) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            
            return pstmt.executeUpdate() > 0;
        }
    }

    // --- Helper Methods ---
    private Map<String, String> parseQuery(String query) {
        if (query == null || query.isEmpty()) return Map.of();
        return Stream.of(query.split("&")).map(s -> s.split("=")).collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));
    }
    
    private void handleOptionsRequest(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        // 5. ADDED DELETE TO THE ALLOWED METHODS FOR CORS
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
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

