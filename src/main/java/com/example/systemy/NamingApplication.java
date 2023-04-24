package com.example.systemy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class NamingApplication {

    public static void main(String[] args) {
        SpringApplication.run(NamingApplication.class, args);
    }
    public NamingApplication() throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = "http://172.27.0.5:8080/requestName";
        Node node = new Node(InetAddress.getLocalHost().getHostName(),InetAddress.getLocalHost().getHostAddress());
//        ResponseEntity<Node> postResponse = restTemplate.postForEntity(baseUrl + "/addNode", node,Node.class);
//
//        Map<Integer,String> location = restTemplate.getForObject(baseUrl + "/{filename}/getFileLocation",Map.class);
//        System.out.println(location);
    }
}
