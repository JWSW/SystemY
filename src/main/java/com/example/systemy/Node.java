package com.example.systemy;

import java.io.*;
import java.net.*;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Data
@AllArgsConstructor
@Entity
@SpringBootApplication
public class Node {
    @Id
    private String nodeName;
    private String ipAddress;
    private int successorId;
    private int predecessorId;
    private String file1 = "file1.txt";
    private String fileTwo = "file2.txt";
//    private Map<String, String> files;

    public Node() {
    }

    public Node(String nodeName, String ipAddress) throws IOException {
        this.nodeName = nodeName;
        this.ipAddress = ipAddress;
        try {
            File file = new File(file1);
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            File file2 = new File(fileTwo);
            // if file doesnt exists, then create it
            if (!file2.exists()) {
                file2.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getNodeName() {
        return nodeName;
    }

    public String getIpAddress() {
        return ipAddress;
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


    public String getFileOwner(String fileName) {
        return fileName;
    }


    @Override
    public String toString() {
        return "Node{" +
                "nodeName='" + nodeName + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }


}
