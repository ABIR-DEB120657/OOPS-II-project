package com.codeandcoffee.attendance_simple.Controller;

import com.codeandcoffee.attendance_simple.model.Grade;
import com.codeandcoffee.attendance_simple.model.Student;
import com.codeandcoffee.attendance_simple.service.AttendanceService;
import com.codeandcoffee.attendance_simple.service.GradeService;
import com.codeandcoffee.attendance_simple.service.LeaveService;
import com.codeandcoffee.attendance_simple.service.StudentService;
import com.codeandcoffee.attendance_simple.service.NotificationService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class GradeController {

    @Autowired private GradeService      gradeService;
    @Autowired private StudentService    studentService;
    @Autowired private LeaveService      leaveService;
    @Autowired private AttendanceService attendanceService;

    // ✅ Autowired Nexus One Master Notification Engine
    @Autowired private NotificationService notificationService;

    @GetMapping("/grades")
    public String grades(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filter,
            Model model) {

        // ── সব student summaries (একজন student = একটা row) ──────────────
        List<Map<String, Object>> allSummaries = gradeService.getStudentSummaries();

        // ── Search / Filter ──────────────────────────────────────────────
        List<Map<String, Object>> summaries = allSummaries;
        if (search != null && !search.trim().isEmpty()) {
            String q = search.toLowerCase();
            summaries = summaries.stream()
                    .filter(s -> s.get("name").toString().toLowerCase().contains(q) ||
                            s.get("department").toString().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }
        if (filter != null && !filter.trim().isEmpty() && !filter.equals("all")) {
            summaries = summaries.stream()
                    .filter(s -> s.get("letterGrade").toString().equals(filter))
                    .collect(Collectors.toList());
        }

        // ── ✅ FIX: passCount & failCount — per STUDENT, not per grade record ──
        // allSummaries ব্যবহার করো (filter করা list না, সব students)
        // একজন student "passed" যদি তার avgMarks >= 40 হয়
        long passCount = allSummaries.stream()
                .filter(s -> (int) s.get("avgMarks") >= 40)
                .count();

        long failCount = allSummaries.stream()
                .filter(s -> (int) s.get("avgMarks") < 40)
                .count();

        // ── ✅ FIX: avgGpa — per student CGPA average, not per grade record ──
        double avgGpa = allSummaries.stream()
                .mapToDouble(s -> (double) s.get("cgpa"))
                .average()
                .orElse(0.0);

        // ── Other stats ──────────────────────────────────────────────────
        Grade  topPerformer = gradeService.getTopPerformer();
        double classAverage = gradeService.getClassAverage();

        // ── Model ────────────────────────────────────────────────────────
        model.addAttribute("summaries",         summaries);
        model.addAttribute("totalStudents",     allSummaries.size());
        model.addAttribute("avgGpa",            Math.round(avgGpa * 100.0) / 100.0);
        model.addAttribute("avgMarks",          classAverage);
        model.addAttribute("topPerformer",      topPerformer);
        model.addAttribute("passCount",         passCount);
        model.addAttribute("failCount",         failCount);
        model.addAttribute("pendingLeaveCount", leaveService.getPendingCount());
        model.addAttribute("search",            search);
        model.addAttribute("filter",            filter);
        return "grades";
    }

    @GetMapping("/grades/student/{id}")
    @ResponseBody
    public Map<String, Object> getStudentResult(@PathVariable int id) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Student info — prefer student.txt, fallback to grades.txt
        Student s = studentService.getStudentById(id);
        String studentName, department, batch;
        if (s != null) {
            studentName = s.getName();
            department  = s.getDepartment();
            batch       = s.getBatch() != null ? s.getBatch() : "";
        } else {
            List<Grade> fb = gradeService.getGradesByStudentRowId(id);
            studentName = fb.isEmpty() ? "Unknown" : fb.get(0).getStudentName();
            department  = fb.isEmpty() ? "CSE"     : fb.get(0).getDepartment();
            batch       = "";
        }

        // Grade data
        Map<String, List<Grade>> bySemester = gradeService.getGradesBySemester(id);
        List<Map<String, Object>> semesterList = new ArrayList<>();
        double totalEarnedGP = 0, totalCredits = 0;

        for (Map.Entry<String, List<Grade>> entry : bySemester.entrySet()) {
            List<Grade> courses = entry.getValue();
            double semGP = 0, semCredits = 0;
            List<Map<String, Object>> courseList = new ArrayList<>();
            for (Grade g : courses) {
                Map<String, Object> c = new LinkedHashMap<>();
                c.put("courseCode",  g.getCourseCode());
                c.put("subject",     g.getSubject());
                c.put("credits",     g.getCredits());
                c.put("marks",       g.getMarks());
                c.put("gradePoint",  g.getGpa());
                c.put("letterGrade", g.getLetterGrade());
                courseList.add(c);
                semGP      += g.getGpa() * g.getCredits();
                semCredits += g.getCredits();
            }
            double tgpa = semCredits > 0
                    ? Math.round((semGP / semCredits) * 100.0) / 100.0
                    : 0;
            totalEarnedGP += semGP;
            totalCredits  += semCredits;

            Map<String, Object> sem = new LinkedHashMap<>();
            sem.put("semester",   entry.getKey());
            sem.put("courses",    courseList);
            sem.put("tgpa",       tgpa);
            sem.put("semCredits", semCredits);
            semesterList.add(sem);
        }

        double cgpa = totalCredits > 0
                ? Math.round((totalEarnedGP / totalCredits) * 100.0) / 100.0
                : 0;

        // Attendance summary
        Map<String, Object> attSummary = new LinkedHashMap<>();
        try {
            var attList  = attendanceService.getAttendanceByStudentId(id);
            long total   = attList.size();
            long present = attList.stream().filter(a -> "P".equalsIgnoreCase(a.getStatus())).count();
            long absent  = attList.stream().filter(a -> "A".equalsIgnoreCase(a.getStatus())).count();
            long late    = attList.stream().filter(a -> "L".equalsIgnoreCase(a.getStatus())).count();
            double rate  = total > 0
                    ? Math.round((present * 100.0 / total) * 10) / 10.0
                    : 0.0;

            List<Map<String, Object>> recent = attList.stream()
                    .sorted(Comparator.comparing(
                            a -> a.getDate().toString(), Comparator.reverseOrder()))
                    .limit(10)
                    .map(a -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("date",   a.getDate().toString());
                        m.put("status", a.getStatus());
                        m.put("remark", a.getRemark() != null ? a.getRemark() : "");
                        return m;
                    })
                    .collect(Collectors.toList());

            attSummary.put("totalClasses", total);
            attSummary.put("present",      present);
            attSummary.put("absent",       absent);
            attSummary.put("late",         late);
            attSummary.put("rate",         rate);
            attSummary.put("recent",       recent);
        } catch (Exception e) {
            attSummary.put("totalClasses", 0);
            attSummary.put("present",      0);
            attSummary.put("absent",       0);
            attSummary.put("late",         0);
            attSummary.put("rate",         0.0);
            attSummary.put("recent",       Collections.emptyList());
        }

        result.put("studentId",    id);
        result.put("name",         studentName);
        result.put("department",   department);
        result.put("batch",        batch);
        result.put("semesters",    semesterList);
        result.put("cgpa",         cgpa);
        result.put("totalCredits", totalCredits);
        result.put("attendance",   attSummary);
        return result;
    }

    // 🔥 FIXED: Added Central Notification Trigger for Live Grade Publication Feed
    @PostMapping("/grades/add")
    public String addGrade(@RequestParam int studentId,
                           @RequestParam String courseCode,
                           @RequestParam String subject,
                           @RequestParam double credits,
                           @RequestParam int marks,
                           @RequestParam String semester) {
        Student s   = studentService.getStudentById(studentId);
        String name = (s != null) ? s.getName()       : "Unknown";
        String dept = (s != null) ? s.getDepartment() : "CSE";

        Grade newGrade = new Grade(studentId, name, dept, courseCode, subject, credits, marks, semester);
        gradeService.saveGrade(newGrade);

        // 🧠 Nexus Intelligence: Dispatch live results published status to Student Inbox
        try {
            String notifMessage = "🏆 Result published for " + courseCode + " (" + subject + ") - " + semester + ". Marks obtained: " + marks + " [Grade: " + newGrade.getLetterGrade() + "].";
            notificationService.dispatchToStudent(studentId, "RESULT", notifMessage);
        } catch (Exception e) {
            System.out.println("⚠️ Grade Notification Trigger experiencing a silent fallback: " + e.getMessage());
        }

        return "redirect:/grades";
    }

    @GetMapping("/grades/export")
    public void exportCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=grades_report.csv");
        PrintWriter writer = response.getWriter();
        writer.println("Student ID,Name,Department,Course Code,Subject," +
                "Credits,Marks,Letter Grade,GPA,Semester");
        for (Grade g : gradeService.getAllGrades()) {
            writer.println(
                    g.getStudentId()   + "," +
                            g.getStudentName() + "," +
                            g.getDepartment()  + "," +
                            g.getCourseCode()  + "," +
                            g.getSubject()     + "," +
                            g.getCredits()     + "," +
                            g.getMarks()       + "," +
                            g.getLetterGrade() + "," +
                            g.getGpa()         + "," +
                            g.getSemester()
            );
        }
        writer.flush();
    }
}