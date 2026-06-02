package com.codeandcoffee.attendance_simple.model;

public class Student {

    private int id;              // row number (1,2,3...)
    private String studentId;    // 06224205101001
    private String name;         // full name
    private String email;
    private String department;
    private String batch;

    // ✅ New Fields for Blood Bank Integration
    private String bloodGroup;
    private String phone;
    private String address;

    // ✅ New Field for Profile Picture
    private boolean hasProfilePic;

    public Student() {}

    // Old Constructor (So other parts of the app don't break)
    public Student(int id, String studentId, String name, String email, String department, String batch) {
        this.id = id; this.studentId = studentId; this.name = name;
        this.email = email; this.department = department; this.batch = batch;
        this.bloodGroup = ""; this.phone = ""; this.address = "";
        this.hasProfilePic = false; // Default
    }

    // ✅ New Constructor with Blood Info
    public Student(int id, String studentId, String name, String email, String department, String batch, String bloodGroup, String phone, String address) {
        this.id = id; this.studentId = studentId; this.name = name;
        this.email = email; this.department = department; this.batch = batch;
        this.bloodGroup = bloodGroup; this.phone = phone; this.address = address;
        this.hasProfilePic = false; // Default
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    // ✅ Getters and Setters for Blood Info
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    // ✅ Getter and Setter for Profile Picture
    public boolean isHasProfilePic() { return hasProfilePic; }
    public void setHasProfilePic(boolean hasProfilePic) { this.hasProfilePic = hasProfilePic; }
}