package backend.models;

public class OrderItem {
    private int id;
    private int orderId;
    private int productId;
    private int quantity;
    private double pricePerItem;

    // This constructor is not strictly needed for this implementation but is good practice
    public OrderItem(int id, int orderId, int productId, int quantity, double pricePerItem) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.pricePerItem = pricePerItem;
    }
    
    // Getters
    public int getId() { return id; }
    public int getOrderId() { return orderId; }
    public int getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getPricePerItem() { return pricePerItem; }
}
