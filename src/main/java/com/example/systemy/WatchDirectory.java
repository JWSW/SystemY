package com.example.systemy;

import java.nio.file.*;   // Import the necessary classes
import java.util.*;

public class WatchDirectoryExample extends Thread {
    private WatchService watchService;


    public WatchDirectoryExample(String directory) throws Exception {
        // Get the directory to watch
        Path path = Paths.get(directory);

        // Create a WatchService object
        watchService = FileSystems.getDefault().newWatchService();

        // Register the directory with the watch service for file creation events
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
    }
        public void run() {
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
