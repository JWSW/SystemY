package com.example.systemy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class Services implements MulticastObserver{
    private Map<Integer, String> nodeMap = new ConcurrentHashMap<>();
    private static final String NODE_MAP_FILE_PATH = "node_map.json";
    protected byte[] buf = new byte[256];

    @Autowired
    MulticastReceive multicastReceive;

    @PostConstruct
    public void init() throws IOException {
        multicastReceive.setObserver(this);
        //multicastReceive.start(); //Hier zit een fout, geen idee wat. Bij clientApp werkt dit prima
        File file = new File(NODE_MAP_FILE_PATH);
        if (file.exists()) {
            ObjectMapper objectMapper = new ObjectMapper();
            nodeMap = objectMapper.readValue(file, new TypeReference<Map<Integer, String>>() {});
        }
        removeNodeByHash(5);
    }

    @PreDestroy
    public void destroy() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(new File(NODE_MAP_FILE_PATH), nodeMap);
    }

    public void addNode(Node node){
        int hash = getHash(node.getNodeName());
        if(!nodeMap.containsKey(hash)) {
            nodeMap.put(hash, node.getIpAddress());
            System.out.println("Node added: " + node);
        }else{
            throw new IllegalStateException("Node exists");
        }
    }

    public void removeNode(String name){
        try {
            int hash = getHash(name);
            nodeMap.remove(hash);
        }catch (IllegalStateException e){
            System.out.println("This node doesn't exist");
        }
    }
    public void removeNodeByHash(Integer id){
        try {
            nodeMap.remove(id);
        }catch (IllegalStateException e){
            System.out.println("This node doesn't exist");
        }
    }

    public int getHash(String name){
        int max = Integer.MAX_VALUE;
        int min = Integer.MIN_VALUE + 1; // add 1 to avoid overflow when calculating the absolute value

        int hash = (name.hashCode() + max) * (32768 / Math.abs(max) + Math.abs(min));
        hash = Math.abs(hash) % 32768; // map the result to the range (0, 32768)

        return hash;
    }

    public Map<Integer, String> getFile(String filename) {
        int fileHash = getHash(filename);
        Integer kleinste = 32769;
        Integer grootste = 0;
        Integer groterDanFile = 32769;
        Integer kleinerDanFile = 0;
        boolean Hoogste = false;
        Map<Integer, String> nodeData = new HashMap<>();
        Set<Integer> hashSet = nodeMap.keySet();
        System.out.println("De map: " + nodeMap);
        System.out.println("De fileHash: " + fileHash);
        for(Integer nodeHash : hashSet){
            System.out.println("De lijst wordt doorlopen: " + nodeHash);
            if(nodeHash<kleinste){
                kleinste = nodeHash;
                System.out.println("Nieuwe kleinste: " + kleinste);
            }
            if(nodeHash> grootste){
                grootste = nodeHash;
                System.out.println("Nieuwe grootste: " + grootste);
            }
            if(fileHash>kleinste){
                if(fileHash<grootste) {
                    Hoogste = false;
                    if (fileHash > nodeHash) {
                        if (nodeHash > kleinerDanFile) {
                            System.out.println("Ja groter dan kleinere: " + nodeHash);
                            kleinerDanFile = nodeHash;
                        }
                    }else if(fileHash<nodeHash){
                        if(nodeHash<groterDanFile){
                            System.out.println("Ja kleiner dan grotere: " + nodeHash);
                            groterDanFile = nodeHash;
                        }
                    }
                }
            }else if(fileHash<kleinste){
                Hoogste = true;
            }else if(fileHash>grootste){
                Hoogste = true;
            }
        }
        System.out.println(Hoogste);
        if(Hoogste){
            System.out.println(grootste);
            nodeData.put(grootste,nodeMap.get(grootste));
        }else{
            nodeData.put(kleinerDanFile,nodeMap.get(kleinerDanFile));
        }
        return nodeData;
    }

    public void unicast(String unicastMessage, String ipAddress, int port) throws IOException {
        DatagramSocket socket;
        InetAddress address;
        socket = new DatagramSocket();
        address = InetAddress.getByName(ipAddress);
        buf = unicastMessage.getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        socket.send(packet);
        socket.close();
    }

    @Override
    public void onMessageReceived(String message) throws IOException {
        String[] parts = message.split(","); // split the string at the space character
        String hostname = parts[0];
        String ipAddress = parts[1];
        Node node = new Node(hostname,ipAddress);
        addNode(node);
        unicast(String.valueOf(nodeMap.size()), ipAddress, 4555);
    }
}
