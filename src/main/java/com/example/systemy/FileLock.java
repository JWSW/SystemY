package com.example.systemy;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.Set;


public class FileLock implements Serializable {

    private static final long serialVersionUID = 1L;
    private String fileName;

    public FileLock(String fileName) {
        this.fileName = fileName;
    }


    public String getFileName() {
        return fileName;
    }

    public boolean lockFile(String filename) {
        String fileName = filename.replace(".swp", "");
        if (fileName.startsWith(".")) {
            fileName = fileName.substring(1);
        }
        System.out.println("File Name: " +fileName);
        Path filePath = Paths.get("/home/Dist/SystemY/replicatedFiles/"+fileName);
        Set<PosixFilePermission> permissions = new HashSet<>();
        permissions.add(PosixFilePermission.OWNER_READ);
        permissions.add(PosixFilePermission.GROUP_READ);
        permissions.add(PosixFilePermission.OTHERS_READ);
        try {
            Files.setPosixFilePermissions(filePath, permissions);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void unlockFile(String filename) {
        // Change file permissions to read-write
        Path filePath = Paths.get(filename);
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-rw-rw-");
        try {
            Files.setPosixFilePermissions(filePath, permissions);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
