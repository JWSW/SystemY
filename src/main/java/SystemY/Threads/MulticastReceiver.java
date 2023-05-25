package SystemY.Threads;
import SystemY.interfaces.MulticastObserver;
import org.springframework.stereotype.Component;

import java.net.*;

/* This class is used to check for incoming multicast messages in a specific multicast group using a thread.*/
@Component
public class MulticastReceiver extends Thread {
        private MulticastObserver observer;

        public void setObserver(MulticastObserver observer) {
            this.observer = observer;
        }

        @Override
        public void run() {
            try {
                // We create a multicast socket and join the group 230.0.0.0 with interface 'eth0'.
                MulticastSocket MultiSocket = new MulticastSocket(44445);
                InetAddress group = InetAddress.getByName("230.0.0.0");
                NetworkInterface iface = NetworkInterface.getByName("eth0");
                InetSocketAddress groupAddress = new InetSocketAddress(group, 44445);
                MultiSocket.setReuseAddress(true);
                MultiSocket.joinGroup(groupAddress,iface);

                while (true) {
                    // Make a byte buffer to receive the datagram packet
                    byte[] buf = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    MultiSocket.receive(packet);

                    // We receive a multicast message
                    String received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Received multicast message: " + received);

                    // We notify the observer
                    if (observer != null) {
                        observer.onMessageReceived(received);
                    }

                    if ("end".equals(received)) {
                        break;
                    }
                }
                // Leave the multicast group if the thread is terminated.
                MultiSocket.leaveGroup(groupAddress,iface);
                MultiSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }




