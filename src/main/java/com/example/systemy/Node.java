package com.example.systemy;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private String baseURL = "http://172.27.0.5:8080/requestName";
    ObjectMapper objectMapper = new ObjectMapper(); // or any other JSON serializer
    private ArrayList<String> fileArray = new ArrayList<String>();



    TimerCallback callback = new TimerCallback() {
        @Override
        public void onTimerFinished(String position) throws JsonProcessingException {
            System.out.println(position + " node offline.");
            Nodefailure(position);
        }
    };

    CountdownTimer countdownTimerPrevious = new CountdownTimer(25, callback, "Previous");
    CountdownTimer countdownTimerNext = new CountdownTimer(25, callback, "Next");
    private boolean nextTimerStopped = false;
    private boolean previousTimerStopped = false;


    public Node() {
    }

    public Node(String nodeName, String ipAddress) throws Exception { // Constructor
//        ServerSocket serverSocket = new ServerSocket(uniPort, 0, InetAddress.getByName("localhost"));
//        if(!serverSocket.isClosed()){
//            killProcess();
//        }
        unicastReceiver = new UnicastReceiver(uniPort);
        unicastHeartbeatPrevious = new UnicastReceiver(heartbeatPortPrevious);
        unicastHeartbeatNext = new UnicastReceiver(heartbeatPortNext);
        previousHeartbeatSender = new HeartbeatSender(previousIP, currentID, heartbeatPortPrevious);
        nextHeartbeatSender = new HeartbeatSender(nextIP, currentID, heartbeatPortNext);
        watchDirectory = new WatchDirectory();

        this.nodeName = nodeName;
        this.ipAddress = ipAddress;
        currentID = getHash(nodeName);
        previousHeartbeatSender.setCurrentID(currentID);
        nextHeartbeatSender.setCurrentID(currentID);

        unicastReceiver.setObserver(this);          // Add the observers
        unicastHeartbeatPrevious.setObserver(this);
        unicastHeartbeatNext.setObserver(this);
        watchDirectory.setObserver(this);

        if(!(previousID ==0)) {
            countdownTimerPrevious.start();
            previousHeartbeatSender.start();
            System.out.println("Previous timer started at Init()");
        }else{
            previousTimerStopped = true;
        }
        if(!(nextID==39999)) {
            countdownTimerNext.start();
            nextHeartbeatSender.start();
            System.out.println("Next timer started at Init()");
        }else{
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

//        File myFile = new File("/home/Dist/SystemY/nodeFiles");
//        if(myFile.createNewFile()){
//            System.out.println("File created: " + myFile.getName());
//        }else{
//            System.out.println("File already exists.");
//        }
//        if (myFile.mkdir()) {
//            System.out.println("Directory created: " + myFile.getName());
//        } else {
//            System.out.println("Directory already exists.");
//        }
        // Get the directory to search
        File directory = new File("/home/Dist/SystemY/nodeFiles");

        // Get the list of files in the directory
        File[] files = directory.listFiles();

        // Loop over the files in the list
        for (File file : files) {
            // Check if the file is a regular file (not a directory)
            if (file.isFile()) {
                // Do something with the file
                System.out.println("File found: " + file.getName());
                fileArray.add(file.getName());
            }
        }
//        File myFile2 = new File(fileTwo);
//        if (myFile2.createNewFile()) {
//            System.out.println("File created: " + myFile2.getName());
//        } else {
//            System.out.println("File already exists.");
//        }
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setNextIP(String nextIP) throws UnknownHostException {
        this.nextIP = nextIP;
        nextHeartbeatSender.setSendingIP(nextIP);
        if(!nextTimerStopped) {
            countdownTimerNext.reset();     // We reset the countdown timer that checks if the node is down
            System.out.println("Next timer has been reset.");
        }else{
            countdownTimerNext.start();
            nextHeartbeatSender.start();
            System.out.println("Next timer has been started.");
            nextTimerStopped = false;
        }
    }

    public void setPreviousIP(String previousIP) throws UnknownHostException {
        this.previousIP = previousIP;
        previousHeartbeatSender.setSendingIP(previousIP);
        if(!previousTimerStopped) {
            countdownTimerPrevious.reset();     // We reset the countdown timer that checks if the node is down
            System.out.println("Previous timer has been reset.");
        }else{
            countdownTimerPrevious.start();
            previousHeartbeatSender.start();
            System.out.println("Previous timer has been started.");
            previousTimerStopped = false;
        }
    }

    private void Nodefailure(String position) throws JsonProcessingException {
        HttpClient client = HttpClient.newHttpClient();
        String json;
        Integer id;
        if(position.equals("Next")){
            //json = objectMapper.writeValueAsString(nextID);
            id = nextID;
            nextHeartbeatSender.stopSending();
            nextTimerStopped = true;
        }else{
            //json = objectMapper.writeValueAsString(previousID);
            id = previousID;
            previousHeartbeatSender.stopSending();
            previousTimerStopped = true;
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseURL + "/" + id + "/get" + position))
                //.header("Content-Type", "application/json")
                .GET()//HttpRequest.BodyPublishers.noBody())//ofString(json))//"{nodeName:" + node.getNodeName() + "ipAddress:" + node.getIpAddress() + "}"))
                .build();
        try{
            System.out.println("Sending request to get new neighbour.");
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
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

//    public void shutDown() throws IOException {
//        unicast("Next" + nextID + "," + nextIP + "," + previousID,previousIP,uniPort); // Send next node parameters to previous node
//        unicast("Previous" + previousID + "," + previousIP + "," + nextID,nextIP,uniPort); // Send previous node parameters to next node
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(baseURL + "/" + currentID + "/removeNode"))
//                //.header("Content-Type", "application/json")
//                .POST(HttpRequest.BodyPublishers.noBody())//ofString(json))//"{nodeName:" + node.getNodeName() + "ipAddress:" + node.getIpAddress() + "}"))
//                .build();
//    }

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
        if ((currentID < hash && hash < nextID) || (nextID<currentID && hash>currentID)) { // Ring topology: if we are the biggest hashID, our nextID is the smallest hashID
            System.out.println("Registered as nextID");
            nextID = hash;
            setNextIP(ipAddress); // This function changes everything that needs to be changed when changing neighbours IP
            response = "Next," + currentID + "," + this.ipAddress + "," + nextID; //The message to send as reply
            unicast(response, ipAddress, port);
        } else if ((previousID < hash && hash < currentID) || (previousID>currentID && hash < currentID)) { // Ring topology: if we are the smallest hashID, our previousID is the biggest hashID
            System.out.println("Registered as previousID");
            previousID = hash;
            setPreviousIP(ipAddress); // This function changes everything that needs to be changed when changing neighbours IP
            response = "Previous," + currentID + "," + this.ipAddress + "," + previousID; //The message to send as reply
            unicast(response, ipAddress, port);
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
            previousID = Integer.parseInt(otherNodeID); // If we receive a reply that sais we are the other node its next,
            setPreviousIP(otherNodeIP);                 // than that node is our previous
            System.out.println("Set as previousID.");
        }else if(Objects.equals(position,"Previous")){ // If we receive a reply that sais we are the other node its previous,
            nextID = Integer.parseInt(otherNodeID);       // than that node is our next
            setNextIP(otherNodeIP);
            System.out.println("Set as nextID.");
        }else if(Integer.parseInt(position)<2){ // If there is only 1 node, set own ID as neighbours.
            if(!nextTimerStopped) {
                nextTimerStopped = true;
                countdownTimerNext.stop();
            }
            if(!previousTimerStopped) {
                previousTimerStopped = true;
                countdownTimerPrevious.stop();
            }

        }else{
            if(Integer.parseInt(position)==previousID && countdownTimerPrevious.isRunning){ // If we receive a packet containing the previousID, it is pinging
                countdownTimerPrevious.reset();         // to say it is still alive
                System.out.println("Previous timer reset because of ping.");
            }else if(Integer.parseInt(position)==nextID && countdownTimerNext.isRunning){ // If we receive a packet containing the nextID, it is pinging
                countdownTimerNext.reset();               // to say it is still alive
                System.out.println("Next timer reset because of ping.");
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
            serverSocket.close(); // Close the socket to free the port
            serverSocket2.close();
            serverSocket3.close();
            System.out.println("Process on port " + uniPort + " has been killed.");
            System.out.println("Process on port " + heartbeatPortNext + " has been killed.");
            System.out.println("Process on port " + heartbeatPortPrevious + " has been killed.");
        } catch (IOException e) {
            // An exception is thrown if the port is already in use
            System.err.println("Unable to kill process on port " + uniPort + ": " + e.getMessage());
            System.err.println("Unable to kill process on port " + heartbeatPortNext + ": " + e.getMessage());
            System.err.println("Unable to kill process on port " + heartbeatPortPrevious + ": " + e.getMessage());
        }
    }

    public void FileEventHandler(String fileName){
        fileArray.add(fileName);
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
                        callback.onTimerFinished(position);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
            }, seconds * 1000L);
        }

        public void stop(){
            isRunning = false;
            timer.cancel();
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

