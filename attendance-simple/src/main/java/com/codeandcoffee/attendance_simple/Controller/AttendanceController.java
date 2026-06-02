package com.codeandcoffee.attendance_simple.Controller;

import com.codeandcoffee.attendance_simple.model.Attendance;
import com.codeandcoffee.attendance_simple.model.Course;
import com.codeandcoffee.attendance_simple.model.Student;
import com.codeandcoffee.attendance_simple.service.*;
import com.codeandcoffee.attendance_simple.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AttendanceController {

    @Autowired private StudentService    studentService;
    @Autowired private TeacherService    teacherService;
    @Autowired private AttendanceService attendanceService;
    @Autowired private LeaveService      leaveService;
    @Autowired private CourseService     courseService;

    @GetMapping("/attendance")
    public String attendance(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String teacherId,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String batch,
            HttpSession session,
            Model model) {

        String redirect = SessionUtil.redirectIfNotAdminOrTeacher(session);
        if (redirect != null) return redirect;

        if (date == null || date.isEmpty()) {
            date = LocalDate.now().toString();
        }

        List<Course> teacherCourses = new ArrayList<>();
        Course selectedCourse = null;

        if (teacherId != null && !teacherId.isEmpty()) {
            teacherCourses = courseService.getCoursesByTeacherId(teacherId);

            if (courseCode != null && !courseCode.isEmpty()) {
                String fc = courseCode;
                selectedCourse = teacherCourses.stream()
                        .filter(c -> c.getCourseCode().equals(fc))
                        .findFirst().orElse(null);
            }
        }

        // ════════════════════════════════════════════════════════════════
        // ── 🎯 NEXUS ONE DEPT & BATCH STRICT GUARD FILTERS ──
        // ════════════════════════════════════════════════════════════════
        List<Student> students = new ArrayList<>();

        // শুধুমাত্র ডিপার্টমেন্ট এবং ব্যাচ দুটোই সিলেক্ট করা থাকলে তবেই লিস্টে স্টুডেন্ট লোড হবে
        if (department != null && !department.isEmpty() && batch != null && !batch.isEmpty()) {
            List<Student> allStudents = studentService.getAllStudents();

            students = allStudents.stream()
                    .filter(s -> s.getDepartment() != null && s.getDepartment().equalsIgnoreCase(department.trim()))
                    .filter(s -> s.getBatch() != null && s.getBatch().equalsIgnoreCase(batch.trim()))
                    .collect(Collectors.toList());
        } else {
            // ড্রপডাউন খালি থাকলে স্টুডেন্ট লিস্ট একদম ফাঁকা (Empty) থাকবে
            students = new ArrayList<>();
        }

        List<Attendance> existing = attendanceService.getAttendanceByDate(date);

        Map<Integer, Attendance> existingMap = new HashMap<>();
        for (Attendance a : existing) {
            existingMap.put(a.getStudentId(), a);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        int counter = 1;
        for (Student s : students) {
            Map<String, Object> row = new HashMap<>();
            row.put("studentId",  s.getStudentId());
            row.put("rowId",      s.getId());
            row.put("name",       s.getName());
            row.put("department", s.getDepartment());
            row.put("batch",      s.getBatch() != null ? s.getBatch() : "—");
            row.put("initials",   getInitials(s.getName()));

            Attendance att = existingMap.get(s.getId());
            row.put("status",  att != null ? att.getStatus() : "P");
            row.put("remark",  att != null ? att.getRemark()  : "");
            row.put("counter", counter++);
            rows.add(row);
        }

        model.addAttribute("rows",               rows);
        model.addAttribute("teachers",           teacherService.getAllTeachers());
        model.addAttribute("selectedDate",       date);
        model.addAttribute("selectedTeacherId",  teacherId  != null ? teacherId  : "");
        model.addAttribute("selectedCourseCode", courseCode != null ? courseCode : "");
        model.addAttribute("selectedDept",       department != null ? department : "");
        model.addAttribute("selectedBatch",      batch      != null ? batch      : "");
        model.addAttribute("selectedCourse",     selectedCourse);
        model.addAttribute("pendingLeaveCount",  leaveService.getPendingCount());

        return "attendance";
    }

    @PostMapping("/attendance/save")
    public String saveAttendance(
            @RequestParam String date,
            @RequestParam(required = false) String teacherId,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String batch,
            @RequestParam Map<String, String> allParams,
            HttpSession session) {

        String redirect = SessionUtil.redirectIfNotAdminOrTeacher(session);
        if (redirect != null) return redirect;

        List<Student>    students = studentService.getAllStudents();
        List<Attendance> records  = new ArrayList<>();

        for (Student s : students) {
            String status = allParams.getOrDefault("status_" + s.getStudentId(), "P");
            String remark = allParams.getOrDefault("remark_" + s.getStudentId(), "");
            records.add(new Attendance(s.getId(), date, status, remark));
        }

        attendanceService.saveAttendanceForDate(records, date);
        return "redirect:/attendance?date=" + date
                + (teacherId  != null ? "&teacherId="  + teacherId  : "")
                + (courseCode != null ? "&courseCode=" + courseCode : "")
                + (department != null ? "&department=" + department : "")
                + (batch      != null ? "&batch="      + batch      : "")
                + "&saved=true";
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "ST";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2)
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}