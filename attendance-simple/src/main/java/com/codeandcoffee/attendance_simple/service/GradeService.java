package com.codeandcoffee.attendance_simple.service;

import com.codeandcoffee.attendance_simple.model.Grade;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GradeService {

    // ── Write path (absolute, for saving) ──────────────────────────────────
    private String getWritePath(String filename) {
        try {
            ClassPathResource resource = new ClassPathResource("data/" + filename);
            return resource.getFile().getAbsolutePath();
        } catch (IOException e) {
            return "src/main/resources/data/" + filename;
        }
    }

    // ── Read all grades from grades.txt ─────────────────────────────────────
    // Format: studentRowId|studentName|department|courseCode|subject|credits|marks|letterGrade|gpa|semester
    public List<Grade> getAllGrades() {
        List<Grade> grades = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource("data/grades.txt");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] p = line.split("\\|", -1);
                    if (p.length >= 10) {
                        try {
                            grades.add(new Grade(
                                    Integer.parseInt(p[0].trim()),   // studentRowId (1,2,3…)
                                    p[1].trim(),                     // studentName
                                    p[2].trim(),                     // department
                                    p[3].trim(),                     // courseCode
                                    p[4].trim(),                     // subject
                                    Double.parseDouble(p[5].trim()), // credits
                                    Integer.parseInt(p[6].trim()),   // marks
                                    p[7].trim(),                     // letterGrade
                                    Double.parseDouble(p[8].trim()), // gpa
                                    p[9].trim()                      // semester
                            ));
                        } catch (NumberFormatException e) {
                            System.out.println("Skipping bad grade line: " + line);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading grades.txt: " + e.getMessage());
        }
        return grades;
    }

    // ── Save one grade (append) ─────────────────────────────────────────────
    public void saveGrade(Grade g) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(getWritePath("grades.txt"), true))) {
            writer.write(
                    g.getStudentId()   + "|" +
                            g.getStudentName() + "|" +
                            g.getDepartment()  + "|" +
                            g.getCourseCode()  + "|" +
                            g.getSubject()     + "|" +
                            g.getCredits()     + "|" +
                            g.getMarks()       + "|" +
                            g.getLetterGrade() + "|" +
                            g.getGpa()         + "|" +
                            g.getSemester()
            );
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Error writing grade: " + e.getMessage());
        }
    }

    // ── Grades by student row-id ────────────────────────────────────────────
    public List<Grade> getGradesByStudentRowId(int studentRowId) {
        return getAllGrades().stream()
                .filter(g -> g.getStudentId() == studentRowId)
                .collect(Collectors.toList());
    }

    // ── Grades grouped by semester (for one student) ────────────────────────
    public Map<String, List<Grade>> getGradesBySemester(int studentRowId) {
        List<Grade> grades = getGradesByStudentRowId(studentRowId);
        // Preserve semester insertion order
        Map<String, List<Grade>> map = new LinkedHashMap<>();
        for (Grade g : grades) {
            map.computeIfAbsent(g.getSemester(), k -> new ArrayList<>()).add(g);
        }
        return map;
    }

    // ── Class average (marks) ───────────────────────────────────────────────
    public double getClassAverage() {
        List<Grade> all = getAllGrades();
        if (all.isEmpty()) return 0.0;
        double sum = all.stream().mapToInt(Grade::getMarks).sum();
        // Average per student (not per grade record)
        Map<Integer, List<Grade>> byStudent = groupByStudent(all);
        double totalAvg = byStudent.values().stream()
                .mapToDouble(list -> list.stream().mapToInt(Grade::getMarks).average().orElse(0))
                .average()
                .orElse(0);
        return Math.round(totalAvg * 10.0) / 10.0;
    }

    // ── Top performer (student with highest avg marks) ──────────────────────
    // Returns the Grade object of their best single course (used for display)
    public Grade getTopPerformer() {
        List<Grade> all = getAllGrades();
        if (all.isEmpty()) return null;

        Map<Integer, List<Grade>> byStudent = groupByStudent(all);

        // Find student with highest CGPA
        int bestId = -1;
        double bestCGPA = -1;
        for (Map.Entry<Integer, List<Grade>> entry : byStudent.entrySet()) {
            double cgpa = calculateCGPA(entry.getValue());
            if (cgpa > bestCGPA) {
                bestCGPA = cgpa;
                bestId   = entry.getKey();
            }
        }

        if (bestId == -1) return null;

        // Return a representative Grade for that student (highest marks record)
        List<Grade> bestGrades = byStudent.get(bestId);
        Grade best = bestGrades.stream()
                .max(Comparator.comparingInt(Grade::getMarks))
                .orElse(bestGrades.get(0));

        // Patch letterGrade/gpa to reflect CGPA
        int avgMarks = (int) Math.round(
                bestGrades.stream().mapToInt(Grade::getMarks).average().orElse(0));
        return new Grade(
                best.getStudentId(),
                best.getStudentName(),
                best.getDepartment(),
                best.getCourseCode(),
                best.getSubject(),
                best.getCredits(),
                avgMarks,
                Grade.calculateLetterGrade(avgMarks),
                bestCGPA,
                best.getSemester()
        );
    }

    // ── CGPA from a list of grades ──────────────────────────────────────────
    public double calculateCGPA(List<Grade> grades) {
        if (grades.isEmpty()) return 0.0;
        double totalPoints  = 0;
        double totalCredits = 0;
        for (Grade g : grades) {
            totalPoints  += g.getGpa() * g.getCredits();
            totalCredits += g.getCredits();
        }
        if (totalCredits == 0) return 0.0;
        return Math.round((totalPoints / totalCredits) * 100.0) / 100.0;
    }

    // ── Student summaries for /grades table ─────────────────────────────────
    // One row per student, sorted by CGPA descending (highest first = Rank 1)
    public List<Map<String, Object>> getStudentSummaries() {
        List<Grade> allGrades = getAllGrades();
        Map<Integer, List<Grade>> byStudent = groupByStudent(allGrades);

        List<Map<String, Object>> summaries = new ArrayList<>();

        for (Map.Entry<Integer, List<Grade>> entry : byStudent.entrySet()) {
            List<Grade> grades = entry.getValue();
            Grade first = grades.get(0);

            double cgpa         = calculateCGPA(grades);
            double totalCredits = grades.stream().mapToDouble(Grade::getCredits).sum();
            int avgMarks        = (int) Math.round(
                    grades.stream().mapToInt(Grade::getMarks).average().orElse(0));
            String letterGrade  = Grade.calculateLetterGrade(avgMarks);

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("studentId",    first.getStudentId());   // row id (1,2,3…)
            summary.put("name",         first.getStudentName()); // name from grades.txt
            summary.put("department",   first.getDepartment());
            summary.put("cgpa",         cgpa);
            summary.put("totalCredits", totalCredits);
            summary.put("avgMarks",     avgMarks);
            summary.put("letterGrade",  letterGrade);

            summaries.add(summary);
        }

        // ✅ CGPA দিয়ে descending sort → Rank 1 = সবচেয়ে বেশি CGPA
        summaries.sort((a, b) ->
                Double.compare((double) b.get("cgpa"), (double) a.get("cgpa")));

        return summaries;
    }

    // ── Helper: group all grades by studentId ───────────────────────────────
    private Map<Integer, List<Grade>> groupByStudent(List<Grade> grades) {
        Map<Integer, List<Grade>> map = new LinkedHashMap<>();
        for (Grade g : grades) {
            map.computeIfAbsent(g.getStudentId(), k -> new ArrayList<>()).add(g);
        }
        return map;
    }
}