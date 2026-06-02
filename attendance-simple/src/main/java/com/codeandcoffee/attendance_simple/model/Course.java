package com.codeandcoffee.attendance_simple.model;

public class Course {

    private String courseCode;
    private String courseTitle;
    private String semester;
    private String teacherId;

    public Course() {}

    public Course(String courseCode, String courseTitle, String semester, String teacherId) {
        this.courseCode  = courseCode;
        this.courseTitle = courseTitle;
        this.semester    = semester;
        this.teacherId   = teacherId;
    }

    public String getCourseCode()  { return courseCode; }
    public String getCourseTitle() { return courseTitle; }
    public String getSemester()    { return semester; }
    public String getTeacherId()   { return teacherId; }

    public void setCourseCode(String courseCode)   { this.courseCode  = courseCode; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public void setSemester(String semester)       { this.semester    = semester; }
    public void setTeacherId(String teacherId)     { this.teacherId   = teacherId; }
}