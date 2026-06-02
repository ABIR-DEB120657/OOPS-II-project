package com.codeandcoffee.attendance_simple.Controller;

import com.codeandcoffee.attendance_simple.model.*;
import com.codeandcoffee.attendance_simple.service.*;
import com.codeandcoffee.attendance_simple.util.SessionUtil;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
public class StudentController {

    @Autowired private StudentService studentService;
    @Autowired private AttendanceService attendanceService;
    @Autowired private GradeService gradeService;
    @Autowired private NotificationService notificationService;
    @Autowired private AssignmentService assignmentService;
    @Autowired private BloodService bloodService;
    @Autowired private TeacherService teacherService;
    @Autowired private AppointmentService appointmentService;
    @Autowired private JobService jobService;
    @Autowired private CourseOutlineService courseOutlineService;
    @Autowired private JavaMailSender mailSender;

    private final ExecutorService emailExecutor = Executors.newCachedThreadPool();

    @PostConstruct
    public void initProfilesDirectory() {
        try {
            File classPathStatic = new org.springframework.core.io.ClassPathResource("static/").getFile();
            File targetProfilesDir = new File(classPathStatic.getAbsolutePath() + File.separator + "images" + File.separator + "profiles");
            if (!targetProfilesDir.exists()) {
                targetProfilesDir.mkdirs();
                System.out.println("✅ Profiles directory created in target: " + targetProfilesDir.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("⚠️ PostConstruct profiles dir warning: " + e.getMessage());
        }
    }

    private String getEmailByName(String name) {
        if(name == null) return null;
        if(name.equalsIgnoreCase("Admin")) return "debabir605@gmail.com";
        for(Student s : studentService.getAllStudents()) {
            if(s.getName().equalsIgnoreCase(name.trim())) return s.getEmail();
        }
        for(Teacher t : teacherService.getAllTeachers()) {
            if(t.getName().equalsIgnoreCase(name.trim())) return t.getEmail();
        }
        return "debabir605@gmail.com";
    }

    private String getTeacherEmailByName(String name) {
        if(name == null || name.trim().isEmpty()) return "debabir605@gmail.com";
        if(name.equalsIgnoreCase("Admin")) return "debabir605@gmail.com";
        for(Teacher t : teacherService.getAllTeachers()) {
            if(t.getName().trim().equalsIgnoreCase(name.trim())) return t.getEmail();
        }
        return "debabir605@gmail.com";
    }

    private String convertNumberToWords(long number) {
        if (number == 0) return "Zero";
        String[] units = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};
        if (number < 20) return units[(int)number];
        if (number < 100) return tens[(int)number / 10] + ((number % 10 != 0) ? " " : "") + units[(int)number % 10];
        if (number < 1000) return units[(int)number / 100] + " Hundred" + ((number % 100 != 0) ? " and " + convertNumberToWords(number % 100) : "");
        if (number < 100000) return convertNumberToWords(number / 1000) + " Thousand" + ((number % 1000 != 0) ? " " + convertNumberToWords(number % 1000) : "");
        if (number < 10000000) return convertNumberToWords(number / 100000) + " Lakh" + ((number % 100000 != 0) ? " " + convertNumberToWords(number % 100000) : "");
        return "Amount too large";
    }

    @GetMapping("/student-portal")
    public String studentPortal(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        if (s != null) {
            session.setAttribute("studentId", s.getId());
            List<Grade> grades = gradeService.getGradesByStudentRowId(s.getId());
            Map<String, List<Grade>> gradesBySemester = new LinkedHashMap<>();
            for (Grade g : grades) { gradesBySemester.computeIfAbsent(g.getSemester(), k -> new ArrayList<>()).add(g); }

            // 🔥 NEW: ড্যাশবোর্ডের জন্য প্রোফাইল পিকচার খোঁজার লজিক
            String studentIdStr = s.getStudentId() != null ? s.getStudentId() : String.valueOf(s.getId());
            String profileImagePath = resolveProfileImagePath(studentIdStr);
            boolean baseHasPic = (profileImagePath != null);
            s.setHasProfilePic(baseHasPic);
            model.addAttribute("profileImagePath", profileImagePath);
            model.addAttribute("hasProfilePic", baseHasPic);
            // 🔥 END NEW LOGIC

            List<String> aiInsights = new ArrayList<>();
            List<String> insightTypes = new ArrayList<>();

            try {
                double attPct = Double.parseDouble(String.valueOf(attendanceService.getAttendancePercentage(s.getStudentId())));
                if (attPct < 75.0 && attPct > 0) {
                    aiInsights.add("📉 Your attendance is low (" + attPct + "%). You need to attend more classes to avoid penalties.");
                    insightTypes.add("warning");
                } else if (attPct >= 90.0) {
                    aiInsights.add("🌟 Excellent! Your attendance is " + attPct + "%. Keep up the great consistency!");
                    insightTypes.add("success");
                }
            } catch (Exception e) {}

            try {
                List<Assignment> assignments = assignmentService.getActiveAssignmentsForStudent(s.getId(), s.getBatch());
                long pendingCount = 0;
                long urgentCount = 0;
                LocalDate today = LocalDate.now();
                LocalDate tomorrow = today.plusDays(1);

                for (Assignment a : assignments) {
                    if ("NOT_SUBMITTED".equals(a.getStatus()) || "PENDING".equals(a.getStatus())) {
                        pendingCount++;
                        try {
                            LocalDate deadline = LocalDate.parse(a.getEndDate());
                            if (deadline.isEqual(today) || deadline.isEqual(tomorrow)) {
                                urgentCount++;
                            }
                        } catch (Exception dateEx) {}
                    }
                }

                if (urgentCount > 0) {
                    aiInsights.add("⚠️ Urgent: You have " + urgentCount + " assignment(s) due within 24 hours!");
                    insightTypes.add("danger");
                } else if (pendingCount > 0) {
                    aiInsights.add("📝 Reminder: You have " + pendingCount + " pending assignment(s) left to complete.");
                    insightTypes.add("info");
                }
            } catch (Exception e) {}

            if (aiInsights.isEmpty()) {
                aiInsights.add("✨ You're all caught up! No pending tasks or warnings.");
                insightTypes.add("success");
            }

            model.addAttribute("aiInsights", aiInsights);
            model.addAttribute("insightTypes", insightTypes);

            List<String> latestNotices = new ArrayList<>();
            try {
                String path = "src/main/resources/data/notices";
                try {
                    File resourceFile = new org.springframework.core.io.ClassPathResource("data/").getFile();
                    String absolutePath = resourceFile.getAbsolutePath();
                    path = absolutePath.replace("target\\classes", "src\\main\\resources")
                            .replace("target/classes", "src/main/resources") + File.separator + "notices";
                } catch (Exception ignored) {}

                File noticesDir = new File(path);
                if (!noticesDir.exists()) {
                    noticesDir.mkdirs();
                }

                File noticeFile = new File(noticesDir, "notice_board.txt");
                if (noticeFile.exists()) {
                    try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(noticeFile))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (!line.trim().isEmpty()) {
                                latestNotices.add(line.trim());
                            }
                        }
                    }
                } else {
                    try (PrintWriter writer = new PrintWriter(new FileWriter(noticeFile))) {
                        writer.println("সকল শিক্ষার্থীদের অবগতির জন্য জানানো যাচ্ছে যে, আগামী 16-May-2026 থেকে Admit Card Download করা যাবে।");
                        writer.println("সদয় অবগতির জন্য জানানো যাচ্ছে যে, Money Receipt Download করে কোন কোন মাসের টাকা প্রদান করা হচ্ছে তা নিশ্চিত হয়ে ব্যাংকে জমা দেওয়ার জন্য অনুরোধ করা হলো।");
                        writer.println("যে সকল ছাত্রছাত্রীদের May মাসের Final Exam Form Fee Money Receipt এর সাথে সংযুক্ত হয়নি, তাদেরকে দয়া করে IQAC-এ যোগাযোগ করার জন্য অনুরোধ করা হলো।");
                    }
                    latestNotices.add("সকল শিক্ষার্থীদের অবগতির জন্য জানানো যাচ্ছে যে, আগামী 16-May-2026 থেকে Admit Card Download করা যাবে।");
                    latestNotices.add("সদয় অবগতির জন্য জানানো যাচ্ছে যে, Money Receipt Download করে কোন কোন মাসের টাকা প্রদান করা হচ্ছে তা নিশ্চিত হয়ে ব্যাংকে জমা দেওয়ার জন্য অনুরোধ করা হলো।");
                    latestNotices.add("যে সকল ছাত্রছাত্রীদের May মাসের Final Exam Form Fee Money Receipt এর সাথে সংযুক্ত হয়নি, তাদেরকে দয়া করে IQAC-এ যোগাযোগ করার জন্য অনুরোধ করা হলো।");
                }
            } catch (Exception e) {
                System.out.println("⚠️ Notice File Read Error: " + e.getMessage());
            }
            model.addAttribute("latestNotices", latestNotices);

