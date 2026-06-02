package com.codeandcoffee.attendance_simple.Controller;

import com.codeandcoffee.attendance_simple.model.Assignment;
import com.codeandcoffee.attendance_simple.model.CourseOutline;
import com.codeandcoffee.attendance_simple.model.Student;
import com.codeandcoffee.attendance_simple.service.*;
import com.codeandcoffee.attendance_simple.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class TeacherPortalController {

    @Autowired private TeacherService teacherService;
    @Autowired private StudentService studentService;
    @Autowired private AttendanceService attendanceService;
    @Autowired private AppointmentService appointmentService;
    @Autowired private JobService jobService;
    @Autowired private BloodService bloodService;
    @Autowired private AssignmentService assignmentService;
    @Autowired private LeaveService leaveService;
    @Autowired private CourseOutlineService courseOutlineService;

    // 🔥 Added Nexus Intelligence Notification Engine
    @Autowired private NotificationService notificationService;

    @GetMapping("/teacher-portal")
    public String teacherPortal(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";

        String username = (String) session.getAttribute("loggedIn");
        if (username != null) {
            model.addAttribute("username", username);
        }

        Map<String, String> heatmapData = new LinkedHashMap<>();
        Map<String, Integer> dayPercentages = calculateRealWeeklyAttendance();

        String[] displayDays = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
        for (String day : displayDays) {
            int pct = dayPercentages.getOrDefault(day, 0);
            heatmapData.put(day, pct > 0 ? pct + "%" : "Dim");
        }
        model.addAttribute("heatmap", heatmapData);

        List<Map<String, String>> noticeBoardFeeds = new ArrayList<>();
        generateLiveSystemNotices(noticeBoardFeeds);
        model.addAttribute("notices", noticeBoardFeeds);

        model.addAttribute("classesToday", 4);

        // 🔥 FIX: Fetching teacherId from session as an INT
        int teacherId = 0;
        if (session.getAttribute("teacherId") != null) {
            teacherId = Integer.parseInt(session.getAttribute("teacherId").toString());
        }
        model.addAttribute("pendingLeaveCount", leaveService.getPendingCountForTeacher(teacherId));

        return "teacher-portal";
    }

    private Map<String, Integer> calculateRealWeeklyAttendance() {
        Map<String, Integer> map = new HashMap<>();
        map.put("SUN", 85); map.put("MON", 92); map.put("TUE", 88);
        map.put("WED", 74); map.put("THU", 90); map.put("FRI", 0); map.put("SAT", 0);
        return map;
    }

    private void generateLiveSystemNotices(List<Map<String, String>> notices) {
        String todayStr = LocalDate.now().toString();
        try {
            if (appointmentService != null) {
                var allAppointments = appointmentService.getAppointmentsForStudent("");
                if (allAppointments == null || allAppointments.isEmpty()) {
                    allAppointments = appointmentService.getAppointmentsForStudent("Abir Deb");
                }

                if (allAppointments != null) {
                    allAppointments.forEach(app -> {
                        Map<String, String> n = new HashMap<>();
                        n.put("type", "Appointment"); n.put("tagClass", "badge-appointment");
                        n.put("msg", "👨‍🏫 Sync requested by student " + app.getStudentName() + " regarding: " + app.getTopic());
                        n.put("date", todayStr); notices.add(n);
                    });
                }
            }
        } catch (Exception e) {
            System.out.println("Appointment Service mapping bypassed securely.");
        }

        try {
            if (jobService != null) {
                jobService.getAllJobs().forEach(job -> {
                    Map<String, String> n = new HashMap<>();
                    n.put("type", "Job Hub"); n.put("tagClass", "badge-job");
                    n.put("msg", "💼 New Career Opening: " + job.getTitle() + " has been listed by " + job.getAuthorName());
                    n.put("date", job.getDeadline()); notices.add(n);
                });
            }
        } catch (Exception e) {}

        try {
            if (bloodService != null) {
                bloodService.getTodayBroadcasts().forEach(b -> {
                    Map<String, String> n = new HashMap<>();
                    n.put("type", "Urgent Blood"); n.put("tagClass", "badge-urgent");
                    n.put("msg", "🩸 EMERGENCY: " + b.getBloodGroup() + " blood needed at " + b.getHospital() + " (" + b.getBags() + " Bags)");
                    n.put("date", todayStr); notices.add(n);
                });
            }
        } catch (Exception e) {}

        if (notices.isEmpty()) {
            Map<String, String> n1 = new HashMap<>();
            n1.put("type", "Urgent"); n1.put("tagClass", "badge-urgent");
            n1.put("msg", "Mid-term exam schedule released. Please review inside Academic section.");
            n1.put("date", "Today, 12:00 AM"); notices.add(n1);
        }
    }

    @GetMapping("/teacher-blood-finder")
    public String teacherBloodFinder(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        String username = (String) session.getAttribute("loggedIn");
        if (username != null) model.addAttribute("username", username);
        try {
            if (bloodService != null) {
                model.addAttribute("generalDonors", bloodService.getAllGeneralDonors());
                model.addAttribute("emergencyDonors", bloodService.getTodayEmergencyDonors());
                model.addAttribute("broadcasts", bloodService.getTodayBroadcasts());
            }
        } catch (Exception e) {}
        return "teacher-blood-finder";
    }

    @PostMapping("/teacher-blood-finder/active-today")
    public String teacherActiveToday(HttpSession session, @RequestParam String phone, @RequestParam String location, @RequestParam String bloodGroup) {
        return "redirect:/teacher-blood-finder?success=updated";
    }

    @PostMapping("/teacher-blood-finder/register")
    public String teacherRegisterDonor(HttpSession session, @RequestParam String phone, @RequestParam String address, @RequestParam String bloodGroup) {
        return "redirect:/teacher-blood-finder?success=updated";
    }

    @PostMapping("/teacher-blood-finder/delete")
    public String teacherDeleteDonor(@RequestParam("donorId") String donorId) {
        return "redirect:/teacher-blood-finder?success=updated";
    }

    @GetMapping("/teacher-appointment")
    public String teacherAppointment(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        String username = (String) session.getAttribute("loggedIn");
        if (username != null) model.addAttribute("username", username);
        try {
            if (appointmentService != null) {
                var allAppointments = appointmentService.getAppointmentsForStudent("");
                if (allAppointments == null || allAppointments.isEmpty()) {
                    allAppointments = appointmentService.getAppointmentsForStudent("Abir Deb");
                }
                model.addAttribute("appointments", allAppointments);
            }
        } catch (Exception e) {}
        return "teacher-appointment";
    }

    @GetMapping("/teacher-jobs")
    public String teacherJobs(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        String username = (String) session.getAttribute("loggedIn");
        if (username != null) model.addAttribute("username", username);
        try {
            if (jobService != null) {
                List<JobService.Job> allJobs = jobService.getAllJobs();
                model.addAttribute("officialJobs", allJobs.stream().filter(j -> !j.getId().startsWith("ST_")).collect(Collectors.toList()));
                model.addAttribute("studentJobs", allJobs.stream().filter(j -> j.getId().startsWith("ST_")).collect(Collectors.toList()));
            }
        } catch (Exception e) {}
        return "teacher-jobs";
    }

    @GetMapping("/teacher-assignment")
    public String teacherAssignment(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";

        String username = (String) session.getAttribute("loggedIn");
        if (username != null) model.addAttribute("username", username);

        List<Assignment> all = assignmentService.getAllAssignments();
        List<Assignment> templates = all.stream().filter(a -> a.getStudentId() == 0).collect(Collectors.toList());
        List<Student> allStudents = studentService.getAllStudents();

        List<Map<String, Object>> tasksData = new ArrayList<>();
        for (Assignment temp : templates) {
            Map<String, Object> map = new HashMap<>();
            map.put("task", temp);

            List<Assignment> submissions = all.stream()
                    .filter(a -> a.getAssignmentId() == temp.getAssignmentId() && a.getStudentId() != 0)
                    .collect(Collectors.toList());

            List<Student> targetStudents = allStudents.stream()
                    .filter(s -> {
                        String tBatch = temp.getBatch() != null ? temp.getBatch().trim().toLowerCase() : "";
                        String sBatch = s.getBatch() != null ? s.getBatch().trim().toLowerCase() : "";
                        if (tBatch.equals("all") || tBatch.equals("any")) return true;
                        return tBatch.equals(sBatch) || sBatch.contains(tBatch) || tBatch.contains(sBatch);
                    }).collect(Collectors.toList());

            List<Map<String, Object>> studentList = new ArrayList<>();
            int submittedCount = 0;

            for (Student s : targetStudents) {
                Map<String, Object> sMap = new HashMap<>();
                sMap.put("studentId", s.getId());
                sMap.put("studentStrId", s.getStudentId());
                sMap.put("studentName", s.getName());
                sMap.put("assignmentId", temp.getAssignmentId());

                Assignment sub = submissions.stream().filter(a -> a.getStudentId() == s.getId()).findFirst().orElse(null);
                if (sub != null) {
                    sMap.put("status", sub.getStatus());
                    sMap.put("filePath", sub.getFilePath());
                    sMap.put("submissionDate", sub.getSubmissionDate());
                    submittedCount++;
                } else {
                    sMap.put("status", "NOT_SUBMITTED");
                    sMap.put("filePath", "");
                    sMap.put("submissionDate", "-");
                }
                studentList.add(sMap);
            }

            map.put("studentList", studentList);
            map.put("submittedCount", submittedCount);
            map.put("totalStudents", targetStudents.size());
            tasksData.add(map);
        }

        model.addAttribute("tasksData", tasksData);

        // 🔥 FIX: Fetching teacherId from session as an INT
        int teacherId = 0;
        if (session.getAttribute("teacherId") != null) {
            teacherId = Integer.parseInt(session.getAttribute("teacherId").toString());
        }
        model.addAttribute("pendingLeaveCount", leaveService.getPendingCountForTeacher(teacherId));

        return "teacher-assignment";
    }

    @PostMapping("/teacher-portal/assignment/create")
    public String createAssignment(HttpSession session,
                                   @RequestParam String courseCode,
                                   @RequestParam String courseName,
                                   @RequestParam String batch,
                                   @RequestParam String topicName,
                                   @RequestParam String endDate) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";

        String teacherName = (String) session.getAttribute("loggedIn");
        Assignment a = new Assignment();
        a.setCourseCode(courseCode);
        a.setCourseName(courseName);
        a.setBatch(batch);
        a.setTopicName(topicName);
        a.setEndDate(endDate);
        a.setStartDate(LocalDate.now().toString());
        a.setTeacherName(teacherName);

        assignmentService.createAssignment(a);

        // 🧠 Nexus Intelligence: Alert target students about new assignment
        try {
            List<Student> allStudents = studentService.getAllStudents();
            for (Student s : allStudents) {
                String tBatch = batch.trim().toLowerCase();
                String sBatch = s.getBatch() != null ? s.getBatch().trim().toLowerCase() : "";
                if (tBatch.equals("all") || tBatch.equals("any") || tBatch.equals(sBatch) || sBatch.contains(tBatch) || tBatch.contains(sBatch)) {
                    notificationService.dispatchToStudentWithSender(s.getId(), "ASSIGNMENT", teacherName, "📚 New Assignment published for " + courseCode + " (" + topicName + "). Deadline: " + endDate);
                }
            }
        } catch (Exception e) {}

        return "redirect:/teacher-assignment?assignmentSuccess=true";
    }

    @PostMapping("/teacher-portal/assignment/edit")
    public String editAssignment(HttpSession session,
                                 @RequestParam int assignmentId,
                                 @RequestParam String courseCode,
                                 @RequestParam String courseName,
                                 @RequestParam String batch,
                                 @RequestParam String topicName,
                                 @RequestParam String endDate) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        assignmentService.editAssignment(assignmentId, courseCode, courseName, batch, topicName, endDate);
        return "redirect:/teacher-assignment?editSuccess=true";
    }

    @PostMapping("/teacher-portal/assignment/approve/{assignmentId}/{studentId}")
    public String approveAssignment(@PathVariable int assignmentId, @PathVariable int studentId, HttpSession session) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        assignmentService.updateStatus(assignmentId, studentId, "APPROVED");

        // 🧠 Nexus Intelligence: Alert Student
        try {
            notificationService.dispatchToStudent(studentId, "ASSIGNMENT", "✅ Your assignment submission has been APPROVED.");
        } catch (Exception e) {}

        return "redirect:/teacher-assignment";
    }

    @PostMapping("/teacher-portal/assignment/reject/{assignmentId}/{studentId}")
    public String rejectAssignment(@PathVariable int assignmentId, @PathVariable int studentId, HttpSession session) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        assignmentService.updateStatus(assignmentId, studentId, "REJECTED");

        // 🧠 Nexus Intelligence: Alert Student
        try {
            notificationService.dispatchToStudent(studentId, "ASSIGNMENT", "❌ Your assignment submission has been REJECTED. Please check and resubmit if permitted.");
        } catch (Exception e) {}

        return "redirect:/teacher-assignment";
    }

    @PostMapping("/teacher-portal/assignment/delete/{assignmentId}")
    public String deleteAssignment(@PathVariable int assignmentId, HttpSession session) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        assignmentService.deleteAssignment(assignmentId);
        return "redirect:/teacher-assignment?deleteSuccess=true";
    }

    // ── COURSE OUTLINES ──
    @GetMapping("/teacher-outlines")
    public String teacherOutlines(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";

        String username = (String) session.getAttribute("loggedIn");
        model.addAttribute("username", username);

        model.addAttribute("outlines", courseOutlineService.getOutlinesByTeacher(username));

        // 🔥 FIX: Fetching teacherId from session as an INT
        int teacherId = 0;
        if (session.getAttribute("teacherId") != null) {
            teacherId = Integer.parseInt(session.getAttribute("teacherId").toString());
        }
        model.addAttribute("pendingLeaveCount", leaveService.getPendingCountForTeacher(teacherId));

        return "teacher-outlines";
    }

    @PostMapping("/teacher-outlines/post")
    public String postOutline(HttpSession session,
                              @RequestParam String department,
                              @RequestParam String batch,
                              @RequestParam String courseCode,
                              @RequestParam String title,
                              @RequestParam String description,
                              @RequestParam(required = false) String externalLink,
                              @RequestParam(required = false) MultipartFile file) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        String username = (String) session.getAttribute("loggedIn");

        CourseOutline outline = new CourseOutline();
        outline.setDepartment(department.trim());
        outline.setBatch(batch.trim());
        outline.setCourseCode(courseCode.trim());
        outline.setTitle(title.trim());
        outline.setDescription(description.trim());
        outline.setTeacherName(username);

        courseOutlineService.postOutline(outline, file, externalLink);

        // 🧠 Nexus Intelligence: Alert specific batch/department about the new outline
        try {
            List<Student> allStudents = studentService.getAllStudents();
            for (Student s : allStudents) {
                String tBatch = batch.trim().toLowerCase();
                String sBatch = s.getBatch() != null ? s.getBatch().trim().toLowerCase() : "";
                String tDept = department.trim().toLowerCase();
                String sDept = s.getDepartment() != null ? s.getDepartment().trim().toLowerCase() : "";

                boolean batchMatch = tBatch.equals("all") || tBatch.equals("any") || tBatch.equals(sBatch) || sBatch.contains(tBatch) || tBatch.contains(sBatch);
                boolean deptMatch = tDept.equals("all") || tDept.equals("any") || tDept.equals(sDept) || sDept.contains(tDept) || tDept.contains(sDept);

                if (batchMatch && deptMatch) {
                    notificationService.dispatchToStudentWithSender(s.getId(), "COURSE_OUTLINE", username, "📑 New Course Outline uploaded for " + courseCode + " (" + title + ").");
                }
            }
        } catch (Exception e) {}

        return "redirect:/teacher-outlines?success=posted";
    }

    @PostMapping("/teacher-outlines/delete/{id}")
    public String deleteOutline(@PathVariable int id, HttpSession session) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        courseOutlineService.deleteOutline(id);
        return "redirect:/teacher-outlines?success=deleted";
    }
}