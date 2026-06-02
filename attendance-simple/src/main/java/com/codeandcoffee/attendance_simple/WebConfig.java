package com.codeandcoffee.attendance_simple;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // প্রজেক্টের রুট ডিরেক্টরি ডিটেক্ট করা
        String userDir = System.getProperty("user.dir");
        if (!userDir.endsWith("attendance-simple") && new File(userDir + "/attendance-simple").exists()) {
            userDir = userDir + "/attendance-simple";
        }

        // assignments ফোল্ডারের সঠিক পাথ তৈরি
        String uploadPath = new File(userDir + "/src/main/resources/static/assignments/").getAbsolutePath();

        // রিসোর্স হ্যান্ডলার ম্যাপিং রেজিস্টার করা
        registry.addResourceHandler("/assignments/**")
                .addResourceLocations("file:" + uploadPath + "/")
                .addResourceLocations("classpath:/static/assignments/");

        System.out.println("✅ Static Routing Active for Assignments Folder: " + uploadPath);
    }
}