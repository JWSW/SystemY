package com.example.systemy;

import java.io.*;
import java.net.*;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Node implements UnicastObserver{

    private String nodeName;
    private String ipAddress;
    private int uniPort = 55525;
    private int currentID;
    private int nextID = 39999;
    private int previousID = 0;
    private static DatagramSocket socket = null;
    private String file1 = "file1.txt";
    private String fileTwo = "file2.txt";
    protected byte[] buf = new byte[256];
    private UnicastReceiver unicastReceiver = new UnicastReceiver(uniPort);


    public Node() {
    }

    public Node(String nodeName, String ipAddress) throws IOException {
        this.nodeName = nodeName;
        this.ipAddress = ipAddress;
        currentID = getHash(nodeName);
        unicastReceiver.setObserver(this);
        Thread receiverThread = new Thread(unicastReceiver);
        receiverThread.start();
        String message = nodeName + "," + ipAddress;
        System.out.println("Send multicast message.");
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

        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 44445);
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

    public void multicastHandlePacket(String packet) throws IOException {
        String[] parts = packet.split(","); // split the string at the space character
        String hostname = parts[0];
        String ipAddress = parts[1];
        String response;
        int port = uniPort;
        int hash = getHash(hostname);
        if (currentID < hash && hash < nextID) {
            System.out.println("Registered as nextID");
            nextID = hash;
            response = "Next," + currentID + "," + nextID;
            unicast(response, ipAddress, port);
        } else if (previousID < hash && hash < currentID) {
            System.out.println("Registered as prevID");
            previousID = hash;
            response = "Previous," + currentID + "," + previousID;
            unicast(response, ipAddress, port);
        }


    }

    public void unicastHandlePacket(String packet) throws IOException {
        String[] parts = packet.split(","); // split the string at the space character
        String position = parts[0];
        String otherNodeID = parts[1];
        String myID = parts[2];
        String response;
        if(Objects.equals(position, "Next")){
            previousID = Integer.parseInt(otherNodeID);
            System.out.println("Set as previousID.");
        }else if(Objects.equals(position,"Previous")){
            nextID = Integer.parseInt(otherNodeID);
            System.out.println("Set as nextID.");
        }else{
            if(Integer.parseInt(position)<2){
                nextID = currentID;
                previousID = currentID;
            }
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

    private static int findFreePort() {
        int port = 0;
        // For ServerSocket port number 0 means that the port number is automatically allocated.
        try (ServerSocket socket = new ServerSocket(0)) {
            // Disable timeout and reuse address after closing the socket.
            socket.setReuseAddress(true);
            port = socket.getLocalPort();
        } catch (IOException ignored) {}
        if (port > 0) {
            return port;
        }
        throw new RuntimeException("Could not find a free port");
    }

    public void killProcess(){
        try {
            // Try to create a server socket on the specified port
            ServerSocket serverSocket = new ServerSocket(uniPort, 0, InetAddress.getByName("localhost"));
            serverSocket.close(); // Close the socket to free the port
            System.out.println("Process on port " + uniPort + " has been killed.");
        } catch (IOException e) {
            // An exception is thrown if the port is already in use
            System.err.println("Unable to kill process on port " + uniPort + ": " + e.getMessage());
        }
    }

    @Override
    public void onMessageReceived(String message) throws IOException {
        unicastHandlePacket(message);
    }

    private class UnicastReceiver implements Runnable {
        private DatagramSocket socket;

        public UnicastReceiver(int port) {
            this.port = port;
            try {
                socket = new DatagramSocket(uniPort);
            } catch (SocketException e) {
                System.err.println("Port " + uniPort + " is already in use");
            }
        }

        private UnicastObserver observer;

        public void setObserver(UnicastObserver observer) {
            this.observer = observer;
        }


        @Override
        public void run() {
            try {

                // Create a buffer to store the incoming message
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // Receive a unicast message
                socket.receive(packet);
                System.out.println("Hij komt hier");

                // Print the received message
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received unicast message: " + receivedMessage);

                //Notify the observer
                if (observer != null) {
                    observer.onMessageReceived(receivedMessage);
                }

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

