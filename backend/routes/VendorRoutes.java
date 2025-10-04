package backend.routes;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;
import backend.db.DBConnection;
import backend.models.Vendors;

import java.io.IOException;
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

public class VendorRoutes implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptionsRequest(exchange);
            return;
        }

        String response = "{\"message\":\"Invalid request\"}";
        int statusCode = 400;

        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> queryParams = parseQuery(query);

                // UPDATED LOGIC: Check if a specific ID is requested
                if (queryParams.containsKey("id")) {
                    int vendorId = Integer.parseInt(queryParams.get("id"));
                    Vendors vendor = getVendorById(vendorId);
                    if (vendor != null) {
                        response = new Gson().toJson(vendor);
                        statusCode = 200;
                    } else {
                        response = "{\"message\":\"Vendor not found\"}";
                        statusCode = 404;
                    }
                } else {
                    // Original logic: get all vendors
                    List<Vendors> vendors = getAllVendors();
                    response = new Gson().toJson(vendors);
                    statusCode = 200;
                }
            }
        } catch (Exception e) {
            response = "{\"success\":false, \"message\":\"Server error: " + e.getMessage() + "\"}";
            statusCode = 500;
            e.printStackTrace();
        }

        sendResponse(exchange, response, statusCode);
    }

    // --- Database Logic ---
    private List<Vendors> getAllVendors() throws SQLException {
        List<Vendors> vendors = new ArrayList<>();
        String sql = "SELECT * FROM vendors";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                vendors.add(new Vendors(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("name"),
                    rs.getString("location"),
                    rs.getString("specialties")
                ));
            }
        }
        return vendors;
    }

    // --- NEW METHOD: Get a single vendor by their ID ---
    private Vendors getVendorById(int vendorId) throws SQLException {
        String sql = "SELECT * FROM vendors WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, vendorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Vendors(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("location"),
                        rs.getString("specialties")
                    );
                }
            }
        }
        return null; // Return null if no vendor is found
    }

    // --- Helper Methods ---
    private Map<String, String> parseQuery(String query) {
        if (query == null || query.isEmpty()) return Map.of();
        return Stream.of(query.split("&")).map(s -> s.split("=")).collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));
    }

    private void handleOptionsRequest(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-control-allow-methods", "GET, OPTIONS");
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

