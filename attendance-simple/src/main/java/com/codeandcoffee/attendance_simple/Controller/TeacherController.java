package com.codeandcoffee.attendance_simple.Controller;

import com.codeandcoffee.attendance_simple.model.Teacher;
import com.codeandcoffee.attendance_simple.model.User;
import com.codeandcoffee.attendance_simple.service.TeacherService;
import com.codeandcoffee.attendance_simple.service.UserService;
import com.codeandcoffee.attendance_simple.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@Controller
public class TeacherController {

    @Autowired private TeacherService teacherService;
    @Autowired private UserService userService;

    @GetMapping("/teachers")
    public String viewTeachers(HttpSession session, Model model) {
        String redirect = SessionUtil.redirectIfNotAdmin(session);
        if (redirect != null) return redirect;

        List<Teacher> teachers = teacherService.getAllTeachers();
        model.addAttribute("teachers", teachers);
        model.addAttribute("totalTeachers", teachers.size());

        return "teachers";
    }

    @GetMapping("/teachers/image/{id}")
    public ResponseEntity<byte[]> getTeacherImage(@PathVariable int id) {
        try {
            File img = teacherService.getTeacherImageFile(id);
            String basePath = img.getAbsolutePath();
            if (!img.exists()) { img = new File(basePath.replace(".jpg", ".JPG")); }
            if (!img.exists()) { img = new File(basePath.replace(".jpg", ".png")); }
            if (!img.exists()) { img = new File(basePath.replace(".jpg", ".PNG")); }
            if (!img.exists()) {
                img = teacherService.getTeacherImageTargetFile(id);
                if (img != null) {
                    String targetPath = img.getAbsolutePath();
                    if (!img.exists()) { img = new File(targetPath.replace(".jpg", ".JPG")); }
                    if (!img.exists()) { img = new File(targetPath.replace(".jpg", ".png")); }
                    if (!img.exists()) { img = new File(targetPath.replace(".jpg", ".PNG")); }
                }
            }
            if (img != null && img.exists()) {
                byte[] imageBytes = Files.readAllBytes(img.toPath());
                MediaType contentType = MediaType.IMAGE_JPEG;
                if (img.getName().toLowerCase().endsWith(".png")) {
                    contentType = MediaType.IMAGE_PNG;
                }
                return ResponseEntity.ok()
                        .contentType(contentType)
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .header(HttpHeaders.PRAGMA, "no-cache")
                        .header(HttpHeaders.EXPIRES, "0")
                        .body(imageBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/teachers/add")
    public String addTeacher(
            @RequestParam String name,
            @RequestParam String department,
            @RequestParam String subject,
            @RequestParam String designation,
            @RequestParam(required = false, defaultValue = "") String email,
            @RequestParam(required = false) MultipartFile image) {

        // 🔥 FIXED: Added strict trim to prevent accidental spaces from breaking email routing
        String cleanName = name.trim();
        String cleanEmail = email.trim();

        Teacher newTeacher = new Teacher();
        newTeacher.setName(cleanName);
        newTeacher.setDepartment(department.trim());
        newTeacher.setSubject(subject.trim());
        newTeacher.setDesignation(designation.trim());
        newTeacher.setEmail(cleanEmail);

        teacherService.addTeacher(newTeacher, image);

        // ইউজার টেবিলেও সেভ হচ্ছে
        User newUser = new User(cleanName, "123456", "TEACHER", cleanName, "i am a teacher", cleanEmail);
        userService.addUser(newUser);

        return "redirect:/teachers";
    }

    @PostMapping("/teachers/edit")
    public String editTeacher(
            @RequestParam int teacherId,
            @RequestParam String name,
            @RequestParam String department,
            @RequestParam String subject,
            @RequestParam String designation,
            @RequestParam(required = false, defaultValue = "") String email,
            @RequestParam(required = false) MultipartFile image) {

        // 🔥 FIXED: Strict trim applied for update events
        String cleanName = name.trim();
        String cleanEmail = email.trim();

        Teacher oldTeacher = teacherService.getTeacherById(teacherId);
        String oldName = oldTeacher != null ? oldTeacher.getName() : cleanName;

        teacherService.editTeacher(teacherId, cleanName, department.trim(), subject.trim(), designation.trim(), cleanEmail, image);

        if (oldTeacher != null) {
            userService.updateTeacherUser(oldName, cleanName, cleanEmail);
        }

        return "redirect:/teachers";
    }

    @PostMapping("/teachers/remove/{teacherId}")
    public String removeTeacher(@PathVariable int teacherId) {

        Teacher t = teacherService.getTeacherById(teacherId);
        if (t != null) {
            userService.removeUserByLinkedId("TEACHER", t.getName());
        }

        teacherService.removeTeacher(teacherId);
        return "redirect:/teachers";
    }
}