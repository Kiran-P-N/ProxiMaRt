package backend.models;

public class Vendors {
    private int id;
    private int userId;
    private String name;
    private String location;
    private String specialties;
    private String imageUrl; // <-- ADDED THIS FIELD

    public Vendors(int id, int userId, String name, String location, String specialties, String imageUrl) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.location = location;
        this.specialties = specialties;
        this.imageUrl = imageUrl; // <-- ADDED THIS LINE
    }

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getSpecialties() { return specialties; }
    public String getImageUrl() { return imageUrl; } // <-- ADDED THIS GETTER
}

