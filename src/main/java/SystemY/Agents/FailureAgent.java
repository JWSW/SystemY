package SystemY.Agents;

import SystemY.Node;
import SystemY.Services;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.Serializable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class FailureAgent implements Runnable, Serializable {


    @Autowired
    private Services services;

    private int failingID;
    private int currentID;
    private Node currentNode;
    private String baseURL = "http://172.27.0.5:8080/requestName";
    private int initiatingNodeID;



    public FailureAgent(int failingID, int currentID,int initiatingNodeId, Node node) {
        this.failingID = failingID;
        this.currentID = currentID;
        this.currentNode = node;
        this.initiatingNodeID = initiatingNodeId;
    }

    @Override
    public void run() {
        // Read the file list of the current node
        Map<String, Integer> fileList = currentNode.getOwnerLocalFiles();

        // Check if the failing node is the owner of any files
        for (String filename: fileList.keySet()) {
            if (fileList.get(filename) == failingID) {
                transferOwnership(String.valueOf(fileList.get(filename)));
                }
            }


        // Terminate the Failure Agent if it passed all nodes in the ring topology
        if (currentID == initiatingNodeID) {
            System.out.println("Failure Agent terminated");
            return;
        }

        // Pass the Failure Agent to the next node in the ring topology
        try {
            passFailureAgentToNextNode();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private void transferOwnership(String filename) {

        // Check if the new owner already has a copy of the file and update logs accordingly
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
            //System.out.println("Sending request to get owner node of " + filename + " with hash: " + currentNode.getHash(filename));
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
        //currentNode.setOwnerFile(filename, Integer.parseInt(ownerNode),nodeIP);
    }

    private void passFailureAgentToNextNode() throws IOException, InterruptedException {
        // Determine the identifier or address of the next node
        int nextNodeId = currentNode.getNextID();

        // Send the Failure Agent to the next node
        String baseURL = "http://"+nextNodeId+":8081/requestNode";
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonFailureAgent = objectMapper.writeValueAsString(this);
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseURL + "/sendFailureAgentToNode/{nodeID}"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonFailureAgent))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        }


}



