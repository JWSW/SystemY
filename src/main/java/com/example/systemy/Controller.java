package com.example.systemy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/requestNode")
public class Controller {
    @Autowired
    private Services services;

    @PostMapping("/{filename}/sendNewFile")
    public void sendNewFile (@PathVariable String filename, @RequestBody Node ){
        services.
    }


    @GetMapping("/syncWithNeighbor")
    public Map<String,Boolean> syncWithNeighbor(){

        Node neighbor = currentNode.getNeighbors()[index]; // retrieve the neighboring node by its index
        String url = neighbor.getBaseUrl() + "/sync/agentFileList"; // construct the URL to the neighbor's sync endpoint
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Boolean> agentFileList = restTemplate.getForObject(url, Map.class); // send GET request and retrieve agent's list
        return agentFileList;

    }
}