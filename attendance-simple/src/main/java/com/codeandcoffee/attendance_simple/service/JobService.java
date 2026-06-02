package com.codeandcoffee.attendance_simple.service;

import com.codeandcoffee.attendance_simple.model.Student;
import com.codeandcoffee.attendance_simple.model.Teacher;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class JobService {

    @Autowired private StudentService studentService;
    @Autowired private TeacherService teacherService;
    @Autowired private NotificationService notificationService;
    @Autowired private JavaMailSender mailSender;

    private final ExecutorService emailExecutor = Executors.newCachedThreadPool();

    public static class Job {
        public String id, title, company, deadline, type;
        public String description, departmentTags, authorName, mediaLink, status;
        public int views, applyCount;

        public Job(String i, String t, String c, String d, String ty, String desc, String tags, String auth, String link, String stat, int v, int a) {
            id = i; title = t; company = c; deadline = d; type = ty;
            description = desc; departmentTags = tags; authorName = auth; mediaLink = link; status = stat;
            views = v; applyCount = a;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getCompany() { return company; }
        public String getDeadline() { return deadline; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public String getDepartmentTags() { return departmentTags; }
        public String getAuthorName() { return authorName; }
        public String getMediaLink() { return mediaLink; }
        public String getStatus() { return status; }
        public int getViews() { return views; }
        public int getApplyCount() { return applyCount; }
    }

    public static class Application {
        public String id, jobId, jobTitle, studentName, dateApplied, status;
        public Application(String i, String ji, String jt, String sn, String d, String s) {
            id = i; jobId = ji; jobTitle = jt; studentName = sn; dateApplied = d; status = s;
        }
        public String getId() { return id; }
        public String getJobTitle() { return jobTitle; }
        public String getStudentName() { return studentName; }
        public String getDateApplied() { return dateApplied; }
        public String getStatus() { return status; }
        public String getJobId() { return jobId; }
    }

    private String getWritePath(String filename) {
        try {
            File rootDir = new File("src/main/resources/data/");
            if (!rootDir.exists()) {
                rootDir.mkdirs();
            }
            return new File(rootDir, filename).getAbsolutePath();
        } catch (Exception e) {
            return "src/main/resources/data/" + filename;
        }
    }

    public List<Job> getAllJobs() {
        List<Job> list = new ArrayList<>();
        boolean needsUpdate = false;

        File officialFile = new File(getWritePath("job_circulars.txt"));
        needsUpdate |= loadJobsFromFile(officialFile, list);

        File studentFile = new File(getWritePath("student_job_circulars.txt"));
        needsUpdate |= loadJobsFromFile(studentFile, list);

        // 🔥 FORCE HARD PURGE: If any fake circulars are caught, erase them from disk immediately
        if (needsUpdate || forceCleanCheck(list)) {
            list.removeIf(this::isTrash);
            rewriteAllJobs(list);
        }
        return list;
    }

    private boolean forceCleanCheck(List<Job> list) {
        for (Job j : list) {
            if (isTrash(j)) return true;
        }
        return false;
    }

    // 🎯 RECONNAISSANCE SIGNATURE FILTER: Spot and destroy the garbage entries
    private boolean isTrash(Job j) {
        if (j.getTitle() == null) return true;
        String t = j.getTitle().trim().toLowerCase();
        String c = j.getCompany() != null ? j.getCompany().trim().toLowerCase() : "";
        String d = j.getDescription() != null ? j.getDescription().trim().toLowerCase() : "";
        String a = j.getAuthorName() != null ? j.getAuthorName().trim().toLowerCase() : "";

        return t.equals("tuition") && d.contains("coblar") ||
                t.equals("cse") && a.contains("abir deb") ||
                t.equals("test") || t.isEmpty() || c.equals("admin") || a.equals("admin");
    }

    private boolean loadJobsFromFile(File file) {
        if (!file.exists()) {
            try {
                ClassPathResource res = new ClassPathResource("data/" + file.getName());
                return res.exists();
            } catch (Exception e) { return false; }
        }
        return file.exists() && file.length() > 0;
    }

    private boolean loadJobsFromFile(File file, List<Job> list) {
        if (!loadJobsFromFile(file)) return false;
        boolean updated = false;

        InputStream is = null;
        try {
            if (file.exists() && file.length() > 0) {
                is = new FileInputStream(file);
            } else {
                ClassPathResource res = new ClassPathResource("data/" + file.getName());
                is = res.getInputStream();
            }

            try (BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] p = line.split("\\|", -1);
                    if (p.length >= 5) {

                        String title = p[1].trim();
                        String company = p[2].trim();
                        String desc = p.length > 5 && !p[5].isEmpty() ? p[5] : "No description.";
                        String tags = p.length > 6 && !p[6].isEmpty() ? p[6] : "General";
                        String auth = p.length > 7 && !p[7].isEmpty() ? p[7] : "Admin";
                        String link = p.length > 8 ? p[8] : "";
                        String stat = p.length > 9 && !p[9].isEmpty() ? p[9] : "ACTIVE";

                        int views = p.length > 10 && !p[10].isEmpty() ? Integer.parseInt(p[10]) : 0;
                        int appCount = p.length > 11 && !p[11].isEmpty() ? Integer.parseInt(p[11]) : 0;

                        Job j = new Job(p[0], title, company, p[3], p[4], desc, tags, auth, link, stat, views, appCount);

                        if (isTrash(j)) {
                            updated = true;
                            continue;
                        }

                        list.add(j);
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return updated;
    }

    public List<Job> getActiveJobs() {
        return getAllJobs().stream().filter(j -> j.getStatus().equals("ACTIVE")).collect(Collectors.toList());
    }

    public Job getJobById(String jobId) {
        return getAllJobs().stream().filter(j -> j.getId().equals(jobId)).findFirst().orElse(null);
    }

    public void incrementViews(String jobId) {
        List<Job> all = getAllJobs();
        for (Job j : all) {
            if (j.getId().equals(jobId)) {
                j.views++;
                break;
            }
        }
        rewriteAllJobs(all);
    }

    public void postNewJob(String title, String company, String desc, String deadline, String type, String tags, String authorName, String mediaLink, String role) {
        String id, status, filename;

        if (role.equalsIgnoreCase("STUDENT")) {
            id = "ST_" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            status = "ACTIVE";
            filename = "student_job_circulars.txt";
        } else {
            id = "J_" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            status = "ACTIVE";
            filename = "job_circulars.txt";
        }

        List<Job> all = getAllJobs();
        all.add(new Job(id, title, company, deadline, type, desc, tags, authorName, mediaLink, status, 0, 0));
        rewriteAllJobs(all);

        String dateStr = LocalDate.now().toString();
        String alertMsg = "💼 New Hub Alert: " + title + " posted by " + authorName;
        List<Student> students = studentService.getAllStudents();
        if (students != null) {
            students.forEach(st -> {
                notificationService.addNotificationFor("JOB", authorName, alertMsg, dateStr, st.getId(), "STUDENT");
            });
        }
    }

    private void rewriteAllJobs(List<Job> jobs) {
        List<Job> officialJobs = jobs.stream().filter(j -> !j.getId().startsWith("ST_")).collect(Collectors.toList());
        List<Job> studentJobs = jobs.stream().filter(j -> j.getId().startsWith("ST_")).collect(Collectors.toList());
        writeToFile("job_circulars.txt", officialJobs);
        writeToFile("student_job_circulars.txt", studentJobs);
    }

    private void writeToFile(String filename, List<Job> jobs) {
        File fileTarget = new File(getWritePath(filename));
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileTarget, false), "UTF-8"))) {
            for (Job j : jobs) {
                w.write(String.join("|", j.id, j.title, j.company, j.deadline, j.type, j.description, j.departmentTags, j.authorName, j.mediaLink, j.status, String.valueOf(j.views), String.valueOf(j.applyCount)));
                w.newLine();
            }
            w.flush();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void applyForJob(String jobId, String jobTitle, String studentName) {
        String id = UUID.randomUUID().toString().substring(0, 6);
        File appFile = new File(getWritePath("job_applications.txt"));
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(appFile, true), "UTF-8"))) {
            w.write(id + "|" + jobId + "|" + jobTitle + "|" + studentName + "|" + LocalDate.now() + "|Submitted");
            w.newLine();
            w.flush();
        } catch (IOException e) {}

        List<Job> jobs = getAllJobs();
        Job appliedJob = null;
        for (Job j : jobs) {
            if (j.getId().equals(jobId)) {
                j.applyCount++;
                appliedJob = j;
                break;
            }
        }
        rewriteAllJobs(jobs);

        if (appliedJob != null) {
            final Job targetedJob = appliedJob;
            Student student = studentService.getAllStudents().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(studentName)).findFirst().orElse(null);

            String authorEmail = resolveEmail(targetedJob.getAuthorName());

            if (authorEmail != null && student != null) {
                String sId = student.getStudentId() != null ? student.getStudentId() : "N/A";
                String sDept = student.getDepartment() != null ? student.getDepartment() : "N/A";
                String sBatch = student.getBatch() != null ? student.getBatch() : "N/A";

                String htmlContent = buildPremiumApplicationTemplate(targetedJob.getAuthorName(), studentName, sId, sDept, sBatch, targetedJob.getTitle());

                emailExecutor.execute(() -> {
                    try {
                        MimeMessage mimeMessage = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                        helper.setTo(authorEmail.trim());
                        helper.setSubject("New Application Received - " + targetedJob.getTitle());
                        helper.setText(htmlContent, true);
                        mailSender.send(mimeMessage);
                    } catch (Exception ex) { ex.printStackTrace(); }
                });
            }
        }
    }

    private String resolveEmail(String authorName) {
        if (authorName.equalsIgnoreCase("Admin")) return "debabir605@gmail.com";
        for (Student s : studentService.getAllStudents()) {
            if (s.getName().equalsIgnoreCase(authorName)) return s.getEmail();
        }
        for (Teacher t : teacherService.getAllTeachers()) {
            if (t.getName().equalsIgnoreCase(authorName)) return t.getEmail();
        }
        return "debabir605@gmail.com";
    }

    private String buildPremiumApplicationTemplate(String teacherName, String sName, String sId, String sDept, String sBatch, String jobTitle) {
        return "<html><body style=\"background-color:#f1f5f9; padding:20px; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">" +
                "<div style=\"max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 15px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.05);\">" +
                "<div style=\"background: linear-gradient(135deg, #0f172a, #1e293b); padding: 30px; text-align: center;\">" +
                "<h2 style=\"color: #ffffff; margin: 0; font-size: 24px; letter-spacing: 2px;\">NEXUS ONE <span style=\"font-weight:300; color:#94a3b8;\">PORTAL</span></h2>" +
                "</div>" +
                "<div style=\"padding: 40px; color: #334155;\">" +
                "<p style=\"font-size: 16px;\">Hello <b>" + teacherName + "</b>,</p>" +
                "<p style=\"font-size: 15px; color: #475569; line-height: 1.6;\">A student has submitted an application request. Please review the details below:</p>" +
                "<table style=\"width: 100%; border-collapse: collapse; margin-top: 25px; margin-bottom: 25px; border: 1px solid #e2e8f0; border-radius: 10px; overflow: hidden;\">" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; width: 38%; font-weight: 700; color: #64748b; background: #f8fafc;\">Student Name</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #0f172a;\">" + sName + "</td></tr>" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #64748b; background: #f8fafc;\">Student ID</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; color: #0f172a;\">" + sId + "</td></tr>" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #64748b; background: #f8fafc;\">Department</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; color: #0f172a;\">" + sDept + "</td></tr>" +
                "<tr><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; font-weight: 700; color: #64748b; background: #f8fafc;\">Batch</td><td style=\"padding: 15px; border-bottom: 1px solid #e2e8f0; color: #0f172a;\">" + sBatch + "</td></tr>" +
                "<tr><td style=\"padding: 15px; font-weight: 700; color: #64748b; background: #f8fafc;\">Job Title</td><td style=\"padding: 15px; color: #ef4444; font-weight: 800;\">" + jobTitle + "</td></tr>" +
                "</table>" +
                "<p style=\"font-size: 15px; color: #475569; line-height: 1.6; margin-bottom: 30px;\">Please log in to your Nexus One Teacher Portal to Approve or Reject this request.</p>" +
                "<div style=\"text-align: center; margin-top: 40px;\">" +
                "<a href=\"http://localhost:8080/login\" style=\"display: inline-block; padding: 15px 35px; background: linear-gradient(135deg, #6366f1, #a855f7); color: #ffffff; text-decoration: none; border-radius: 50px; font-weight: 800; font-size: 16px; box-shadow: 0 10px 20px rgba(99, 102, 241, 0.3);\">Login to Portal</a>" +
                "</div>" +
                "</div>" +
                "<div style=\"background: #f8fafc; padding: 25px; text-align: center; font-size: 13px; color: #94a3b8; border-top: 1px solid #e2e8f0;\">" +
                "&copy; 2026 Nexus One University Portal.<br>This is an automated system notification." +
                "</div>" +
                "</div></body></html>";
    }

    public void cancelApplication(String jobId, String studentName) {
        File file = new File(getWritePath("job_applications.txt"));
        List<String> validLines = new ArrayList<>();
        boolean found = false;

        if (file.exists()) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
                String line;
                while ((line = r.readLine()) != null) {
                    String[] p = line.split("\\|");
                    if (p.length >= 6 && p[1].equals(jobId) && p[3].equalsIgnoreCase(studentName)) {
                        found = true;
                    } else {
                        validLines.add(line);
                    }
                }
            } catch (Exception e) {}
        }

        List<Job> all = getAllJobs();
        boolean removedFromJobs = all.removeIf(j -> j.getId().equals(jobId));
        if (removedFromJobs) {
            rewriteAllJobs(all);
            found = true;
        }

        if (found) {
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF-8"))) {
                for (String l : validLines) { w.write(l); w.newLine(); }
                w.flush();
            } catch (Exception e) {}

            if (!removedFromJobs) {
                List<Job> jobs = getAllJobs();
                for (Job j : jobs) {
                    if (j.getId().equals(jobId) && j.applyCount > 0) {
                        j.applyCount--;
                        break;
                    }
                }
                rewriteAllJobs(jobs);
            }
        }
    }

    public List<Application> getApplicationsForStudent(String studentName) {
        List<Application> list = new ArrayList<>();
        File file = new File(getWritePath("job_applications.txt"));
        if (!file.exists()) return list;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length >= 6 && p[3].equalsIgnoreCase(studentName)) {
                    list.add(new Application(p[0], p[1], p[2], p[3], p[4], p[5]));
                }
            }
        } catch (Exception e) {}
        return list;
    }
}