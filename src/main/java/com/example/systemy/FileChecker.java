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
                        if (line.contains("nano")) {
                            String[] tokens = line.trim().split("\\s+");
                            String pid = tokens[1];
                            System.out.println("PID of file being edited: " + pid);


                            // Retrieve the filename from the PID
                            String fileName = getFileNameFromPid(pid);
                            System.out.println(fileName + " is being edited");

                            // Implement your lock or unlock logic here based on the PID
                        }
                    }

                    reader.close();

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getFileNameFromPid(String pid) throws IOException {
        System.out.println("Getting filename for PID: " + pid);

        Process process = Runtime.getRuntime().exec("ps -p " + pid + " -o cmd= | awk -F ' ' '{print $NF}' | sed 's:^.*/::'");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        String fileName = "";

        while ((line = reader.readLine()) != null) {
            fileName = line;
        }

        reader.close();

        System.out.println("Retrieved filename: " + fileName);

        return fileName;
    }


}
