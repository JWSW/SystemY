package com.example.systemy;

import com.example.systemy.interfaces.Observer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class TCPReceiver extends Thread {
    private int port;
    String directory = "/home/Dist/SystemY/nodeFiles/";
    String fileName = "receivedFile.txt";
    private Observer observer;
    private ServerSocket serverSocket;
    public boolean isAccepted = false;

    public TCPReceiver(int port) {
        this.port = port;
    }

    public void setObserver(Observer observer){
        this.observer = observer;
    }

    public void setFileName(String fileName){
        this.fileName = fileName;
        System.out.println("Filename has been set to " + this.fileName);
    }

    public void close() throws IOException {
        serverSocket.close();
    }

    public void open() throws IOException {
        serverSocket = new ServerSocket(port);
        Socket socket = serverSocket.accept();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            Socket socket = serverSocket.accept();
//            System.out.println("Connection accepted");
            isAccepted = true;
            InputStream inputStream = socket.getInputStream();
//            System.out.println("Filename now: " + fileName);
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);

            Path filePath = Path.of(directory, fileName);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File received successfully.");
            if (observer != null) {
                observer.onMessageReceived("fileReceived","No message");
            }
            isAccepted = false;
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }
}
