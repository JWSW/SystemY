package com.example.systemy.Agents;

import com.example.systemy.Node;

import java.io.IOException;
import java.io.Serializable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class FailureAgent implements Runnable, Serializable {
    private int failingID;
    private int currentID;
    private Node currentNode;
    private String baseURL = "http://172.27.0.5:8080/requestName";

    public FailureAgent(int failingID, int currentID) {
        this.failingID = failingID;
        this.currentID = currentID;
    }

    @Override
    public void run() {
        // Read the file list of the current node
        Map<String, Map<Integer, String>> fileList = currentNode.getOwnerMap(); //moet agentlist zijn

        // Check if the failing node is the owner of any files
        for (String filename: fileList.keySet()) {
            for (Integer nodeID: fileList.get(filename).keySet()) {

            if (nodeID == failingID) {
                transferFileToNewOwner(filename);
            }
        }

        // Terminate the Failure Agent if it passed all nodes in the ring topology
       /* if (currentID == currentNodeIdThatStartedAgent) {
            System.out.println("Failure Agent terminated");
            return;

            }*/

        }

        // Pass the Failure Agent to the next node in the ring topology
        passFailureAgentToNextNode();


    }

    private void transferFileToNewOwner(String filename) {
        // Implement logic to transfer the file to the new owner
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
            System.out.println("Sending request to get owner node of " + filename + " with hash: " + currentNode.getHash(filename));
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
    }

    private void passFailureAgentToNextNode() {
        // Implement logic to pass the Failure Agent to the next node in the ring topology
    }
}
