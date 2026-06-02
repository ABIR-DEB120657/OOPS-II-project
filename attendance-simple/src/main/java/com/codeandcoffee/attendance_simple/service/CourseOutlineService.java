package com.codeandcoffee.attendance_simple.service;

import com.codeandcoffee.attendance_simple.model.CourseOutline;
import com.codeandcoffee.attendance_simple.util.DataPathConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseOutlineService {

    @Autowired private DataPathConfig dataPathConfig;

    private File getUploadDirTarget() {
        try {
            String targetRoot = new ClassPathResource("").getFile().getAbsolutePath();
            File dir = new File(targetRoot + File.separator + "static" + File.separator + "outlines");
            if (!dir.exists()) dir.mkdirs();
            return dir;
        } catch (IOException e) {
            File fallback = new File("target/classes/static/outlines");
            if (!fallback.exists()) fallback.mkdirs();
            return fallback;
        }
    }

    private File getUploadDirSrc() {
        try {
            String targetRoot = new ClassPathResource("").getFile().getAbsolutePath();
            String srcDirPath = targetRoot.replace("target\\classes", "src\\main\\resources")
                    .replace("target/classes", "src/main/resources")
                    + File.separator + "static" + File.separator + "outlines";
            File srcDir = new File(srcDirPath);
            if (!srcDir.exists()) srcDir.mkdirs();
            return srcDir;
        } catch (IOException e) {
            File fallback = new File("src/main/resources/static/outlines");
            if (!fallback.exists()) fallback.mkdirs();
            return fallback;
        }
    }

    public List<CourseOutline> getAllOutlines() {
        List<CourseOutline> list = new ArrayList<>();
        File file = new File(dataPathConfig.getPath("course_outline.txt"));
        if (!file.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
                String[] p = line.split("\\|", -1);
                if (p.length >= 9) {
                    list.add(new CourseOutline(
                            Integer.parseInt(p[0].trim()),
                            p[1].trim(), p[2].trim(), p[3].trim(),
                            p[4].trim(), p[5].trim(), p[6].trim(),
                            p[7].trim(), p[8].trim()
                    ));
                }
            }
        } catch (IOException e) {
            System.out.println("⚠️ Error reading course_outline.txt");
        }
        return list;
    }

    public List<CourseOutline> getOutlinesForStudent(String department, String batch) {
        return getAllOutlines().stream().filter(o -> {
            boolean deptMatch = o.getDepartment().equalsIgnoreCase("All") || o.getDepartment().equalsIgnoreCase(department.trim());
            boolean batchMatch = o.getBatch().equalsIgnoreCase("All") || o.getBatch().equalsIgnoreCase(batch.trim());
            return deptMatch && batchMatch;
        }).collect(Collectors.toList());
    }

    public List<CourseOutline> getOutlinesByTeacher(String teacherName) {
        return getAllOutlines().stream()
                .filter(o -> o.getTeacherName().equalsIgnoreCase(teacherName.trim()))
                .collect(Collectors.toList());
    }

    public void postOutline(CourseOutline outline, MultipartFile file, String externalLink) {
        List<CourseOutline> all = getAllOutlines();
        int newId = all.stream().mapToInt(CourseOutline::getId).max().orElse(0) + 1;
        outline.setId(newId);
        outline.setPostDate(LocalDate.now().toString());

        if (file != null && !file.isEmpty()) {
            String fileName = newId + "_" + file.getOriginalFilename().replaceAll("\\s+", "_");
            try {
                Files.copy(file.getInputStream(), new File(getUploadDirSrc(), fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(file.getInputStream(), new File(getUploadDirTarget(), fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                outline.setFileOrLink(fileName);
            } catch (IOException e) {
                outline.setFileOrLink(externalLink != null ? externalLink.trim() : "");
            }
        } else {
            outline.setFileOrLink(externalLink != null ? externalLink.trim() : "");
        }

        all.add(outline);
        saveAllOutlines(all);
    }

    public void deleteOutline(int id) {
        List<CourseOutline> all = getAllOutlines();
        all.removeIf(o -> o.getId() == id);
        saveAllOutlines(all);
    }

    private void saveAllOutlines(List<CourseOutline> list) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataPathConfig.getPath("course_outline.txt"), false))) {
            for (CourseOutline o : list) {
                writer.write(o.getId() + "|" +
                        (o.getDepartment() != null ? o.getDepartment() : "") + "|" +
                        (o.getBatch() != null ? o.getBatch() : "") + "|" +
                        (o.getCourseCode() != null ? o.getCourseCode() : "") + "|" +
                        (o.getTitle() != null ? o.getTitle() : "") + "|" +
                        (o.getDescription() != null ? o.getDescription() : "") + "|" +
                        (o.getFileOrLink() != null ? o.getFileOrLink() : "") + "|" +
                        (o.getTeacherName() != null ? o.getTeacherName() : "") + "|" +
                        (o.getPostDate() != null ? o.getPostDate() : ""));
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("⚠️ Error writing inside course_outline.txt");
        }
    }
}