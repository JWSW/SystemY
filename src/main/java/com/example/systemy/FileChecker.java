package com.example.systemy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileChecker extends Thread {
    private final String[] directories;
    Map<String, Boolean> files = new ConcurrentHashMap<>();
    private boolean isLockActive;

    private String fileName;
    public FileChecker(String... directories) {
        this.directories = directories;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Map<String, Boolean> updatedFiles = new ConcurrentHashMap<>();

                for (String directory : directories) {
                    Process process = Runtime.getRuntime().exec("lsof +D " + directory);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;

                    while ((line = reader.readLine()) != null) {
                        if (line.contains("nano")) {
                            String[] tokens = line.trim().split("\\s+");
                            String pid = tokens[1];

                            // Retrieve the filename from the PID
                            fileName = getFileNameFromPid(pid);

                            // Update the file status in the updatedFiles map
                            updatedFiles.put(fileName, true);
                        }
                    }
                    reader.close();
                }

                for (String fileName : files.keySet()) {
                    if (updatedFiles.containsKey(fileName)) {
                        // If the file is found in the updatedFiles map, it is still being edited
                        files.put(fileName, true);
                    } else {
                        // If the file is not found, it is no longer being edited
                        files.put(fileName, false);
                    }
                }

                // Remove files that are no longer being edited from the map
                files.entrySet().removeIf(entry -> !entry.getValue());
                System.out.println("files:"+files);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getFileNameFromPid(String pid) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "ps -p " + pid + " -o cmd= | awk -F ' ' '{print $NF}' | sed 's:^.*/::'");
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        String fileName = "";

        while ((line = reader.readLine()) != null) {
            fileName = line;
        }
        reader.close();
        return fileName;
    }
    public void lockFile(String filename) {
        System.out.println("File Name: " + fileName);
        String filePath = "/home/Dist/SystemY/replicatedFiles/" + fileName;
        try {
            Runtime.getRuntime().exec("chmod 000 " + filePath);
            System.out.println("File " + filename + " is locked");
            setLockActive(true);
        } catch (IOException e) {
            System.out.println("File is already being edited by another node");
        }
    }

    public void unlockFile(String filename) {
        // Change file permissions to read-write
        System.out.println("File Name: " + fileName);
        String filePath = "/home/Dist/SystemY/replicatedFiles/" + fileName;
        try {
            Runtime.getRuntime().exec("chmod 666 " + filePath);
            System.out.println("File " + filename + " is unlocked");
            setLockActive(true);

        } catch (IOException e) {
            System.out.println("Failed to unlock " + filename);
        }
    }

    public boolean isLockActive() {
        return isLockActive;
    }

    public void setLockActive(boolean active) {
        isLockActive = active;
    }

    public String getFileLockRequest() {
        return fileName;
    }
}
