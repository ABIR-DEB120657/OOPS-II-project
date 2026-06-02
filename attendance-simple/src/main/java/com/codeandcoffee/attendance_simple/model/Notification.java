package com.codeandcoffee.attendance_simple.model;

public class Notification {
    private int id;
    private String type;
    private String title;      // ✅ নতুন field
    private String sender;
    private String message;
    private String date;
    private String status;
    private int recipientId;
    private String recipientType;
    private boolean read;

    public Notification() {}

    // ✅ পুরনো constructor — backward compatible
    public Notification(int id, String type, String sender, String message,
                        String date, String status, int recipientId, String recipientType) {
        this.id = id;
        this.type = type;
        this.sender = sender;
        this.message = message;
        this.date = date;
        this.status = status;
        this.recipientId = recipientId;
        this.recipientType = recipientType;
        this.read = !"unread".equalsIgnoreCase(status);
        // ✅ title auto-generate from type
        this.title = generateTitle(type, sender);
    }

    // ✅ নতুন constructor — title সহ
    public Notification(int id, String type, String title, String sender,
                        String message, String date, String status,
                        int recipientId, String recipientType) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.sender = sender;
        this.message = message;
        this.date = date;
        this.status = status;
        this.recipientId = recipientId;
        this.recipientType = recipientType;
        this.read = !"unread".equalsIgnoreCase(status);
    }

    // 🔥 FIXED: Expanded to support ALL Nexus One Modules seamlessly
    private String generateTitle(String type, String sender) {
        if (type == null) return "Notification";
        switch (type.toUpperCase()) {
            case "LEAVE":           return "📅 Leave Request Update";
            case "BLOOD":           return "🩸 Urgent Blood Request";
            case "ASSIGNMENT":      return "📚 New Assignment Alert";
            case "GRADE":
            case "RESULT":          return "🏆 Result Published";
            case "ATTENDANCE":      return "📊 Daily Attendance Update";
            case "COURSE_OUTLINE":  return "📑 Course Outline Uploaded";
            case "APPOINTMENT":     return "👨‍🏫 Faculty Sync Update";
            case "JOB":
            case "CAREER":          return "💼 Career Hub Opportunity";
            case "FINANCE":         return "💎 Finance & Payment Bill";
            case "NOTICE":          return "🔔 Notice from " + (sender != null ? sender : "Admin");
            default:                return "🔔 System Notification";
        }
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title != null ? title : generateTitle(type, sender); }
    public void setTitle(String title) { this.title = title; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getRecipientId() { return recipientId; }
    public void setRecipientId(int recipientId) { this.recipientId = recipientId; }

    public String getRecipientType() { return recipientType; }
    public void setRecipientType(String recipientType) { this.recipientType = recipientType; }

    public boolean isUnread() { return "unread".equalsIgnoreCase(status); }
    public boolean isRead() { return read; }
    public boolean getRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}