package com.codeandcoffee.attendance_simple.model;

import java.time.LocalDate;

public class Job {

    private String id;
    private String title;
    private String company;
    private String deadline;
    private String type;            // e.g., Full-time, Internship, Tuition
    private String description;
    private String departmentTags;  // e.g., CSE, EEE, ALL
    private String authorName;      // Name of the Teacher/Admin/Student who posted
    private String mediaLink;       // Drive link, Image link or PDF
    private String status;          // ACTIVE, PENDING, EXPIRED
    private int views;
    private int applyCount;

    // Default Constructor
    public Job() {
    }

    // Parameterized Constructor
    public Job(String id, String title, String company, String deadline, String type,
               String description, String departmentTags, String authorName,
               String mediaLink, String status, int views, int applyCount) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.deadline = deadline;
        this.type = type;
        this.description = description;
        this.departmentTags = departmentTags;
        this.authorName = authorName;
        this.mediaLink = mediaLink;
        this.status = status;
        this.views = views;
        this.applyCount = applyCount;
    }

    // Helper Method to check if job is expired
    public boolean isExpired() {
        try {
            return LocalDate.now().isAfter(LocalDate.parse(deadline));
        } catch (Exception e) {
            return false;
        }
    }

    // ── Getters and Setters ──

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDepartmentTags() { return departmentTags; }
    public void setDepartmentTags(String departmentTags) { this.departmentTags = departmentTags; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getMediaLink() { return mediaLink; }
    public void setMediaLink(String mediaLink) { this.mediaLink = mediaLink; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public int getApplyCount() { return applyCount; }
    public void setApplyCount(int applyCount) { this.applyCount = applyCount; }
}