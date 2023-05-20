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
                boolean isBeingEdited = false;
                System.out.println("test");
                while ((line = reader.readLine()) != null) {
                    // Check if the line contains the file information
                    if (line.contains("nano") || line.contains("vi") || line.contains("vim")) {
                        isBeingEdited = true;
                        break; // Exit the loop once the file is found being edited
                    }
                }
                reader.close();

                if (isBeingEdited) {
                    System.out.println("The file is being edited.");
                } else {
                    System.out.println("The file is not being edited.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}