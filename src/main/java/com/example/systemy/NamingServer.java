package com.example.systemy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class NamingServer {

    @RestController
    class NamingServerController {

        private String[] nodes = {"node1", "nodes2", "node3", "nodes4And5"}; // replace with your node names

        @GetMapping("/getPrimaryNode")
        public String getPrimaryNode() {
            return nodes[0];
        }

        @GetMapping("/getSuccessorNode")
        public String getSuccessorNode(String currentNode) {
            int currentIndex = java.util.Arrays.asList(nodes).indexOf(currentNode);
            int successorIndex = (currentIndex + 1) % nodes.length;
            return nodes[successorIndex];
        }
    }

//    @Bean
//    public NamingServerController namingServerController() {
//        return new NamingServerController();
//    }

    public static void main(String[] args) {
        SpringApplication.run(NamingServer.class, args);
    }
}

