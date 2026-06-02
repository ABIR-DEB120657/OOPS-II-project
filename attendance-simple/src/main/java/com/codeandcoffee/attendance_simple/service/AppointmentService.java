package com.codeandcoffee.attendance_simple.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    public static class Appointment {
        public String id, studentName, teacherName, topic, time, status, date;

        public Appointment(String id, String sName, String tName, String topic, String time, String status, String date) {
            this.id = id; this.studentName = sName; this.teacherName = tName;
            this.topic = topic; this.time = time; this.status = status; this.date = date;
        }
        public String getId() { return id; }
        public String getStudentName() { return studentName; }
        public String getTeacherName() { return teacherName; }
        public String getTopic() { return topic; }
        public String getTime() { return time; }
        public String getStatus() { return status; }
        public String getDate() { return date; }

        // 🔥 FIXED: HTML পেজ থেকে duration কল করলে যেন time রিটার্ন করে, তার জন্য এই মেথড যোগ করা হলো
        public String getDuration() { return time; }
    }

    private String getWritePath() {
        try { return new ClassPathResource("data/appointments.txt").getFile().getAbsolutePath(); }
        catch (IOException e) { return "src/main/resources/data/appointments.txt"; }
    }

    public void saveAppointment(String studentName, String teacherName, String topic, String time) {
        String id = UUID.randomUUID().toString().substring(0, 6);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(getWritePath(), true))) {
            // UI তে কালার ঠিকমতো আসার জন্য 'PENDING' বড় হাতের করে দিলাম
            w.write(id + "|" + studentName + "|" + teacherName + "|" + topic + "|" + time + "|PENDING|" + LocalDate.now());
            w.newLine();
        } catch (IOException e) {}
    }

    public List<Appointment> getAppointmentsForStudent(String studentName) {
        List<Appointment> list = new ArrayList<>();
        File file = new File(getWritePath());
        if (!file.exists()) return list;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\\|");
                // 🔥 FIXED: Teacher Portal থেকে studentName ফাঁকা পাঠালে যেন সব অ্যাপয়েন্টমেন্ট রিটার্ন করে
                if (p.length >= 7 && (studentName == null || studentName.isEmpty() || p[1].equalsIgnoreCase(studentName))) {
                    list.add(new Appointment(p[0], p[1], p[2], p[3], p[4], p[5], p[6]));
                }
            }
        } catch (Exception e) {}
        return list;
    }

    // ✅ Delete Logic returning the deleted appointment for Email Notification
    public Appointment deleteAppointment(String id, String studentName) {
        List<Appointment> list = new ArrayList<>();
        File file = new File(getWritePath());
        Appointment deletedAppt = null;

        if (!file.exists()) return null;

        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length >= 7) {
                    if (p[0].equals(id) && p[1].equalsIgnoreCase(studentName)) {
                        deletedAppt = new Appointment(p[0], p[1], p[2], p[3], p[4], p[5], p[6]);
                    } else {
                        list.add(new Appointment(p[0], p[1], p[2], p[3], p[4], p[5], p[6]));
                    }
                }
            }
        } catch (Exception e) {}

        if (deletedAppt != null) {
            try (BufferedWriter w = new BufferedWriter(new FileWriter(getWritePath(), false))) {
                for (Appointment a : list) {
                    w.write(a.id + "|" + a.studentName + "|" + a.teacherName + "|" + a.topic + "|" + a.time + "|" + a.status + "|" + a.date);
                    w.newLine();
                }
            } catch (IOException e) {}
        }
        return deletedAppt;
    }
}