package com.codeandcoffee.attendance_simple.service;

import com.codeandcoffee.attendance_simple.model.Teacher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class TeacherService {

    // ✅ Target ফোল্ডারের পাথ (যাতে রিস্টার্ট ছাড়াই ওয়েবসাইটে আপডেট হয়)
    private String getTargetPath(String filename) {
        try {
            return new ClassPathResource("data/" + filename).getFile().getAbsolutePath();
        } catch (IOException e) {
            return "target/classes/data/" + filename;
        }
    }

    // ✅ Src ফোল্ডারের পাথ (যাতে ডাটা পার্মানেন্টলি সেভ থাকে)
    private String getSrcPath(String filename) {
        try {
            String targetPath = new ClassPathResource("data/" + filename).getFile().getAbsolutePath();
            return targetPath.replace("target\\classes", "src\\main\\resources")
                    .replace("target/classes", "src/main/resources");
        } catch (IOException e) {
            return "src/main/resources/data/" + filename;
        }
    }

    // 📸 IMAGE PATH LOGIC (Target & Src)
    public File getTeacherImageFile(int id) {
        try {
            String targetPath = new ClassPathResource("static/").getFile().getAbsolutePath();
            String srcPathStr = targetPath.replace("target\\classes", "src\\main\\resources")
                    .replace("target/classes", "src/main/resources");
            File dir = new File(srcPathStr, "teachers");
            if (!dir.exists()) dir.mkdirs();
            return new File(dir, id + ".jpg");
        } catch (Exception e) {
            return new File("src/main/resources/static/teachers/" + id + ".jpg");
        }
    }

    public File getTeacherImageTargetFile(int id) {
        try {
            File dir = new File(new ClassPathResource("static/").getFile().getAbsolutePath(), "teachers");
            if (!dir.exists()) dir.mkdirs();
            return new File(dir, id + ".jpg");
        } catch (Exception e) {
            return new File("target/classes/static/teachers/" + id + ".jpg");
        }
    }

    private void saveTeacherImage(int teacherId, MultipartFile image) {
        if (image == null || image.isEmpty() || image.getSize() == 0) return;
        try {
            File srcFile = getTeacherImageFile(teacherId);
            try (InputStream is = image.getInputStream()) {
                Files.copy(is, srcFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            File targetFile = getTeacherImageTargetFile(teacherId);
            Files.copy(srcFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✅ Image Saved Successfully!");
        } catch (Exception e) {
            System.out.println("❌ Error saving image: " + e.getMessage());
        }
    }

    public List<Teacher> getAllTeachers() {
        List<Teacher> teachers = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource("data/teacher.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 5) {
                    try {
                        String email = parts.length > 5 ? parts[5].trim() : "";
                        teachers.add(new Teacher(
                                Integer.parseInt(parts[0].trim()), parts[1].trim(),
                                parts[2].trim(), parts[3].trim(), parts[4].trim(), email
                        ));
                    } catch (NumberFormatException e) { }
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Error reading teacher.txt: " + e.getMessage());
        }
        return teachers;
    }

    public Teacher getTeacherById(int id) {
        return getAllTeachers().stream().filter(t -> t.getTeacherId() == id).findFirst().orElse(null);
    }

    public Teacher getTeacherByName(String name) {
        if (name == null) return null;
        return getAllTeachers().stream().filter(t -> t.getName().equalsIgnoreCase(name.trim())).findFirst().orElse(null);
    }

    public int getTotalTeachers() {
        return getAllTeachers().size();
    }

    public void addTeacher(Teacher teacher, MultipartFile image) {
        int newId = getAllTeachers().stream().mapToInt(Teacher::getTeacherId).max().orElse(0) + 1;
        String email = teacher.getEmail() != null ? teacher.getEmail() : "";
        String dataLine = newId + "|" + teacher.getName() + "|" + teacher.getDepartment() + "|" + teacher.getSubject() + "|" + teacher.getDesignation() + "|" + email;

        // ১. Target এ সেভ করা
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getTargetPath("teacher.txt"), true))) {
            writer.newLine();
            writer.write(dataLine);
        } catch (IOException e) {}

        // ২. Src এ সেভ করা
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getSrcPath("teacher.txt"), true))) {
            writer.newLine();
            writer.write(dataLine);
        } catch (IOException e) {}

        saveTeacherImage(newId, image);
    }

    public void editTeacher(int id, String name, String department, String subject, String designation, String email, MultipartFile image) {
        List<Teacher> all = getAllTeachers();
        for (Teacher t : all) {
            if (t.getTeacherId() == id) {
                t.setName(name); t.setDepartment(department);
                t.setSubject(subject); t.setDesignation(designation);
                t.setEmail(email);
                break;
            }
        }
        saveAllTeachers(all);
        saveTeacherImage(id, image);
    }

    public void removeTeacher(int id) {
        List<Teacher> all = getAllTeachers();
        all.removeIf(t -> t.getTeacherId() == id);
        saveAllTeachers(all);

        File imgFile = getTeacherImageFile(id);
        if (imgFile.exists()) imgFile.delete();

        File targetImgFile = getTeacherImageTargetFile(id);
        if (targetImgFile.exists()) targetImgFile.delete();
    }

    private void saveAllTeachers(List<Teacher> teachers) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getTargetPath("teacher.txt"), false))) {
            for (Teacher t : teachers) {
                String email = t.getEmail() != null ? t.getEmail() : "";
                writer.write(t.getTeacherId() + "|" + t.getName() + "|" + t.getDepartment() + "|" + t.getSubject() + "|" + t.getDesignation() + "|" + email);
                writer.newLine();
            }
        } catch (IOException e) {}

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getSrcPath("teacher.txt"), false))) {
            for (Teacher t : teachers) {
                String email = t.getEmail() != null ? t.getEmail() : "";
                writer.write(t.getTeacherId() + "|" + t.getName() + "|" + t.getDepartment() + "|" + t.getSubject() + "|" + t.getDesignation() + "|" + email);
                writer.newLine();
            }
        } catch (IOException e) {}
    }
}