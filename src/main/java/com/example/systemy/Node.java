package com.example.systemy;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class Node implements UnicastObserver{

    private String nodeName;
    private String ipAddress;
    private int uniPort = 55525;
    private int heartbeatPort = 55520;
    private int currentID;
    private int nextID = 39999;
    private String nextIP = "";
    private int previousID = 0;
    private String previousIP = "";
    private static DatagramSocket socket = null;
    private String file1 = "file1.txt";
    private String fileTwo = "file2.txt";
    protected byte[] buf = new byte[256];
    private UnicastReceiver unicastReceiver = new UnicastReceiver(uniPort);
    private UnicastReceiver unicastHeartbeat = new UnicastReceiver(heartbeatPort);
    private HeartbeatSender heartbeatSender = new HeartbeatSender(previousIP,nextIP, heartbeatPort, currentID);
    private String baseURL = "http://172.27.0.5:8080/requestName";
    ObjectMapper objectMapper = new ObjectMapper(); // or any other JSON serializer



    TimerCallback callback = new TimerCallback() {
        @Override
        public void onTimerFinished(String position) throws JsonProcessingException {
            System.out.println(position + " node dead.");
            Nodefailure(position);
        }
    };

    CountdownTimer countdownTimerPrevious = new CountdownTimer(25, callback, "Previous");
    CountdownTimer countdownTimerNext = new CountdownTimer(25, callback, "Next");



    public Node() {
    }

    public Node(String nodeName, String ipAddress) throws IOException {
        this.nodeName = nodeName;
        this.ipAddress = ipAddress;
        currentID = getHash(nodeName);
        unicastReceiver.setObserver(this);
        countdownTimerPrevious.start();
        countdownTimerNext.start();
        heartbeatSender.start();
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

    private void Nodefailure(String position) throws JsonProcessingException {
        HttpClient client = HttpClient.newHttpClient();
        String json;
        if(position.equals("Next")){
            json = objectMapper.writeValueAsString(nextID);
        }else{
            json = objectMapper.writeValueAsString(previousID);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseURL + "/get" + position))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))//"{nodeName:" + node.getNodeName() + "ipAddress:" + node.getIpAddress() + "}"))
                .build();
        try{
            System.out.println("Sending request to get new neighbour.");
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response: " + response.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
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
        System.out.println("Processing multicast packet: " + hash + ", " + ipAddress);
        System.out.println("NextID: " + nextID);
        System.out.println("PreviousID: " + previousID);
        if (currentID < hash && hash <= nextID) {
            System.out.println("Registered as nextID");
            nextID = hash;
            response = "Next," + currentID + "," + ipAddress + "," + nextID;
            unicast(response, ipAddress, port);
            heartbeatSender.setNextIP(nextIP);
        } else if (previousID <= hash && hash < currentID) {
            System.out.println("Registered as previousID");
            previousID = hash;
            response = "Previous," + currentID + "," + ipAddress + "," + previousID;
            unicast(response, ipAddress, port);
            heartbeatSender.setNextIP(previousIP);
        }


    }

    public void unicastHandlePacket(String packet) throws IOException {
        String otherNodeID = "";
        String otherNodeIP = "";
        String myID = "";
        String[] parts = packet.split(","); // split the string at the space character
        String position = parts[0];
        if(parts.length>1) {
            otherNodeID = parts[1];
            otherNodeIP = parts[2];
            myID = parts[3];
        }
        String response;
        if(Objects.equals(position, "Next")){
            previousID = Integer.parseInt(otherNodeID);
            previousIP = otherNodeIP;
            System.out.println("Set as previousID.");
        }else if(Objects.equals(position,"Previous")){
            nextID = Integer.parseInt(otherNodeID);
            nextIP = otherNodeIP;
            System.out.println("Set as nextID.");
        }else if(Integer.parseInt(position)<2){
            nextID = currentID;
            previousID = currentID;
        }else{
            if(Integer.parseInt(position)==previousID){
                countdownTimerPrevious.reset();
            }else if(Integer.parseInt(position)==nextID){
                countdownTimerNext.reset();
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



    /*This function is used to see if the neighbouring nodes are still alive, with a countdown timer to take action if a
    it takes to long for the nodes to give sign of life.
     */


    public class CountdownTimer {
        private Timer timer;
        private int seconds;
        private TimerCallback callback;
        private String position;

        public CountdownTimer(int seconds, TimerCallback callback, String position) {
            this.seconds = seconds;
            this.callback = callback;
            this.timer = new Timer();
            this.position = position;
        }

        public void start() {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        callback.onTimerFinished(position);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
            }, seconds * 1000L);
        }

        public void reset() {
            timer.cancel();
            timer = new Timer();
        }
    }

    public interface TimerCallback {
        void onTimerFinished(String position) throws JsonProcessingException;
    }



    /*This function is used to check for incoming UDP packets. It runs continuously in search of new packets.*/


    private class UnicastReceiver implements Runnable {
        private DatagramSocket socket;

        public UnicastReceiver(int port) {
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
                while (true) {
                    // Create a buffer to store the incoming message
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);


                    // Receive a unicast message
                    socket.receive(packet);

                    // Print the received message
                    String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Received unicast message: " + receivedMessage);

                    //Notify the observer
                    if (observer != null) {
                        observer.onMessageReceived(receivedMessage);
                    }

                    if ("end".equals(receivedMessage)) {
                        break;
                    }
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

