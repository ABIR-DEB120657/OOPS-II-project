package com.codeandcoffee.attendance_simple.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DataPathConfig {

    @Value("${app.data.path}")
    private String dataPath;

    public String getPath(String filename) {
        // ✅ trailing slash নিশ্চিত করো
        if (!dataPath.endsWith("/") && !dataPath.endsWith("\\")) {
            dataPath = dataPath + "/";
        }
        return dataPath + filename;
    }
}