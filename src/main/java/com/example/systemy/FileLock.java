package com.example.systemy;

import java.io.Serializable;


public class FileLock implements Serializable {

    private static final long serialVersionUID = 1L;
    private String fileName;

    public FileLock(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
    public void FileLockRequest(String fileName) {
        this.fileName = fileName;
    }

}
