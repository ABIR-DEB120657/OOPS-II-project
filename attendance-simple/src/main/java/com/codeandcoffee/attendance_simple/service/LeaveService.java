package com.codeandcoffee.attendance_simple.service;

import com.codeandcoffee.attendance_simple.model.LeaveRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaveService {

    // 🔥 Added Nexus Intelligence Notification Engine
    @Autowired
    @Lazy
    private NotificationService notificationService;

    private String getWritePath(String filename) {
        try {
            return new ClassPathResource("data/" + filename).getFile().getAbsolutePath();
        } catch (IOException e) {
            return "src/main/resources/data/" + filename;
        }
    }

    public List<LeaveRequest> getAllLeaveRequests() {
        List<LeaveRequest> list = new ArrayList<>();
        try {
            ClassPathResource res = new ClassPathResource("data/leave.txt");
            try (BufferedReader r = new BufferedReader(new InputStreamReader(res.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("leaveId")) continue;
                    String[] p = line.split("\\|", -1);
                    if (p.length < 8) continue;
                    int teacherId = (p.length >= 9 && !p[8].trim().isEmpty()) ? safeInt(p[8].trim()) : 0;
                    list.add(new LeaveRequest(
                            safeInt(p[0].trim()), safeInt(p[1].trim()),
                            p[2].trim(), p[3].trim(), p[4].trim(), p[5].trim(),
                            p[6].trim(), p[7].trim(), teacherId
                    ));
                }
            }
        } catch (IOException e) { System.out.println("Error reading leave.txt: " + e.getMessage()); }
        return list;
    }

    private int safeInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    public LeaveRequest getLeaveRequestById(int leaveId) {
        return getAllLeaveRequests().stream().filter(l -> l.getLeaveId() == leaveId).findFirst().orElse(null);
    }

    public List<LeaveRequest> getPendingRequests() { return getAllLeaveRequests().stream().filter(l -> "PENDING".equals(l.getStatus())).collect(Collectors.toList()); }
    public List<LeaveRequest> getRequestsForTeacher(int teacherId) { return getAllLeaveRequests().stream().filter(l -> l.getTeacherId() == teacherId).collect(Collectors.toList()); }
    public List<LeaveRequest> getPendingForTeacher(int teacherId) { return getRequestsForTeacher(teacherId).stream().filter(l -> "PENDING".equals(l.getStatus())).collect(Collectors.toList()); }
    public List<LeaveRequest> getRequestsForStudent(int studentId) { return getAllLeaveRequests().stream().filter(l -> l.getStudentId() == studentId).collect(Collectors.toList()); }

    public int getPendingCount() { return getPendingRequests().size(); }
    public int getPendingCountForTeacher(int teacherId) { return (int) getPendingForTeacher(teacherId).stream().count(); }

    public void updateStatus(int leaveId, String status) {
        List<LeaveRequest> list = getAllLeaveRequests();
        list.stream().filter(l -> l.getLeaveId() == leaveId).findFirst().ifPresent(l -> {
            l.setStatus(status);

            // 🧠 Nexus Intelligence: Notify student about leave status change
            try {
                String statusEmoji = status.equalsIgnoreCase("APPROVED") ? "✅" : (status.equalsIgnoreCase("REJECTED") ? "❌" : "⏳");
                String message = statusEmoji + " Your leave application from " + l.getFromDate() + " to " + l.getToDate() + " has been marked as: " + status;
                notificationService.dispatchToStudent(l.getStudentId(), "LEAVE", message);
            } catch (Exception e) {
                System.out.println("⚠️ Leave Status Notification Silent Error: " + e.getMessage());
            }
        });
        saveAll(list);
    }

    public void submitLeave(int studentId, String studentName, String department, String fromDate, String toDate, String reason, int teacherId) {
        List<LeaveRequest> all = getAllLeaveRequests();
        int newId = all.stream().mapToInt(LeaveRequest::getLeaveId).max().orElse(0) + 1;
        all.add(new LeaveRequest(newId, studentId, studentName, department, fromDate, toDate, reason, "PENDING", teacherId));
        saveAll(all);

        // 🧠 Nexus Intelligence: Notify teacher about new leave request
        try {
            if (teacherId > 0) {
                String message = "📅 New leave request from " + studentName + " (" + fromDate + " to " + toDate + "). Reason: " + reason;
                notificationService.dispatchToTeacher(teacherId, "LEAVE", studentName, message);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Leave Request Notification Silent Error: " + e.getMessage());
        }
    }

    public void deleteLeaveRequest(int leaveId) {
        List<LeaveRequest> list = getAllLeaveRequests();
        list.removeIf(l -> l.getLeaveId() == leaveId);
        saveAll(list);
    }

    public void updateLeaveRequest(int leaveId, String department, String fromDate, String toDate, String reason, int teacherId) {
        List<LeaveRequest> list = getAllLeaveRequests();
        for (LeaveRequest l : list) {
            if (l.getLeaveId() == leaveId) {
                l.setDepartment(department);
                l.setFromDate(fromDate);
                l.setToDate(toDate);
                l.setReason(reason);
                l.setTeacherId(teacherId);
                l.setStatus("PENDING");
                break;
            }
        }
        saveAll(list);
    }

    private void saveAll(List<LeaveRequest> list) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(getWritePath("leave.txt"), false))) {
            for (LeaveRequest l : list) {
                w.write(l.getLeaveId() + "|" + l.getStudentId() + "|" + l.getStudentName() + "|" + l.getDepartment() + "|" + l.getFromDate() + "|" + l.getToDate() + "|" + l.getReason() + "|" + l.getStatus() + "|" + l.getTeacherId());
                w.newLine();
            }
        } catch (IOException e) { System.out.println("Error saving leave.txt: " + e.getMessage()); }
    }
}