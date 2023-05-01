package com.example.systemy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class Services implements MulticastObserver{
    private Map<Integer, String> nodeMap = new ConcurrentHashMap<>();
    private static final String NODE_MAP_FILE_PATH = "node_map.json";
    private Node node;
    private String packet;
    private String baseURL = "http://172.27.0.5:8080/requestName";
    ObjectMapper objectMapper = new ObjectMapper(); // or any other JSON serializer

    @Autowired
    MulticastReceive multicastReceive;
    //UnicastReceiver unicastReceiver;

    @PostConstruct
    public void init() throws IOException {
        node = new Node(InetAddress.getLocalHost().getHostName(), InetAddress.getLocalHost().getHostAddress());
//        String json = objectMapper.writeValueAsString(node);
//        HttpClient client = HttpClient.newHttpClient();
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(baseURL + "/addNode"))
//                .header("Content-Type", "application/json")
//                .POST(HttpRequest.BodyPublishers.ofString(json))//"{nodeName:" + node.getNodeName() + "ipAddress:" + node.getIpAddress() + "}"))
//                .build();
//        try{
//            System.out.println("Sending request to add node.");
//            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
//            System.out.println("Response: " + response.body());
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//        }
        multicastReceive.setObserver(this);
        multicastReceive.start();

        File file = new File(NODE_MAP_FILE_PATH);
        if (file.exists()) {
            ObjectMapper objectMapper = new ObjectMapper();
            nodeMap = objectMapper.readValue(file, new TypeReference<Map<Integer, String>>() {});
        }
        removeNodeByHash(5);
    }

    @PreDestroy
    public void destroy() throws IOException {
//        node.shutDown();
        node.killProcess();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(new File(NODE_MAP_FILE_PATH), nodeMap);
    }

    public void addNode(Node node){
        int hash = getHash(node.getNodeName());
        if(!nodeMap.containsKey(hash)) {
            nodeMap.put(hash, node.getIpAddress());
            System.out.println(node);
        }else{
            throw new IllegalStateException("Node exists");
        }
    }

    public void removeNode(String name){
        int hash = getHash(name);
        nodeMap.remove(hash);
    }
    public void removeNodeByHash(Integer id){
        nodeMap.remove(id);
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

    @Override
    public void onMessageReceived(String message) throws IOException {
        packet = message;
        node.multicastHandlePacket(packet);
    }


}
