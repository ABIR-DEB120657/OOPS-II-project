package com.codeandcoffee.attendance_simple.service;

import com.codeandcoffee.attendance_simple.model.Student;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudentService {

    public StudentService() {}

    private String getTargetPath(String filename) {
        try {
            return new ClassPathResource("data/" + filename).getFile().getAbsolutePath();
        } catch (IOException e) {
            return "src/main/resources/data/" + filename;
        }
    }

    private String getSrcPath(String filename) {
        try {
            String targetPath = new ClassPathResource("data/" + filename).getFile().getAbsolutePath();
            return targetPath.replace("target\\classes", "src\\main\\resources")
                    .replace("target/classes", "src/main/resources");
        } catch (IOException e) {
            return "src/main/resources/data/" + filename;
        }
    }

    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource("data/student.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1); // -1 ensures empty trailing parts are captured
                if (parts.length >= 6) {
                    try {
                        int rowId       = Integer.parseInt(parts[0].trim());
                        String stdId    = parts[1].trim();

                        // ✅ student.txt থেকে ডিপার্টমেন্ট এবং ব্যাচ আলাদা ইনডেক্সে রিড করা হচ্ছে
                        String department = parts[2].trim();
                        if (department.isEmpty()) department = "CSE"; // Fallback default

                        String batch = parts[3].trim();
                        if (batch.isEmpty()) batch = "18th"; // Fallback default

                        String fullName = parts[4].trim();
                        String email    = parts[5].trim();

                        // ✅ ব্লাড ফাইন্ডারের ডেটা রিড
                        String bloodGroup = parts.length > 6 ? parts[6].trim() : "";
                        String phone      = parts.length > 7 ? parts[7].trim() : "";
                        String address    = parts.length > 8 ? parts[8].trim() : "";

                        // ✅ Student অবজেক্ট তৈরি (এখন ডাইনামিক ডিপার্টমেন্ট এবং ব্যাচ পাস হচ্ছে)
                        students.add(new Student(rowId, stdId, fullName, email, department, batch, bloodGroup, phone, address));
                    } catch (NumberFormatException e) {}
                }
            }
            reader.close();
        } catch (IOException e) { }
        return students;
    }

    public Student getStudentByStudentId(String studentId) {
        return getAllStudents().stream().filter(s -> s.getStudentId().equals(studentId)).findFirst().orElse(null);
    }

    public Student getStudentById(int id) {
        return getAllStudents().stream().filter(s -> s.getId() == id).findFirst().orElse(null);
    }

    public void addStudent(Student student) {
        List<Student> students = getAllStudents();
        int nextRowId = students.stream().mapToInt(Student::getId).max().orElse(0) + 1;
        student.setId(nextRowId);
        saveAllStudents(students);
    }

    public void removeStudent(int id) {
        List<Student> students = getAllStudents();
        if (students.removeIf(s -> s.getId() == id)) {
            saveAllStudents(students);
        }
    }

    // ✅ Update existing student (Used by BloodService)
    public void updateStudent(Student updatedStudent) {
        List<Student> students = getAllStudents();
        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).getId() == updatedStudent.getId()) {
                students.set(i, updatedStudent);
                break;
            }
        }
        saveAllStudents(students);
    }

    private void saveAllStudents(List<Student> students) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getTargetPath("student.txt"), false))) {
            for (Student s : students) writeStudentLine(writer, s);
        } catch (IOException e) { }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getSrcPath("student.txt"), false))) {
            for (Student s : students) writeStudentLine(writer, s);
        } catch (IOException e) { }
    }

    // ✅ Writes line ensuring new columns are appended safely
    private void writeStudentLine(BufferedWriter writer, Student s) throws IOException {
        String bg = s.getBloodGroup() != null ? s.getBloodGroup() : "";
        String ph = s.getPhone() != null ? s.getPhone() : "";
        String ad = s.getAddress() != null ? s.getAddress() : "";

        // ✅ সেভ করার সময়ও ডাইনামিক ডিপার্টমেন্ট এবং ব্যাচ আলাদা পাইপ দিয়ে রাইট হবে
        String dept = s.getDepartment() != null ? s.getDepartment() : "CSE";
        String bch = s.getBatch() != null ? s.getBatch() : "18th";

        // Format: id|studentId|department|batch|name|email|bloodGroup|phone|address
        writer.write(s.getId() + "|" + s.getStudentId() + "|" + dept + "|" + bch + "|" + s.getName() + "|" + s.getEmail() + "|" + bg + "|" + ph + "|" + ad);
        writer.newLine();
    }

    public int getTotalStudents() {
        return getAllStudents().size();
    }
}