            model.addAttribute("student", s);
            model.addAttribute("attendance", attendanceService.getAttendancePercentage(s.getStudentId()));
            model.addAttribute("cgpa", gradeService.calculateCGPA(grades));
            model.addAttribute("gradesBySemester", gradesBySemester);
            model.addAttribute("unreadNotifCount", notificationService.getUnreadCountForStudent(s.getId()));
            model.addAttribute("initials", getInitials(s.getName()));
        }
        return "student-portal";
    }

    private Map<String, String> readStudentProfileFromTxt(String studentId) {
        Map<String, String> profileData = new LinkedHashMap<>();
        try {
            String path = "src/main/resources/data/profiles";
            try {
                File resourceFile = new org.springframework.core.io.ClassPathResource("data/").getFile();
                String absolutePath = resourceFile.getAbsolutePath();
                path = absolutePath.replace("target\\classes", "src\\main\\resources")
                        .replace("target/classes", "src/main/resources") + File.separator + "profiles";
            } catch (Exception ignored) {}

            File file = new File(path, "Profile_" + studentId + ".txt");
            if (!file.exists()) return profileData;

            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        String rawKey = parts[0].trim().toLowerCase().replaceAll("\\s+", "");
                        String val = parts[1].trim();

                        if (rawKey.contains("name")) {
                            if (rawKey.contains("father")) profileData.put("fatherName", val);
                            else if (rawKey.contains("mother")) profileData.put("motherName", val);
                            else profileData.put("name", val);
                        } else if (rawKey.contains("dob")) profileData.put("dob", val);
                        else if (rawKey.contains("bloodgroup")) profileData.put("bloodGroup", val);
                        else if (rawKey.contains("gender")) profileData.put("gender", val);
                        else if (rawKey.contains("religion")) profileData.put("religion", val);
                        else if (rawKey.equals("mobile")) profileData.put("mobile", val);
                        else if (rawKey.equals("email")) profileData.put("email", val);
                        else if (rawKey.contains("presentaddr")) profileData.put("presentAddress", val);
                        else if (rawKey.contains("perm.addr") || rawKey.contains("permanent")) profileData.put("permanentAddress", val);
                        else if (rawKey.contains("fathermob")) profileData.put("fatherMobile", val);
                        else if (rawKey.contains("mothermob")) profileData.put("motherMobile", val);
                        else if (rawKey.contains("sscboard")) profileData.put("sscBoard", val);
                        else if (rawKey.contains("sscyear")) profileData.put("sscYear", val);
                        else if (rawKey.contains("sscgpa")) profileData.put("sscGpa", val);
                        else if (rawKey.contains("sscschool")) profileData.put("sscInst", val);
                        else if (rawKey.contains("hscboard")) profileData.put("hscBoard", val);
                        else if (rawKey.contains("hscyear")) profileData.put("hscYear", val);
                        else if (rawKey.contains("hscgpa")) profileData.put("hscGpa", val);
                        else if (rawKey.contains("hsccollege")) profileData.put("hscInst", val);
                        else if (rawKey.contains("admissiondt")) profileData.put("admissionDate", val);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Profile read error: " + e.getMessage());
        }
        return profileData;
    }

    private String resolveProfileImagePath(String studentId) {
        String[] extensions = {"jpg", "jpeg", "png", "webp"};
        for (String ext : extensions) {
            String relativePath = "/images/profiles/" + studentId + "." + ext;
            try {
                File resourceFile = new org.springframework.core.io.ClassPathResource("static/").getFile();
                File imgFile = new File(resourceFile.getAbsolutePath() + File.separator + "images" + File.separator + "profiles" + File.separator + studentId + "." + ext);
                if (imgFile.exists()) return relativePath;
            } catch (Exception ignored) {
                File staticFile = new File("src/main/resources/static/images/profiles/" + studentId + "." + ext);
                if (staticFile.exists()) return relativePath;
            }
        }
        return null;
    }

    @GetMapping("/student-profile")
    public String studentProfile(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        if (s != null) {
            String studentId = s.getStudentId() != null ? s.getStudentId() : String.valueOf(s.getId());

            Map<String, String> profileData = readStudentProfileFromTxt(studentId);
            model.addAttribute("profileData", profileData);

            String profileImagePath = resolveProfileImagePath(studentId);
            model.addAttribute("profileImagePath", profileImagePath);

            boolean baseHasPic = (profileImagePath != null);
            s.setHasProfilePic(baseHasPic);
            model.addAttribute("hasProfilePic", baseHasPic);

            model.addAttribute("student", s);
            model.addAttribute("initials", getInitials(s.getName()));
            model.addAttribute("unreadNotifCount", notificationService.getUnreadCountForStudent(s.getId()));
        }
        return "student-profile";
    }

    @PostMapping("/student-profile/save")
    public String saveStudentProfile(HttpSession session, @RequestParam Map<String, String> allParams,
                                     @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";

        Student s = findStudent(session.getAttribute("loggedIn").toString());
        if (s != null) {
            String studentId = s.getStudentId() != null ? s.getStudentId() : String.valueOf(s.getId());

            saveStudentProfileToTxt(allParams, studentId);

            if (profileImage != null && !profileImage.isEmpty()) {
                try {
                    byte[] bytes = profileImage.getBytes();

                    File srcDir = new File("src/main/resources/static/images/profiles/");
                    if (!srcDir.exists()) srcDir.mkdirs();
                    File srcFile = new File(srcDir, studentId + ".jpg");
                    Files.write(srcFile.toPath(), bytes);
                    System.out.println("✅ Profile image saved to src: " + srcFile.getAbsolutePath());

                    try {
                        File classPathStatic = new org.springframework.core.io.ClassPathResource("static/").getFile();
                        File targetDir = new File(classPathStatic.getAbsolutePath() + File.separator + "images" + File.separator + "profiles");
                        if (!targetDir.exists()) {
                            targetDir.mkdirs();
                        }
                        File targetFile = new File(targetDir, studentId + ".jpg");
                        Files.write(targetFile.toPath(), bytes);
                        System.out.println("✅ Profile image saved to target: " + targetFile.getAbsolutePath());
                    } catch (Exception ex) {
                        System.out.println("⚠️ Target write warning: " + ex.getMessage());
                    }

                    s.setHasProfilePic(true);
                } catch (Exception e) {
                    System.out.println("⚠️ Image upload error: " + e.getMessage());
                }
            }
            notificationService.dispatchToStudent(s.getId(), "NOTICE", "✅ Your profile information has been successfully updated and synced.");
        }
        return "redirect:/student-profile?success=true";
    }

    private void saveStudentProfileToTxt(Map<String, String> data, String studentId) {
        try {
            String path = "src/main/resources/data/profiles";
            try {
                File resourceFile = new org.springframework.core.io.ClassPathResource("data/").getFile();
                String absolutePath = resourceFile.getAbsolutePath();
                path = absolutePath.replace("target\\classes", "src\\main\\resources")
                        .replace("target/classes", "src/main/resources") + File.separator + "profiles";
            } catch (Exception ignored) {}

            File dir = new File(path);
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "Profile_" + studentId + ".txt");

            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("=======================================================");
                writer.println("       NEXUS ONE - COMPLETE STUDENT PROFILE RECORD     ");
                writer.println("=======================================================");
                writer.println("👤 PERSONAL INFORMATION");
                writer.println("Name         : " + data.getOrDefault("name", ""));
                writer.println("Student ID   : " + studentId);
                writer.println("DOB          : " + data.getOrDefault("dob", ""));
                writer.println("Blood Group  : " + data.getOrDefault("bloodGroup", ""));
                writer.println("Gender       : " + data.getOrDefault("gender", ""));
                writer.println("Religion     : " + data.getOrDefault("religion", ""));
                writer.println("-------------------------------------------------------");
                writer.println("📱 CONTACT INFORMATION");
                writer.println("Mobile       : " + data.getOrDefault("mobile", ""));
                writer.println("Email        : " + data.getOrDefault("email", ""));
                writer.println("Present Addr : " + data.getOrDefault("presentAddress", ""));
                writer.println("Perm. Addr   : " + data.getOrDefault("permanentAddress", ""));
                writer.println("-------------------------------------------------------");
                writer.println("👨‍👩‍👦 FAMILY INFORMATION");
                writer.println("Father's Name: " + data.getOrDefault("fatherName", ""));
                writer.println("Father's Mob : " + data.getOrDefault("fatherMobile", ""));
                writer.println("Mother's Name: " + data.getOrDefault("motherName", ""));
                writer.println("Mother's Mob : " + data.getOrDefault("motherMobile", ""));
                writer.println("-------------------------------------------------------");
                writer.println("🎓 ACADEMIC BACKGROUND");
                writer.println("SSC Board    : " + data.getOrDefault("sscBoard", ""));
                writer.println("SSC Year     : " + data.getOrDefault("sscYear", ""));
                writer.println("SSC GPA      : " + data.getOrDefault("sscGpa", ""));
                writer.println("SSC School   : " + data.getOrDefault("sscInst", ""));
                writer.println("HSC Board    : " + data.getOrDefault("hscBoard", ""));
                writer.println("HSC Year     : " + data.getOrDefault("hscYear", ""));
                writer.println("HSC GPA      : " + data.getOrDefault("hscGpa", ""));
                writer.println("HSC College  : " + data.getOrDefault("hscInst", ""));
                writer.println("-------------------------------------------------------");
                writer.println("🏛️ ADMISSION INFORMATION");
                writer.println("Program      : Bachelor of Computer Science & Engineering");
                writer.println("Admission Dt : " + data.getOrDefault("admissionDate", ""));
                writer.println("=======================================================");
                writer.println("Generated on : " + LocalDate.now().toString());
            }
        } catch (Exception e) {
            System.out.println("⚠️ Failed to write profile txt: " + e.getMessage());
        }
    }

    @GetMapping("/student-results")
    public String studentResultsPage(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        if (s != null) {
            String studentId = s.getStudentId() != null ? s.getStudentId() : String.valueOf(s.getId());

            List<Map<String, Object>> semesterResults = new ArrayList<>();
            double totalEarnedCredits = 0;
            double totalEnrolledCreditsForCgpa = 0;
            double totalGradePointsForCgpa = 0;

            try {
                String path = "src/main/resources/data/results";
                try {
                    File resourceFile = new org.springframework.core.io.ClassPathResource("data/").getFile();
                    String absolutePath = resourceFile.getAbsolutePath();
                    path = absolutePath.replace("target\\classes", "src\\main\\resources")
                            .replace("target/classes", "src/main/resources") + File.separator + "results";
                } catch (Exception ignored) {}

                File resultFile = new File(path, "Result_" + studentId + ".txt");

                if (resultFile.exists()) {
                    Scanner scanner = new Scanner(resultFile);
                    Map<String, List<Map<String, Object>>> semDataMap = new LinkedHashMap<>();
                    int lineCount = 0;

                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine().trim();
                        lineCount++;

                        if (lineCount <= 10) {
                            continue;
                        }

                        if (!line.isEmpty() && !line.startsWith("==") && line.contains("|")) {
                            String[] parts = line.split("\\|");
                            if (parts.length >= 7) {
                                String semName = parts[0].trim();
                                Map<String, Object> course = new HashMap<>();
                                course.put("code", parts[1].trim());
                                course.put("title", parts[2].trim());
                                course.put("type", parts[3].trim());
                                course.put("credits", Double.parseDouble(parts[4].trim()));
                                course.put("grade", parts[5].trim());
                                course.put("gradePoint", Double.parseDouble(parts[6].trim()));
                                semDataMap.computeIfAbsent(semName, k -> new ArrayList<>()).add(course);
                            }
                        }
                    }
                    scanner.close();

                    for (Map.Entry<String, List<Map<String, Object>>> entry : semDataMap.entrySet()) {
                        Map<String, Object> semResult = new HashMap<>();
                        semResult.put("semesterName", entry.getKey());
                        List<Map<String, Object>> courses = entry.getValue();
                        semResult.put("courses", courses);

                        double semEnrolledCr = 0;
                        double semEarnedCr = 0;
                        double semTotalGp = 0;

                        for (Map<String, Object> c : courses) {
                            double cr = (double) c.get("credits");
                            double gp = (double) c.get("gradePoint");
                            String grade = (String) c.get("grade");

                            semEnrolledCr += cr;
                            if (!"F".equalsIgnoreCase(grade) && gp > 0) {
                                semEarnedCr += cr;
                            }
                            semTotalGp += (cr * gp);
                        }

                        double tgpa = semEnrolledCr > 0 ? (semTotalGp / semEnrolledCr) : 0.0;

                        semResult.put("enrolledCredits", String.format("%.1f", semEnrolledCr));
                        semResult.put("earnedCredits", String.format("%.1f", semEarnedCr));
                        semResult.put("tgpa", String.format("%.2f", tgpa));

                        semesterResults.add(semResult);

                        totalEarnedCredits += semEarnedCr;
                        totalEnrolledCreditsForCgpa += semEnrolledCr;
                        totalGradePointsForCgpa += semTotalGp;
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Result file read error: " + e.getMessage());
            }

            if (!semesterResults.isEmpty()) {
                double cgpa = totalEnrolledCreditsForCgpa > 0 ? (totalGradePointsForCgpa / totalEnrolledCreditsForCgpa) : 0.0;
                model.addAttribute("semesterResults", semesterResults);
                model.addAttribute("totalCredits", String.format("%.1f", totalEarnedCredits));
                model.addAttribute("cgpa", String.format("%.2f", cgpa));
            } else {
                model.addAttribute("semesterResults", null);
            }

            model.addAttribute("student", s);
            model.addAttribute("unreadNotifCount", notificationService.getUnreadCountForStudent(s.getId()));
            model.addAttribute("initials", getInitials(s.getName()));
        }
        return "student-results";
    }

    @GetMapping("/student-outlines")
    public String studentOutlines(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        if (s != null) {
            String sDept = s.getDepartment() != null ? s.getDepartment() : "All";
            String sBatch = s.getBatch() != null ? s.getBatch() : "All";

            List<CourseOutline> outlines = courseOutlineService.getOutlinesForStudent(sDept, sBatch);
            Collections.reverse(outlines);

            model.addAttribute("outlines", outlines);
            model.addAttribute("student", s);
            model.addAttribute("unreadNotifCount", notificationService.getUnreadCountForStudent(s.getId()));
            model.addAttribute("initials", getInitials(s.getName()));
        }
        return "student-outlines";
    }

    @GetMapping("/student-blood-finder")
    public String bloodFinderPage(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        model.addAttribute("generalDonors", bloodService.getAllGeneralDonors());
        model.addAttribute("emergencyDonors", bloodService.getTodayEmergencyDonors());
        model.addAttribute("broadcasts", bloodService.getTodayBroadcasts());
        model.addAttribute("student", s);
        model.addAttribute("unreadNotifCount", notificationService.getUnreadCountForStudent(s.getId()));
        model.addAttribute("initials", getInitials(s.getName()));
        return "student-blood-finder";
    }

    @PostMapping("/student-blood-finder/broadcast")
    public String broadcastEmergency(@RequestParam String bloodGroup, @RequestParam String bags, @RequestParam String time, @RequestParam String hospital, @RequestParam String contact, HttpSession session) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        String senderName = (s != null) ? s.getName() : "Unknown";

        bloodService.saveBroadcast(bloodGroup, bags, time, hospital, contact, senderName);

        if (s != null) {
            String sId = s.getStudentId() != null ? s.getStudentId() : "N/A";
            String sDept = s.getDepartment() != null ? s.getDepartment() : "N/A";
            String sBatch = s.getBatch() != null ? s.getBatch() : "N/A";

            String htmlBody = buildPremiumBloodEmail("URGENT BLOOD REQUIRED", senderName, sId, sDept, sBatch, bloodGroup, bags, time, hospital, contact);
            sendBloodBroadcastEmail("🚨 URGENT: " + bloodGroup + " Blood Needed!", htmlBody, senderName, hospital);

            notificationService.dispatchToStudent(s.getId(), "BLOOD", "🚨 Your emergency blood broadcast for " + bloodGroup + " has been successfully transmitted.");
        }
        return "redirect:/student-blood-finder?success=broadcast";
    }

    private String buildPremiumBloodEmail(String title, String sName, String sId, String sDept, String sBatch, String bloodGroup, String bags, String time, String hospital, String contact) {
        return "<html><body style=\"background-color:#f1f5f9; padding:20px; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">" +
                "<div style=\"max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 15px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.05);\">" +
                "<div style=\"background: linear-gradient(135deg, #b91c1c, #ef4444); padding: 30px; text-align: center;\">" +
                "<h2 style=\"color: #ffffff; margin: 0; font-size: 24px; letter-spacing: 2px;\">KYAU <span style=\"font-weight:300; color:#fca5a5;\">BLOOD BANK</span></h2>" +
                "</div>" +
                "<div style=\"padding: 40px; color: #334155;\">" +
                "<h3 style=\"color: #ef4444; border-bottom: 2px solid #f1f5f9; padding-bottom: 15px; margin-top: 0; text-align: center; font-size: 18px; letter-spacing: 1px;\">" + title + "</h3>" +
                "<p style=\"font-size: 16px;\">Dear Students & Faculty,</p>" +
                "<p style=\"font-size: 15px; color: #475569; line-height: 1.6;\">An urgent blood request has been broadcasted. Every drop saves a life. Please review the details below:</p>" +
                "<table style=\"width: 100%; border-collapse: collapse; margin-top: 30px; border: 1px solid #e2e8f0; border-radius: 10px; overflow: hidden;\">" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; width: 35%; font-weight: 700; color: #64748b; background: #f8fafc;\">Requested By</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #0f172a;\">" + sName + "</td></tr>" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #64748b; background: #f8fafc;\">Student ID</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; color: #0f172a;\">" + sId + "</td></tr>" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #64748b; background: #f8fafc;\">Program</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; color: #0f172a;\">" + sDept + ", Batch " + sBatch + "</td></tr>" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #64748b; background: #f8fafc;\">Blood Group</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; color: #ef4444; font-weight: 800; font-size: 18px;\">" + bloodGroup + " <span style=\"font-size: 13px; color: #64748b; font-weight: normal;\">(" + bags + " Bags)</span></td></tr>" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #64748b; background: #f8fafc;\">Time & Hospital</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; color: #0f172a; line-height: 1.5;\"><b>Time:</b> " + time + "<br><b>Location:</b> " + hospital + "</td></tr>" +
                "<tr><td style=\"padding: 15px; font-weight: 700; color: #64748b; background: #f8fafc;\">Contact</td><td style=\"padding: 15px; color: #0f172a; font-weight: 800;\">" + contact + "</td></tr>" +
                "</table>" +
                "<div style=\"text-align: center; margin-top: 40px;\">" +
                "<a href=\"http://localhost:8080/student-blood-finder\" style=\"display: inline-block; padding: 15px 35px; background: linear-gradient(135deg, #ef4444, #b91c1c); color: #ffffff; text-decoration: none; border-radius: 50px; font-weight: 800; font-size: 16px; box-shadow: 0 10px 20px rgba(239, 68, 68, 0.3);\">Login to Portal</a>" +
                "</div>" +
                "</div>" +
                "<div style=\"background: #f8fafc; padding: 25px; text-align: center; font-size: 13px; color: #94a3b8; border-top: 1px solid #e2e8f0;\">" +
                "&copy; 2026 Nexus One University Portal.<br>This is an automated emergency alert." +
                "</div>" +
                "</div></body></html>";
    }

    private void sendBloodBroadcastEmail(String subject, String htmlBody, String senderName, String hospital) {
        String date = LocalDate.now().toString();
        List<Student> allStudents = studentService.getAllStudents();
        String webMsg = "Urgent blood needed at " + hospital;

        allStudents.forEach(st -> {
            notificationService.addNotificationFor("BLOOD", senderName, webMsg, date, st.getId(), "STUDENT");
        });

        emailExecutor.execute(() -> {
            for (Student st : allStudents) {
                if (st.getEmail() != null && st.getEmail().contains("@")) {
                    try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                        helper.setFrom("debabir605@gmail.com");
                        helper.setTo(st.getEmail().trim());
                        helper.setSubject(subject);
                        helper.setText(htmlBody, true);
                        mailSender.send(message);
                    } catch (Exception e) {}
                }
            }
        });
    }

    @GetMapping("/student-appointment")
    public String appointmentPage(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        if (s != null) {
            model.addAttribute("student", s);
            model.addAttribute("appointments", appointmentService.getAppointmentsForStudent(s.getName()));
            model.addAttribute("teachers", teacherService.getAllTeachers());
            model.addAttribute("unreadNotifCount", notificationService.getUnreadCountForStudent(s.getId()));
            model.addAttribute("initials", getInitials(s.getName()));
        }
        return "student-appointment";
    }

    @PostMapping("/student-appointment/request")
    public String requestAppointment(@RequestParam String teacherName, @RequestParam String teacherEmail, @RequestParam String topic, @RequestParam String duration, @RequestParam(required = false) String department, HttpSession session) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        if (s != null) {
            appointmentService.saveAppointment(s.getName(), teacherName, topic, duration);
            notificationService.addNotification("APPOINTMENT", s.getName(), "Appointment requested with " + teacherName + " regarding " + topic, LocalDate.now().toString());

            try {
                Teacher targetTeacher = teacherService.getAllTeachers().stream().filter(t -> t.getName().equalsIgnoreCase(teacherName)).findFirst().orElse(null);
                if (targetTeacher != null) {
                    notificationService.dispatchToTeacher(targetTeacher.getTeacherId(), "APPOINTMENT", s.getName(), "📅 New faculty sync request regarding: " + topic);
                }
                notificationService.dispatchToStudent(s.getId(), "APPOINTMENT", "✅ Your sync request to " + teacherName + " has been successfully dispatched.");
            } catch (Exception ex) { System.out.println("Notification Engine Silent Error: " + ex.getMessage()); }

            if (teacherEmail != null && !teacherEmail.isEmpty()) {
                String sName = s.getName();
                String sId = s.getStudentId() != null ? s.getStudentId() : "N/A";
                String sBatch = s.getBatch() != null ? s.getBatch() : "N/A";
                String sDept = department != null ? department : (s.getDepartment() != null ? s.getDepartment() : "N/A");

                String htmlBody = buildPremiumAppointmentEmail("NEW FACULTY SYNC REQUEST", teacherName, sName, sId, sDept, sBatch, topic, duration, "#6366f1");

                emailExecutor.execute(() -> {
                    try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                        helper.setFrom("debabir605@gmail.com");
                        helper.setTo(teacherEmail.trim());
                        helper.setSubject("New Appointment Request - Nexus One");
                        helper.setText(htmlBody, true);
                        mailSender.send(message);
                    } catch (Exception e) { e.printStackTrace(); }
                });
            }
        }
        return "redirect:/student-appointment?success=true";
    }

    @PostMapping("/student-appointment/delete/{id}")
    public String deleteAppointment(@PathVariable String id, HttpSession session) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        if (s != null) {
            appointmentService.deleteAppointment(id, s.getName());
            notificationService.dispatchToStudent(s.getId(), "APPOINTMENT", "⚠️ You have revoked a faculty sync request.");
        }
        return "redirect:/student-appointment?deleted=true";
    }

    private String buildPremiumAppointmentEmail(String actionTitle, String teacherName, String sName, String sId, String sDept, String sBatch, String topic, String duration, String statusColor) {
        return "<html><body style=\"background-color:#f1f5f9; padding:20px; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">" +
                "<div style=\"max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 15px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.05);\">" +
                "<div style=\"background: linear-gradient(135deg, #0f172a, #1e293b); padding: 30px; text-align: center;\">" +
                "<h2 style=\"color: #ffffff; margin: 0; font-size: 24px; letter-spacing: 2px;\">NEXUS ONE <span style=\"font-weight:300; color:#94a3b8;\">PORTAL</span></h2>" +
                "</div>" +
                "<div style=\"padding: 40px; color: #334155;\">" +
                "<p style=\"font-size: 16px;\">Hello " + teacherName + ",</p>" +
                "<p style=\"font-size: 15px; color: #475569; line-height: 1.6;\">You have received a new appointment request from your student.</p>" +
                "<table style=\"width: 100%; border-collapse: collapse; margin-top: 25px; margin-bottom: 25px; border: 1px solid #e2e8f0; border-radius: 10px; overflow: hidden;\">" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; width: 38%; font-weight: 700; color: #64748b; background: #f8fafc;\">🎓 Student Name</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #0f172a;\">" + sName + "</td></tr>" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #64748b; background: #f8fafc;\">🆔 Student ID</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; color: #0f172a;\">" + sId + "</td></tr>" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #64748b; background: #f8fafc;\">🏫 Department</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; color: #0f172a;\">" + sDept + "</td></tr>" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #64748b; background: #f8fafc;\">📦 Batch</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; color: #0f172a;\">" + sBatch + "</td></tr>" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #64748b; background: #f8fafc;\">📝 Topic</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; color: #ef4444; font-weight: 800;\">" + topic + "</td></tr>" +
                "<tr><td style=\"padding: 15px; font-weight: 700; color: #64748b; background: #f8fafc;\">⏱️ Requested Duration</td><td style=\"padding: 15px; color: #0f172a; line-height: 1.5;\">" + duration + "</td></tr>" +
                "</table>" +
                "<p style=\"font-size: 15px; color: #475569; line-height: 1.6; margin-bottom: 30px;\">Please log in to your Nexus One Teacher Portal to Approve or Reject this request.</p>" +
                "<div style=\"text-align: center; margin-bottom: 20px;\">" +
                "<a href=\"http://localhost:8080/login\" style=\"display: inline-block; padding: 15px 35px; background: linear-gradient(135deg, #6366f1, #a855f7); color: #ffffff; text-decoration: none; border-radius: 50px; font-weight: 800; font-size: 16px; box-shadow: 0 10px 20px rgba(99, 102, 241, 0.3);\">Login to Portal</a>" +
                "</div>" +
                "<p style=\"font-size: 14px; color:#64748b; margin:0;\">Regards,<br><b>Nexus One System</b><br>Khwaja Yunus Ali University</p>" +
                "</div>" +
                "<div style=\"background: #f8fafc; padding: 25px; text-align: center; font-size: 13px; color: #94a3b8; border-top: 1px solid #e2e8f0;\">" +
                "&copy; 2026 Nexus One University Portal.<br>This is an automated system notification." +
                "</div>" +
                "</div></body></html>";
    }

    @GetMapping("/student-jobs")
    public String jobsPage(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        if (s != null) {
            List<JobService.Job> allJobs = jobService.getActiveJobs();
            allJobs.forEach(j -> jobService.incrementViews(j.getId()));
            List<JobService.Job> updatedJobs = jobService.getActiveJobs();

            List<JobService.Job> institutionalJobs = new ArrayList<>();
            List<JobService.Job> studentTuitions = new ArrayList<>();

            for (JobService.Job j : updatedJobs) {
                if (j.getId() != null && j.getId().startsWith("ST_")) {
                    studentTuitions.add(j);
                } else {
                    institutionalJobs.add(j);
                }
            }

            model.addAttribute("student", s);
            model.addAttribute("officialJobs", institutionalJobs);
            model.addAttribute("studentJobs", studentTuitions);
            model.addAttribute("applications", jobService.getApplicationsForStudent(s.getName()));
            model.addAttribute("unreadNotifCount", notificationService.getUnreadCountForStudent(s.getId()));
            model.addAttribute("initials", getInitials(s.getName()));
        }
        return "student-jobs";
    }

    @PostMapping("/student-jobs/post")
    public String postJob(@RequestParam String title, @RequestParam String company, @RequestParam String type, @RequestParam String deadline, @RequestParam String departmentTags, @RequestParam(required = false) String mediaLink, @RequestParam String description, HttpSession session) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        if (s != null) {
            jobService.postNewJob(title, company, description, deadline, type, departmentTags, s.getName(), mediaLink != null ? mediaLink : "", "STUDENT");
            notificationService.dispatchToStudent(s.getId(), "JOB", "✨ Your post '" + title + "' has been submitted for admin approval.");
        }
        return "redirect:/student-jobs?success=posted";
    }

    @PostMapping("/student-jobs/apply")
    public String applyForJob(@RequestParam String jobId, @RequestParam String jobTitle, HttpSession session) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        if (s != null) {
            jobService.applyForJob(jobId, jobTitle, s.getName());
            notificationService.dispatchToStudent(s.getId(), "JOB", "📝 You successfully applied for the position: " + jobTitle);
        }
        return "redirect:/student-jobs";
    }

    @PostMapping("/student-jobs/cancel-apply")
    public String cancelApplication(@RequestParam String jobId, HttpSession session) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        if (s != null) {
            jobService.cancelApplication(jobId, s.getName());
            notificationService.dispatchToStudent(s.getId(), "JOB", "⚠️ You canceled your application for Job ID: " + jobId);
        }
        return "redirect:/student-jobs";
    }

    @GetMapping("/student-finance")
    public String studentFinancePage(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        model.addAttribute("student", s);
        model.addAttribute("unreadNotifCount", (s != null) ? notificationService.getUnreadCountForStudent(s.getId()) : 0);
        model.addAttribute("initials", getInitials(s != null ? s.getName() : ""));
        return "student-finance";
    }

    @GetMapping("/student-finance/preview-slip")
    public String previewSlip(@RequestParam(value = "months", required = false) String months, HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        if (s == null) return "redirect:/login";

        long baseTuition = 4500;
        long baseTransport = 1500;
        long fixedFees = 500 + 250 + 500 + 500 + 2000;
        long totalPayable = 0;

        if (months != null && !months.isEmpty() && !months.equals("No months selected")) {
            String[] monthArr = months.split(",\\s*");
            int monthCount = monthArr.length;

            long totalTuition = baseTuition * monthCount;
            long totalTransport = baseTransport * monthCount;
            totalPayable = fixedFees + totalTuition + totalTransport;

            model.addAttribute("tuitionFee", String.format("%,d.00", totalTuition));
            model.addAttribute("transportFee", String.format("%,d.00", totalTransport));
            model.addAttribute("totalPayable", String.format("%,d.00", totalPayable));
            model.addAttribute("amountInWords", convertNumberToWords(totalPayable));
        } else {
            months = "No months selected";
            model.addAttribute("tuitionFee", "0.00");
            model.addAttribute("transportFee", "0.00");
            model.addAttribute("totalPayable", "0.00");
            model.addAttribute("amountInWords", "Zero");
        }

        model.addAttribute("student", s);
        model.addAttribute("months", months);
        return "pdf-slip";
    }

    @PostMapping("/student-finance/generate-slip")
    @ResponseBody
    public String generateSlip(@RequestParam("months") String months,
                               @RequestParam(value = "pdfFile", required = false) MultipartFile pdfFile,
                               HttpSession session) {
        if (!SessionUtil.isLoggedIn(session)) return "Error";
        Student s = findStudent(session.getAttribute("loggedIn").toString());

        if (s != null && s.getEmail() != null) {
            String subject = "💳 Your Payment Slip - Nexus One";
            String body = "Dear " + s.getName() + ",\n\nYour payment slip for the month(s) of " + months + " has been generated successfully.\n\nPlease find the attached PDF slip for your records.\n\nBest Regards,\nNexus One Finance";

            byte[] pdfBytes = null;
            try {
                if (pdfFile != null && !pdfFile.isEmpty()) {
                    pdfBytes = pdfFile.getBytes();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            final byte[] finalPdfBytes = pdfBytes;

            notificationService.dispatchToStudent(s.getId(), "FINANCE", "💎 Your payment slip for " + months + " has been generated and emailed successfully.");

            emailExecutor.execute(() -> {
                try {
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true);
                    helper.setFrom("debabir605@gmail.com");
                    helper.setTo(s.getEmail().trim());
                    helper.setSubject(subject);
                    helper.setText(body);

                    if (finalPdfBytes != null && finalPdfBytes.length > 0) {
                        helper.addAttachment("Nexus_One_Payment_Slip.pdf", new ByteArrayResource(finalPdfBytes));
                    }
                    mailSender.send(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            return "Success";
        }
        return "Failed";
    }

    @GetMapping("/student-assignments")
    public String assignments(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        model.addAttribute("assignments", assignmentService.getActiveAssignmentsForStudent(s.getId(), s.getBatch()));
        model.addAttribute("student", s);
        model.addAttribute("unreadNotifCount", notificationService.getUnreadCountForStudent(s.getId()));
        model.addAttribute("initials", getInitials(s.getName()));
        return "student-assignments";
    }

    @PostMapping("/student-assignments/submit")
    public String submitAssignment(@RequestParam("assignmentId") int assignmentId, @RequestParam("file") MultipartFile file, HttpSession session) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());

        if (s != null && !file.isEmpty()) {
            assignmentService.submitAssignment(assignmentId, s.getId(), s.getStudentId(), s.getName(), file);

            Assignment template = assignmentService.getTemplate(assignmentId);
            if (template != null) {
                try {
                    Teacher targetTeacher = teacherService.getAllTeachers().stream().filter(t -> t.getName().equalsIgnoreCase(template.getTeacherName())).findFirst().orElse(null);
                    if (targetTeacher != null) {
                        notificationService.dispatchToTeacher(targetTeacher.getTeacherId(), "ASSIGNMENT", s.getName(), "📥 New assignment code submitted for " + template.getCourseCode());
                    }
                    notificationService.dispatchToStudent(s.getId(), "ASSIGNMENT", "✅ Successfully submitted your code for " + template.getCourseCode());
                } catch (Exception e) {}

                if (s.getEmail() != null && !s.getEmail().isEmpty()) {
                    String studentHtmlBody = buildStudentConfirmationEmail("✅ Assignment Submitted Successfully!", s.getName(), template.getCourseCode(), template.getTopicName(), template.getTeacherName(), "Submitted for Evaluation", "#10b981");
                    sendSimpleHtmlEmail(s.getEmail(), "✅ Assignment Submitted - Nexus One", studentHtmlBody);
                }

                String teacherEmail = getTeacherEmailByName(template.getTeacherName());
                if (teacherEmail != null) {
                    String teacherHtmlBody = buildTeacherNotificationEmail("📥 New Assignment Submission!", template.getTeacherName(), s.getName(), s.getStudentId(), template.getCourseCode(), template.getTopicName(), "Pending Evaluation", "#6366f1");
                    sendSimpleHtmlEmail(teacherEmail, "📥 New Assignment Submission - Nexus One", teacherHtmlBody);
                }
            }
        }
        return "redirect:/student-assignments?success=submitted";
    }

    @PostMapping("/student-assignments/cancel")
    public String cancelAssignment(@RequestParam("assignmentId") int assignmentId, HttpSession session) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());

        if (s != null) {
            Assignment template = assignmentService.getTemplate(assignmentId);
            assignmentService.cancelSubmission(assignmentId, s.getId());

            if (template != null) {
                try {
                    Teacher targetTeacher = teacherService.getAllTeachers().stream().filter(t -> t.getName().equalsIgnoreCase(template.getTeacherName())).findFirst().orElse(null);
                    if (targetTeacher != null) {
                        notificationService.dispatchToTeacher(targetTeacher.getTeacherId(), "ASSIGNMENT", s.getName(), "⚠️ Submission reverted by student for " + template.getCourseCode());
                    }
                    notificationService.dispatchToStudent(s.getId(), "ASSIGNMENT", "⚠️ You have canceled your submission for " + template.getCourseCode());
                } catch (Exception e) {}

                if (s.getEmail() != null && !s.getEmail().isEmpty()) {
                    String studentHtmlBody = buildStudentConfirmationEmail("⚠️ Assignment Canceled!", s.getName(), template.getCourseCode(), template.getTopicName(), template.getTeacherName(), "Canceled by Student", "#ef4444");
                    sendSimpleHtmlEmail(s.getEmail(), "⚠️ Assignment Canceled - Nexus One", studentHtmlBody);
                }

                String teacherEmail = getTeacherEmailByName(template.getTeacherName());
                if (teacherEmail != null) {
                    String teacherHtmlBody = buildTeacherNotificationEmail("⚠️ Submission Canceled!", template.getTeacherName(), s.getName(), s.getStudentId(), template.getCourseCode(), template.getTopicName(), "Student Reverted Submission", "#ef4444");
                    sendSimpleHtmlEmail(teacherEmail, "⚠️ Assignment Canceled - Nexus One", teacherHtmlBody);
                }
            }
        }
        return "redirect:/student-assignments?success=canceled";
    }

    private String buildTeacherNotificationEmail(String title, String teacherName, String sName, String sId, String course, String topic, String status, String color) {
        return "<html><body style=\"background-color:#f8fafc; padding:20px; font-family:'Segoe UI',sans-serif;\">" +
                "<div style=\"max-width:550px; margin:0 auto; background:#fff; border-radius:15px; border-top:5px solid " + color + "; box-shadow:0 8px 20px rgba(0,0,0,0.05); overflow:hidden;\">" +
                "<div style=\"padding:30px;\">" +
                "<h2 style=\"color:" + color + "; margin-top:0; font-size:22px; border-bottom:1px solid #f1f5f9; padding-bottom:15px;\">" + title + "</h2>" +
                "<p style=\"font-size:16px; color:#475569;\">Hello " + teacherName + ",</p>" +
                "<p style=\"font-size:15px; color:#64748b; margin-bottom:20px;\">An update occurred in your assignment module. Details are below:</p>" +
                "<div style=\"background:#f8fafc; border-radius:10px; padding:20px; border:1px solid #e2e8f0;\">" +
                "<p style=\"margin:5px 0; color:#334155; font-size:15px;\">🎓 <b>Student Name:</b> " + sName + "</p>" +
                "<p style=\"margin:5px 0; color:#334155; font-size:15px;\">🆔 <b>Student ID:</b> " + sId + "</p>" +
                "<p style=\"margin:5px 0; color:#334155; font-size:15px;\">📘 <b>Course:</b> " + course + "</p>" +
                "<p style=\"margin:5px 0; color:#334155; font-size:15px;\">📝 <b>Topic:</b> " + topic + "</p>" +
                "<p style=\"margin:5px 0; color:" + color + "; font-size:15px;\">📌 <b>Status:</b> <b>" + status + "</b></p>" +
                "</div>" +
                "<div style=\"text-align:center; margin-top:30px;\">" +
                "<a href=\"http://localhost:8080/teacher-assignment\" style=\"background:" + color + "; color:#fff; padding:12px 30px; text-decoration:none; border-radius:30px; font-weight:bold; font-size:14px;\">Check Portal</a>" +
                "</div></div></div></body></html>";
    }

    private String buildStudentConfirmationEmail(String title, String sName, String course, String topic, String teacherName, String status, String color) {
        return "<html><body style=\"background-color:#f8fafc; padding:20px; font-family:'Segoe UI',sans-serif;\">" +
                "<div style=\"max-width:550px; margin:0 auto; background:#fff; border-radius:15px; border-top:5px solid " + color + "; box-shadow:0 8px 20px rgba(0,0,0,0.05); overflow:hidden;\">" +
                "<div style=\"padding:30px;\">" +
                "<h2 style=\"color:" + color + "; margin-top:0; font-size:22px; border-bottom:1px solid #f1f5f9; padding-bottom:15px;\">" + title + "</h2>" +
                "<p style=\"font-size:16px; color:#475569;\">Hello " + sName + ",</p>" +
                "<p style=\"font-size:15px; color:#64748b; margin-bottom:20px;\">This is a confirmation regarding your assignment submission.</p>" +
                "<div style=\"background:#f8fafc; border-radius:10px; padding:20px; border:1px solid #e2e8f0;\">" +
                "<p style=\"margin:5px 0; color:#334155; font-size:15px;\">📘 <b>Course:</b> " + course + "</p>" +
                "<p style=\"margin:5px 0; color:#334155; font-size:15px;\">📝 <b>Topic:</b> " + topic + "</p>" +
                "<p style=\"margin:5px 0; color:#334155; font-size:15px;\">👨‍🏫 <b>Assigned To:</b> " + teacherName + "</p>" +
                "<p style=\"margin:5px 0; color:" + color + "; font-size:15px;\">📌 <b>Status:</b> <b>" + status + "</b></p>" +
                "</div>" +
                "<div style=\"text-align:center; margin-top:30px;\">" +
                "<a href=\"http://localhost:8080/student-assignments\" style=\"background:" + color + "; color:#fff; padding:12px 30px; text-decoration:none; border-radius:30px; font-weight:bold; font-size:14px;\">Check Portal</a>" +
                "</div></div></div></body></html>";
    }

    private void sendSimpleHtmlEmail(String to, String subject, String htmlBody) {
        emailExecutor.execute(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom("debabir605@gmail.com");
                helper.setTo(to.trim());
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                mailSender.send(message);
                System.out.println("✅ Assignment email alert successfully delivered to: " + to);
            } catch (Exception e) {
                System.out.println("❌ Mail system tracking error console log: " + e.getMessage());
            }
        });
    }

    @GetMapping("/my-notifications")
    public String myNotificationsPage(HttpSession session, Model model) {
        if (!SessionUtil.isLoggedIn(session)) return "redirect:/login";
        Student s = findStudent(session.getAttribute("loggedIn").toString());
        if (s != null) {
            List<Notification> list = notificationService.getNotificationsForStudent(s.getId());
            if (list != null) { Collections.reverse(list); }
            model.addAttribute("notifications", (list != null) ? list : new ArrayList<>());
            model.addAttribute("student", s);
            model.addAttribute("unreadNotifCount", notificationService.getUnreadCountForStudent(s.getId()));
            model.addAttribute("initials", getInitials(s.getName()));
        }
        return "my-notifications";
    }

    @PostMapping("/my-notifications/read/{id}")
    public String markNotificationAsRead(@PathVariable int id) { notificationService.markAsRead(id); return "redirect:/my-notifications"; }

    @PostMapping("/my-notifications/read-all")
    public String markAllNotificationsAsRead(HttpSession session) {
        Object loggedInObj = session.getAttribute("loggedIn");
        if (loggedInObj != null) { Student s = findStudent(loggedInObj.toString()); if (s != null) { notificationService.markAllAsReadForStudent(s.getId()); } }
        return "redirect:/my-notifications";
    }

    @PostMapping("/my-notifications/delete/{id}")
    public String deleteNotification(@PathVariable int id) { notificationService.deleteNotification(id); return "redirect:/my-notifications"; }

    private Student findStudent(String val) {
        for (Student s : studentService.getAllStudents()) { if (val.equals(s.getStudentId()) || val.equalsIgnoreCase(s.getName())) return s; }
        return null;
    }

    private String getInitials(String n) {
        if (n == null || n.trim().isEmpty()) return "ST";
        String[] parts = n.trim().split("\\s+");
        if (parts.length >= 2) return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        return n.substring(0, Math.min(2, n.length())).toUpperCase();
    }
}