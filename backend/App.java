package backend;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.IOException;

import backend.routes.UserRoutes;
import backend.routes.AdminRoutes;
import backend.routes.VendorRoutes;
import backend.routes.ProductRoutes;
import backend.routes.OrderRoutes; // <-- 1. ADD THIS IMPORT

public class App {

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        System.out.println("Starting server on port: " + port);

        // --- Define Server Routes ---
        server.createContext("/api/users/", new UserRoutes());
        server.createContext("/api/admin/", new AdminRoutes());
        server.createContext("/api/vendors/", new VendorRoutes());
        server.createContext("/api/products/", new ProductRoutes());
        server.createContext("/api/orders/", new OrderRoutes()); // <-- 2. ADD THIS LINE
        
        server.setExecutor(null);
        server.start();
    }
}

