package com.codeandcoffee.attendance_simple.Controller;

import com.codeandcoffee.attendance_simple.model.Course;
import com.codeandcoffee.attendance_simple.model.Student;
import com.codeandcoffee.attendance_simple.service.CourseService;
import com.codeandcoffee.attendance_simple.service.NotificationService;
import com.codeandcoffee.attendance_simple.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CourseApiController {

    @Autowired
    private CourseService courseService;

    // 🔥 Added Nexus Intelligence Notification Engines
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StudentService studentService;

    // ✅ আপনার আগের অরিজিনাল মেথড (১০০% অপরিবর্তিত)
    @GetMapping("/courses")
    public List<Course> getCoursesByTeacher(
            @RequestParam(required = false) String teacherId) {

        if (teacherId == null || teacherId.isEmpty()) {
            return courseService.getAllCourses();
        }
        return courseService.getCoursesByTeacherId(teacherId);
    }

    // 🔥 🎯 DYNAMIC FILTER ENDPOINT FOR BATCH ASSIGNMENT (অপরিবর্তিত)
    // এটি ডাইনামিকালি "courses.txt" থেকে অ্যাসাইনমেন্টের জন্য কোর্স অপশন ফিল্টার করবে
    @GetMapping("/courses/filter")
    public List<Course> getCoursesByDeptAndBatch(
            @RequestParam String department,
            @RequestParam String batch) {

        List<Course> allCourses = new ArrayList<>();
        try {
            allCourses = courseService.getAllCourses();
        } catch (Exception e) {
            return List.of(); // কোনো এক্সেপশন হলে সেফ ফলব্যাক ফাঁকা লিস্ট
        }

        if (allCourses == null || allCourses.isEmpty()) return List.of();

        String deptClean = department.trim().toLowerCase();

        // স্মার্ট ফিল্টার লজিক: কোর্সের কোডের মাঝে ডিপার্টমেন্টের নাম (যেমন: CSE) মিলিয়ে নেবে
        return allCourses.stream()
                .filter(c -> c.getCourseCode() != null && c.getCourseCode().toLowerCase().contains(deptClean))
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════
    // 🧠 NEXUS INTELLIGENCE: COURSE OUTLINE UPLOAD & NOTIFICATION TRIGGER
    // ══════════════════════════════════════════════════════════════════
    @PostMapping("/courses/outline/upload-alert")
    public String triggerOutlineNotification(
            @RequestParam String courseCode,
            @RequestParam String title,
            @RequestParam String batch,
            @RequestParam String teacherName) {

        // 🧠 Nexus Intelligence: Notify all students of the specific batch
        try {
            List<Student> allStudents = studentService.getAllStudents();
            int dispatchedCount = 0;

            if (allStudents != null && !allStudents.isEmpty()) {
                for (Student s : allStudents) {
                    // শুধুমাত্র নির্দিষ্ট ব্যাচের স্টুডেন্টদের ফিল্টার করে নোটিফিকেশন পাঠানো
                    if (s.getBatch() != null && s.getBatch().trim().equalsIgnoreCase(batch.trim())) {
                        String msg = "📑 New Course Outline uploaded for " + courseCode + " (" + title + "). Check your outlines section.";
                        notificationService.dispatchToStudentWithSender(s.getId(), "COURSE_OUTLINE", teacherName, msg);
                        dispatchedCount++;
                    }
                }
                System.out.println("🚀 Outline Dispatcher Engine Success: Notified " + dispatchedCount + " students in batch " + batch);
                return "Success: Course outline alert dispatched to " + dispatchedCount + " students.";
            }
        } catch (Exception e) {
            System.out.println("⚠️ Course Outline Notification Error: " + e.getMessage());
            return "Failed to dispatch notifications.";
        }

        return "Success: Outline uploaded, but no students found in batch " + batch;
    }
}