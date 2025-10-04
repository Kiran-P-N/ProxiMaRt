package backend.models;

import java.sql.Timestamp;

public class Orders { // <-- Name changed to match filename
    private int id;
    private int customerId;
    private Timestamp orderDate;
    private double totalAmount;
    private String status;

    // Constructor name also changed to match the class name
    public Orders(int id, int customerId, Timestamp orderDate, double totalAmount, String status) {
        this.id = id;
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    // Getters
    public int getId() { return id; }
    public int getCustomerId() { return customerId; }
    public Timestamp getOrderDate() { return orderDate; }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
}
