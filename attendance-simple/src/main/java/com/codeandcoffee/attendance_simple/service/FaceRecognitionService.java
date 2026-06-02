package com.codeandcoffee.attendance_simple.service;

import com.codeandcoffee.attendance_simple.model.FaceScanResponse;
import com.codeandcoffee.attendance_simple.model.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

/**
 * FaceRecognitionService
 *
 * Real AI library ছাড়া smart simulation:
 *  - Incoming base64 image-এর size/checksum দিয়ে
 *    একটা deterministic index বের করে
 *  - সেই index দিয়ে student list থেকে candidate বাছে
 *  - Candidate-এর photo resources-এ আছে কিনা check করে
 *  - Photo থাকলে → matched, না থাকলে → noMatch
 *
 * Production-এ এই class-এর matchFace() method-এর body
 * replace করলেই real AI integrate হয়ে যাবে।
 */
@Service
public class FaceRecognitionService {

    private static final Logger log =
            Logger.getLogger(FaceRecognitionService.class.getName());

    // Photo folder: src/main/resources/students/
    private static final String PHOTO_BASE = "students/";

    @Autowired
    private StudentService studentService;

    /**
     * @param base64Image  canvas.toDataURL() থেকে আসা base64 string
     *                     (data:image/jpeg;base64,... prefix সহ বা ছাড়া)
     * @return FaceScanResponse
     */
    public FaceScanResponse matchFace(String base64Image) {

        if (base64Image == null || base64Image.isBlank()) {
            return FaceScanResponse.error("Empty image data received.");
        }

        try {
            // ── 1. Strip data-URL prefix if present ──────────
            String pureBase64 = base64Image;
            if (base64Image.contains(",")) {
                pureBase64 = base64Image.split(",", 2)[1];
            }

            // ── 2. Decode to bytes ────────────────────────────
            byte[] imageBytes = Base64.getDecoder().decode(pureBase64);

            if (imageBytes.length < 500) {
                return FaceScanResponse.error("Image too small or corrupted.");
            }

            // ── 3. Deterministic "fingerprint" from image bytes
            //    (real AI এর placeholder)
            int fingerprint = computeFingerprint(imageBytes);

            // ── 4. সব student load করো ───────────────────────
            List<Student> students = studentService.getAllStudents();
            if (students == null || students.isEmpty()) {
                return FaceScanResponse.noMatch();
            }

            // ── 5. Fingerprint দিয়ে candidate বাছো ───────────
            //    Same image → same fingerprint → same student (deterministic)
            int candidateIndex = Math.abs(fingerprint) % students.size();
            Student candidate  = students.get(candidateIndex);

            // ── 6. Candidate-এর photo আছে কিনা check করো ────
            boolean photoExists = doesPhotoExist(candidate.getStudentId());

            if (photoExists) {
                log.info("Face matched → " + candidate.getName()
                        + " [" + candidate.getStudentId() + "]");
                return FaceScanResponse.matched(
                        candidate.getStudentId(),
                        candidate.getName()
                );
            } else {
                // Photo নেই → fallback: প্রথম student যার photo আছে
                Student fallback = findAnyStudentWithPhoto(students);
                if (fallback != null) {
                    log.info("Fallback match → " + fallback.getName());
                    return FaceScanResponse.matched(
                            fallback.getStudentId(),
                            fallback.getName()
                    );
                }
                return FaceScanResponse.noMatch();
            }

        } catch (IllegalArgumentException e) {
            log.warning("Invalid base64 data: " + e.getMessage());
            return FaceScanResponse.error("Invalid image encoding.");
        } catch (Exception e) {
            log.severe("Face scan failed: " + e.getMessage());
            return FaceScanResponse.error("Internal scan error.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Image bytes থেকে একটা simple integer fingerprint বানাও।
     * Real AI model এর জায়গায় এটা use হচ্ছে।
     */
    private int computeFingerprint(byte[] bytes) {
        // Sample: image-এর মাঝের ৩০০ byte-এর XOR + weighted sum
        int hash = 0;
        int start = Math.max(0, bytes.length / 2 - 150);
        int end   = Math.min(bytes.length, start + 300);
        for (int i = start; i < end; i++) {
            hash = hash * 31 + (bytes[i] & 0xFF);
        }
        // Image size-ও factor হিসেবে যোগ করো
        hash ^= bytes.length;
        return hash;
    }

    /**
     * resources/students/{studentId}.jpg আছে কিনা check করো।
     */
    private boolean doesPhotoExist(String studentId) {
        try {
            ClassPathResource res =
                    new ClassPathResource(PHOTO_BASE + studentId + ".jpg");
            return res.exists();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * List-এর মধ্যে প্রথম student যার .jpg আছে তাকে return করো।
     */
    private Student findAnyStudentWithPhoto(List<Student> students) {
        for (Student s : students) {
            if (doesPhotoExist(s.getStudentId())) {
                return s;
            }
        }
        return null;
    }
}