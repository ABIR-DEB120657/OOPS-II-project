package com.codeandcoffee.attendance_simple.model;

public class LeaveRequest {
    private int leaveId;
    private int studentId;
    private String studentName;
    private String department;
    private String fromDate;
    private String toDate;
    private String reason;
    private String status;      // PENDING, APPROVED, REJECTED
    private int teacherId;

    public LeaveRequest() {}

    public LeaveRequest(int leaveId, int studentId, String studentName,
                        String department, String fromDate, String toDate,
                        String reason, String status) {
        this(leaveId, studentId, studentName, department,
                fromDate, toDate, reason, status, 0);
    }

    public LeaveRequest(int leaveId, int studentId, String studentName,
                        String department, String fromDate, String toDate,
                        String reason, String status, int teacherId) {
        this.leaveId     = leaveId;
        this.studentId   = studentId;
        this.studentName = studentName;
        this.department  = department;
        this.fromDate    = fromDate;
        this.toDate      = toDate;
        this.reason      = reason;
        this.status      = status;
        this.teacherId   = teacherId;
    }

    public int    getLeaveId()     { return leaveId; }
    public int    getStudentId()   { return studentId; }
    public String getStudentName() { return studentName; }
    public String getDepartment()  { return department; }
    public String getFromDate()    { return fromDate; }
    public String getToDate()      { return toDate; }
    public String getReason()      { return reason; }
    public String getStatus()      { return status; }
    public int    getTeacherId()   { return teacherId; }

    public void setLeaveId(int v)      { this.leaveId = v; }
    public void setStudentId(int v)    { this.studentId = v; }
    public void setStudentName(String v){ this.studentName = v; }
    public void setDepartment(String v){ this.department = v; }
    public void setFromDate(String v)  { this.fromDate = v; }
    public void setToDate(String v)    { this.toDate = v; }
    public void setReason(String v)    { this.reason = v; }
    public void setStatus(String v)    { this.status = v; }
    public void setTeacherId(int v)    { this.teacherId = v; }
}