package com.androidpractice.toolpool;

import java.io.Serializable;
import java.util.List;

public class Listing implements Serializable {
    private String listingId;
    private String title;
    private String description;
    private String category;
    private String address;
    private double deposit;
    private String userId;
    private long lendDate;
    private long returnDate;
    private String condition;
    private double latitude;
    private double longitude;
    private List<String> photoUrls;
    private boolean booked;

    // Required no-argument constructor for Firebase
    public Listing() {
        this.listingId = null;
        this.title = "";
        this.description = "";
        this.category = "";
        this.address = "";
        this.deposit = 0.0;
        this.userId = "";
        this.lendDate = 0L;
        this.returnDate = 0L;
        this.condition = "";
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.photoUrls = null;
        this.booked = false;
    }

    // Updated constructor with all fields, including latitude and longitude
    public Listing(String listingId, String title, String description, String category, String address,
                   double deposit, long lendDate, long returnDate, String userId, List<String> photoUrls,
                   String condition, boolean booked, double latitude, double longitude) {
        this.listingId = listingId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.address = address;
        this.deposit = deposit;
        this.lendDate = lendDate;
        this.returnDate = returnDate;
        this.userId = userId;
        this.photoUrls = photoUrls;
        this.condition = condition;
        this.booked = booked;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and setters
    public boolean isBooked() { return booked; }
    public void setBooked(boolean booked) { this.booked = booked; }
    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }
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
    public long getLendDate() { return lendDate; }
    public void setLendDate(long lendDate) { this.lendDate = lendDate; }
    public long getReturnDate() { return returnDate; }
    public void setReturnDate(long returnDate) { this.returnDate = returnDate; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public List<String> getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrls = photoUrls; }
}