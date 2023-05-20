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


    private boolean isLockActive;

    public FileLock(String fileName) {
        this.fileName = fileName;
    }


    public String getFileName() {
        return fileName;
    }

    public boolean lockFile(String filename) {
        String fileName = filename.replaceAll("^\\.(.*)\\..*$", "$1");
        System.out.println("File Name: " + fileName);
        String filePath = "/home/Dist/SystemY/replicatedFiles/" + fileName;
        try {
            Runtime.getRuntime().exec("chmod 000 " + filePath);
            System.out.println("File " + filename + " is locked");
            return true;
        } catch (IOException e) {
            System.out.println("File is already being edited by another node");
            return false;
        }
    }

    public void unlockFile(String filename) {
        // Change file permissions to read-write
        String fileName = filename.replaceAll("^\\.(.*)\\..*$", "$1");
        System.out.println("File Name: " + fileName);
        String filePath = "/home/Dist/SystemY/replicatedFiles/" + fileName;
        try {
            Runtime.getRuntime().exec("chmod 666 " + filePath);
            System.out.println("File " + filename + " is unlocked");

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

}
