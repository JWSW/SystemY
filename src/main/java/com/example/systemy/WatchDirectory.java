package com.example.systemy;

import com.example.systemy.Agents.SyncAgent;
import com.example.systemy.interfaces.Observer;

import java.io.IOException;
import java.nio.file.*;

public class WatchDirectory extends Thread {
    private WatchService watchService;
    private FileLock fileLock;
    private com.example.systemy.interfaces.Observer observer;
    private Node currentNode;

    public WatchDirectory(Node node) {
        this.currentNode = node;
    }

    public void setObserver(Observer observer) {
        this.observer = observer;
    }

    public void run() {
        // Get the directory to watch
        Path path1 = Paths.get("/home/Dist/SystemY/nodeFiles");
        Path path2 = Paths.get("/home/Dist/SystemY/replicatedFiles");


        // Create a WatchService object
        try {
            watchService = FileSystems.getDefault().newWatchService();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Register the directory with the watch service for file creation events
        try {
            path1.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            path2.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
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
                if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                    Path modifiedFile = (Path) event.context();
                    //System.out.println(modifiedFile);
                    String fileName = modifiedFile.getFileName().toString();
                    //System.out.println(fileName);
                    if (fileName.endsWith(".swp")) {
                        System.out.println("FilelockRequest");
                        FileLock fileLock = new FileLock(fileName);
                        currentNode.FileLockRequest(fileLock);
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