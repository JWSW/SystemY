package com.example.systemy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class Services {
    private Map<Integer, String> nodeMap = new ConcurrentHashMap<>();
    private static final String NODE_MAP_FILE_PATH = "node_map.json";
    @PostConstruct
    public void init() throws IOException {
        File file = new File(NODE_MAP_FILE_PATH);
        if (file.exists()) {
            ObjectMapper objectMapper = new ObjectMapper();
            nodeMap = objectMapper.readValue(file, new TypeReference<Map<Integer, String>>() {});
        }
    }
    @PreDestroy
    public void destroy() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(new File(NODE_MAP_FILE_PATH), nodeMap);
    }
    public void addNode(Node node){
        int hash = getHash(node.getNodeName());
        nodeMap.put(hash, node.getIpAddress());
    }

    public void removeNode(String name){
        int hash = getHash(name);
        nodeMap.remove(hash);
    }

    public int getHash(String name){
        int max = Integer.MAX_VALUE;
        int min = Integer.MIN_VALUE + 1; // add 1 to avoid overflow when calculating the absolute value

        int hash = (name.hashCode() + max) * (32768 / Math.abs(max) + Math.abs(min));
        hash = Math.abs(hash) % 32768; // map the result to the range (0, 32768)

        return hash;
    }
}
