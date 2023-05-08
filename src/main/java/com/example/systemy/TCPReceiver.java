package com.example.systemy;

import com.example.systemy.interfaces.Observer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPReceiver extends Thread {
    private int port;
    String fileName = "receivedFile.txt";
    private Observer observer;

    public TCPReceiver(int port) {
        this.port = port;
    }

    public void setObserver(Observer observer){
        this.observer = observer;
    }

    public void setFileName(String fileName){
        this.fileName = fileName;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port);
             Socket socket = serverSocket.accept();
             InputStream inputStream = socket.getInputStream();
             FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }

            System.out.println("File received successfully.");
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }
}
