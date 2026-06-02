package com.codeandcoffee.attendance_simple.Controller;

import com.codeandcoffee.attendance_simple.model.LeaveRequest;
import com.codeandcoffee.attendance_simple.model.Student;
import com.codeandcoffee.attendance_simple.model.Teacher;
import com.codeandcoffee.attendance_simple.service.LeaveService;
import com.codeandcoffee.attendance_simple.service.NotificationService;
import com.codeandcoffee.attendance_simple.service.StudentService;
import com.codeandcoffee.attendance_simple.service.TeacherService;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
public class LeaveController {

    @Autowired private LeaveService        leaveService;
    @Autowired private NotificationService notificationService;
    @Autowired private TeacherService      teacherService;
    @Autowired private StudentService      studentService;
    @Autowired private JavaMailSender      mailSender;

    private final ExecutorService emailExecutor = Executors.newCachedThreadPool();

    // ══════════════════════════════════════════════════════════════════
    //  ADMIN
    // ══════════════════════════════════════════════════════════════════
    @GetMapping("/leave")
    public String adminLeave(@RequestParam(defaultValue = "pending") String tab, Model model) {
        List<LeaveRequest> pending = leaveService.getPendingRequests();
        List<LeaveRequest> all     = leaveService.getAllLeaveRequests();
        model.addAttribute("pendingRequests", pending);
        model.addAttribute("allRequests", all);
        model.addAttribute("pendingLeaveCount", pending.size());
        model.addAttribute("tab", tab);
        return "leave";
    }

    @PostMapping("/leave/approve/{id}")
    public String adminApprove(@PathVariable int id) {
        LeaveRequest req = leaveService.getLeaveRequestById(id);
        leaveService.updateStatus(id, "APPROVED");

        // 🧠 Nexus Intelligence: Alert Student on Admin Approval
        if (req != null) {
            notificationService.dispatchToStudent(req.getStudentId(), "LEAVE", "✅ Your leave request (" + req.getFromDate() + " to " + req.getToDate() + ") has been APPROVED by the Administration.");
        }
        return "redirect:/leave";
    }

    @PostMapping("/leave/reject/{id}")
    public String adminReject(@PathVariable int id) {
        LeaveRequest req = leaveService.getLeaveRequestById(id);
        leaveService.updateStatus(id, "REJECTED");

        // 🧠 Nexus Intelligence: Alert Student on Admin Rejection
        if (req != null) {
            notificationService.dispatchToStudent(req.getStudentId(), "LEAVE", "❌ Your leave request (" + req.getFromDate() + " to " + req.getToDate() + ") has been REJECTED by the Administration.");
        }
        return "redirect:/leave";
    }

    // ══════════════════════════════════════════════════════════════════
    //  STUDENT
    // ══════════════════════════════════════════════════════════════════
    @GetMapping("/student-leave")
    public String studentLeaveForm(HttpSession session, Model model) {
        Integer studentId = getStudentId(session);
        if (studentId == null) return "redirect:/login";

        String loggedIn = session.getAttribute("loggedIn").toString();
        Student student = findStudent(loggedIn);

        List<LeaveRequest> leaves = leaveService.getRequestsForStudent(studentId);
        Collections.reverse(leaves);

        model.addAttribute("teachers", teacherService.getAllTeachers());
        model.addAttribute("leaves", leaves);
        model.addAttribute("student", student);
        model.addAttribute("unreadNotifCount", notificationService.getUnreadCountForStudent(studentId));
        model.addAttribute("initials", getInitials(student != null ? student.getName() : loggedIn));

        return "student-leave";
    }

    @PostMapping("/student-leave/submit")
    public String submitLeave(@RequestParam int teacherId, @RequestParam String department, @RequestParam String fromDate, @RequestParam String toDate, @RequestParam String reason, HttpSession session) {
        Integer studentId = getStudentId(session);
        if (studentId == null) return "redirect:/login";

        Student student = findStudent(session.getAttribute("loggedIn").toString());
        String sName = student != null ? student.getName() : "Unknown";
        String sId = student != null ? student.getStudentId() : "Unknown";
        String sBatch = student != null ? student.getBatch() : "Unknown";

        leaveService.submitLeave(studentId, sName, department, fromDate, toDate, reason, teacherId);

        Teacher teacher = teacherService.getTeacherById(teacherId);
        String tName = teacher != null ? teacher.getName() : "Teacher";

        String htmlBody = buildPremiumEmail("NEW LEAVE REQUEST", tName, sName, sId, department, sBatch, fromDate, toDate, reason, "#6366f1");
        sendHtmlEmail(teacherId, "🔔 New Leave Request from " + sName, htmlBody);

        return "redirect:/student-leave?success=submitted";
    }

