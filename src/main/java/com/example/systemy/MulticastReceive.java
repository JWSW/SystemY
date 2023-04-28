package com.example.systemy;
import org.springframework.stereotype.Component;

import java.net.*;

@Component
public class MulticastReceive extends Thread {
    private String message = "";
    private boolean hasReceived = false;

    public String getMessage(){
        return message;
    }



        private MulticastObserver observer;

        public void setObserver(MulticastObserver observer) {
            this.observer = observer;
        }

        @Override
        public void run() {
            try {
                MulticastSocket MultiSocket = new MulticastSocket(44445);
                InetAddress group = InetAddress.getByName("230.0.0.0");
                NetworkInterface iface = NetworkInterface.getByName("eth0");


//                try {
//                    MultiSocket.bind(new InetSocketAddress(44445));
//                } catch (SocketException e) {
//                    System.err.println("Port " + 44445 + " is already in use");
//                    // Handle the exception gracefully here
//                }
                InetSocketAddress groupAddress = new InetSocketAddress(group, 44445);
                MultiSocket.setReuseAddress(true);
                MultiSocket.joinGroup(groupAddress,iface);

                while (true) {
                    byte[] buf = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    MultiSocket.receive(packet);

                    String received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Received multicast message: " + received);

                    if (observer != null) {
                        observer.onMessageReceived(received);
                    }

                    if ("end".equals(received)) {
                        break;
                    }
                }
                MultiSocket.leaveGroup(groupAddress,iface);
                MultiSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
    @Override
    public void run() {
        try {
            MulticastSocket MultiSocket = new MulticastSocket(4446);
            InetAddress group = InetAddress.getByName("230.0.0.0");
            NetworkInterface iface = NetworkInterface.getByName("eth0");

            MultiSocket.bind(new InetSocketAddress(4446));
            InetSocketAddress groupAddress = new InetSocketAddress(group, 4446);
            MultiSocket.setReuseAddress(true);
            MultiSocket.joinGroup(groupAddress,iface);

            while (true) {
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                MultiSocket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received message: " + received);
                message = received;
                hasReceived = true;

                if ("end".equals(received)) {
                    break;
                }
            }
            MultiSocket.leaveGroup(groupAddress,iface);
            MultiSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/


