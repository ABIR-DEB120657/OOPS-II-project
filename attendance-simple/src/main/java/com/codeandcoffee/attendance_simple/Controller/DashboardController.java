package com.codeandcoffee.attendance_simple.Controller;

import com.codeandcoffee.attendance_simple.model.Grade;
import com.codeandcoffee.attendance_simple.model.Student;
import com.codeandcoffee.attendance_simple.service.*;
import com.codeandcoffee.attendance_simple.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.*;

@Controller
public class DashboardController {

    @Autowired private StudentService      studentService;
    @Autowired private TeacherService      teacherService;
    @Autowired private AttendanceService   attendanceService;
    @Autowired private LeaveService        leaveService;
    @Autowired private GradeService        gradeService;
    @Autowired private NotificationService notificationService;

    @GetMapping({"/dashboard", "/"})
    public String dashboard(HttpSession session, Model model) {

        // ✅ Session check
        if (!SessionUtil.isLoggedIn(session)) {
            return "redirect:/login";
        }

        // ✅ Role অনুযায়ী redirect
        String role = (String) session.getAttribute("role");
        if ("STUDENT".equals(role)) return "redirect:/student-portal";
        if ("TEACHER".equals(role)) return "redirect:/teacher-portal";

        // ── ADMIN only নিচে ──────────────────────────────────────────
        String today = LocalDate.now().toString();

        // ── Basic stats ──────────────────────────────────────────────
        model.addAttribute("totalStudents",  studentService.getTotalStudents());
        model.addAttribute("totalTeachers",  teacherService.getTotalTeachers());
        model.addAttribute("presentToday",   attendanceService.countPresentToday(today));
        model.addAttribute("absentToday",    attendanceService.countAbsentToday(today));
        model.addAttribute("lateToday",      attendanceService.countLateToday(today));

        // ── Attendance rate ──────────────────────────────────────────
        List<Student> allStudents = studentService.getAllStudents();
        double avgAttendance = allStudents.stream()
                .mapToDouble(s -> attendanceService
                        .getAttendancePercentage(s.getStudentId()))
                .average()
                .orElse(0.0);
        model.addAttribute("attendanceRate", (int) Math.round(avgAttendance));

        // ── Grade stats ──────────────────────────────────────────────
        model.addAttribute("classAverage", gradeService.getClassAverage());
        model.addAttribute("topGrade",     gradeService.getTopPerformer());

        // ── At-risk students ──────────────────────────────────
        List<Map<String, Object>> atRiskList = new ArrayList<>();
        for (Student s : allStudents) {
            if (s == null) continue;
            double pct = attendanceService.getAttendancePercentage(s.getStudentId());
            if (pct < 75) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("name",       s.getName() != null ? s.getName() : "Unknown");
                entry.put("percentage", pct);
                entry.put("studentId",  s.getStudentId());
                atRiskList.add(entry);
            }
        }
        model.addAttribute("atRiskStudents", atRiskList);

        // ── Leave ────────────────────────────────────────────────────
        model.addAttribute("pendingLeaveCount", leaveService.getPendingCount());

        // ── Date ────────────────────────────────────────────────────
        model.addAttribute("today", today);

        // ── ✅ Notifications ─────────────────────────────────
        model.addAttribute("unreadNotifCount", notificationService.getUnreadCount());

        model.addAttribute("recentNotifications",
                notificationService.getForAdmin().stream()
                        .filter(n -> "unread".equalsIgnoreCase(n.getStatus()))
                        .limit(5)
                        .toList());

        return "dashboard";
    }
}