package backend.models;

public class Product {
    private int id;
    private int vendorId;
    private String name;
    private String category;
    private double price;

    public Product(int id, int vendorId, String name, String category, double price) {
        this.id = id;
        this.vendorId = vendorId;
        this.name = name;
        this.category = category;
        this.price = price;
    }

    // Getters are needed for the Gson library to convert this object to JSON
    public int getId() { return id; }
    public int getVendorId() { return vendorId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
}
