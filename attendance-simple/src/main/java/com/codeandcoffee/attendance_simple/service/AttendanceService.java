package com.codeandcoffee.attendance_simple.service;

import com.codeandcoffee.attendance_simple.model.Attendance;
import com.codeandcoffee.attendance_simple.model.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Autowired
    @Lazy
    private StudentService studentService;

    // 🔥 Added Notification Engine
    @Autowired
    @Lazy
    private NotificationService notificationService;

    // ✅ ১. target ফোল্ডারের রানটাইম পাথ বের করার ফিক্সড মেথড
    private String getTargetPath(String filename) {
        try {
            ClassPathResource resource = new ClassPathResource("data/" + filename);
            return resource.getFile().getAbsolutePath();
        } catch (IOException e) {
            return "target/classes/data/" + filename;
        }
    }

    // ✅ ২. সরাসরি মেইন src ফোল্ডারের (Source File) পাথ বের করার মেথড
    private String getSrcPath(String filename) {
        try {
            ClassPathResource resource = new ClassPathResource("data/");
            String targetPath = resource.getFile().getAbsolutePath();
            return targetPath.replace("target\\classes", "src\\main\\resources")
                    .replace("target/classes", "src/main/resources") + File.separator + filename;
        } catch (IOException e) {
            return "src/main/resources/data/" + filename;
        }
    }

    // ── 🎯 COURSE & DATE SPECIFIC ATTENDANCE READ ──
    public List<Attendance> getAttendanceByDateAndCourse(String date, String courseCode) {
        String filename = courseCode.trim() + "_" + date.trim() + ".txt";
        List<Attendance> list = new ArrayList<>();

        File file = new File(getTargetPath(filename));
        if (!file.exists()) {
            file = new File(getSrcPath(filename));
        }
        if (!file.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length < 3) continue;
                try {
                    list.add(new Attendance(
                            Integer.parseInt(parts[0].trim()),
                            parts[1].trim(),
                            parts[2].trim(),
                            parts.length > 3 ? parts[3].trim() : ""
                    ));
                } catch (NumberFormatException e) {
                    System.out.println("Skipping bad line: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading course attendance: " + e.getMessage());
        }
        return list;
    }

    // ── GLOBAL ATTENDANCE READ (FALLBACK) ──
    public List<Attendance> getAllAttendance() {
        List<Attendance> list = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource("data/attendance.txt");
            // ✅ Fixed: Added try-with-resources to prevent memory leak
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
                    String[] parts = line.split("\\|", -1);
                    if (parts.length < 3) continue;
                    try {
                        list.add(new Attendance(
                                Integer.parseInt(parts[0].trim()),
                                parts[1].trim(),
                                parts[2].trim(),
                                parts.length > 3 ? parts[3].trim() : ""
                        ));
                    } catch (NumberFormatException e) { }
                }
            }
        } catch (IOException e) {
            System.out.println("Global Attendance File Not Found.");
        }
        return list;
    }

    public List<Attendance> getAttendanceByStudentId(int studentId) {
        return getAllAttendance().stream().filter(a -> a.getStudentId() == studentId).collect(Collectors.toList());
    }

    public List<Attendance> getAttendanceByDate(String date) {
        return getAllAttendance().stream().filter(a -> a.getDate().equals(date)).collect(Collectors.toList());
    }

    // ── 3. GLOBAL ATTENDANCE SAVE (FALLBACK) ──
    public void saveAttendanceForDate(List<Attendance> newRecords, String date) {
        List<Attendance> all = getAllAttendance();
        all.removeIf(a -> a.getDate().equals(date));
        all.addAll(newRecords);

        saveToFileEngine(getTargetPath("attendance.txt"), all, date, "GLOBAL");
        saveToFileEngine(getSrcPath("attendance.txt"), all, date, "GLOBAL");
    }

    // 🔥 4. COURSE & DATE SPECIFIC SAVE & DISPATCH NOTIFICATIONS
    public void saveAttendanceForCourse(List<Attendance> newRecords, String date, String courseCode) {
        String filename = courseCode.trim() + "_" + date.trim() + ".txt";

        saveToFileEngine(getTargetPath(filename), newRecords, date, courseCode);
        saveToFileEngine(getSrcPath(filename), newRecords, date, courseCode);

        // 🧠 3. Nexus Intelligence: Dispatch Auto-Notifications
        try {
            for (Attendance a : newRecords) {
                String message = "";
                if (a.getStatus().equals("A")) {
                    message = "You have been marked ABSENT for " + courseCode + " class on " + date + ". Please maintain regular attendance.";
                } else if (a.getStatus().equals("L")) {
                    message = "You have been marked LATE for " + courseCode + " class on " + date + ". Try to be on time.";
                } else if (a.getStatus().equals("P")) {
                    message = "Your attendance is recorded as PRESENT for " + courseCode + " class on " + date + ". Keep it up!";
                }

                if (!message.isEmpty()) {
                    notificationService.dispatchToStudent(a.getStudentId(), "ATTENDANCE", message);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Silent Error in Attendance Notification Dispatcher: " + e.getMessage());
        }
    }

    // 🔥 কোর রাইটিং ইঞ্জিন
    private void saveToFileEngine(String filePath, List<Attendance> newRecords, String date, String courseCode) {
        List<Attendance> all = new ArrayList<>();
        File file = new File(filePath);

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
                    String[] parts = line.split("\\|", -1);
                    if (parts.length >= 3) {
                        if (!parts[1].trim().equals(date)) {
                            all.add(new Attendance(
                                    Integer.parseInt(parts[0].trim()),
                                    parts[1].trim(),
                                    parts[2].trim(),
                                    parts.length > 3 ? parts[3].trim() : ""
                            ));
                        }
                    }
                }
            } catch (IOException e) { }
        } else {
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
        }

        all.addAll(newRecords);

        String deptName = "CSE";
        String batchName = "18th";

        if (!newRecords.isEmpty()) {
            Student sampleStudent = studentService.getStudentById(newRecords.get(0).getStudentId());
            if (sampleStudent != null) {
                deptName = sampleStudent.getDepartment() != null ? sampleStudent.getDepartment() : "CSE";
                batchName = sampleStudent.getBatch() != null ? sampleStudent.getBatch() : "18th";
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write("# ════════════════════════════════════════════════"); writer.newLine();
            writer.write("# 📚 NEXUS ONE ATTENDANCE RECORD SHEET"); writer.newLine();
            writer.write("# ════════════════════════════════════════════════"); writer.newLine();
            writer.write("# 📘 COURSE CODE : " + courseCode.trim()); writer.newLine();
            writer.write("# 🏢 DEPARTMENT  : " + deptName); writer.newLine();
            writer.write("# 🎯 BATCH       : " + batchName); writer.newLine();
            writer.write("# 📅 CLASS DATE  : " + date.trim()); writer.newLine();
            writer.write("# ════════════════════════════════════════════════"); writer.newLine();
            writer.newLine();

            for (Attendance a : all) {
                writer.write(a.getStudentId() + "|" +
                        a.getDate() + "|" +
                        a.getStatus() + "|" +
                        (a.getRemark() != null ? a.getRemark() : ""));
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving attendance file on path: " + filePath + " -> " + e.getMessage());
        }
    }

    public double getAttendancePercentage(int studentId) {
        List<Attendance> records = getAttendanceByStudentId(studentId);
        if (records.isEmpty()) return 0.0;
        long present = records.stream()
                .filter(a -> a.getStatus().equals("P") || a.getStatus().equals("L"))
                .count();
        return Math.round((present * 100.0 / records.size()) * 10.0) / 10.0;
    }

    public double getAttendancePercentage(String studentId) {
        Student student = studentService.getStudentByStudentId(studentId);
        if (student == null) return 0.0;
        return getAttendancePercentage(student.getId());
    }

    public long countPresentToday(String today) {
        return getAttendanceByDate(today).stream().filter(a -> a.getStatus().equals("P")).count();
    }

    public long countAbsentToday(String today) {
        return getAttendanceByDate(today).stream().filter(a -> a.getStatus().equals("A")).count();
    }

    public long countLateToday(String today) {
        return getAttendanceByDate(today).stream().filter(a -> a.getStatus().equals("L")).count();
    }
}