package SystemY.Threads;

import java.io.IOException;
import java.net.*;

public class HeartbeatSender extends Thread{
    private final int port;
    private int currentID;
    private final String sendIP;
    DatagramPacket sendPacket;
    protected byte[] buf = new byte[256];

    public HeartbeatSender(String sendIP, int currentID, int port) {
        this.port = port;
        this.currentID = currentID;
        this.sendIP = sendIP;
    }

    public void setCurrentID(int currentID) {
        this.currentID = currentID;
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket;
            InetAddress sendAddress = InetAddress.getByName(sendIP);
            socket = new DatagramSocket();

            String message = String.valueOf(currentID);
            buf = message.getBytes();

            sendPacket = new DatagramPacket(buf, buf.length, sendAddress, port);

            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("Sending ping to " + sendAddress + " or " + sendIP + " with port " + port);
                socket.send(sendPacket);
                sleep(10000);
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
