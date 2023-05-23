package SystemY;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import SystemY.interfaces.Observer;
import SystemY.Agents.FailureAgent;
import SystemY.Threads.HeartbeatSender;
import SystemY.Threads.WatchDirectory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.json.JSONException;
import org.json.JSONObject;


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
    private Map<String,File> fileMap = new ConcurrentHashMap<>(); // This is useless
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
    private ObjectMapper objectMapper = new ObjectMapper(); // or any other JSON serializer
    private Map<Integer,String> fileArray = new ConcurrentHashMap<>(); //This map stores the hash of the file with its corresponding filename. It stores all local files.
    private Map<String, Map<Integer,String>> ownerMap = new ConcurrentHashMap<>(); // This map stores the filename with the corresponding locations where the file is found (the node's parameters)
    private boolean filesNotified = false;
    private FailureAgent failureAgent;





    TimerCallback callback = new TimerCallback() {
        @Override
        public void onTimerFinished(String position) throws JsonProcessingException {
            System.out.println(position + " node offline.");
            Nodefailure(position);
            if (position.equals("Previous")) {
                failureAgent = new FailureAgent(previousID,currentID);
            } else {
                failureAgent = new FailureAgent(nextID,currentID);
            }
            Thread FailureAgent1 = new Thread(failureAgent);
            FailureAgent1.start();

        }
    };

    private CountdownTimer countdownTimerPrevious = new CountdownTimer(25, callback, "Previous");
    private CountdownTimer countdownTimerNext = new CountdownTimer(25, callback, "Next");
    private boolean nextTimerStopped = false;
    private boolean previousTimerStopped = false;
    private int tcpPort = 2001;
    private boolean receivingFile = false;


    public Node() {
    }

    public Node(String nodeName, String ipAddress) throws Exception { // Constructor
        unicastReceiver = new UnicastReceiver(uniPort);
        unicastHeartbeatPrevious = new UnicastReceiver(heartbeatPortPrevious);
        unicastHeartbeatNext = new UnicastReceiver(heartbeatPortNext);
        previousHeartbeatSender = new HeartbeatSender(previousIP, currentID, heartbeatPortPrevious);
        nextHeartbeatSender = new HeartbeatSender(nextIP, currentID, heartbeatPortNext);
        watchDirectory = new WatchDirectory(this);


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
        System.out.println("Ownermap begin: " + ownerMap);

        searchFiles();
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
            }
        }
    }

    public void notifyFiles(Boolean isOwnFiles) throws IOException {
        if(isOwnFiles) {
            for (Integer fileHash : fileArray.keySet()) {
                notifyNamingServer(fileArray.get(fileHash), isOwnFiles);
            }
        }else{
            for (String filename : ownerMap.keySet()) {
                if(fileArray.containsValue(filename)) {
                    notifyNamingServer(filename, true);
                }else{
                    notifyNamingServer(filename, false);
                }
            }
            //System.out.println(ownerMap);
        }
    }


    public void notifyFile(String filename, Boolean isOwnFiles) throws IOException {
        notifyNamingServer(filename, isOwnFiles);
    }

    public void setNextIP(String nextIP) throws UnknownHostException {
        this.nextIP = nextIP;
        nextHeartbeatSender.stop();
        if(!nextTimerStopped) {
            countdownTimerNext.reset();     // We reset the countdown timer that checks if the node is down
            //System.out.println("Next timer has been reset.");
            if(!nextHeartbeatSender.isAlive()){
                nextHeartbeatSender = new HeartbeatSender(nextIP, currentID, heartbeatPortNext);
                nextHeartbeatSender.start();
            }
        }else{
            countdownTimerNext.start();
            //System.out.println("Next timer has been started.");
            nextHeartbeatSender = new HeartbeatSender(nextIP, currentID, heartbeatPortNext);
            nextHeartbeatSender.start();
            //System.out.println("heartbeatSender has been started.");
            nextTimerStopped = false;
        }
    }

    public void setPreviousIP(String previousIP) throws UnknownHostException {
        this.previousIP = previousIP;
        previousHeartbeatSender.stop();
        if(!previousTimerStopped) {
            countdownTimerPrevious.reset();     // We reset the countdown timer that checks if the node is down
            //System.out.println("Previous timer has been reset.");
            if(!previousHeartbeatSender.isAlive()){
                previousHeartbeatSender = new HeartbeatSender(previousIP, currentID, heartbeatPortPrevious);
                previousHeartbeatSender.start();
            }
        }else{
            countdownTimerPrevious.start();
           // System.out.println("Previous timer has been started.");
            previousHeartbeatSender = new HeartbeatSender(previousIP, currentID, heartbeatPortPrevious);
            previousHeartbeatSender.start();
            //System.out.println("HeartbeatSender has been started.");
            previousTimerStopped = false;
        }
    }


    /*Afblijven Abdel, dit is voor lab 5*/
    public void notifyNamingServer(String filename, Boolean isOwnFiles) throws IOException {
        HttpClient client = HttpClient.newHttpClient();
        Map<Integer,String> tempMap = new ConcurrentHashMap<>();
        String packet;
        String[] parts;
        String ownerNode = "";
        Integer nodeHash = 0;
        String nodeIP = "";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseURL + "/" + filename + "/getFileLocation"))
                .GET()
                .build();
        try {
            System.out.println("Sending request to get owner node of " + filename + " with hash: " + getHash(filename));
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response: " + response.body());

            packet = response.body();
            parts = packet.split(",");
            nodeHash = Integer.valueOf(parts[0]);
            nodeIP = parts[1];
            ownerNode = packet;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        if(nodeHash!=currentID) {
            sendFile(ownerNode, filename, isOwnFiles);
            if(ownerNode.contains(filename)) {
                ownerMap.remove(filename);
            }
        }else{
            System.out.println("Node self is owner of " + filename);
            tempMap.put(currentID,ipAddress);
            ownerMap.put(filename,tempMap);
        }
    }


    /*Afblijven Abdel, dit is voor lab 5*/
    private void sendFile(String nodeParameters, String filename, Boolean isOwnFiles) throws IOException {
        String[] parts = nodeParameters.split(",");
        String nodeHash = parts[0];
        String nodeIP = parts[1];
        String directory = "";
        if(isOwnFiles) {
            directory = "/home/Dist/SystemY/nodeFiles/";
            //System.out.println(directory);
        }else{
            directory = "/home/Dist/SystemY/replicatedFiles/";
            //System.out.println(directory);
        }
        String jsonData="";

        try {
            // Read the file content
            File file = new File(directory + filename);
            byte[] fileContent = Files.readAllBytes(Paths.get(directory + filename));
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(fileContent);
            fileInputStream.close();

            // Encode the file content as Base64
            String base64Content = Base64.getEncoder().encodeToString(fileContent);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("fileData", base64Content);

            // Convert the JSON object to a string
            jsonData = jsonObject.toString();
        }catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        try {
            HttpRequest request2 = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + nodeIP + ":8081/requestNode" + "/" + filename + "/" + currentID + "/" + ipAddress + "/sendNewFile"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                    .build();
            System.out.println("Sending POST request to owner of file " + filename);
            System.out.println(request2);
            HttpResponse<String> response2 = HttpClient.newHttpClient().send(request2, HttpResponse.BodyHandlers.ofString());
            if(response2.body().isEmpty()){
                System.out.println("File is sent succesfully.");
            }else {
                System.out.println("Response: " + response2.body());
            }
        }catch (IOException | InterruptedException e) {
            System.out.println("Error sending file: " + e.getMessage());
            e.printStackTrace();
        }

        /* Here we send the locations where the file is stored to the new owner*/
        if(ownerMap.containsKey(filename)) {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonMap = objectMapper.writeValueAsString(ownerMap.get(filename));
            try {
                HttpRequest request2 = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + nodeIP + ":8081/requestNode" + "/" + filename + "/" + currentID + "/sendFileLocations"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonMap))
                        .build();
                System.out.println("Sending POST request to owner of file with all file locations: " + jsonMap);
                System.out.println(request2);
                HttpResponse<String> response2 = HttpClient.newHttpClient().send(request2, HttpResponse.BodyHandlers.ofString());
                if (response2.body().isEmpty()) {
                    System.out.println("Map is sent succesfully.");
                } else {
                    System.out.println("Response: " + response2.body());
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Error sending Map: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void setFileLocations(String filename, Integer nodeID, ConcurrentHashMap<Integer, String> locationsMap){
        if(ownerMap.containsKey(filename)){
            for(Integer id : locationsMap.keySet()) {
                if(!Objects.equals(nodeID, id)) {
                    ownerMap.get(filename).put(id, locationsMap.get(id));
                }else{
                    if(ownerMap.get(filename).containsKey(nodeID)){
                        ownerMap.get(filename).remove(nodeID);
                        System.out.println("Previous nodID is removed from file locations.");
                    }
                }
            }
            System.out.println("Ownermap new locations (if there were more than node that shut down): " + filename + " with " + ownerMap.get(filename));
        }
    }

    public void notifyLocalFiles(){
        for(Integer fileHash : fileArray.keySet()) {
            String filename = fileArray.get(fileHash);
            String packet;
            String[] parts;
            String ownerNode = "";
            Integer nodeHash = 0;
            String nodeIP = "";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseURL + "/" + filename + "/getFileLocation"))
                    .GET()
                    .build();
            try {
                System.out.println("Sending request to get owner node of " + filename + " with hash: " + getHash(filename));
                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Response: " + response.body());

                packet = response.body();
                parts = packet.split(",");
                nodeHash = Integer.valueOf(parts[0]);
                nodeIP = parts[1];
                ownerNode = packet;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            if (nodeHash != currentID) {
                // Doe hier iets dat de owner laat weten dat de local file verdwijnt van het netwerk.
                HttpRequest request2 = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + nodeIP + ":8081/requestNode" + "/" + filename + "/" + currentID + "/notifyTermination"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                try {
                    System.out.println("Sending POST to owner node of " + filename + " to notify termination.");
                    HttpResponse<String> response = HttpClient.newHttpClient().send(request2, HttpResponse.BodyHandlers.ofString());
                    System.out.println("Response: " + response.body());

                    packet = response.body();
                    parts = packet.split(",");
                    nodeHash = Integer.valueOf(parts[0]);
                    nodeIP = parts[1];
                    ownerNode = packet;
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                if (ownerNode.contains(filename)) {
                    ownerMap.remove(filename);
                }
            } else {
                System.out.println("Node self is owner of " + filename + ", so no actions are taken.");
                //Doe niets denk ik
            }
        }
    }

    public void isTerminated(String filename, Integer nodeID){
        if(ownerMap.get(filename).keySet().size()>2) { // If the node is located at more than 2 nodes (other than the node itself and the original node).
            if(ownerMap.get(filename).containsKey(nodeID)) {
                ownerMap.get(filename).remove(nodeID);
            }
        }else{
            ownerMap.remove(filename);
        }
    }

    public void deleteFile(String filename, Boolean isOwnFiles){
        File myObj;
        if(isOwnFiles) {
            myObj = new File("/home/Dist/SystemY/nodeFiles/" + filename);
        }else{
            myObj = new File("/home/Dist/SystemY/replicatedFiles/" + filename);
        }
        if (myObj.delete()) {
            System.out.println("Deleted the file: " + myObj.getName());
        } else {
            System.out.println("Failed to delete the file.");
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
        try{
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
            notifyLocalFiles();
            for(String filename : ownerMap.keySet()){
                if(fileArray.containsValue(filename)) {
                    //System.out.println(fileArray);
                    //System.out.println(filename);
                    sendFile(previousID + "," + previousIP, filename, true);
                }else{
                    sendFile(previousID + "," + previousIP, filename, false);
                    deleteFile(filename,false);
                }
            }
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
            if(!filesNotified){
                notifyFiles(true);
                filesNotified = true;
            }

        } else if (previousID < hash && hash < currentID){// || (previousID>currentID && hash < currentID)) { // Ring topology: if we are the smallest hashID, our previousID is the biggest hashID
            previousID = hash;
            setPreviousIP(ipAddress); // This function changes everything that needs to be changed when changing neighbours IP
            System.out.println("Registered as previousID");
            response = "Previous," + currentID + "," + this.ipAddress + "," + previousID; //The message to send as reply
            unicast(response, ipAddress, uniPort);
            if(!filesNotified){
                notifyFiles(true);
                filesNotified = true;
            }

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
        if(amountOfNodes>2){
            notifyFiles(false);
        }
    }

    public synchronized void unicastHandlePacket(String packet) throws IOException {
        String otherNodeID = "";
        String otherNodeIP = "";
        String myID = "";
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
                notifyFiles(true);
                filesNotified = true;
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
                notifyFiles(true);
                filesNotified = true;
            }
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
        }else if(Integer.parseInt(position)<2) { // If there is only 1 node, set own ID as neighbours (actually, the ID's are 0 for the next and 39999 for the previous).
            amountOfNodes = 3;
            if (!nextTimerStopped) {
                nextTimerStopped = true;
                countdownTimerNext.stop();
                //System.out.println("next timer stopped");
            }
            if (!previousTimerStopped) {
                previousTimerStopped = true;
                countdownTimerPrevious.stop();
                //System.out.println("previous timer stopped");
            }
        }else if(Integer.parseInt(position)==2) { // If there are only 2 nodes, set other node as both previous and next node.
            amountOfNodes = 2;
        }else if(Integer.parseInt(position)>2 && Integer.parseInt(position)<maxNodes) { // If there more than 2 nodes, start offline countdownTimers to maybe get connected to highest or lowest node
            amountOfNodes = 3;
            notifyFiles(true);
            if (nextTimerStopped) {
                nextTimerStopped = false;
                countdownTimerNext.start();
                //System.out.println("Next timer has started.");
            }
            if (previousTimerStopped) {
                previousTimerStopped = false;
                countdownTimerPrevious.start();
                //System.out.println("Previous timer has started.");
            }
        }else{
            if((Integer.parseInt(position)==previousID) && countdownTimerPrevious.isRunning){ // If we receive a packet containing the previousID, it is pinging
                countdownTimerPrevious.reset();
                //System.out.println("Previous timer reset because of ping.");
                if(previousID==nextID){
                    countdownTimerNext.reset();               // to say it is still alive
                    //System.out.println("Next timer also reset because of ping.");
                }
            }else if(Integer.parseInt(position)==nextID && countdownTimerNext.isRunning) { // If we receive a packet containing the nextID, it is pinging
                countdownTimerNext.reset();               // to say it is still alive
                //System.out.println("Next timer reset because of ping.");
                if(previousID==nextID){
                    countdownTimerPrevious.reset();
                    //System.out.println("Previous timer also reset because of ping.");
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

    public void killSpecificProcess(int port) throws IOException {
        try {
            ServerSocket serverSocket4 = new ServerSocket(port, 0, InetAddress.getByName("localhost"));
            serverSocket4.close();
            System.out.println("Process on port " + port + " has been killed.");
        }catch (IOException e) {
            // An exception is thrown if the port is already in use
            System.err.println("Unable to kill process on port " + port + ": " + e.getMessage());
        }
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
        try {
            notifyFile(fileName, true);
        }catch (IOException e) {
            System.err.println("Could not notify file " + fileName + ": " + e.getMessage());
        }
        System.out.println("All owner files: " + ownerMap);
    }

    @Override
    public void onMessageReceived(String type, String message) throws IOException {
        if("Unicast".equals(type)) {
            unicastHandlePacket(message);
        }else if("FileEvent".equals(type)){
            FileEventHandler(message);
        }
    }


    public void setFileList(Map<String, Boolean> fileList) {
    }

    public String[] getNeighbors() {
        String[] neighbors = new String[2];
        neighbors[0] = previousIP;
        neighbors[1] = nextIP;
        return neighbors;

    }

/*    public Map<String, Boolean> getFileList() {

    }*/

    public void setOwnerFile(String filename, int nodeID, String nodeIP) {
        Map<Integer,String> tempMap = new ConcurrentHashMap<>();
        if(fileArray.containsValue(filename) && (nextID!=previousID || currentID == nextID || currentID == previousID)){
            try {
                sendFile(previousID + "," + previousIP, filename, true);
            }catch (IOException e) {
                System.err.println("Could not notify file " + filename + ": " + e.getMessage());
            }
        }
        if(ownerMap.containsKey(filename)){
            tempMap = ownerMap.get(filename);
            if(!tempMap.containsKey(nodeID)) { // To prevent doubles
                tempMap.put(nodeID, nodeIP);
            }
            if(!tempMap.containsKey(currentID)) { // To check if this node is already added.
                tempMap.put(currentID,ipAddress);
            }
            ownerMap.replace(filename,tempMap);
        }else {
            tempMap.put(nodeID, nodeIP);
            if(!tempMap.containsKey(currentID)) { // To check if this node is already added.
                tempMap.put(currentID,ipAddress);
            }
            ownerMap.put(filename, tempMap);
        }
        System.out.println("Ownermap: " + ownerMap);
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
}
