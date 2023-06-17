package SystemY.Agents;

import SystemY.Node;
import SystemY.Services;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.Serializable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class FailureAgent implements Runnable, Serializable {

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
        System.out.println("FailureAgent has started");
        // Read the file list of the current node
        Map<String, Integer> fileList = currentNode.getOwnerLocalFiles();

        // Check if the failing node is the owner of any files
        for (String filename: fileList.keySet()) {
            if (fileList.get(filename) == failingID) {
                transferOwnership(String.valueOf(fileList.get(filename)));
                }
        }


        // Terminate the Failure Agent if it passed all nodes in the ring topology
        if (currentID == initiatingNodeID && !currentNode.isFirstNode) {
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
        System.out.println("transferOwnership");
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
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response: " + response.body());

            packet = response.body();
            parts = packet.split(",");
            nodeHash = Integer.valueOf(parts[0]);
            nodeIP = parts[1];
            ownerNode = packet;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        currentNode.setOwnerFile(filename, Integer.parseInt(ownerNode),nodeIP);
        System.out.println("transferOwnership completed");

    }

    private void passFailureAgentToNextNode() throws IOException, InterruptedException {


        // Determine the address of the next node
        String nextNodeIP = currentNode.getNextIP();
        String endpointURL = "http://" + nextNodeIP + ":8081/requestNode/sendFailureAgentToNextNode";
        // Create a map to hold the parameters
        Map<String, Integer> failureAgentParams = new HashMap<>();
        failureAgentParams.put("failingID", failingID);
        failureAgentParams.put("initiatingNodeID", initiatingNodeID);

        // Convert the map to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonFailureAgent = objectMapper.writeValueAsString(failureAgentParams);

        // Send the Failure Agent to the next node
        try {
            System.out.println("jsonFailureAgent:" +jsonFailureAgent);
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointURL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonFailureAgent))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                System.out.println("Failure Agent sent successfully.");
            } else {
                System.out.println("Failure Agent sending failed. Response: " + response.body());
            }
        } catch (JsonProcessingException e) {
            System.out.println("Error serializing FailureAgent to JSON: " + e.getMessage());
        } catch (IOException | InterruptedException e) {
            System.out.println("Error sending FailureAgent: " + e.getMessage());
            e.printStackTrace();
        }
    }



}



