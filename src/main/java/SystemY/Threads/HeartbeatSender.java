package SystemY.Threads;

import java.io.IOException;
import java.net.*;

/* This class is used to send pings at a certain rate to one of the neighboring nodes to let it know we are still alive.*/
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
            // Create a datagram socket with a certain port
            DatagramSocket socket;
            InetAddress sendAddress = InetAddress.getByName(sendIP);
            socket = new DatagramSocket();

            //We send the id of this node as a message
            String message = String.valueOf(currentID);
            buf = message.getBytes();
            sendPacket = new DatagramPacket(buf, buf.length, sendAddress, port);

            // We will continue sending pings at certain time intervals unless we are interrupted
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("Sending ping to " + sendAddress + " with port " + port);
                socket.send(sendPacket);
                sleep(10000);
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
