package com.codeandcoffee.attendance_simple.util;

import jakarta.servlet.http.HttpSession;

public class SessionUtil {

    // Login করা আছে কিনা
    public static boolean isLoggedIn(HttpSession session) {
        return session != null &&
                session.getAttribute("loggedIn") != null &&
                session.getAttribute("role") != null;
    }

    // Role check — সবসময় "role" attribute দিয়ে check করো
    public static boolean isAdmin(HttpSession session) {
        return "ADMIN".equals(session.getAttribute("role"));
    }

    public static boolean isTeacher(HttpSession session) {
        return "TEACHER".equals(session.getAttribute("role"));
    }

    public static boolean isStudent(HttpSession session) {
        return "STUDENT".equals(session.getAttribute("role"));
    }

    // ✅ Admin only — login না থাকলে /login, admin না হলে /access-denied
    public static String redirectIfNotAdmin(HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/login";
        if (!isAdmin(session))    return "redirect:/access-denied";
        return null;
    }

    // ✅ Admin অথবা Teacher — login না থাকলে /login, অন্য role হলে /access-denied
    public static String redirectIfNotAdminOrTeacher(HttpSession session) {
        if (!isLoggedIn(session))                     return "redirect:/login";
        if (!isAdmin(session) && !isTeacher(session)) return "redirect:/access-denied";
        return null;
    }
}