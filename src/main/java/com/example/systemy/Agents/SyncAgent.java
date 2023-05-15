package com.example.systemy.Agents;

import com.example.systemy.Node;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

            syncWithNeighbors();
        }

        // Keep checking for lock requests
//        while (true) {
//            for (String fileName : ownerMap.keySet()) {
//                boolean updated = false;
//
//                // If the agent's list doesn't contain a file that the node owns, add it to the list
//                if (!agentFileList.containsKey(fileName)) {
//                    agentFileList.put(fileName, false);
//                    updated = true;
//                }
//
//                // If the agent's list contains a file that the node no longer owns, remove it from the list
//                if (!ownerMap.containsKey(fileName) && agentFileList.containsKey(fileName)) {
//                    agentFileList.remove(fileName);
//                    updated = true;
//                }
//
//                // If the local file list was updated, synchronize with the node's file list
//                if (updated) {
//                    currentNode.setFileList(agentFileList);
//                }
//
//                // Check if there is a lock request on the current node
//                if (currentNode.hasLockRequest()) {
//                    String lockedFile = currentNode.getLockedFile();
//
//                    // If the file is not locked on the agent's list, lock it and synchronize the lists
//                    if (!agentFileList.containsKey(lockedFile)) {
//                        boolean locked = currentNode.lockFile(lockedFile, LOCK_WAIT_TIME);
//
//                        if (locked) {
//                            agentFileList.keySet();
//                            currentNode.setFileList(agentFileList);
//                        }
//                    }
//
//                    // Remove the lock when it is not needed anymore
//                    currentNode.unlockFile(lockedFile);
//                    agentFileList.remove(lockedFile);
//                    currentNode.setFileList(agentFileList);
//                }
//            }
//        }
    }

    private void syncWithNeighbors() {
        for (Node neighbor : currentNode.getNeighbors()) {
            if (neighbor != null) {
                Map<String, Boolean> neighborFileList = neighbor.getFileList();
                agentFileList.putAll(neighborFileList);
            }
        }
    }
}