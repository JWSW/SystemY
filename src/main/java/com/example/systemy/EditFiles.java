package com.example.systemy;
import java.util.Scanner;
public class EditFiles extends Thread {
    private Node currentNode;
    private String fileName;
    private boolean isEditingRequested;
    
    public EditFiles(Node node) {
        currentNode = node;}

    @Override
    public void run() {
                Scanner scanner = new Scanner(System.in);
                String command = scanner.nextLine();

                if (command.startsWith("nano ")) {
                    String fileName = command.substring(5); // Extract the file name
                    System.out.println("Editing file: " + fileName);
                    // Perform your logic here for editing the file
                } else {
                    System.out.println("Invalid command: " + command);
                }
    }
}

