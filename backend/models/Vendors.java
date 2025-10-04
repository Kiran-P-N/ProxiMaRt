package backend.models;

// This is a "POJO" (Plain Old Java Object) or a "model" class.
// Its only purpose is to act as a blueprint to hold the data for a single vendor,
// matching the structure of your 'vendors' database table.
public class Vendors {
    // These private fields match the columns in your database
    private int id;
    private int userId;
    private String name;
    private String location;
    private String specialties;

    // A public constructor to easily create new Vendor objects from the database data.
    public Vendors(int id, int userId, String name, String location, String specialties) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.location = location;
        this.specialties = specialties;
    }

    // --- Getters ---
    // These are public methods that allow other parts of your code (like the Gson library)
    // to read the values of the private fields. This is essential for JSON conversion.

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }
    
    public String getSpecialties() {
        return specialties;
    }
}
