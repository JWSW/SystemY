package com.example.systemy.Agents;

import com.example.systemy.FileChecker;
import com.example.systemy.FileLock;
import com.example.systemy.Node;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class SyncAgent implements Runnable, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final long SYNC_INTERVAL = 5; // in seconds

    private final Map<String, Boolean> agentFileList;
    Map<String, Map<Integer, String>> ownerMap = new ConcurrentHashMap<>();
    boolean updated;
    private final FileChecker fileChecker;
    String directory1 = "/home/Dist/SystemY/replicatedFiles/";
    String directory2 = "/home/Dist/SystemY/nodeFiles/";
    private final Node currentNode;

    public SyncAgent(Node currentNode) {
        this.currentNode = currentNode;
        agentFileList =  new ConcurrentHashMap<>();
        ownerMap = currentNode.getOwnerMap();
        updated = false;
        fileChecker = new FileChecker(directory1, directory2);
    }





    @Override
    public void run() {
        fileChecker.start();
        // Periodically synchronize with neighboring nodes
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(SYNC_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                syncWithNeighbors();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (String fileName : ownerMap.keySet()) {
                boolean updated = false;
                // If the agent's list doesn't contain a file that the node owns, add it to the list
                if (!agentFileList.containsKey(fileName)) {
                    agentFileList.put(fileName, false);
                    updated = true;
                }

                // If the agent's list contains a file that the node no longer owns, remove it from the list
                if (!ownerMap.containsKey(fileName) && agentFileList.containsKey(fileName)) {
                    agentFileList.remove(fileName);
                    updated = true;
                }

                // If the local file list was updated, synchronize with the node's file list
                if (updated) {
                    currentNode.setFileList(agentFileList);
                }

                // Check if there is a lock request on the current node
                if (currentNode.getLockRequest() != null) {
                    FileLock lockRequest = currentNode.getLockRequest();
                    String filename = lockRequest.getFileName();

                    // If the file is not locked on the agent's list, lock it and synchronize the lists
                    if (!agentFileList.containsKey(filename)) {
                        boolean locked = lockRequest.lockFile(filename);
                        System.out.println(locked);

                        if (locked) {
                            agentFileList.replace(filename, true);
                            currentNode.setFileList(agentFileList);
                            lockRequest.setLockActive(true);
                        }
                    } else {System.out.println("Is Locked");}
                    if (!lockRequest.isLockActive()) {
                        // Remove the lock when it is no longer needed
                        lockRequest.unlockFile(filename);
                        agentFileList.remove(filename);
                        currentNode.setFileList(agentFileList);
                    }
                }
            }
        }
    }

    private void syncWithNeighbors() throws IOException, InterruptedException {
        for (String neighbor : currentNode.getNeighbors()) {
            if (neighbor.isEmpty()) {
                String baseURL = "http://"+neighbor+":8081/requestNode";
                System.out.println("Sync request");
                HttpRequest request1 = HttpRequest.newBuilder()
                        .uri(URI.create(baseURL + "/syncWithNeighbor"))
                        .GET()
                        .build();
                HttpResponse<String> response = HttpClient.newHttpClient().send(request1, HttpResponse.BodyHandlers.ofString());
                String jsonMap = response.body();
                System.out.println("Sync response: " + jsonMap);

                // Parse the JSON string and convert it into a Map object
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Boolean> receivedMap = objectMapper.readValue(jsonMap, new TypeReference<>() {});
                agentFileList.putAll(receivedMap);

            }
        }
        System.out.println(agentFileList);
    }

    public Map<String, Boolean> getAgentFileList() {
        return agentFileList;
    }
}