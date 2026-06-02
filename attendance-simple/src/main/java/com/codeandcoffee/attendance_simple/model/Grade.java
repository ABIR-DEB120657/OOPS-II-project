package com.codeandcoffee.attendance_simple.model;

public class Grade {

    private int studentId;
    private String studentName;
    private String department;
    private String courseCode;
    private String subject;
    private double credits;
    private int marks;
    private String letterGrade;
    private double gpa;
    private String semester;

    public Grade() {}

    // ✅ grades.txt থেকে পড়ার জন্য (10 parameter)
    public Grade(int studentId, String studentName, String department,
                 String courseCode, String subject, double credits,
                 int marks, String letterGrade, double gpa, String semester) {
        this.studentId   = studentId;
        this.studentName = studentName;
        this.department  = department;
        this.courseCode  = courseCode;
        this.subject     = subject;
        this.credits     = credits;
        this.marks       = marks;
        this.letterGrade = letterGrade;
        this.gpa         = gpa;
        this.semester    = semester;
    }

    // ✅ Form থেকে grade add করার জন্য (auto grade/gpa calculate)
    public Grade(int studentId, String studentName, String department,
                 String courseCode, String subject, double credits,
                 int marks, String semester) {
        this.studentId   = studentId;
        this.studentName = studentName;
        this.department  = department;
        this.courseCode  = courseCode;
        this.subject     = subject;
        this.credits     = credits;
        this.marks       = marks;
        this.semester    = semester;
        this.letterGrade = calculateLetterGrade(marks);
        this.gpa         = calculateGpa(marks);
    }

    // ✅ Marks থেকে Letter Grade বের করে
    public static String calculateLetterGrade(int marks) {
        if (marks >= 80) return "A+";
        if (marks >= 75) return "A";
        if (marks >= 70) return "A-";
        if (marks >= 65) return "B+";
        if (marks >= 60) return "B";
        if (marks >= 55) return "B-";
        if (marks >= 50) return "C+";
        if (marks >= 45) return "C";
        if (marks >= 40) return "D";
        return "F";
    }

    // ✅ Marks থেকে GPA বের করে
    public static double calculateGpa(int marks) {
        if (marks >= 80) return 4.00;
        if (marks >= 75) return 3.75;
        if (marks >= 70) return 3.50;
        if (marks >= 65) return 3.25;
        if (marks >= 60) return 3.00;
        if (marks >= 55) return 2.75;
        if (marks >= 50) return 2.50;
        if (marks >= 45) return 2.25;
        if (marks >= 40) return 2.00;
        return 0.00;
    }

    // ════════════════════════════════════════
    // ✅ Primary Getters
    // ════════════════════════════════════════
    public int getStudentId()       { return studentId; }
    public String getStudentName()  { return studentName; }
    public String getDepartment()   { return department; }
    public String getCourseCode()   { return courseCode; }
    public String getSubject()      { return subject; }
    public double getCredits()      { return credits; }
    public int getMarks()           { return marks; }
    public String getLetterGrade()  { return letterGrade; }
    public double getGpa()          { return gpa; }
    public String getSemester()     { return semester; }

    // ════════════════════════════════════════
    // ✅ Alias Getters — HTML template এর জন্য
    // ════════════════════════════════════════
    public String getCourseTitle()  { return subject; }     // g.courseTitle → subject
    public double getCredit()       { return credits; }     // g.credit → credits
    public String getGrade()        { return letterGrade; } // g.grade → letterGrade
    public int getStudentRowId()    { return studentId; }   // g.studentRowId → studentId

    // 🚀 FIX: Student Portal HTML এর জন্য নতুন Alias Getters
    public String getCourseName()   { return subject; }     // g.courseName → subject
    public double getGradePoint()   { return gpa; }         // g.gradePoint → gpa

    // ════════════════════════════════════════
    // ✅ Setters
    // ════════════════════════════════════════
    public void setStudentId(int studentId)          { this.studentId = studentId; }
    public void setStudentName(String studentName)   { this.studentName = studentName; }
    public void setDepartment(String department)     { this.department = department; }
    public void setCourseCode(String courseCode)     { this.courseCode = courseCode; }
    public void setSubject(String subject)           { this.subject = subject; }
    public void setCredits(double credits)           { this.credits = credits; }
    public void setMarks(int marks)                  { this.marks = marks; }
    public void setLetterGrade(String letterGrade)   { this.letterGrade = letterGrade; }
    public void setGpa(double gpa)                   { this.gpa = gpa; }
    public void setSemester(String semester)         { this.semester = semester; }
}