package com.example.systemy;
import java.io.IOException;
import java.nio.file.*;
import java.util.Scanner;
public class EditFiles implements Runnable {
    private Node currentNode;
    private String fileName;
    private boolean isEditingRequested;
    WatchService watchService = FileSystems.getDefault().newWatchService();
    public EditFiles(Node node) throws IOException {
        currentNode = node;}

    @Override
    public void run() {
        Path directory = Paths.get("/SystemY/replicatedFiles");
        Path currentDirectory = Paths.get("");
        System.out.println(currentDirectory.toAbsolutePath());
        try {
            directory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        while (true) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                // Handle interruption
                break;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    // A file was modified
                    Path modifiedFile = (Path) event.context();
                    System.out.println("File modified: " + modifiedFile);

                    // Check if the modified file matches the file you want to monitor
                    if (modifiedFile.toString().equals("filename.txt")) {
                        // Perform actions when the file is modified
                        // For example, lock the file or update the local file list
                        // You can call your EditFiles thread or any other relevant logic here
                    }
                }
            }

            // Reset the key to receive further events
            key.reset();
        }
    }
}

