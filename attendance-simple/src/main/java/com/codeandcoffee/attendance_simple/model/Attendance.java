package com.codeandcoffee.attendance_simple.model;

public class Attendance {

    private int studentId;
    private String date;      // format: 2026-05-08
    private String status;    // P, A, L, E
    private String remark;

    public Attendance() {}

    public Attendance(int studentId, String date, String status, String remark) {
        this.studentId = studentId;
        this.date = date;
        this.status = status;
        this.remark = remark;
    }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}