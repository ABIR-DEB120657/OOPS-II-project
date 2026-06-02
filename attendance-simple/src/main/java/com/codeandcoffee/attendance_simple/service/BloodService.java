package com.codeandcoffee.attendance_simple.service;

import com.codeandcoffee.attendance_simple.model.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BloodService {

    @Autowired
    private StudentService studentService;

    public static class BloodDonor {
        public String name, phone, email, bloodGroup, address, date;
        public BloodDonor(String n, String p, String e, String b, String a, String d) {
            this.name = n; this.phone = p; this.email = e;
            this.bloodGroup = b; this.address = a; this.date = d;
        }
        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
        public String getBloodGroup() { return bloodGroup; }
        public String getAddress() { return address; }
    }

    public static class BloodBroadcast {
        // 🔥 FIXED: Added 'contactNumber' explicitly to match HTML template
        public String bloodGroup, bags, time, hospital, contact, contactNumber, senderName, date;

        public BloodBroadcast(String bg, String b, String t, String h, String c, String sn, String d) {
            this.bloodGroup = bg; this.bags = b; this.time = t;
            this.hospital = h; this.contact = c; this.contactNumber = c; // Mapped 'contact' to 'contactNumber'
            this.senderName = sn; this.date = d;
        }

        public String getBloodGroup() { return bloodGroup; }
        public String getBags() { return bags; }
        public String getTime() { return time; }
        public String getHospital() { return hospital; }
        public String getContact() { return contact; }
        // 🔥 FIXED: Added getter for contactNumber
        public String getContactNumber() { return contactNumber; }
        public String getSenderName() { return senderName; }
    }

    private String getWritePath(String filename) {
        try { return new ClassPathResource("data/" + filename).getFile().getAbsolutePath(); }
        catch (IOException e) { return "src/main/resources/data/" + filename; }
    }

    public void registerGeneralDonor(String name, String phone, String email, String bloodGroup, String address) {
        if (studentService != null) {
            Student s = studentService.getAllStudents().stream().filter(st -> st.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
            if (s != null) {
                s.setBloodGroup(bloodGroup);
                s.setPhone(phone);
                s.setAddress(address);
                studentService.updateStudent(s);
                return;
            }
        }
        try (BufferedWriter w = new BufferedWriter(new FileWriter(getWritePath("blood_directory.txt"), true))) {
            w.write(name + "|" + phone + "|" + email + "|" + bloodGroup + "|" + address + "|" + LocalDate.now());
            w.newLine();
        } catch (IOException e) {}
    }

    public List<BloodDonor> getAllGeneralDonors() {
        List<BloodDonor> list = new ArrayList<>();
        File file = new File(getWritePath("blood_directory.txt"));
        if (file.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = r.readLine()) != null) {
                    String[] p = line.split("\\|");
                    if (p.length >= 6) list.add(new BloodDonor(p[0], p[1], p[2], p[3], p[4], p[5]));
                }
            } catch (Exception e) {}
        }
        if (studentService != null) {
            for (Student s : studentService.getAllStudents()) {
                if (s.getBloodGroup() != null && !s.getBloodGroup().isEmpty()) {
                    boolean exists = list.stream().anyMatch(d -> d.name.equalsIgnoreCase(s.getName()));
                    if (!exists) {
                        list.add(new BloodDonor(s.getName(), s.getPhone(), s.getEmail(), s.getBloodGroup(), s.getAddress(), "Permanent"));
                    }
                }
            }
        }
        return list;
    }

    public void deleteGeneralDonor(String name) {
        if (studentService != null) {
            Student s = studentService.getAllStudents().stream().filter(st -> st.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
            if (s != null && s.getBloodGroup() != null && !s.getBloodGroup().isEmpty()) {
                s.setBloodGroup("");
                s.setPhone("");
                s.setAddress("");
                studentService.updateStudent(s);
            }
        }
        List<BloodDonor> list = new ArrayList<>();
        File file = new File(getWritePath("blood_directory.txt"));
        if (file.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = r.readLine()) != null) {
                    String[] p = line.split("\\|");
                    if (p.length >= 6) list.add(new BloodDonor(p[0], p[1], p[2], p[3], p[4], p[5]));
                }
            } catch (Exception e) {}
        }
        list.removeIf(d -> d.name.equalsIgnoreCase(name));
        try (BufferedWriter w = new BufferedWriter(new FileWriter(getWritePath("blood_directory.txt"), false))) {
            for (BloodDonor d : list) {
                w.write(d.name + "|" + d.phone + "|" + d.email + "|" + d.bloodGroup + "|" + d.address + "|" + d.date);
                w.newLine();
            }
        } catch (IOException e) {}
    }

    public void registerEmergencyDonor(String name, String phone, String email, String bloodGroup, String address) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(getWritePath("emergency_blood.txt"), true))) {
            w.write(name + "|" + phone + "|" + email + "|" + bloodGroup + "|" + address + "|" + LocalDate.now().toString());
            w.newLine();
        } catch (IOException e) {}
    }

    public List<BloodDonor> getTodayEmergencyDonors() {
        List<BloodDonor> list = new ArrayList<>();
        File file = new File(getWritePath("emergency_blood.txt"));
        if (!file.exists()) return list;
        String today = LocalDate.now().toString();
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length >= 6 && p[5].equals(today)) list.add(new BloodDonor(p[0], p[1], p[2], p[3], p[4], p[5]));
            }
        } catch (Exception e) {}
        return list;
    }

    public void deleteEmergencyDonor(String name) {
        List<BloodDonor> list = getTodayEmergencyDonors();
        list.removeIf(d -> d.name.equalsIgnoreCase(name));
        try (BufferedWriter w = new BufferedWriter(new FileWriter(getWritePath("emergency_blood.txt"), false))) {
            for (BloodDonor d : list) {
                w.write(d.name + "|" + d.phone + "|" + d.email + "|" + d.bloodGroup + "|" + d.address + "|" + d.date);
                w.newLine();
            }
        } catch (IOException e) {}
    }

    public void saveBroadcast(String bg, String bags, String time, String hospital, String contact, String sender) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(getWritePath("blood_broadcasts.txt"), true))) {
            w.write(bg + "|" + bags + "|" + time + "|" + hospital + "|" + contact + "|" + sender + "|" + LocalDate.now().toString());
            w.newLine();
        } catch (IOException e) {}
    }

    public List<BloodBroadcast> getTodayBroadcasts() {
        List<BloodBroadcast> list = new ArrayList<>();
        File file = new File(getWritePath("blood_broadcasts.txt"));
        if (!file.exists()) return list;
        String today = LocalDate.now().toString();
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length >= 7 && p[6].equals(today)) list.add(new BloodBroadcast(p[0], p[1], p[2], p[3], p[4], p[5], p[6]));
            }
        } catch (Exception e) {}
        return list;
    }

    public void updateBroadcast(String sn, String bg, String bags, String time, String hospital, String contact) {
        List<BloodBroadcast> list = getTodayBroadcasts();
        for (BloodBroadcast b : list) {
            if (b.senderName.equalsIgnoreCase(sn)) {
                b.bloodGroup = bg; b.bags = bags; b.time = time; b.hospital = hospital; b.contact = contact; b.contactNumber = contact;
            }
        }
        try (BufferedWriter w = new BufferedWriter(new FileWriter(getWritePath("blood_broadcasts.txt"), false))) {
            for (BloodBroadcast b : list) {
                w.write(b.bloodGroup + "|" + b.bags + "|" + b.time + "|" + b.hospital + "|" + b.contact + "|" + b.senderName + "|" + b.date);
                w.newLine();
            }
        } catch (IOException e) {}
    }
}