package com.example.systemy;

import java.net.*;

public class HeartbeatSender extends Thread{
    private int port;
    private int currentID;
    private String previousIP;
    private String nextIP;
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

    public void setPreviousIP(String previousIP) {
        this.previousIP = previousIP;
    }

    public void setNextIP(String nextIP) {
        this.nextIP = nextIP;
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket;
            InetAddress address1;
            InetAddress address2;
            socket = new DatagramSocket();
            address1 = InetAddress.getByName(previousIP);
            address2 = InetAddress.getByName(nextIP);

            String message = String.valueOf(currentID);
            buf = message.getBytes();

            DatagramPacket packet1 = new DatagramPacket(buf, buf.length, address1, port);
            DatagramPacket packet2 = new DatagramPacket(buf, buf.length, address2, port);

            while (true) {
                socket.send(packet1);
                socket.send(packet2);

                sleep(10000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
