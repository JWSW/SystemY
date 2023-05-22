package com.example.systemy.Agents;

import com.example.systemy.Threads.FileChecker;
import com.example.systemy.Node;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Iterator;
import java.util.List;
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
                    System.out.println("agentFileList=  " + agentFileList);
                }
            }
            // Check if there is a lock request on the current node
            if (!fileChecker.getFileLockRequest().isEmpty()) {
                Map<String, Boolean> updatedFiles = fileChecker.getFileLockRequest();

                for (Map.Entry<String, Boolean> entry : updatedFiles.entrySet()) {
                    String filename = entry.getKey();

                    boolean isBeingEdited = entry.getValue();

                    // Get the file status from the agent's file list
                    boolean currentStatus = agentFileList.getOrDefault(filename, false);

                    // If the file is not locked on the agent's list, lock it and synchronize the lists
                    if (currentStatus != isBeingEdited) {
                        System.out.println("LockRequest " + entry.getKey());
                        if (isBeingEdited) {
                            fileChecker.lockFile(filename);
                            System.out.println("File " + filename + " is locked");
                            if (fileChecker.isLockActive()) {
                                agentFileList.replace(filename, true);
                                currentNode.setFileList(agentFileList);
                            }

                        }
                        } else {
                        System.out.println(filename + " is already being edited");
                    }
                }
            }

            if (!fileChecker.getRemoveList().isEmpty()) {
                List<String> removeList = fileChecker.getRemoveList();
                Iterator<String> iterator = removeList.iterator();

                while (iterator.hasNext()) {

                    String filename = iterator.next();
                    // Remove the file from the files map
                    iterator.remove();
                    // Update the agentList
                    agentFileList.replace(filename, false);
                    currentNode.setFileList(agentFileList);

                    // Unlock the file
                    fileChecker.unlockFile(filename);
                    System.out.println("File " + filename + " is unlocked");
                }
                // Clear the removeList after processing
                fileChecker.clearRemoveList();
            }
            for (Map.Entry<String, Boolean> entry : agentFileList.entrySet()) {
                String filename = entry.getKey();
                boolean isBeingEdited = entry.getValue();

                if (isBeingEdited) {
                    // Lock the file if it's being edited
                    fileChecker.lockFile(filename);
                } else {
                    // Unlock the file if it's not being edited
                    fileChecker.unlockFile(filename);
                }
            }
        }
    }





            private void syncWithNeighbors() throws IOException, InterruptedException {
        for (String neighbor : currentNode.getNeighbors()) {
            if (!neighbor.isEmpty()) {
                String baseURL = "http://"+neighbor+":8081/requestNode";
                //System.out.println("Sync request");
                HttpRequest request1 = HttpRequest.newBuilder()
                        .uri(URI.create(baseURL + "/syncWithNeighbor"))
                        .GET()
                        .build();
                HttpResponse<String> response = HttpClient.newHttpClient().send(request1, HttpResponse.BodyHandlers.ofString());
                String jsonMap = response.body();
                //System.out.println("Sync response: " + jsonMap);

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