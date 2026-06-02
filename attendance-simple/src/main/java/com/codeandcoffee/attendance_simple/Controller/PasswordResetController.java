package com.codeandcoffee.attendance_simple.Controller;

import com.codeandcoffee.attendance_simple.model.Student;
import com.codeandcoffee.attendance_simple.model.Teacher;
import com.codeandcoffee.attendance_simple.model.User;
import com.codeandcoffee.attendance_simple.service.StudentService;
import com.codeandcoffee.attendance_simple.service.TeacherService;
import com.codeandcoffee.attendance_simple.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Random;

@Controller
public class PasswordResetController {

    @Autowired
    private UserService userService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private TeacherService teacherService; // ✅ টিচারদের ডাটা চেক করার জন্য সার্ভিস যোগ করা হলো

    @Autowired
    private JavaMailSender mailSender;

    // ── Step 1: Forgot password page ──
    @GetMapping("/forgot-password")
    public String forgotPage() {
        return "forgot-password";
    }

    // ── Step 1: Verify Username ──
    @PostMapping("/forgot-password/verify-user")
    public String verifyUser(@RequestParam String username, Model model) {
        User user = userService.findByUsername(username);

        if (user == null) {
            model.addAttribute("error", "এই username টি পাওয়া যায়নি।");
            model.addAttribute("step", 1);
            return "forgot-password";
        }

        model.addAttribute("username", username);
        model.addAttribute("step", 2);
        return "forgot-password";
    }

    // ── Step 2: Verify Email MATCH & Send OTP ──
    @PostMapping("/forgot-password/send-otp")
    public String sendOtp(
            @RequestParam String username,
            @RequestParam String email,
            HttpSession session,
            Model model) {

        User user = userService.findByUsername(username);
        boolean isEmailValid = false;

        if (user == null) {
            model.addAttribute("error", "ইউজার খুঁজে পাওয়া যায়নি।");
            model.addAttribute("step", 1);
            return "forgot-password";
        }

        // ── ১. ইউজার যদি STUDENT হয় ──
        if ("STUDENT".equals(user.getRole())) {
            Student student = studentService.getStudentByStudentId(username);
            if (student != null && student.getEmail() != null && email.trim().equalsIgnoreCase(student.getEmail().trim())) {
                isEmailValid = true;
            }
        }
        // ── ২. ইউজার যদি TEACHER হয় ──
        else if ("TEACHER".equals(user.getRole())) {
            // প্রথমে ইউজারের নিজের ইমেইল ফিল্ড চেক করবে (users.txt থেকে)
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                if (email.trim().equalsIgnoreCase(user.getEmail().trim())) {
                    isEmailValid = true;
                }
            }
            // যদি users.txt এ কোনো কারণে মিস হয়, তবে teacher.txt ফাইল থেকে চেক করবে (Double Safety)
            else {
                Teacher teacher = teacherService.getTeacherByName(user.getLinkedId());
                if (teacher != null && teacher.getEmail() != null && email.trim().equalsIgnoreCase(teacher.getEmail().trim())) {
                    isEmailValid = true;
                }
            }
        }
        // ── ৩. ইউজার যদি ADMIN হয় ──
        else if ("ADMIN".equals(user.getRole())) {
            // এডমিনদের জন্য যদি নির্দিষ্ট কোনো ইমেইল থাকে, এখানে চেক করতে পারেন
            if (user.getEmail() != null && email.trim().equalsIgnoreCase(user.getEmail().trim())) {
                isEmailValid = true;
            }
        }

        // ❌ ভ্যালিডেশন ফেইল হলে (ইমেইল না মিললে) লাল ওয়ার্নিং দাও
        if (!isEmailValid) {
            model.addAttribute("error", "ভুল ইমেইল! এই ইমেইলটি আপনার অ্যাকাউন্টের সাথে রেজিস্টার্ড নয়।");
            model.addAttribute("username", username);
            model.addAttribute("step", 2);
            return "forgot-password";
        }

        // ── ৩. জেনারেট 6-Digit OTP ──
        String otp = String.format("%06d", new Random().nextInt(999999));
        session.setAttribute("RESET_OTP_" + username, otp);

        // ── ৪. ইমেইল সেন্ড করা ──
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email.trim());
            message.setSubject("Nexus One - Password Reset OTP");
            message.setText("Hello " + username + ",\n\nYour OTP for password recovery is: " + otp + "\n\nPlease do not share this code with anyone.\n\nRegards,\nNexus One Team (KYAU)");
            mailSender.send(message);
        } catch (Exception e) {
            model.addAttribute("error", "ইমেইল পাঠানো সম্ভব হয়নি। ইন্টারনেট কানেকশন চেক করুন।");
            model.addAttribute("username", username);
            model.addAttribute("step", 2);
            return "forgot-password";
        }

        // Success -> Go to OTP Verification Step
        model.addAttribute("message", "An OTP has been sent to " + email);
        model.addAttribute("username", username);
        model.addAttribute("email", email);
        model.addAttribute("step", 3);
        return "forgot-password";
    }

    // ── Step 3: Verify OTP ──
    @PostMapping("/forgot-password/verify-otp")
    public String verifyOtp(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String otp,
            HttpSession session,
            Model model) {

        String sessionOtp = (String) session.getAttribute("RESET_OTP_" + username);

        if (sessionOtp == null || !sessionOtp.equals(otp.trim())) {
            model.addAttribute("error", "ভুল OTP! দয়া করে সঠিক কোডটি দিন।");
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            model.addAttribute("step", 3);
            return "forgot-password";
        }

        model.addAttribute("username", username);
        model.addAttribute("step", 4);
        return "forgot-password";
    }

    // ── Step 4: Reset Password ──
    @PostMapping("/forgot-password/reset")
    public String resetPassword(
            @RequestParam String username,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            Model model) {

        if (newPassword.length() < 6) {
            model.addAttribute("error", "Password কমপক্ষে 6 character হতে হবে।");
            model.addAttribute("username", username);
            model.addAttribute("step", 4);
            return "forgot-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Password দুটো মিলছে না।");
            model.addAttribute("username", username);
            model.addAttribute("step", 4);
            return "forgot-password";
        }

        boolean success = userService.resetPassword(username, newPassword);

        if (!success) {
            model.addAttribute("error", "Password reset করা সম্ভব হয়নি।");
            model.addAttribute("username", username);
            model.addAttribute("step", 4);
            return "forgot-password";
        }

        session.removeAttribute("RESET_OTP_" + username);
        model.addAttribute("step", 5);
        return "forgot-password";
    }
}