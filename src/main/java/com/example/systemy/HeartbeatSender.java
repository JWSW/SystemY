package com.example.systemy;

import java.net.*;

public class HeartbeatSender extends Thread{
    private int port;
    private int currentID;
    private String previousIP;
    private String nextIP;
    private InetAddress previousAddress;
    private InetAddress nextAddress;
    DatagramPacket previousPacket;
    DatagramPacket nextPacket;
    protected byte[] buf = new byte[256];

    public HeartbeatSender(String previousIP, String nextIP, int currentID, int port) {
        this.port = port;
        this.currentID = currentID;
        this.previousIP = previousIP;
        this.nextIP = nextIP;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPreviousIP(String previousIP) throws UnknownHostException {
        this.previousIP = previousIP;
        previousAddress = InetAddress.getByName(previousIP);
        previousPacket = new DatagramPacket(buf, buf.length, previousAddress, port);
    }

    public void setNextIP(String nextIP) throws UnknownHostException {
        this.nextIP = nextIP;
        nextAddress = InetAddress.getByName(nextIP);
        nextPacket = new DatagramPacket(buf, buf.length, nextAddress, port);
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket;
            previousAddress = InetAddress.getByName(previousIP);
            nextAddress = InetAddress.getByName(nextIP);
            socket = new DatagramSocket();

            String message = String.valueOf(currentID);
            buf = message.getBytes();

            previousPacket = new DatagramPacket(buf, buf.length, previousAddress, port);
            nextPacket = new DatagramPacket(buf, buf.length, nextAddress, port);

            while (true) {
                System.out.println("Sending ping to " + previousAddress);
                socket.send(previousPacket);
                System.out.println("Sending ping to " + nextAddress);
                socket.send(nextPacket);

                sleep(10000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
