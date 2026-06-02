package com.codeandcoffee.attendance_simple.model;

public class Teacher {

    private int teacherId;
    private String name;
    private String department;
    private String subject;
    private String designation;
    private String email; // ✅ নতুন ইমেইল ফিল্ড

    public Teacher() {}

    public Teacher(int teacherId, String name, String department,
                   String subject, String designation, String email) {
        this.teacherId   = teacherId;
        this.name        = name;
        this.department  = department;
        this.subject     = subject;
        this.designation = designation;
        this.email       = email; // ✅
    }

    public int    getTeacherId()    { return teacherId; }
    public String getName()         { return name; }
    public String getDepartment()   { return department; }
    public String getSubject()      { return subject; }
    public String getDesignation()  { return designation; }
    public String getEmail()        { return email; } // ✅

    // ✅ T001, T002 format — courses.txt এর সাথে match করে
    public String getTeacherCode() {
        return String.format("T%03d", teacherId);
    }

    public void setTeacherId(int t)        { this.teacherId = t; }
    public void setName(String n)          { this.name = n; }
    public void setDepartment(String d)    { this.department = d; }
    public void setSubject(String s)       { this.subject = s; }
    public void setDesignation(String d)   { this.designation = d; }
    public void setEmail(String e)         { this.email = e; } // ✅
}