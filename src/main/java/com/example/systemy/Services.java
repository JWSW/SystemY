package com.example.systemy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
    public void addNode(String name, String ipAddr){

    }
    public void removeNode(String name, String ipAddr){

    }
}
