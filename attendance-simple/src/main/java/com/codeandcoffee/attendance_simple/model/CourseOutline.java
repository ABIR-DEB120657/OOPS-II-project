package com.codeandcoffee.attendance_simple.model;

public class CourseOutline {
    private int id;
    private String department;
    private String batch;
    private String courseCode;
    private String title;
    private String description;
    private String fileOrLink;
    private String teacherName;
    private String postDate;

    public CourseOutline() {}

    public CourseOutline(int id, String department, String batch, String courseCode, String title, String description, String fileOrLink, String teacherName, String postDate) {
        this.id = id;
        this.department = department;
        this.batch = batch;
        this.courseCode = courseCode;
        this.title = title;
        this.description = description;
        this.fileOrLink = fileOrLink;
        this.teacherName = teacherName;
        this.postDate = postDate;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFileOrLink() { return fileOrLink; }
    public void setFileOrLink(String fileOrLink) { this.fileOrLink = fileOrLink; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getPostDate() { return postDate; }
    public void setPostDate(String postDate) { this.postDate = postDate; }
}