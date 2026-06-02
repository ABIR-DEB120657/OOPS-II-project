package com.codeandcoffee.attendance_simple.model;

public class Assignment {
    private int assignmentId;
    private String courseCode;
    private String courseName;
    private String teacherName;
    private int teacherId;
    private String topicName;
    private String batch;
    private String startDate;
    private String endDate;

    private int studentId;
    private String studentStrId;
    private String studentName;
    private String filePath;
    private String status;
    private String submissionDate;

    public Assignment() {}

    public int getAssignmentId() { return assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public int getTeacherId() { return teacherId; }
    public void setTeacherId(int teacherId) { this.teacherId = teacherId; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentStrId() { return studentStrId; }
    public void setStudentStrId(String studentStrId) { this.studentStrId = studentStrId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(String submissionDate) { this.submissionDate = submissionDate; }
}