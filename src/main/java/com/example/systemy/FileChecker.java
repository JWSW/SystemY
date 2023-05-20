package com.example.systemy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileChecker extends Thread {
    private final String[] directories;

    public FileChecker(String... directories) {
        this.directories = directories;
    }

    @Override
    public void run() {
        try {
            for (String directory : directories) {
                Process process = Runtime.getRuntime().exec("lsof +D " + directory);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    // Process the open file information here
                    System.out.println("Open file: " + line);
                    // Implement your lock or unlock logic here based on the file information
                    // For example, you can use Java's File class to modify file permissions
                }
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}