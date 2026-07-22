package com.charity.app.payload;

public class AuthResponse {
    private String token;
    private String username;
    private String role;
    private Long userId;
    private String fullName;

    public AuthResponse(String token, String username, String role, Long userId, String fullName) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.userId = userId;
        this.fullName = fullName;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public Long getUserId() { return userId; }
    public String getFullName() { return fullName; }
}
