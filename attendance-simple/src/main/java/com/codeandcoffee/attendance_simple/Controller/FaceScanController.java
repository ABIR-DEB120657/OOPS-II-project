package com.codeandcoffee.attendance_simple.controller;

import com.codeandcoffee.attendance_simple.model.FaceScanResponse;
import com.codeandcoffee.attendance_simple.service.FaceRecognitionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/attendance")
public class FaceScanController {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    /**
     * POST /attendance/face-scan
     *
     * Request JSON:
     * {
     *   "imageData": "data:image/jpeg;base64,/9j/4AAQ...",
     *   "date":       "2026-05-24",
     *   "courseCode": "CSE-4203"
     * }
     *
     * Response JSON: FaceScanResponse
     */
    @PostMapping("/face-scan")
    public ResponseEntity<?> handleFaceScan(
            @RequestBody Map<String, String> payload,
            HttpSession session) {

        // ── 1. Session check: only teachers can scan ──────────
        Object teacherId = session.getAttribute("teacherId");
        if (teacherId == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "Unauthorized: Teacher login required."
                    ));
        }

        // ── 2. Payload validation ─────────────────────────────
        String imageData  = payload.get("imageData");
        String date       = payload.get("date");
        String courseCode = payload.get("courseCode");

        if (imageData == null || imageData.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "Missing imageData in request."
                    ));
        }

        // date & courseCode are optional for the scan itself
        // but log them for audit purposes
        System.out.printf("[FaceScan] Teacher=%s | Date=%s | Course=%s%n",
                teacherId, date, courseCode);

        // ── 3. Delegate to service ────────────────────────────
        FaceScanResponse response =
                faceRecognitionService.matchFace(imageData);

        // ── 4. Return appropriate HTTP status ─────────────────
        HttpStatus status = response.isSuccess()
                ? HttpStatus.OK
                : HttpStatus.OK; // 200 always; JS checks response.success

        return ResponseEntity.status(status).body(response);
    }
}