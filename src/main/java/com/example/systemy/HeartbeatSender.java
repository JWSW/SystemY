package com.example.systemy;

import java.io.IOException;
import java.net.*;

public class HeartbeatSender extends Thread{
    private int port;
    private int currentID;
    private String sendIP;
    private InetAddress sendAddress;
    DatagramPacket sendPacket;
    private boolean stopping = false;
    protected byte[] buf = new byte[256];

    public HeartbeatSender(String sendIP, int currentID, int port) {
        this.port = port;
        this.currentID = currentID;
        this.sendIP = sendIP;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setCurrentID(int currentID) {
        this.currentID = currentID;
    }

    public void setSendingIP(String sendigIP) throws UnknownHostException {
        this.sendIP = sendigIP;
        sendAddress = InetAddress.getByName(sendigIP);
        sendPacket = new DatagramPacket(buf, buf.length, sendAddress, port);
        System.out.println("Changed sending parameters: " + this.sendIP + ", " + sendAddress + ", " + sendPacket);
    }

    public void stopSending(){
        stopping = true;
    }

    @Override
    public void run() {
        try {
            stopping = false;
            DatagramSocket socket;
            sendAddress = InetAddress.getByName(sendIP);
            socket = new DatagramSocket();

            String message = String.valueOf(currentID);
            buf = message.getBytes();

            sendPacket = new DatagramPacket(buf, buf.length, sendAddress, port);

            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("Sending ping to " + sendAddress + " or " + sendIP + " with port " + port);
                socket.send(sendPacket);
                if (stopping) {
                    System.out.println("HeartbeatSender has stopped.");
                    break;
                }

                sleep(10000);
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
