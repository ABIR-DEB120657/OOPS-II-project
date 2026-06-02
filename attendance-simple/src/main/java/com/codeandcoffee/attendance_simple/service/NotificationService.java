package com.codeandcoffee.attendance_simple.service;

import com.codeandcoffee.attendance_simple.model.Notification;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private String getWritePath(String f) {
        try {
            return new ClassPathResource("data/" + f).getFile().getAbsolutePath();
        } catch (IOException e) {
            return "src/main/resources/data/" + f;
        }
    }

    public List<Notification> getAllNotifications() {
        List<Notification> list = new ArrayList<>();
        try {
            ClassPathResource res = new ClassPathResource("data/notifications.txt");
            try (BufferedReader r = new BufferedReader(new InputStreamReader(res.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("id|")) continue;
                    String[] p = line.split("\\|", -1);
                    if (p.length < 6) continue;
                    try {
                        int recipientId = p.length >= 7 ? Integer.parseInt(p[6].trim()) : 0;
                        String recipientType = p.length >= 8 ? p[7].trim() : "ADMIN";
                        list.add(new Notification(
                                Integer.parseInt(p[0].trim()), p[1].trim(),
                                p[2].trim(), p[3].trim(),
                                p[4].trim(), p[5].trim(),
                                recipientId, recipientType
                        ));
                    } catch (Exception e) { }
                }
            }
        } catch (IOException e) { }
        return list;
    }

    // ── VIEW FILTERS ──
    public List<Notification> getForAdmin() {
        return getAllNotifications().stream().filter(n -> "ADMIN".equalsIgnoreCase(n.getRecipientType())).collect(Collectors.toList());
    }

    public List<Notification> getNotificationsForStudent(int studentId) {
        return getAllNotifications().stream()
                .filter(n -> "STUDENT".equalsIgnoreCase(n.getRecipientType()) && n.getRecipientId() == studentId)
                .collect(Collectors.toList());
    }

    public List<Notification> getForTeacher(int teacherId) {
        return getAllNotifications().stream()
                .filter(n -> "TEACHER".equalsIgnoreCase(n.getRecipientType()) && n.getRecipientId() == teacherId)
                .collect(Collectors.toList());
    }

    // ── UNREAD COUNTS ──
    public int getUnreadCount() {
        return (int) getForAdmin().stream().filter(n -> "unread".equalsIgnoreCase(n.getStatus())).count();
    }

    public int getUnreadCountForStudent(int studentId) {
        return (int) getNotificationsForStudent(studentId).stream().filter(n -> "unread".equalsIgnoreCase(n.getStatus())).count();
    }

    public int getUnreadCountForTeacher(int teacherId) {
        return (int) getForTeacher(teacherId).stream().filter(n -> "unread".equalsIgnoreCase(n.getStatus())).count();
    }

    // ── ADD NOTIFICATIONS ──
    public void addNotification(String type, String sender, String message, String date) {
        addNotificationFor(type, sender, message, date, 0, "ADMIN");
    }

    public void addNotificationFor(String type, String sender, String message, String date, int recipientId, String recipientType) {
        List<Notification> all = getAllNotifications();
        int newId = all.stream().mapToInt(Notification::getId).max().orElse(0) + 1;
        try (BufferedWriter w = new BufferedWriter(new FileWriter(getWritePath("notifications.txt"), true))) {
            w.write(newId + "|" + type + "|" + sender + "|" + message + "|" + date + "|unread|" + recipientId + "|" + recipientType);
            w.newLine();
        } catch (IOException e) { }
    }

    // ── STATUS UPDATES (Fixed for NotificationController Errors) ──
    public void markAsRead(int id) {
        updateStatus(id, "read");
    }

    public void markAllAsRead() {
        List<Notification> all = getAllNotifications();
        saveAllWithStatus(all, null, null, "read");
    }

    public void markAllAsReadForStudent(int studentId) {
        saveAllWithStatus(getAllNotifications(), "STUDENT", studentId, "read");
    }

    public void markAllAsReadForTeacher(int teacherId) {
        saveAllWithStatus(getAllNotifications(), "TEACHER", teacherId, "read");
    }

    public void deleteNotification(int id) {
        List<Notification> all = getAllNotifications();
        try (BufferedWriter w = new BufferedWriter(new FileWriter(getWritePath("notifications.txt"), false))) {
            for (Notification n : all) {
                if (n.getId() != id) writeLine(w, n, n.getStatus());
            }
        } catch (IOException e) { }
    }

    private void updateStatus(int id, String status) {
        List<Notification> all = getAllNotifications();
        try (BufferedWriter w = new BufferedWriter(new FileWriter(getWritePath("notifications.txt"), false))) {
            for (Notification n : all) {
                String s = (n.getId() == id) ? status : n.getStatus();
                writeLine(w, n, s);
            }
        } catch (IOException e) { }
    }

    private void saveAllWithStatus(List<Notification> all, String type, Integer id, String status) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(getWritePath("notifications.txt"), false))) {
            for (Notification n : all) {
                String s = n.getStatus();
                if (type == null || (type.equalsIgnoreCase(n.getRecipientType()) && n.getRecipientId() == id)) {
                    s = status;
                }
                writeLine(w, n, s);
            }
        } catch (IOException e) { }
    }

    private void writeLine(BufferedWriter w, Notification n, String status) throws IOException {
        w.write(n.getId() + "|" + n.getType() + "|" + n.getSender() + "|" + n.getMessage() + "|" +
                n.getDate() + "|" + status + "|" + n.getRecipientId() + "|" + n.getRecipientType());
        w.newLine();
    }

    // ══════════════════════════════════════════════════════════════
    // 🧠 PREMIUM NEXUS ONE SYSTEM DISPATCHERS (NEW MASTER FEATURE)
    // ══════════════════════════════════════════════════════════════

    // ১. সিস্টেম থেকে অটোমেটিক নোটিফিকেশন পাঠানোর জন্য (যেমন: Attendance Alert, Result Published)
    public void dispatchToStudent(int studentId, String type, String message) {
        String date = java.time.LocalDate.now().toString();
        addNotificationFor(type.toUpperCase(), "Nexus System", message, date, studentId, "STUDENT");
    }

    // ২. নির্দিষ্ট সেন্ডারের নামসহ নোটিফিকেশন পাঠানোর জন্য (যেমন: Teacher's Assignment, Course Outline)
    public void dispatchToStudentWithSender(int studentId, String type, String sender, String message) {
        String date = java.time.LocalDate.now().toString();
        addNotificationFor(type.toUpperCase(), sender, message, date, studentId, "STUDENT");
    }

    // ৩. টিচারকে সরাসরি নোটিফিকেশন পাঠানোর জন্য (যেমন: Leave Request from Student, Faculty Sync request)
    public void dispatchToTeacher(int teacherId, String type, String sender, String message) {
        String date = java.time.LocalDate.now().toString();
        addNotificationFor(type.toUpperCase(), sender, message, date, teacherId, "TEACHER");
    }
}