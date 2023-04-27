//package com.example.systemy;
//
//import org.springframework.stereotype.Component;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.ServerSocket;
//import java.net.Socket;
//
//@Component
//public class UnicastReceiver extends Thread{
//    private DatagramSocket socket = null;
//    private DatagramPacket packet = null;
//    byte[] buf = new byte[256];
//
//    public void run() {
//        try {
//            socket = new DatagramSocket(4555);
//            System.out.println("Waiting for client on port 4555...");
//
//            packet = new DatagramPacket(buf, buf.length);
//            socket.receive(packet);
//            System.out.println("Received message: " + new String(packet.getData(), 0, packet.getLength()));
////            Socket clientSocket = serverSocket.accept();
////            System.out.println("Accepted connection from " + clientSocket.getInetAddress());
////
////            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
////            String line;
////            while ((line = in.readLine()) != null) {
////                System.out.println("Received message: " + line);
////            }
////
////            in.close();
////            clientSocket.close();
////            serverSocket.close();
//        } catch (Exception e) {
//        e.printStackTrace();
//    } finally {
//        // Close the socket
//        if (socket != null) {
//            socket.close();
//        }
//    }
//    }
//}
