package com.example.systemy;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class NamingServer {
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

//    public static void main(String[] args) {
//        SpringApplication.run(NamingServer.class, args);
//    }

    @RestController
    public class NamingServerController {
        private static final int HASH_RANGE = 100000;
        private static final String FILE_TO_NODE_MAP_KEY = "fileToNodeMap";

        @GetMapping("/getSuccessorNode/{filename}")


        public String getSuccessorNode(@PathVariable String filename) throws IOException {
            int fileHash = Math.abs(filename.hashCode());
            int minDiff = Integer.MAX_VALUE;
            int selectedNodeId = -1;

            for (Map.Entry<Integer, String> entry : nodeMap.entrySet()) {
                int nodeId = entry.getKey();
                String nodeIp = entry.getValue();
                int nodeHash = Math.abs(getHash(nodeIp));
                if (nodeHash <= fileHash) {
                    int diff = fileHash - nodeHash;
                    if (diff < minDiff) {
                        minDiff = diff;
                        selectedNodeId = nodeId;
                    }
                }
            }

            if (selectedNodeId == -1) {
                selectedNodeId = Collections.max(nodeMap.keySet());
            }

            return nodeMap.get(selectedNodeId);
        }


        @GetMapping("/getNodeIp/{nodeId}")
        public ResponseEntity<String> getNodeIp(@PathVariable int nodeId) {
            String nodeIp = nodeMap.get(nodeId);
            if (nodeIp == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Node not found.");
            }
            return ResponseEntity.ok(nodeIp);
        }

        @GetMapping("/getFileLocation/{filename}")
        public ResponseEntity<String> getFileLocation(@PathVariable String filename) {
            int fileHash = getHash(filename);
            int nodeId = nodeMap.keySet().stream()
                    .filter(node -> node >= fileHash)
                    .findFirst()
                    .orElse(nodeMap.keySet().stream().findFirst().orElse(0));
            String nodeIp = nodeMap.get(nodeId);
            if (nodeIp == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found.");
            }
            return ResponseEntity.ok(nodeIp);
        }

        @GetMapping("/addNode/{nodeId}/{nodeIp}")
        public void addNode(@PathVariable int nodeId, @PathVariable String nodeIp) {
            nodeMap.put(nodeId, nodeIp);
        }

        @GetMapping("/removeNode/{nodeId}")
        public void removeNode(@PathVariable int nodeId) {
            nodeMap.remove(nodeId);
        }

        private int getHash(String value) {
            int max = Integer.MAX_VALUE;
            int min = Integer.MIN_VALUE + 1; // add 1 to avoid overflow when calculating the absolute value

            int hash = (value.hashCode() + max) * (32768 / Math.abs(max) + Math.abs(min));
            hash = Math.abs(hash) % 32768; // map the result to the range (0, 32768)

            return hash;
        }
    }
}
