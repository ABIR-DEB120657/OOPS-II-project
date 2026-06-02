package com.codeandcoffee.attendance_simple.model;

public class FaceScanResponse {

    private boolean success;
    private String studentId;
    private String studentName;
    private String message;
    private int matchedCount;

    // ── Constructors ──────────────────────────────────────────
    public FaceScanResponse() {}

    public FaceScanResponse(boolean success, String studentId,
                            String studentName, String message,
                            int matchedCount) {
        this.success      = success;
        this.studentId    = studentId;
        this.studentName  = studentName;
        this.message      = message;
        this.matchedCount = matchedCount;
    }

    // ── Static factory helpers ────────────────────────────────
    public static FaceScanResponse matched(String studentId,
                                           String studentName) {
        return new FaceScanResponse(
                true, studentId, studentName,
                "Student matched: " + studentName,
                1
        );
    }

    public static FaceScanResponse noMatch() {
        return new FaceScanResponse(
                false, null, null,
                "No matching student found. Please mark manually.",
                0
        );
    }

    public static FaceScanResponse error(String reason) {
        return new FaceScanResponse(
                false, null, null,
                "Scan error: " + reason,
                0
        );
    }

    // ── Getters & Setters ─────────────────────────────────────
    public boolean isSuccess()               { return success; }
    public void    setSuccess(boolean s)     { this.success = s; }

    public String  getStudentId()            { return studentId; }
    public void    setStudentId(String id)   { this.studentId = id; }

    public String  getStudentName()          { return studentName; }
    public void    setStudentName(String n)  { this.studentName = n; }

    public String  getMessage()              { return message; }
    public void    setMessage(String m)      { this.message = m; }

    public int     getMatchedCount()         { return matchedCount; }
    public void    setMatchedCount(int c)    { this.matchedCount = c; }
}