package com.codeandcoffee.attendance_simple.Controller;

import com.codeandcoffee.attendance_simple.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired private NotificationService notificationService;

    // ── 1. Admin notifications ────────────────────────────────────────────
    @GetMapping
    public String notifications(Model model) {
        model.addAttribute("notifications", notificationService.getForAdmin());
        model.addAttribute("unreadCount",   notificationService.getUnreadCount());
        return "notifications";
    }

    // ── 2. Student নিজের notifications ───────────────────────────────────
    @GetMapping("/my")
    public String myNotifications(HttpSession session, Model model) {
        Object sid = session.getAttribute("studentId");
        if (sid == null) return "redirect:/login";
        int studentId = Integer.parseInt(sid.toString());

        model.addAttribute("notifications", notificationService.getNotificationsForStudent(studentId));
        model.addAttribute("unreadCount", notificationService.getUnreadCountForStudent(studentId));

        // সাইডবারের জন্য স্টুডেন্ট ইনফো (যদি লাগে)
        model.addAttribute("student", session.getAttribute("loggedIn"));
        return "my-notifications";
    }

    // ── 3. Teacher এর notifications ───────────────────────────────────────
    @GetMapping("/teacher")
    public String teacherNotifications(HttpSession session, Model model) {
        Object tid = session.getAttribute("teacherId");
        if (tid == null) return "redirect:/login";
        int teacherId = Integer.parseInt(tid.toString());

        model.addAttribute("notifications", notificationService.getForTeacher(teacherId));
        model.addAttribute("unreadCount", notificationService.getUnreadCountForTeacher(teacherId));

        model.addAttribute("username", session.getAttribute("loggedIn"));
        model.addAttribute("photoPath", teacherId > 0 ? "/teachers/" + teacherId + ".jpg" : "/teachers/0.jpg");

        return "teacher-notifications";
    }

    // ── 4. Status Updates (FIXED) ─────────────────────────────────────────
    @PostMapping("/read/{id}")
    public String markRead(@PathVariable("id") int id, @RequestParam(value = "back", defaultValue = "/notifications") String back) {
        notificationService.markAsRead(id);
        return "redirect:" + back;
    }

    // ✅ FIX: এখানে back প্যারামিটার অ্যাড করা হয়েছে যাতে যেখান থেকে ক্লিক করা হবে, সেখানেই ফেরত যায়
    @PostMapping("/read-all")
    public String markAllRead(@RequestParam(value = "back", defaultValue = "/notifications") String back) {
        notificationService.markAllAsRead();
        return "redirect:" + back;
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable("id") int id, @RequestParam(value = "back", defaultValue = "/notifications") String back) {
        notificationService.deleteNotification(id);
        return "redirect:" + back;
    }

    @PostMapping("/send-alert")
    @ResponseBody
    public String sendAlert(@RequestParam("studentName") String studentName) {
        notificationService.addNotification("ALERT", "Admin",
                studentName + " এর attendance critical level এ আছে!",
                java.time.LocalDate.now().toString());
        return "ok";
    }

    // ══════════════════════════════════════════════════════════════
    // 🧠 5. NEXUS INTELLIGENCE: MANUAL DIRECT DISPATCHERS (NEW FEATURE)
    // ══════════════════════════════════════════════════════════════

    // অ্যাডমিন প্যানেল থেকে নির্দিষ্ট স্টুডেন্টকে সরাসরি সিস্টেম অ্যালার্ট পাঠানোর জন্য
    @PostMapping("/dispatch/student")
    @ResponseBody
    public String dispatchDirectToStudent(@RequestParam("studentId") int studentId, @RequestParam("message") String message) {
        notificationService.dispatchToStudent(studentId, "NOTICE", message);
        return "Success: Alert sent to student ID " + studentId;
    }

    // অ্যাডমিন প্যানেল থেকে নির্দিষ্ট টিচারকে সরাসরি সিস্টেম অ্যালার্ট পাঠানোর জন্য
    @PostMapping("/dispatch/teacher")
    @ResponseBody
    public String dispatchDirectToTeacher(@RequestParam("teacherId") int teacherId, @RequestParam("message") String message) {
        notificationService.dispatchToTeacher(teacherId, "NOTICE", "Admin", message);
        return "Success: Alert sent to teacher ID " + teacherId;
    }
}