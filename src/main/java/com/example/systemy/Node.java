package com.example.systemy;

import java.io.*;
import java.net.*;

import jakarta.persistence.Basic;
import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.springframework.beans.factory.annotation.Autowired;

@Data
@AllArgsConstructor
@Entity
public class Node {
    @Autowired
    private MulticastReceive multicastReceive;

    @Id
    private String nodeName;
    private String ipAddress;
    private int NextID;
    private int PreviousID;
    private static DatagramSocket socket = null;
    private String file1 = "file1.txt";
    private String fileTwo = "file2.txt";
    protected byte[] buf = new byte[256];

//    private Map<String, String> files;

    public Node() {
    }

    public Node(String nodeName, String ipAddress) throws IOException {
        this.nodeName = nodeName;
        this.ipAddress = ipAddress;
        try {
            File file = new File(file1);
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            File file2 = new File(fileTwo);
            // if file doesnt exists, then create it
            if (!file2.exists()) {
                file2.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String message = InetAddress.getLocalHost().getHostName() + " " + InetAddress.getLocalHost().getHostAddress();
        multicast(message);
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getNextID() {
        return NextID;
    }

    public void setNextID(int successorId) {
        this.NextID = successorId;
    }

    public int getPreviousID() {
        return PreviousID;
    }

    public void setPreviousID(int predecessorId) {
        this.PreviousID = predecessorId;
    }

//    public void run() throws IOException {
//        try {
//            MulticastSocket MultiSocket = null;
//            InetAddress group = InetAddress.getByName("230.0.0.0");
//            InetSocketAddress groupAddress = new InetSocketAddress(group, 4446);
//            MultiSocket.setReuseAddress(true);
//
//            NetworkInterface iface = NetworkInterface.getByName("eth0");
//
//            MultiSocket.bind(new InetSocketAddress(4446));
//            MultiSocket.joinGroup(groupAddress, iface);
//
//            while (true) {
//                DatagramPacket packet = new DatagramPacket(buf, buf.length);
//                MultiSocket.receive(packet);
//                String received = new String(packet.getData(), 0, packet.getLength());
//                if ("end".equals(received)) {
//                    break;
//                }
//            }
//            MultiSocket.leaveGroup(groupAddress, iface);
//            MultiSocket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void multicast(String multicastMessage) throws IOException {
        DatagramSocket socket;
        InetAddress group;
        socket = new DatagramSocket();
        group = InetAddress.getByName("230.0.0.0");
        buf = multicastMessage.getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 4446);
        socket.send(packet);
        socket.close();
    }

//    public void addFile(String fileName, String owner) {
//        files.put(fileName, owner);
//    }

    public String getFileOwner(String fileName) {
        return fileName;
    }

//    public void removeFile(String fileName) {
//        files.remove(fileName);
//    }

    @Override
    public String toString() {
        return "Node{" +
                "nodeName='" + nodeName + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", NextID=" + NextID +
                ", PreviousID=" + PreviousID +
                '}';
    }


//    public static void main(String[] args) throws IOException {
//        String baseUrl = "http://localhost:8080/requestName";
//        Node node = new Node(InetAddress.getLocalHost().getHostName(),InetAddress.getLocalHost().getHostAddress());
//        System.out.println(node);
//    }
}