    @PostMapping("/student-leave/edit")
    public String editLeave(@RequestParam int leaveId, @RequestParam int teacherId, @RequestParam String department, @RequestParam String fromDate, @RequestParam String toDate, @RequestParam String reason, HttpSession session) {
        Integer studentId = getStudentId(session);
        LeaveRequest req = leaveService.getLeaveRequestById(leaveId);

        if (studentId != null && req != null && req.getStudentId() == studentId) {
            leaveService.updateLeaveRequest(leaveId, department, fromDate, toDate, reason, teacherId);

            Student student = findStudent(session.getAttribute("loggedIn").toString());
            String sName = student != null ? student.getName() : req.getStudentName();
            String sId = student != null ? student.getStudentId() : "Unknown";
            String sBatch = student != null ? student.getBatch() : "Unknown";

            Teacher teacher = teacherService.getTeacherById(teacherId);
            String tName = teacher != null ? teacher.getName() : "Teacher";

            String htmlBody = buildPremiumEmail("UPDATED LEAVE REQUEST", tName, sName, sId, department, sBatch, fromDate, toDate, reason, "#f59e0b");
            sendHtmlEmail(teacherId, "✏️ Updated Leave Request from " + sName, htmlBody);
        }
        return "redirect:/student-leave?success=edited";
    }

    @PostMapping("/student-leave/cancel")
    public String cancelLeave(@RequestParam int leaveId, HttpSession session) {
        Integer studentId = getStudentId(session);
        LeaveRequest req = leaveService.getLeaveRequestById(leaveId);

        if (studentId != null && req != null && req.getStudentId() == studentId) {
            leaveService.deleteLeaveRequest(leaveId);

            Student student = findStudent(session.getAttribute("loggedIn").toString());
            String sName = student != null ? student.getName() : req.getStudentName();
            String sId = student != null ? student.getStudentId() : "Unknown";
            String sBatch = student != null ? student.getBatch() : "Unknown";

            Teacher teacher = teacherService.getTeacherById(req.getTeacherId());
            String tName = teacher != null ? teacher.getName() : "Teacher";

            String htmlBody = buildPremiumEmail("CANCELED LEAVE REQUEST", tName, sName, sId, req.getDepartment(), sBatch, req.getFromDate(), req.getToDate(), req.getReason(), "#ef4444");
            sendHtmlEmail(req.getTeacherId(), "❌ Canceled Leave Request from " + sName, htmlBody);
        }
        return "redirect:/student-leave?success=canceled";
    }

    // ══════════════════════════════════════════════════════════════════
    //  TEACHER
    // ══════════════════════════════════════════════════════════════════
    @GetMapping("/teacher-leave")
    public String teacherLeave(HttpSession session, Model model) {
        Integer teacherId = getTeacherId(session);
        if (teacherId == null) return "redirect:/login";

        // 🔥 FIXED: Added 'colors' array to prevent template resolving errors
        model.addAttribute("colors", new String[]{"#4f46e5","#0891b2","#16a34a","#d97706","#dc2626","#9333ea","#0d9488"});

        model.addAttribute("pendingRequests", leaveService.getPendingForTeacher(teacherId));
        model.addAttribute("allRequests", leaveService.getRequestsForTeacher(teacherId));
        model.addAttribute("pendingLeaveCount", leaveService.getPendingCountForTeacher(teacherId));
        return "teacher-leave";
    }

    @PostMapping("/teacher-leave/approve/{id}")
    public String teacherApprove(@PathVariable int id, HttpSession session) {
        LeaveRequest req = leaveService.getLeaveRequestById(id);
        leaveService.updateStatus(id, "APPROVED");

        // 🧠 Nexus Intelligence: Alert Student on Teacher Approval
        if (req != null) {
            String loggedIn = session.getAttribute("loggedIn") != null ? session.getAttribute("loggedIn").toString() : "Your Faculty";
            notificationService.dispatchToStudent(req.getStudentId(), "LEAVE", "✅ Your leave request (" + req.getFromDate() + " to " + req.getToDate() + ") has been APPROVED by " + loggedIn + ".");
        }
        return "redirect:/teacher-leave";
    }

    @PostMapping("/teacher-leave/reject/{id}")
    public String teacherReject(@PathVariable int id, HttpSession session) {
        LeaveRequest req = leaveService.getLeaveRequestById(id);
        leaveService.updateStatus(id, "REJECTED");

        // 🧠 Nexus Intelligence: Alert Student on Teacher Rejection
        if (req != null) {
            String loggedIn = session.getAttribute("loggedIn") != null ? session.getAttribute("loggedIn").toString() : "Your Faculty";
            notificationService.dispatchToStudent(req.getStudentId(), "LEAVE", "❌ Your leave request (" + req.getFromDate() + " to " + req.getToDate() + ") has been REJECTED by " + loggedIn + ".");
        }
        return "redirect:/teacher-leave";
    }

    // ══════════════════════════════════════════════════════════════════
    //  Helpers & HTML Mail Generator
    // ══════════════════════════════════════════════════════════════════

