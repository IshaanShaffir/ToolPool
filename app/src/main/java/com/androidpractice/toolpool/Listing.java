package com.androidpractice.toolpool;

public class Listing {
    private String title;
    private String description;
    private String category;
    private String address;
    private double deposit;
    private String userId;
    private String listingId;
    private long timestamp;
    private long lendDate;    // Added for lend date
    private long returnDate;  // Added for return date

    // Required empty constructor for Firebase
    public Listing() {}

    public Listing(String title, String description, String category, String address,
                   double deposit, String userId, long lendDate, long returnDate) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.address = address;
        this.deposit = deposit;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
        this.lendDate = lendDate;
        this.returnDate = returnDate;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getDeposit() { return deposit; }
    public void setDeposit(double deposit) { this.deposit = deposit; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public long getLendDate() { return lendDate; }
    public void setLendDate(long lendDate) { this.lendDate = lendDate; }
    public long getReturnDate() { return returnDate; }
    public void setReturnDate(long returnDate) { this.returnDate = returnDate; }
}