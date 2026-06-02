package com.codeandcoffee.attendance_simple.service;

import com.codeandcoffee.attendance_simple.model.Course;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    public List<Course> getAllCourses() {
        List<Course> courses = new ArrayList<>();

        // ✅ Classpath থেকে পড়ো — hardcoded path NO!
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("data/courses.txt")) {
            if (is == null) {
                System.out.println("❌ courses.txt not found in classpath: data/courses.txt");
                return courses;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 4) {
                    courses.add(new Course(
                            parts[0].trim(),   // CSE-1101
                            parts[1].trim(),   // Computer Fundamentals
                            parts[2].trim(),   // 1st Year - 1st Semester
                            parts[3].trim()    // T001
                    ));
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading courses.txt: " + e.getMessage());
        }

        System.out.println("✅ Total courses loaded: " + courses.size());
        return courses;
    }

    // ✅ T001, T002 দিয়ে filter
    public List<Course> getCoursesByTeacherId(String teacherId) {
        List<Course> result = getAllCourses().stream()
                .filter(c -> c.getTeacherId().equalsIgnoreCase(teacherId.trim()))
                .collect(Collectors.toList());
        System.out.println("✅ Courses for " + teacherId + ": " + result.size());
        return result;
    }
}