package com.example.systemy;

import java.io.Serializable;

public class FileLock implements Serializable {

    private static final long serialVersionUID = 1L;
    private String fileName;
    private Boolean lockActive;

    public FileLock(String fileName, boolean lockActive) {
        this.fileName = fileName;
        this.lockActive = false;
    }

    public String getFileName() {
        return fileName;
    }

    public Boolean getLockActive() {
        return lockActive;
    }

    public void setLockActive(Boolean lockActive) {
        this.lockActive = lockActive;
    }
}
