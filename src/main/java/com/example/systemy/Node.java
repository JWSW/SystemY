package com.example.systemy;

import java.io.*;
import java.net.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

@Data
@AllArgsConstructor
public class Node {
    @Autowired
    private MulticastReceive multicastReceive;

    private String nodeName;
    private String ipAddress;
    private int uniPort = 4555;
    private int currentID;
    private int nextID = 39999;
    private int previousID = 0;
    private static DatagramSocket socket = null;
    private String file1 = "file1.txt";
    private String fileTwo = "file2.txt";
    protected byte[] buf = new byte[256];
    private UnicastReceiver unicastReceiver = new UnicastReceiver(uniPort);

//    private Map<String, String> files;

    public Node() {
    }

    public Node(String nodeName, String ipAddress) throws IOException {
        this.nodeName = nodeName;
        this.ipAddress = ipAddress;
        currentID = getHash(nodeName);
        Thread receiverThread = new Thread(unicastReceiver);
        receiverThread.start();
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
        String message = nodeName + "," + ipAddress;
        multicast(message);
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getNextID() {
        return nextID;
    }

    public void setNextID(int successorId) {
        this.nextID = successorId;
    }

    public int getPreviousID() {
        return previousID;
    }

    public void setPreviousID(int predecessorId) {
        this.previousID = predecessorId;
    }


    public void multicast(String multicastMessage) throws IOException {
        DatagramSocket socket;
        InetAddress group;
        socket = new DatagramSocket();
        group = InetAddress.getByName("230.0.0.0");
        buf = multicastMessage.getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
        socket.send(packet);
        socket.close();
    }

//    public void addFile(String fileName, String owner) {
//        files.put(fileName, owner);
//    }

    public String getFileOwner(String fileName) {
        return fileName;
    }

//    public void removeFile(String fileName) {
//        files.remove(fileName);
//    }

    @Override
    public String toString() {
        return "Node{" +
                "nodeName='" + nodeName + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", NextID=" + nextID +
                ", PreviousID=" + previousID +
                '}';
    }

    public void handlePacket(String packet) throws IOException {
        String[] parts = packet.split(","); // split the string at the space character
        String hostname = parts[0];
        String ipAddress = parts[1];
        String response;
        int port = 4555;
        int hash = getHash(hostname);
        if (currentID < hash && hash < nextID) {
            nextID = hash;
            response = currentID + "," + nextID;
            unicast(response, ipAddress, port);
        } else if (previousID < hash && hash < currentID) {
            previousID = hash;
            response = currentID + "," + previousID;
            unicast(response, ipAddress, port);
        }


    }

    public void unicast(String unicastMessage, String ipAddress, int port) throws IOException {
        DatagramSocket socket;
        InetAddress address;
        socket = new DatagramSocket();
        address = InetAddress.getByName(ipAddress);
        buf = unicastMessage.getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        socket.send(packet);
        socket.close();
    }


    public int getHash(String name) {
        int max = Integer.MAX_VALUE;
        int min = Integer.MIN_VALUE + 1; // add 1 to avoid overflow when calculating the absolute value

        int hash = (name.hashCode() + max) * (32768 / Math.abs(max) + Math.abs(min));
        hash = Math.abs(hash) % 32768; // map the result to the range (0, 32768)

        return hash;
    }

    private class UnicastReceiver implements Runnable {
        private DatagramSocket socket;
        private int port;

        public UnicastReceiver(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                // Create a new DatagramSocket bound to the specified port
                socket = new DatagramSocket(port);

                // Create a buffer to store the incoming message
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // Receive a unicast message
                socket.receive(packet);

                // Print the received message
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received message: " + receivedMessage);

                // Do something with the received message here

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Close the socket when done
                if (socket != null) {
                    socket.close();
                }
            }
        }
    }
}

