package com.codeandcoffee.attendance_simple.Controller;

import com.codeandcoffee.attendance_simple.model.Student;
import com.codeandcoffee.attendance_simple.model.Teacher;
import com.codeandcoffee.attendance_simple.model.User;
import com.codeandcoffee.attendance_simple.service.StudentService;
import com.codeandcoffee.attendance_simple.service.TeacherService;
import com.codeandcoffee.attendance_simple.service.UserService;
import com.codeandcoffee.attendance_simple.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @Autowired private UserService    userService;
    @Autowired private TeacherService teacherService;
    @Autowired private StudentService studentService;

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (SessionUtil.isLoggedIn(session)) {
            String role = (String) session.getAttribute("role");
            if ("STUDENT".equals(role)) return "redirect:/student-portal";
            if ("TEACHER".equals(role)) return "redirect:/teacher-portal";
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        User user = userService.authenticate(username, password);

        if (user == null) {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }

        session.setAttribute("role", user.getRole());

        if ("STUDENT".equals(user.getRole())) {
            String studentIdStr = user.getLinkedId();
            session.setAttribute("loggedIn", studentIdStr);

            // ✅ Student object খুঁজে session এ সব set করো
            Student student = studentService.getAllStudents().stream()
                    .filter(s -> studentIdStr.equals(s.getStudentId()))
                    .findFirst().orElse(null);

            if (student != null) {
                session.setAttribute("studentId",   student.getId());
                session.setAttribute("studentStrId", student.getStudentId());
                session.setAttribute("department",  student.getDepartment());
                session.setAttribute("studentName", student.getName());
            }

        } else if ("TEACHER".equals(user.getRole())) {
            String teacherName = user.getLinkedId();
            session.setAttribute("loggedIn", teacherName);

            Teacher teacher = teacherService.getTeacherByName(teacherName);
            if (teacher != null) {
                session.setAttribute("teacherId",  teacher.getTeacherId());
                session.setAttribute("department", teacher.getDepartment());
            } else {
                session.setAttribute("teacherId", 0);
            }

        } else {
            session.setAttribute("loggedIn", user.getUsername());
        }

        switch (user.getRole()) {
            case "ADMIN":   return "redirect:/dashboard";
            case "TEACHER": return "redirect:/teacher-portal";
            case "STUDENT": return "redirect:/student-portal";
            default:        return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied(HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");
        model.addAttribute("role", role != null ? role : "UNKNOWN");
        return "access-denied";
    }
}