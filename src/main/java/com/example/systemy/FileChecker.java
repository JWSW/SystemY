package com.example.systemy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileChecker extends Thread {
    private final String[] directories;
    Map<String, Boolean> files = new ConcurrentHashMap<>();
    List <String> removeList= new ArrayList<>();;
    private boolean isLockActive;

    private String fileName;
    public FileChecker(String... directories) {
        this.directories = directories;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Create a temporary map to store the updated file statuses
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
                            String fileName = getFileNameFromPid(pid);

                            // Update the file status in the temporary map
                            updatedFiles.put(fileName, true);
                        }
                    }
                    reader.close();
                }

                // Update the file statuses in the main files map
                for (Map.Entry<String, Boolean> entry : updatedFiles.entrySet()) {
                    String fileName = entry.getKey();
                        files.put(fileName, true);
                }
                for (Map.Entry<String, Boolean> entry : files.entrySet()) {
                    String fileName = entry.getKey();
                    if (!updatedFiles.containsKey(fileName)) {
                        files.remove(fileName);
                        removeList.add(fileName);
                    }
                }

                // Remove files that are no longer being edited from the map
                files.entrySet().removeIf(entry -> !entry.getValue());


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
            System.out.println(fileName);
        }
        reader.close();
        return fileName;
    }
    public void lockFile(String filename) {
        String filePath = "/home/Dist/SystemY/replicatedFiles/" + fileName;
        try {
            Runtime.getRuntime().exec("chmod 000 " + filePath);
            setLockActive(true);
        } catch (IOException e) {
            System.out.println("Failed to lock "+filename);
        }
    }

    public void unlockFile(String filename) {
        // Change file permissions to read-write
        String filePath = "/home/Dist/SystemY/replicatedFiles/" + fileName;
        try {
            Runtime.getRuntime().exec("chmod 666 " + filePath);
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

    public Map<String, Boolean> getFileLockRequest() {
        return files;
    }

    public List<String> getRemoveList() {
        return removeList;
    }

    public void clearRemoveList() {
        removeList.clear();
    }
}
