package com.example.systemy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        if(!nodeMap.containsKey(hash)) {
            nodeMap.put(hash, node.getIpAddress());
        }else{
            throw new IllegalStateException("Node exists");
        }
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

    public Map<Integer, String> getFile(String filename) {
        int fileHash = getHash(filename);
        Integer kleinste = 32769;
        Integer grootste = 0;
        Integer groterDanFile = 0;
        Integer kleinerDanFile = 32769;
        boolean Hoogste = false;
        Map<Integer, String> nodeData = new HashMap<>();
        Set<Integer> hashSet = nodeMap.keySet();

        for(Integer nodeHash : hashSet){
            if(nodeHash<kleinste){
                kleinste = nodeHash;
            }else if(nodeHash> grootste){
                grootste = nodeHash;
            }else if(fileHash>kleinste){
                if(fileHash<grootste) {
                    Hoogste = false;
                    if (fileHash > nodeHash) {
                        if (nodeHash > kleinerDanFile) {
                            nodeHash = kleinerDanFile;
                        }
                    }else if(fileHash<nodeHash){
                        if(nodeHash<groterDanFile){
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
        if(Hoogste){
            nodeData.put(grootste,nodeMap.get(grootste));
        }else{
            nodeData.put(kleinerDanFile,nodeMap.get(kleinerDanFile));
        }
        return nodeData;
    }
}
