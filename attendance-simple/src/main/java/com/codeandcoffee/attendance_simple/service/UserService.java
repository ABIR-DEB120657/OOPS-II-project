package com.codeandcoffee.attendance_simple.service;

import com.codeandcoffee.attendance_simple.model.User;
import com.codeandcoffee.attendance_simple.util.DataPathConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private DataPathConfig dataPathConfig;

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String filePath = dataPathConfig.getPath("users.txt");
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 4) {
                    String secAnswer = parts.length >= 5 ? parts[4].trim() : "";
                    String email = parts.length >= 6 ? parts[5].trim() : ""; // ✅ ৬ নাম্বার ঘর থেকে ইমেইল রিড করা

                    users.add(new User(
                            parts[0].trim(),
                            parts[1].trim(),
                            parts[2].trim(),
                            parts[3].trim(),
                            secAnswer,
                            email // ✅ মডেল এ সেট করা হলো
                    ));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading users.txt: " + e.getMessage());
        }
        return users;
    }

    public User authenticate(String username, String password) {
        return getAllUsers().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username.trim())
                        && u.getPassword().equals(password))
                .findFirst()
                .orElse(null);
    }

    public User findByUsername(String username) {
        return getAllUsers().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst()
                .orElse(null);
    }

    public boolean verifySecurityAnswer(String username, String answer) {
        User user = findByUsername(username);
        if (user == null) return false;
        return user.getSecurityAnswer().equalsIgnoreCase(answer.trim());
    }

    public boolean resetPassword(String username, String newPassword) {
        List<User> users = getAllUsers();
        boolean found = false;
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(username.trim())) {
                u.setPassword(newPassword);
                found = true;
                break;
            }
        }
        if (found) {
            saveAllUsers(users);
        }
        return found;
    }

    public void addUser(User user) {
        List<User> users = getAllUsers();
        if (users.stream().noneMatch(u -> u.getUsername().equalsIgnoreCase(user.getUsername().trim()))) {
            users.add(user);
            saveAllUsers(users);
        }
    }

    public void removeUserByLinkedId(String role, String linkedId) {
        List<User> users = getAllUsers();
        users.removeIf(u -> u.getRole().equals(role) && u.getLinkedId().equalsIgnoreCase(linkedId.trim()));
        saveAllUsers(users);
    }

    // ✅ এডিট করার সময় নাম এবং ইমেইল দুটোই আপডেট করার নতুন মেথড
    public void updateTeacherUser(String oldName, String newName, String newEmail) {
        List<User> users = getAllUsers();
        for (User u : users) {
            if (u.getRole().equals("TEACHER") && u.getLinkedId().equalsIgnoreCase(oldName.trim())) {
                u.setUsername(newName); // ইউজারনেমও নতুন নাম হবে
                u.setLinkedId(newName);  // ফুল নেমও নতুন নাম হবে
                u.setEmail(newEmail);    // ✅ ইমেইল আপডেট হবে
            }
        }
        saveAllUsers(users);
    }

    private void saveAllUsers(List<User> users) {
        String filePath = dataPathConfig.getPath("users.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
            for (User u : users) {
                // ✅ শেষের ঘরে ইমেইল সহ রাইট করা হচ্ছে
                writer.write(
                        u.getUsername() + "|" +
                                u.getPassword() + "|" +
                                u.getRole() + "|" +
                                u.getLinkedId() + "|" +
                                (u.getSecurityAnswer() != null ? u.getSecurityAnswer() : "") + "|" +
                                (u.getEmail() != null ? u.getEmail() : "")
                );
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving users.txt: " + e.getMessage());
        }
    }
}