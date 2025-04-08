package com.androidpractice.toolpool;

public class ChatPartner {
    private String userId;
    private String username;

    public ChatPartner(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
}