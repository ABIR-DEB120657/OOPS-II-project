package com.codeandcoffee.attendance_simple.service;

import com.codeandcoffee.attendance_simple.model.Assignment;
import com.codeandcoffee.attendance_simple.util.DataPathConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    @Autowired private DataPathConfig dataPathConfig;

    private File getUploadDirTarget() {
        try {
            String targetRoot = new ClassPathResource("").getFile().getAbsolutePath();
            File assignDir = new File(targetRoot + File.separator + "static" + File.separator + "assignments");
            if (!assignDir.exists()) assignDir.mkdirs();
            return assignDir;
        } catch (IOException e) {
            File fallback = new File("target/classes/static/assignments");
            if (!fallback.exists()) fallback.mkdirs();
            return fallback;
        }
    }

    private File getUploadDirSrc() {
        try {
            String targetRoot = new ClassPathResource("").getFile().getAbsolutePath();
            String srcDirPath = targetRoot.replace("target\\classes", "src\\main\\resources")
                    .replace("target/classes", "src/main/resources")
                    + File.separator + "static" + File.separator + "assignments";
            File srcDir = new File(srcDirPath);
            if (!srcDir.exists()) srcDir.mkdirs();
            return srcDir;
        } catch (IOException e) {
            File fallback = new File("src/main/resources/static/assignments");
            if (!fallback.exists()) fallback.mkdirs();
            return fallback;
        }
    }

    public List<Assignment> getAllAssignments() {
        List<Assignment> list = new ArrayList<>();
        File file = new File(dataPathConfig.getPath("assignment.txt"));
        if (!file.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length < 11) continue;

                try {
                    Assignment a = new Assignment();
                    a.setAssignmentId(Integer.parseInt(parts[0].trim()));
                    a.setCourseCode(parts[1].trim());
                    a.setCourseName(parts[2].trim());
                    a.setTeacherName(parts[3].trim());
                    a.setTopicName(parts[4].trim());
                    a.setBatch(parts[5].trim());
                    a.setStartDate(parts[6].trim());
                    a.setEndDate(parts[7].trim());
                    a.setFilePath(parts[8].trim());
                    a.setStudentId(Integer.parseInt(parts[9].trim()));
                    a.setStatus(parts[10].trim());

                    if (parts.length >= 12) {
                        a.setSubmissionDate(parts[11].trim());
                    } else {
                        a.setSubmissionDate("-");
                    }

                    a.setTeacherId(0);
                    a.setStudentStrId("");
                    a.setStudentName("");
                    list.add(a);
                } catch (Exception ignored) {}
            }
        } catch (IOException ignored) {}
        return list;
    }

    public Assignment getTemplate(int assignmentId) {
        return getAllAssignments().stream()
                .filter(a -> a.getAssignmentId() == assignmentId && a.getStudentId() == 0)
                .findFirst().orElse(null);
    }

    public List<Assignment> getActiveAssignmentsForStudent(int studentRowId, String studentBatch) {
        List<Assignment> all = getAllAssignments();
        List<Assignment> templates = all.stream()
                .filter(a -> {
                    if (a.getStudentId() != 0) return false;
                    String tBatch = a.getBatch() != null ? a.getBatch().trim().toLowerCase() : "";
                    String sBatch = studentBatch != null ? studentBatch.trim().toLowerCase() : "";
                    if (tBatch.equals("all") || tBatch.equals("any")) return true;
                    return tBatch.equals(sBatch) || sBatch.contains(tBatch) || tBatch.contains(sBatch);
                }).collect(Collectors.toList());

        List<Assignment> studentSubmissions = all.stream()
                .filter(a -> a.getStudentId() == studentRowId)
                .collect(Collectors.toList());

        List<Assignment> result = new ArrayList<>();
        for (Assignment temp : templates) {
            Assignment sub = studentSubmissions.stream()
                    .filter(s -> s.getAssignmentId() == temp.getAssignmentId())
                    .findFirst().orElse(null);
            if (sub != null) {
                result.add(sub);
            } else {
                Assignment notSub = new Assignment();
                notSub.setAssignmentId(temp.getAssignmentId());
                notSub.setCourseCode(temp.getCourseCode());
                notSub.setCourseName(temp.getCourseName());
                notSub.setTeacherName(temp.getTeacherName());
                notSub.setTopicName(temp.getTopicName());
                notSub.setBatch(temp.getBatch());
                notSub.setEndDate(temp.getEndDate());
                notSub.setStudentId(studentRowId);
                notSub.setStatus("NOT_SUBMITTED");
                result.add(notSub);
            }
        }
        return result;
    }

    public void createAssignment(Assignment a) {
        List<Assignment> all = getAllAssignments();
        int newId = all.stream().mapToInt(Assignment::getAssignmentId).max().orElse(0) + 1;
        a.setAssignmentId(newId);
        a.setStudentId(0);
        a.setFilePath("");
        a.setStatus("TEMPLATE");
        a.setSubmissionDate("");
        all.add(a);
        saveAllAssignments(all);
    }

    // ✅ FIX: এই method টা missing ছিল — TeacherController line 172 এ call হচ্ছিল
    // file থাকলে → assignment folder এ save করবে + filePath set করবে
    // file না থাকলে বা empty হলে → শুধু assignment data save করবে
    public void saveAssignment(Assignment a, MultipartFile file) {
        // File upload handle
        if (file != null && !file.isEmpty()) {
            String safeFileName = a.getAssignmentId() + "_"
                    + (a.getCourseCode() != null ? a.getCourseCode() : "file") + "_"
                    + file.getOriginalFilename().replaceAll("\\s+", "_");
            try {
                Files.copy(file.getInputStream(),
                        new File(getUploadDirSrc(), safeFileName).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                Files.copy(file.getInputStream(),
                        new File(getUploadDirTarget(), safeFileName).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                a.setFilePath(safeFileName);
            } catch (IOException e) {
                System.out.println("⚠️ File upload failed: " + e.getMessage());
                a.setFilePath("");
            }
        } else {
            // File নেই — filePath empty রাখো
            if (a.getFilePath() == null) a.setFilePath("");
        }

        // Assignment data save (createAssignment এর মতোই — ID auto-generate)
        List<Assignment> all = getAllAssignments();
        int newId = all.stream().mapToInt(Assignment::getAssignmentId).max().orElse(0) + 1;
        a.setAssignmentId(newId);
        a.setStudentId(0);
        a.setStatus("TEMPLATE");
        if (a.getSubmissionDate() == null) a.setSubmissionDate("");
        if (a.getStartDate() == null) {
            a.setStartDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        all.add(a);
        saveAllAssignments(all);
    }

    // 🔥 Edit Assignment Logic
    public void editAssignment(int id, String code, String name, String batch,
                               String topic, String endDate) {
        List<Assignment> all = getAllAssignments();
        for (Assignment a : all) {
            if (a.getAssignmentId() == id) {
                a.setCourseCode(code);
                a.setCourseName(name);
                a.setBatch(batch);
                a.setTopicName(topic);
                a.setEndDate(endDate);
            }
        }
        saveAllAssignments(all);
    }

    public void submitAssignment(int assignmentId, int studentId, String studentStrId,
                                 String studentName, MultipartFile file) {
        if (file == null || file.isEmpty()) return;
        String fileName = assignmentId + "_"
                + studentStrId + "_"
                + file.getOriginalFilename().replaceAll("\\s+", "_");
        try {
            Files.copy(file.getInputStream(),
                    new File(getUploadDirSrc(), fileName).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            Files.copy(file.getInputStream(),
                    new File(getUploadDirTarget(), fileName).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            List<Assignment> all = getAllAssignments();
            all.removeIf(a -> a.getAssignmentId() == assignmentId && a.getStudentId() == studentId);
            Assignment template = all.stream()
                    .filter(a -> a.getAssignmentId() == assignmentId && a.getStudentId() == 0)
                    .findFirst().orElse(null);

            if (template != null) {
                Assignment submission = new Assignment();
                submission.setAssignmentId(assignmentId);
                submission.setCourseCode(template.getCourseCode());
                submission.setCourseName(template.getCourseName());
                submission.setTeacherName(template.getTeacherName());
                submission.setTopicName(template.getTopicName());
                submission.setBatch(template.getBatch());
                submission.setStartDate(template.getStartDate());
                submission.setEndDate(template.getEndDate());
                submission.setFilePath(fileName);
                submission.setStudentId(studentId);
                submission.setStatus("PENDING");
                submission.setSubmissionDate(
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM, hh:mm a")));
                all.add(submission);
                saveAllAssignments(all);
            }
        } catch (IOException e) {
            System.out.println("⚠️ Error saving file: " + e.getMessage());
        }
    }

    // 🔥 Cancel Submission Logic
    public void cancelSubmission(int assignmentId, int studentId) {
        List<Assignment> all = getAllAssignments();
        all.removeIf(a -> a.getAssignmentId() == assignmentId && a.getStudentId() == studentId);
        saveAllAssignments(all);
    }

    public Assignment updateStatus(int assignmentId, int studentId, String status) {
        List<Assignment> all = getAllAssignments();
        Assignment match = null;
        for (Assignment a : all) {
            if (a.getAssignmentId() == assignmentId && a.getStudentId() == studentId) {
                a.setStatus(status);
                match = a;
                break;
            }
        }
        saveAllAssignments(all);
        return match;
    }

    public void deleteAssignment(int assignmentId) {
        List<Assignment> all = getAllAssignments();
        all.removeIf(a -> a.getAssignmentId() == assignmentId);
        saveAllAssignments(all);
    }

    private void saveAllAssignments(List<Assignment> list) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(dataPathConfig.getPath("assignment.txt"), false))) {
            for (Assignment a : list) {
                writer.write(
                        a.getAssignmentId() + "|" +
                                (a.getCourseCode()      != null ? a.getCourseCode()      : "") + "|" +
                                (a.getCourseName()      != null ? a.getCourseName()      : "") + "|" +
                                (a.getTeacherName()     != null ? a.getTeacherName()     : "") + "|" +
                                (a.getTopicName()       != null ? a.getTopicName()       : "") + "|" +
                                (a.getBatch()           != null ? a.getBatch()           : "") + "|" +
                                (a.getStartDate()       != null ? a.getStartDate()       : "") + "|" +
                                (a.getEndDate()         != null ? a.getEndDate()         : "") + "|" +
                                (a.getFilePath()        != null ? a.getFilePath()        : "") + "|" +
                                a.getStudentId() + "|" +
                                (a.getStatus()          != null ? a.getStatus()          : "") + "|" +
                                (a.getSubmissionDate()  != null ? a.getSubmissionDate()  : "")
                );
                writer.newLine();
            }
        } catch (IOException ignored) {}
    }
}