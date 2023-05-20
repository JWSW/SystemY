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
            while (true) {
            for (String directory : directories) {
                 Process process = Runtime.getRuntime().exec("lsof +D " + directory);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 String line;
                 boolean isBeingEdited = false;
                    while ((line = reader.readLine()) != null) {
                        // Check if the line contains the file information
                        String[] tokens = line.split("\\s+");
                        String pid = tokens[1];

                        // Retrieve the filename from the PID
                        String fileName = getFileNameFromPid(pid);
                        System.out.println(line);
                        //System.out.println("Open file: " + fileName);
                    }

                    reader.close();

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getFileNameFromPid(String pid) throws IOException {
        Process process = Runtime.getRuntime().exec("ls -l /proc/" + pid + "/fd");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        String fileName = "";

        while ((line = reader.readLine()) != null) {
            if (line.contains("->")) {
                String[] tokens = line.split("->");
                fileName = tokens[1].trim();
                break;
            }
        }

        reader.close();

        return fileName;
    }
}
