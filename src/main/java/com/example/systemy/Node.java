package com.example.systemy;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class Node implements Observer {

    private String nodeName;
    private String ipAddress;
    private int uniPort = 55525;
    private int heartbeatPortPrevious = 55520;
    private int heartbeatPortNext = 55521;
    private int currentID;
    private int nextID = 39999;
    private String nextIP = "";
    private int previousID = 0;
    private String previousIP = "";
    private int maxNodes = 10;
    private int amountOfNodes = 1;
    private static DatagramSocket socket = null;
    private Map<File,String> fileMap;
    private WatchDirectory watchDirectory;
    private String fileTest = "fileTest.txt";
    private String fileTwo = "file2.txt";
    protected byte[] buf = new byte[256];
    private UnicastReceiver unicastReceiver;// = new UnicastReceiver(uniPort);
    private UnicastReceiver unicastHeartbeatPrevious;// = new UnicastReceiver(heartbeatPortPrevious);
    private UnicastReceiver unicastHeartbeatNext;// = new UnicastReceiver(heartbeatPortNext);
    private HeartbeatSender previousHeartbeatSender;// = new HeartbeatSender(previousIP, currentID, heartbeatPortPrevious);
    private HeartbeatSender nextHeartbeatSender;// = new HeartbeatSender(nextIP, currentID, heartbeatPortNext);
    private TCPReceiever tcpReceiever;
    private String baseURL = "http://172.27.0.5:8080/requestName";
    private ObjectMapper objectMapper = new ObjectMapper(); // or any other JSON serializer
    private Map<Integer,String> fileArray = new ConcurrentHashMap<>();
    private Map<String, Map<Integer,String>> ownerMap = new ConcurrentHashMap<>();



    TimerCallback callback = new TimerCallback() {
        @Override
        public void onTimerFinished(String position) throws JsonProcessingException {
            System.out.println(position + " node offline.");
            Nodefailure(position);
        }
    };

    private CountdownTimer countdownTimerPrevious = new CountdownTimer(25, callback, "Previous");
    private CountdownTimer countdownTimerNext = new CountdownTimer(25, callback, "Next");
    private boolean nextTimerStopped = false;
    private boolean previousTimerStopped = false;
    private int tcpPort = 45612;


    public Node() {
    }

    public Node(String nodeName, String ipAddress) throws Exception { // Constructor
        unicastReceiver = new UnicastReceiver(uniPort);
        unicastHeartbeatPrevious = new UnicastReceiver(heartbeatPortPrevious);
        unicastHeartbeatNext = new UnicastReceiver(heartbeatPortNext);
        previousHeartbeatSender = new HeartbeatSender(previousIP, currentID, heartbeatPortPrevious);
        nextHeartbeatSender = new HeartbeatSender(nextIP, currentID, heartbeatPortNext);
        watchDirectory = new WatchDirectory();
        tcpReceiever = new TCPReceiever(tcpPort);

        this.nodeName = nodeName;
        this.ipAddress = ipAddress;
        currentID = getHash(nodeName);
        previousHeartbeatSender.setCurrentID(currentID);
        nextHeartbeatSender.setCurrentID(currentID);

        unicastReceiver.setObserver(this);          // Add the observers
        unicastHeartbeatPrevious.setObserver(this);
        unicastHeartbeatNext.setObserver(this);
        watchDirectory.setObserver(this);
        tcpReceiever.setObserver(this);

        if(!(previousID ==0)) {
            countdownTimerPrevious.start();
            previousHeartbeatSender.start();
            System.out.println("Previous timer started at Init()");
        }else{
            countdownTimerPrevious.stop();
            previousTimerStopped = true;
        }
        if(!(nextID==39999)) {
            countdownTimerNext.start();
            nextHeartbeatSender.start();
            System.out.println("Next timer started at Init()");
        }else{
            countdownTimerNext.stop();
            nextTimerStopped = true;
        }

        watchDirectory.start();
        Thread receiverThreadHeartbeatPrevious = new Thread(unicastHeartbeatPrevious);
        Thread receiverThreadHeartbeatNext = new Thread(unicastHeartbeatNext);
        Thread receiverThread = new Thread(unicastReceiver);
        receiverThreadHeartbeatPrevious.start();
        receiverThreadHeartbeatNext.start();
        receiverThread.start();

        String message = nodeName + "," + ipAddress;
        System.out.println("Send multicast message.");
        multicast(message);

        searchFiles();
        notifyFiles();
    }

    /* Dit is voor lab 5 ook*/
    public void searchFiles() throws IOException {
        // Get the directory to search
        File directory = new File("/home/Dist/SystemY/nodeFiles");
//
//        // Get the list of files in the directory
        File[] files = directory.listFiles();

        // Loop over the files in the list
        for (File file : files) {
            // Check if the file is a regular file (not a directory)
            if (file.isFile()) {
                // Do something with the file
                System.out.println("File found: " + file.getName());
                fileArray.put(getHash(file.getName()),file.getName());
//                notifyNamingServer(getHash(file.getName()));
            }
        }
    }

    public void notifyFiles() throws IOException {
        for(Integer fileHash : fileArray.keySet()){
            notifyNamingServer(fileHash);
        }
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setNextIP(String nextIP) throws UnknownHostException {
        this.nextIP = nextIP;
        nextHeartbeatSender.stop();
        if(!nextTimerStopped) {
            countdownTimerNext.reset();     // We reset the countdown timer that checks if the node is down
            System.out.println("Next timer has been reset.");
            if(!nextHeartbeatSender.isAlive()){
                nextHeartbeatSender = new HeartbeatSender(nextIP, currentID, heartbeatPortNext);
                nextHeartbeatSender.start();
            }
        }else{
            countdownTimerNext.start();
            System.out.println("Next timer has been started.");
            nextHeartbeatSender = new HeartbeatSender(nextIP, currentID, heartbeatPortNext);
            nextHeartbeatSender.start();
            System.out.println("heartbeatSender has been started.");
            nextTimerStopped = false;
        }
    }

    public void setPreviousIP(String previousIP) throws UnknownHostException {
        this.previousIP = previousIP;
        previousHeartbeatSender.stop();
        if(!previousTimerStopped) {
            countdownTimerPrevious.reset();     // We reset the countdown timer that checks if the node is down
            System.out.println("Previous timer has been reset.");
            if(!previousHeartbeatSender.isAlive()){
                previousHeartbeatSender = new HeartbeatSender(previousIP, currentID, heartbeatPortPrevious);
                previousHeartbeatSender.start();
            }
        }else{
            countdownTimerPrevious.start();
            System.out.println("Previous timer has been started.");
            previousHeartbeatSender = new HeartbeatSender(previousIP, currentID, heartbeatPortPrevious);
            previousHeartbeatSender.start();
            System.out.println("HeartbeatSender has been started.");
            previousTimerStopped = false;
        }
    }


    /*Afblijven Abdel, dit is voor lab 5*/
    public void notifyNamingServer(Integer hash) throws IOException {
        HttpClient client = HttpClient.newHttpClient();
        String packet;
        String[] parts;
        String ownerNode;
        Integer nodeHash = 0;
        String nodeIP = "";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseURL + "/" + fileArray.get(hash) + "/getFileLocation"))
                .GET()
                .build();
        try {
            System.out.println("Sending request to get owner node of " + hash);
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response: " + response.body());

            packet = response.body();
            parts = packet.split(",");
//            nodeHash = Integer.valueOf(parts[0]);
//            nodeIP = parts[1];
//            ownerNode = packet;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
//        if(nodeHash!=currentID) {
//            unicast("filename," + fileArray.get(hash) + "," + currentID + "," + ipAddress,nodeIP,uniPort);
//            sendFile(ownerNode, hash);
//        }
    }

    /*Afblijven Abdel, dit is voor lab 5*/
    private void sendFile(String nodeParameters, int hash){
        String[] parts = nodeParameters.split(",");
        String nodeName = parts[0];
        String nodeIP = parts[1];
        try (Socket socket = new Socket(nodeIP, tcpPort);
             FileInputStream fileInputStream = new FileInputStream(fileArray.get(hash));
             OutputStream outputStream = socket.getOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();

            System.out.println("File sent successfully.");
        } catch (IOException e) {
            System.out.println("Error sending file: " + e.getMessage());
        }
    }

    private void requestRemoveNode(Integer id) throws IOException, InterruptedException {
        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create(baseURL + "/" + id + "/removeNodeByHashId"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        System.out.println("Sending request to remove offline node.");
        HttpResponse<String> response2 = HttpClient.newHttpClient().send(request2, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response: " + response2.body());
    }

        private void Nodefailure(String position) throws JsonProcessingException {
        HttpClient client = HttpClient.newHttpClient();
        String json;
        Integer id;
        if(position.equals("Next")){
            //json = objectMapper.writeValueAsString(nextID);
            id = nextID;
            countdownTimerNext.stop();
//            nextHeartbeatSender.stop();
//            nextTimerStopped = true;
        }else{
            id = previousID;
            countdownTimerPrevious.stop();
//            previousHeartbeatSender.stop();
//            previousTimerStopped = true;
        }
        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create(baseURL + "/" + id + "/get" + position))
                //.header("Content-Type", "application/json")
                .GET()//HttpRequest.BodyPublishers.noBody())//ofString(json))//"{nodeName:" + node.getNodeName() + "ipAddress:" + node.getIpAddress() + "}"))
                .build();
//        HttpRequest request2 = HttpRequest.newBuilder()
//                .uri(URI.create(baseURL + "/" + id + "/removeNodeByHashId"))
//                .GET()
//                .build();
        try{
//            System.out.println("Sending request to remove offline node.");
//            HttpResponse<String> response2 = HttpClient.newHttpClient().send(request2, HttpResponse.BodyHandlers.ofString());
//            System.out.println("Response: " + response2.body());
            requestRemoveNode(id);


            System.out.println("Sending request to get new neighbour.");
            HttpResponse<String> response = HttpClient.newHttpClient().send(request1, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response: " + response.body());

            String packet = response.body();
            String[] parts = packet.split(",");
            if(Integer.parseInt(parts[0])!=currentID) {
                if (position.equals("Next")) {
                    nextID = Integer.parseInt(parts[0]);
                    setNextIP(parts[1]);    // There are a couple things that need to be changed when changing your neighbours
                } else {                      // IP, so this function does it all together so we don't forget anything
                    previousID = Integer.parseInt(parts[0]);
                    setPreviousIP(parts[1]); // There are a couple things that need to be changed when changing your neighbours
                }                            // IP, so this function does it all together so we don't forget anything
            }else{
                System.out.println("Response was own node: " + packet + ", currentID: " + currentID);
            }
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

    public void shutDown() throws IOException, InterruptedException {
        System.out.println("Shutting down");
        if (nextID != 39999) {
            System.out.println("Sending new next node");
            unicast("Previous," + nextID + "," + nextIP + "," + previousID, previousIP, uniPort); // Send next node parameters to previous node
        }
        if (previousID != 0) {
            System.out.println("Sending new previous node");
            unicast("Next," + previousID + "," + previousIP + "," + nextID, nextIP, uniPort); // Send previous node parameters to next node
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseURL + "/" + currentID + "/removeNodeByHashId"))
                //.header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())//ofString(json))//"{nodeName:" + node.getNodeName() + "ipAddress:" + node.getIpAddress() + "}"))
                .build();
        System.out.println("Sending request to remove node");
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response: " + response.body());
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

    public synchronized void multicastHandlePacket(String packet) throws IOException {
        String[] parts = packet.split(","); // split the string at the space character
        String hostname = parts[0];
        String ipAddress = parts[1];
        String response;
        String message;
        int hash = getHash(hostname);
        System.out.println("Processing multicast packet: " + hash + ", " + ipAddress);
        if (currentID < hash && hash < nextID){// || (nextID<currentID && hash>currentID)) { // Ring topology: if we are the biggest hashID, our nextID is the smallest hashID
            nextID = hash;
            setNextIP(ipAddress); // This function changes everything that needs to be changed when changing neighbours IP
            System.out.println("Registered as nextID");
            response = "Next," + currentID + "," + this.ipAddress + "," + nextID; //The message to send as reply
            unicast(response, ipAddress, uniPort);
        } else if (previousID < hash && hash < currentID){// || (previousID>currentID && hash < currentID)) { // Ring topology: if we are the smallest hashID, our previousID is the biggest hashID
            previousID = hash;
            setPreviousIP(ipAddress); // This function changes everything that needs to be changed when changing neighbours IP
            System.out.println("Registered as previousID");
            response = "Previous," + currentID + "," + this.ipAddress + "," + previousID; //The message to send as reply
            unicast(response, ipAddress, uniPort);
        }else if(currentID < hash && hash > nextID && currentID>nextID){ // The following 'else if' statements are to be able to close the ring, the first to the last and vice versa
            nextID = hash;
            setNextIP(ipAddress); // This function changes everything that needs to be changed when changing neighbours IP
            System.out.println("Registered as nextID");
            response = "Next," + currentID + "," + this.ipAddress + "," + nextID; //The message to send as reply
            unicast(response, ipAddress, uniPort);
        }else if(currentID > hash && hash < previousID && currentID<previousID){

            previousID = hash;
            setPreviousIP(ipAddress); // This function changes everything that needs to be changed when changing neighbours IP
            System.out.println("Registered as previousID");
            response = "Previous," + currentID + "," + this.ipAddress + "," + previousID; //The message to send as reply
            unicast(response, ipAddress, uniPort);
        }else if(nextID>hash && nextID<currentID){
            nextID = hash;
            setNextIP(ipAddress); // This function changes everything that needs to be changed when changing neighbours IP
            System.out.println("Registered as nextID");
            response = "Next," + currentID + "," + this.ipAddress + "," + nextID; //The message to send as reply
            unicast(response, ipAddress, uniPort);
        }else if(hash>previousID && previousID>currentID){
            previousID = hash;
            setPreviousIP(ipAddress); // This function changes everything that needs to be changed when changing neighbours IP
            System.out.println("Registered as previousID");
            response = "Previous," + currentID + "," + this.ipAddress + "," + previousID; //The message to send as reply
            unicast(response, ipAddress, uniPort);
        }
    }

    public synchronized void unicastHandlePacket(String packet) throws IOException {
        String otherNodeID = "";
        String otherNodeIP = "";
        String myID = "";
        Map<Integer, String> nodeMap = new ConcurrentHashMap<>();
        String[] parts = packet.split(","); // split the string at the space character
        String position = parts[0];
        if (parts.length > 1) {
            otherNodeID = parts[1];
            otherNodeIP = parts[2];
            myID = parts[3];
        }
        String response;
        if (Objects.equals(position, "Next")) {
            setPreviousIP(otherNodeIP);                 // If we receive a reply that sais we are the other node its next,
            previousID = Integer.parseInt(otherNodeID); // than that node is our previous
            System.out.println("Set as previousID.");
            if (amountOfNodes == 2) {                       // If there are only 2 nodes, then they are both each others previous and next node
                amountOfNodes = 3;
                setNextIP(otherNodeIP);
                nextID = Integer.parseInt(otherNodeID);
                System.out.println("Also set as nextID.");
                response = "Next," + currentID + "," + this.ipAddress + "," + previousID; //The message to send as reply
                unicast(response, otherNodeIP, uniPort);
            }
        } else if (Objects.equals(position, "Previous")) { // If we receive a reply that sais we are the other node its previous,
            setNextIP(otherNodeIP);                       // than that node is our next
            nextID = Integer.parseInt(otherNodeID);
            System.out.println("Set as nextID.");
            if (amountOfNodes == 2) {                       // If there are only 2 nodes, then they are both each others previous and next node
                amountOfNodes = 3;
                setPreviousIP(otherNodeIP);
                previousID = Integer.parseInt(otherNodeID);
                System.out.println("Also set as previousID.");
                response = "Previous," + currentID + "," + this.ipAddress + "," + previousID; //The message to send as reply
                unicast(response, otherNodeIP, uniPort);
            }
        } else if (position.equals("filename")) {
            tcpReceiever.setFileName(otherNodeID);
            nodeMap.put(Integer.valueOf(otherNodeIP), myID); // Here the variable names are not what they say they are, it is first the nodeID and then the nodeIP
        }else if(position.equals("getPreviousNeighbour")) { //If the other node (if it were woth our next and previous), it tells us to get another previous neighbour
            if(previousID==nextID) { //Dit moet in aparte if-statements gebeuren om errors te voorkomen
                String packet2 = "";
                HttpRequest request1 = HttpRequest.newBuilder()
                        .uri(URI.create(baseURL + "/" + previousID + "/getPrevious"))
                        .GET()
                        .build();
                try {
                    System.out.println("Sending request to get new neighbour.");
                    HttpResponse<String> response1 = HttpClient.newHttpClient().send(request1, HttpResponse.BodyHandlers.ofString());
                    System.out.println("Response: " + response1.body());
                    packet2 = response1.body();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                String[] parts2 = packet2.split(",");
                if (Integer.parseInt(parts[0]) != currentID) {
                    previousID = Integer.parseInt(parts2[0]);
                    setPreviousIP(parts2[1]);
                } else {
                    System.out.println("Response was own node: " + packet + ", currentID: " + currentID);
                }
            }
        } else if (position.equals("getNextNeighbour")) { //If the other node (if it were woth our next and previous), it tells us to get another next neighbour
            if(previousID==nextID) { //Dit moet in aparte if-statements gebeuren om errors te voorkomen
                String packet2 = "";
                HttpRequest request1 = HttpRequest.newBuilder()
                        .uri(URI.create(baseURL + "/" + nextID + "/getNext"))
                        .GET()
                        .build();
                try {
                    System.out.println("Sending request to get new neighbour.");
                    HttpResponse<String> response1 = HttpClient.newHttpClient().send(request1, HttpResponse.BodyHandlers.ofString());
                    System.out.println("Response: " + response1.body());
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                String[] parts2 = packet2.split(",");
                if (Integer.parseInt(parts[0]) != currentID) {
                    nextID = Integer.parseInt(parts2[0]);
                    setNextIP(parts2[1]);
                } else {
                    System.out.println("Response was own node: " + packet + ", currentID: " + currentID);
                }
            }
        }else if(Integer.parseInt(position)<2) { // If there is only 1 node, set own ID as neighbours.
            amountOfNodes = 3;
            if (!nextTimerStopped) {
                nextTimerStopped = true;
                countdownTimerNext.stop();
                System.out.println("next timer stopped");
            }
            if (!previousTimerStopped) {
                previousTimerStopped = true;
                countdownTimerPrevious.stop();
                System.out.println("previous timer stopped");
            }
        }else if(Integer.parseInt(position)==2) { // If there are only 2 nodes, set other node as both previous and next node.
            amountOfNodes = 2;
        }else if(Integer.parseInt(position)>2 && Integer.parseInt(position)<maxNodes) { // If there 4 nodes, start offline countdownTimers to maybe get connected to highest or lowest node
            amountOfNodes = 3;
            if (nextTimerStopped) {
                nextTimerStopped = false;
                countdownTimerNext.start();
                System.out.println("Next timer has started.");
            }
            if (previousTimerStopped) {
                previousTimerStopped = false;
                countdownTimerPrevious.start();
                System.out.println("Previous timer has started.");
            }
        }else{
            if((Integer.parseInt(position)==previousID) && countdownTimerPrevious.isRunning){ // If we receive a packet containing the previousID, it is pinging
                countdownTimerPrevious.reset();
                System.out.println("Previous timer reset because of ping.");
                if(previousID==nextID){
                    countdownTimerNext.reset();               // to say it is still alive
                    System.out.println("Next timer also reset because of ping.");
                }
            }else if(Integer.parseInt(position)==nextID && countdownTimerNext.isRunning) { // If we receive a packet containing the nextID, it is pinging
                countdownTimerNext.reset();               // to say it is still alive
                System.out.println("Next timer reset because of ping.");
                if(previousID==nextID){
                    countdownTimerPrevious.reset();
                    System.out.println("Previous timer also reset because of ping.");
                }
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
            ServerSocket serverSocket2 = new ServerSocket(heartbeatPortNext, 0, InetAddress.getByName("localhost"));
            ServerSocket serverSocket3 = new ServerSocket(heartbeatPortPrevious, 0, InetAddress.getByName("localhost"));
            ServerSocket serverSocket4 = new ServerSocket(tcpPort, 0, InetAddress.getByName("localhost"));
            serverSocket.close(); // Close the socket to free the port
            serverSocket2.close();
            serverSocket3.close();
            serverSocket4.close();
            System.out.println("Process on port " + uniPort + " has been killed.");
            System.out.println("Process on port " + heartbeatPortNext + " has been killed.");
            System.out.println("Process on port " + heartbeatPortPrevious + " has been killed.");
            System.out.println("Process on port " + tcpPort + " has been killed.");
        } catch (IOException e) {
            // An exception is thrown if the port is already in use
            System.err.println("Unable to kill process on port " + uniPort + ": " + e.getMessage());
            System.err.println("Unable to kill process on port " + heartbeatPortNext + ": " + e.getMessage());
            System.err.println("Unable to kill process on port " + heartbeatPortPrevious + ": " + e.getMessage());
            System.err.println("Unable to kill process on port " + tcpPort + ": " + e.getMessage());
        }
    }

    public void FileEventHandler(String fileName){
        fileArray.put(getHash(fileName),fileName);
        System.out.println("All files: " + fileArray);
    }

    @Override
    public void onMessageReceived(String type, String message) throws IOException {
        if("Unicast".equals(type)) {
            unicastHandlePacket(message);
        }else if("FileEvent".equals(type)){
            FileEventHandler(message);
        }
    }



    /*This function is used to see if the neighbouring nodes are still alive, with a countdown timer to take action if a
    it takes to long for the nodes to give sign of life.
     */


    public class CountdownTimer {
        private Timer timer;
        private int seconds;
        private TimerCallback callback;
        private String position;
        private boolean isRunning = false;
        private boolean isCanceled;

        public CountdownTimer(int seconds, TimerCallback callback, String position) {
            this.seconds = seconds;
            this.callback = callback;
            this.timer = new Timer();
            this.position = position;
        }

        public void start() {
            isRunning = true;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        synchronized (CountdownTimer.this) {
                            if (!isCanceled) {
                                callback.onTimerFinished(position);
                            }
                        }
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
            }, seconds * 1000L);
        }

        public void stop(){
            isCanceled = true;
            timer.cancel();
            timer = new Timer();
            isCanceled = false;
        }

        public void reset() {
            timer.cancel();
            timer = new Timer();
            start();
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
                socket = new DatagramSocket(port);
            } catch (SocketException e) {
                System.err.println("Port " + port + " is already in use");
                killProcess();
            }
        }

        private Observer observer;

        public void setObserver(Observer observer) {
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
                        observer.onMessageReceived("Unicast",receivedMessage);
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


    public class WatchDirectory extends Thread {
        private WatchService watchService;
        private Observer observer;

        public void setObserver(Observer observer) {
            this.observer = observer;
        }

        public void run() {
            // Get the directory to watch
            Path path = Paths.get("/home/Dist/SystemY/nodeFiles");

            // Create a WatchService object
            try {
                watchService = FileSystems.getDefault().newWatchService();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Register the directory with the watch service for file creation events
            try {
                path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Start an infinite loop to wait for new files
            while (true) {
                // Wait for the watch service to receive a new event
                WatchKey key = null;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Loop over the events in the key
                for (WatchEvent<?> event : key.pollEvents()) {
                    // Check if the event is a create event
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        // Get the file name from the event
                        Path fileName = (Path) event.context();

                        // Do something with the new file
                        System.out.println("New file created: " + fileName.toString());
                        if (observer != null) {
                            try {
                                observer.onMessageReceived("FileEvent",fileName.toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                // Reset the key for the next set of events
                boolean valid = key.reset();

                // If the key is no longer valid, break out of the loop
                if (!valid) {
                    System.out.println("Unvalid watch key!");
                    break;
                }
            }
        }
    }
}

