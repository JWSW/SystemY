package com.example.systemy;

import java.io.*;
import java.net.*;
import java.util.*;

public class Node {
    private String nodeName;
    private String ipAddress;
    private int successorId;
    private int predecessorId;
    private String file1 = "file1.txt";
    private String fileTwo = "file2.txt";
    private Map<String, String> files;

    public Node(String nodeName, String ipAddress) throws IOException {
        this.nodeName = nodeName;
        this.ipAddress = ipAddress;
        this.successorId = 5;
        this.predecessorId = 5;
        this.files = new HashMap<>();
        try {
            File file = new File(file1);
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        try {
            File file2 = new File(fileTwo);
            // if file doesnt exists, then create it
            if (!file2.exists()) {
                file2.createNewFile();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getNodeName() {
        return nodeName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public int getSuccessorId() {
        return successorId;
    }

    public void setSuccessorId(int successorId) {
        this.successorId = successorId;
    }

    public int getPredecessorId() {
        return predecessorId;
    }

    public void setPredecessorId(int predecessorId) {
        this.predecessorId = predecessorId;
    }

    public void addFile(String fileName, String owner) {
        files.put(fileName, owner);
    }

    public String getFileOwner(String fileName) {
        return files.get(fileName);
    }

    public void removeFile(String fileName) {
        files.remove(fileName);
    }

}
