package com.example.systemy;

import java.io.*;
import java.net.*;
import java.util.*;

public class Node {
    private int nodeId;
    private String ipAddress;
    private int port;
    private int successorId;
    private int predecessorId;
    private Map<String, String> files;

    public Node(int nodeId, String ipAddress, int port) {
        this.nodeId = nodeId;
        this.ipAddress = ipAddress;
        this.port = port;
        this.successorId = nodeId;
        this.predecessorId = nodeId;
        this.files = new HashMap<>();
    }

    public int getNodeId() {
        return nodeId;
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
