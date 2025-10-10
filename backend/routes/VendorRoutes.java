package backend.routes;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;

import backend.db.DBConnection;
import backend.models.Vendors;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VendorRoutes implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptionsRequest(exchange);
            return;
        }

        String response = "[]";
        int statusCode = 200;

        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                String searchTerm = getQueryParam(query, "q");
                String vendorIdStr = getQueryParam(query, "id");

                if (vendorIdStr != null) {
                    int vendorId = Integer.parseInt(vendorIdStr);
                    Vendors vendor = getVendorById(vendorId);
                    response = new Gson().toJson(vendor);

                } else if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                    List<Vendors> vendors = searchVendorsAndProducts(searchTerm);
                    response = new Gson().toJson(vendors);
                } else {
                    List<Vendors> vendors = getAllVendors();
                    response = new Gson().toJson(vendors);
                }
            }
        } catch (SQLException e) {
            response = "{\"success\":false, \"message\":\"Database error: " + e.getMessage() + "\"}";
            statusCode = 500;
            e.printStackTrace();
        }

        sendResponse(exchange, response, statusCode);
    }

    private Vendors getVendorById(int vendorId) throws SQLException {
        String sql = "SELECT * FROM vendors WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, vendorId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Vendors(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("name"),
                    rs.getString("location"),
                    rs.getString("specialties"),
                    rs.getString("image_url") // <-- FETCH IMAGE URL
                );
            }
        }
        return null;
    }

    private List<Vendors> searchVendorsAndProducts(String searchTerm) throws SQLException {
        List<Vendors> vendors = new ArrayList<>();
        String sql = "SELECT DISTINCT v.* FROM vendors v " +
                     "LEFT JOIN products p ON v.id = p.vendor_id " +
                     "WHERE v.name LIKE ? OR p.name LIKE ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String likeTerm = "%" + searchTerm + "%";
            pstmt.setString(1, likeTerm);
            pstmt.setString(2, likeTerm);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                vendors.add(new Vendors(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("name"),
                    rs.getString("location"),
                    rs.getString("specialties"),
                    rs.getString("image_url") // <-- FETCH IMAGE URL
                ));
            }
        }
        return vendors;
    }

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
                    rs.getString("specialties"),
                    rs.getString("image_url") // <-- FETCH IMAGE URL
                ));
            }
        }
        return vendors;
    }
    
    private String getQueryParam(String query, String paramName) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf("=");
            if (idx > 0 && pair.substring(0, idx).equals(paramName)) {
                return URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
    
    private void handleOptionsRequest(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
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

