package com.example.systemy;

import java.io.File;
import java.io.IOException;
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

    public static void main(String[] args) {
        SpringApplication.run(NamingServer.class, args);
    }

    @RestController
    public class NamingServerController {
        private static final int HASH_RANGE = 100000;
        private static final String FILE_TO_NODE_MAP_KEY = "fileToNodeMap";

        @GetMapping("/getSuccessorNode/{nodeName}")
        public int getSuccessorNode(@PathVariable String nodeName) {
            int nodeHash = getHash(nodeName);
            int successorNodeHash = nodeMap.keySet().stream()
                    .filter(node -> node > nodeHash)
                    .findFirst()
                    .orElse(nodeMap.keySet().stream().findFirst().orElse(0));
            return successorNodeHash;
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
            int hash = value.hashCode() % HASH_RANGE;
            if (hash < 0) {
                hash += HASH_RANGE;
            }
            return hash;
        }
    }
}
