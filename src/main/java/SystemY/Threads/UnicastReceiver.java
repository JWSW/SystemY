package SystemY.Threads;

/*This function is used to check for incoming UDP packets. It runs continuously in search of new packets.*/


import SystemY.interfaces.Observer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UnicastReceiver implements Runnable {
    private DatagramSocket socket;

    public UnicastReceiver(int port) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("Port " + port + " is already in use");
        }
    }

    private Observer observer;

    public void setObserver(Observer observer) {
        this.observer = observer;
    }


    @Override
    public void run() {
        try {
            while (true) {
                // Create a buffer to store the incoming message
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);


                // Receive a unicast message
                socket.receive(packet);

                // Print the received message
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received unicast message: " + receivedMessage);

                //Notify the observer
                if (observer != null) {
                    observer.onMessageReceived("Unicast",receivedMessage);
                }

                if ("end".equals(receivedMessage)) {
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Close the socket when done
            if (socket != null) {
                socket.close();
            }
        }
    }
}
