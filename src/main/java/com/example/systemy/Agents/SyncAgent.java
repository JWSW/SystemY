package com.example.systemy.Agents;

import com.example.systemy.Node;

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
    private static final long SYNC_INTERVAL = 10; // in seconds
    private static final long LOCK_WAIT_TIME = 2; // in seconds
    private final Map<String, Boolean> agentFileList;
    Map<String, Map<Integer, String>> ownerMap = new ConcurrentHashMap<>();
    boolean updated;
    private final Node currentNode;

    public SyncAgent(Node currentNode) {
        this.currentNode = currentNode;
        agentFileList =  new ConcurrentHashMap<>();
        ownerMap = currentNode.getOwnerMap();
        updated = false;
    }


    @Override
    public void run() {
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
                if (currentNode.hasLockRequest()) {
                    String lockedFile = currentNode.getLockedFile();

                    // If the file is not locked on the agent's list, lock it and synchronize the lists
                    if (!agentFileList.containsKey(lockedFile)) {
                        boolean locked = currentNode.lockFile(lockedFile, LOCK_WAIT_TIME);

                        if (locked) {
                            agentFileList.replace(lockedFile, true);
                            currentNode.setFileList(agentFileList);
                        }
                    }

                    // Remove the lock when it is not needed anymore
                    currentNode.unlockFile(lockedFile);
                    agentFileList.remove(lockedFile);
                    currentNode.setFileList(agentFileList);
                }
            }
            System.out.println(agentFileList);
        }
    }

    private void syncWithNeighbors() throws IOException, InterruptedException {
        for (String neighbor : currentNode.getNeighbors()) {
            if (neighbor != null) {
                String baseURL = "http://"+neighbor+":8081/requestNode";
                System.out.println("Sync request");
                HttpRequest request1 = HttpRequest.newBuilder()
                        .uri(URI.create(baseURL + "/syncWithNeighbor"))
                        .GET()
                        .build();
                HttpResponse<String> response = HttpClient.newHttpClient().send(request1, HttpResponse.BodyHandlers.ofString());
                String jsonMap = response.body();
                System.out.println("Sync response: "+jsonMap);
                // Parse the JSON string and convert it into a Map object
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> receivedMap = objectMapper.readValue(jsonMap, new TypeReference<>() {
                });

                // Merge the received map with your existing map
                receivedMap.putAll(receivedMap);
            }
        }
    }

    public Map<String, Boolean> getAgentFileList() {
        return agentFileList;
    }
}