    private String buildPremiumEmail(String actionTitle, String teacherName, String sName, String sId, String sDept, String sBatch, String fromDate, String toDate, String reason, String statusColor) {
        String actionWord = actionTitle.split(" ")[0].toLowerCase();

        return "<html><body style=\"background-color:#f1f5f9; padding:20px; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">" +
                "<div style=\"max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 15px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.05);\">" +

                "<div style=\"background: linear-gradient(135deg, #0f172a, #1e293b); padding: 30px; text-align: center;\">" +
                "<h2 style=\"color: #ffffff; margin: 0; font-size: 24px; letter-spacing: 2px;\">NEXUS ONE <span style=\"font-weight:300; color:#94a3b8;\">PORTAL</span></h2>" +
                "</div>" +

                "<div style=\"padding: 40px; color: #334155;\">" +
                "<h3 style=\"color: " + statusColor + "; border-bottom: 2px solid #f1f5f9; padding-bottom: 15px; margin-top: 0; text-align: center; font-size: 18px; letter-spacing: 1px;\">" + actionTitle + "</h3>" +
                "<p style=\"font-size: 16px;\">Hello <b>" + teacherName + "</b>,</p>" +
                "<p style=\"font-size: 15px; color: #475569; line-height: 1.6;\">A student has <b>" + actionWord + "</b> a leave request. Please review the application details below:</p>" +

                "<table style=\"width: 100%; border-collapse: collapse; margin-top: 30px; border: 1px solid #e2e8f0; border-radius: 10px; overflow: hidden;\">" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; width: 35%; font-weight: 700; color: #64748b; background: #f8fafc;\">Student Name</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #0f172a;\">" + sName + "</td></tr>" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #64748b; background: #f8fafc;\">Student ID</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; color: #0f172a;\">" + sId + "</td></tr>" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #64748b; background: #f8fafc;\">Department</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; color: #0f172a;\">" + sDept + "</td></tr>" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #64748b; background: #f8fafc;\">Batch</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; color: #0f172a;\">" + sBatch + "</td></tr>" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #64748b; background: #f8fafc;\">Duration</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; color: #ef4444; font-weight: 800;\">" + fromDate + " to " + toDate + "</td></tr>" +
                "<tr><td style=\"padding: 15px; font-weight: 700; color: #64748b; background: #f8fafc;\">Reason</td><td style=\"padding: 15px; color: #0f172a; line-height: 1.5;\">" + reason + "</td></tr>" +
                "</table>" +

                "<div style=\"text-align: center; margin-top: 40px;\">" +
                "<a href=\"http://localhost:8080/login\" style=\"display: inline-block; padding: 15px 35px; background: linear-gradient(135deg, #6366f1, #a855f7); color: #ffffff; text-decoration: none; border-radius: 50px; font-weight: 800; font-size: 16px; box-shadow: 0 10px 20px rgba(99, 102, 241, 0.3);\">Login to Review Application</a>" +
                "</div>" +
                "</div>" +

                "<div style=\"background: #f8fafc; padding: 25px; text-align: center; font-size: 13px; color: #94a3b8; border-top: 1px solid #e2e8f0;\">" +
                "&copy; 2026 Nexus One University Portal.<br>This is an automated system notification. Please do not reply to this email." +
                "</div>" +

                "</div></body></html>";
    }

    private void sendHtmlEmail(int teacherId, String subject, String htmlBody) {
        emailExecutor.execute(() -> {
            try {
                Teacher teacher = teacherService.getTeacherById(teacherId);
                if (teacher != null && teacher.getEmail() != null) {
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                    helper.setTo(teacher.getEmail());
                    helper.setSubject(subject);
                    helper.setText(htmlBody, true);
                    mailSender.send(message);
                }
            } catch (Exception e) {
                System.out.println("⚠️ Email Error: " + e.getMessage());
            }
        });
    }

    private Integer getStudentId(HttpSession s) { Object o = s.getAttribute("studentId"); return o == null ? null : Integer.parseInt(o.toString()); }
    private Integer getTeacherId(HttpSession s) { Object o = s.getAttribute("teacherId"); return o == null ? null : Integer.parseInt(o.toString()); }
    private LeaveRequest findById(int id) { return leaveService.getAllLeaveRequests().stream().filter(l -> l.getLeaveId() == id).findFirst().orElse(null); }
    private Student findStudent(String val) { for (Student s : studentService.getAllStudents()) { if (val.equals(s.getStudentId()) || val.equalsIgnoreCase(s.getName())) return s; } return null; }
    private String getInitials(String n) { if (n == null || n.trim().isEmpty()) return "ST"; String[] parts = n.trim().split("\\s+"); return parts.length >= 2 ? ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase() : n.substring(0, Math.min(2, n.length())).toUpperCase(); }
}