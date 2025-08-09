package backend;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.IOException;

// We will only import the route handler that is complete
import backend.routes.UserRoutes;

// We comment out the others because they are not built yet
// import backend.routes.VendorRoutes;
// import backend.routes.ProductRoutes;
// import backend.routes.OrderRoutes;

public class App {

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        System.out.println("Starting server on port: " + port);

        // --- Define Server Routes ---
        // This is the only route that is ready to be used
        server.createContext("/api/users/", new UserRoutes());
        
        // We comment out the others until they are ready
        // server.createContext("/api/vendors/", new VendorRoutes());
        // server.createContext("/api/products/", new ProductRoutes());
        // server.createContext("/api/orders/", new OrderRoutes());

        server.setExecutor(null); // Use the default executor
        server.start(); // Start the server
    }
}