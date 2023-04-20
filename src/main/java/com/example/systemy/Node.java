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

    public void startServer() {
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new NodeHandler(clientSocket, this)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void joinNetwork(Node existingNode) throws IOException {
        Socket socket = new Socket(existingNode.getIpAddress(), existingNode.getPort());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        out.writeUTF("findSuccessor");
        out.writeInt(nodeId);
        out.writeUTF(ipAddress);
        out.writeInt(port);
        out.flush();

        int successorId = in.readInt();
        String successorIpAddress = in.readUTF();
        int successorPort = in.readInt();

        this.successorId = successorId;

        // Notify successor to update predecessor
        socket = new Socket(successorIpAddress, successorPort);
        out = new DataOutputStream(socket.getOutputStream());
        out.writeUTF("notify");
        out.writeInt(nodeId);
        out.writeUTF(ipAddress);
        out.writeInt(port);
        out.flush();
    }
    public int getHash(String value) {
        int max = Integer.MAX_VALUE;
        int min = Integer.MIN_VALUE + 1; // add 1 to avoid overflow when calculating the absolute value

        int hash = (value.hashCode() + max) * (32768 / Math.abs(max) + Math.abs(min));
        hash = Math.abs(hash) % 32768; // map the result to the range (0, 32768)

        return hash;
    }
}
