package com.example.systemy.Agents;

import com.example.systemy.Node;

import java.io.Serializable;

import java.util.Map;
import java.util.Objects;

public class FailureAgent implements Runnable, Serializable {
    private int failingID;
    private int currentID;
    private Node currentNode;

    public FailureAgent(int failingID, int currentID) {
        this.failingID = failingID;
        this.currentID = currentID;
    }

    @Override
    public void run() {
        // Read the file list of the current node
        Map<String, Map<Integer, String>> fileList = currentNode.getOwnerMap();

        // Check if the failing node is the owner of any files
        for (String filename: fileList.keySet()) {
            for (Integer nodeID: fileList.get(filename).keySet()) {
/*
            if (nodeID == failingID) {
                transferFileToNewOwner(filename);
            }
        }

        // Terminate the Failure Agent if it passed all nodes in the ring topology
        if (currentNodeId == currentNodeIdThatStartedAgent) {
            System.out.println("Failure Agent terminated");
            return;
            */
            }

        }

        // Pass the Failure Agent to the next node in the ring topology
        passFailureAgentToNextNode();


    }

    private void transferFileToNewOwner(String filename) {
        // Implement logic to transfer the file to the new owner
        // Check if the new owner already has a copy of the file and update logs accordingly
    }

    private void passFailureAgentToNextNode() {
        // Implement logic to pass the Failure Agent to the next node in the ring topology
    }
}
