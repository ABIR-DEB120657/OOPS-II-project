package com.codeandcoffee.attendance_simple.Controller;

import com.codeandcoffee.attendance_simple.model.Attendance;
import com.codeandcoffee.attendance_simple.model.Student;
import com.codeandcoffee.attendance_simple.service.AttendanceService;
import com.codeandcoffee.attendance_simple.service.LeaveService;
import com.codeandcoffee.attendance_simple.service.StudentService;
import com.codeandcoffee.attendance_simple.util.SessionUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Controller
public class ReportController {

    @Autowired private StudentService studentService;
    @Autowired private AttendanceService attendanceService;
    @Autowired private LeaveService leaveService;

    @GetMapping("/reports")
    public String reports(
            @RequestParam(required = false) String studentId,
            HttpSession session,
            Model model) {

        // ✅ FIXED: Session check - Admin অথবা Teacher উভয়কেই অ্যালাউ করা হলো
        String redirect = SessionUtil.redirectIfNotAdminOrTeacher(session);
        if (redirect != null) return redirect;

        List<Student> allStudents = studentService.getAllStudents();

        // Build summary list
        List<Map<String, Object>> summaryList = new ArrayList<>();
        for (Student s : allStudents) {
            double pct = attendanceService.getAttendancePercentage(s.getStudentId());
            Map<String, Object> entry = new HashMap<>();
            entry.put("studentId",  s.getStudentId());
            entry.put("name",       s.getName());
            entry.put("department", s.getDepartment());
            entry.put("percentage", pct);
            entry.put("initials",   getInitials(s.getName()));
            summaryList.add(entry);
        }

        // Selected student detail
        Map<String, Object> selected = null;

        if (studentId != null && !studentId.isEmpty()) {
            Student s = studentService.getStudentByStudentId(studentId);
            if (s != null) {
                List<Attendance> records = attendanceService.getAttendanceByStudentId(s.getId());
                long present = records.stream().filter(a -> a.getStatus().equals("P")).count();
                long absent  = records.stream().filter(a -> a.getStatus().equals("A")).count();
                long late    = records.stream().filter(a -> a.getStatus().equals("L")).count();
                double pct   = attendanceService.getAttendancePercentage(s.getStudentId());

                // Monthly breakdown
                Map<String, long[]> monthly = new LinkedHashMap<>();
                String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                        "Jul","Aug","Sep","Oct","Nov","Dec"};
                for (String m : months) monthly.put(m, new long[]{0,0,0});

                for (Attendance a : records) {
                    try {
                        int month = Integer.parseInt(a.getDate().split("-")[1]);
                        String key = months[month - 1];
                        if (a.getStatus().equals("P"))      monthly.get(key)[0]++;
                        else if (a.getStatus().equals("A")) monthly.get(key)[1]++;
                        else if (a.getStatus().equals("L")) monthly.get(key)[2]++;
                    } catch (Exception ignored) {}
                }

                selected = new HashMap<>();
                selected.put("studentId",  s.getStudentId());
                selected.put("name",       s.getName());
                selected.put("email",      s.getEmail());
                selected.put("department", s.getDepartment());
                selected.put("batch",      s.getBatch());
                selected.put("initials",   getInitials(s.getName()));
                selected.put("total",      records.size());
                selected.put("present",    present);
                selected.put("absent",     absent);
                selected.put("late",       late);
                selected.put("percentage", pct);
                selected.put("monthly",    monthly);

                String feedback;
                if (pct >= 90)      feedback = "Excellent attendance — keep it up!";
                else if (pct >= 75) feedback = "Good attendance. Stay consistent!";
                else if (pct >= 60) feedback = "Warning: Attendance is below recommended level.";
                else                feedback = "Critical: Immediate improvement required!";
                selected.put("feedback", feedback);
            }

        } else if (!allStudents.isEmpty()) {
            return reports(allStudents.get(0).getStudentId(), session, model);
        }

        model.addAttribute("summaryList", summaryList);
        model.addAttribute("selected", selected);
        model.addAttribute("selectedId", studentId);
        model.addAttribute("pendingLeaveCount", leaveService.getPendingCount());

        return "reports";
    }

    // Export CSV
    @GetMapping("/reports/export")
    public void exportCsv(HttpServletResponse response, HttpSession session) throws IOException {

        // ✅ FIXED: Export CSV এর ক্ষেত্রেও Admin এবং Teacher উভয়কেই অ্যালাউ করা হলো
        if (!SessionUtil.isAdmin(session) && !SessionUtil.isTeacher(session)) {
            response.sendRedirect("/access-denied");
            return;
        }

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=attendance_report.csv");

        PrintWriter writer = response.getWriter();
        writer.println("Student ID,Name,Department,Semester,Attendance %,Status");

        for (Student s : studentService.getAllStudents()) {
            double pct = attendanceService.getAttendancePercentage(s.getStudentId());
            String status = pct >= 90 ? "Excellent" : pct >= 80 ? "Good" : pct >= 60 ? "Warning" : "Critical";
            writer.println(
                    s.getStudentId() + "," +
                            s.getName() + "," +
                            s.getDepartment() + "," +
                            s.getBatch() + "," +
                            pct + "," +
                            status
            );
        }
        writer.flush();
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "ST";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2)
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}