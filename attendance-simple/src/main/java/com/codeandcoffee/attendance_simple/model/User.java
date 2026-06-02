package com.codeandcoffee.attendance_simple.model;

public class User {
    private String username;
    private String password;
    private String role;
    private String linkedId; // Full Name or ID
    private String securityAnswer;
    private String email; // ✅ নতুন ইমেইল ফিল্ড

    public User() {}

    // আগের কোডের সাথে সামঞ্জস্য রাখার জন্য কনস্ট্রাক্টর
    public User(String username, String password, String role, String linkedId, String securityAnswer) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.linkedId = linkedId;
        this.securityAnswer = securityAnswer;
        this.email = "";
    }

    // ✅ নতুন কনস্ট্রাক্টর (ইমেইল সহ)
    public User(String username, String password, String role, String linkedId, String securityAnswer, String email) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.linkedId = linkedId;
        this.securityAnswer = securityAnswer;
        this.email = email;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getLinkedId() { return linkedId; }
    public void setLinkedId(String linkedId) { this.linkedId = linkedId; }

    public String getSecurityAnswer() { return securityAnswer; }
    public void setSecurityAnswer(String securityAnswer) { this.securityAnswer = securityAnswer; }

    public String getEmail() { return email; } // ✅ Getter
    public void setEmail(String email) { this.email = email; } // ✅ Setter
}