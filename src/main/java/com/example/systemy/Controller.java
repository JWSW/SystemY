package com.example.systemy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/requestNode")
public class Controller {
    @Autowired
    private Services services;

    @PostMapping("/{filename}/{nodeID}/{nodeIP}/sendNewFile")
    public void sendNewFile (@PathVariable String filename, @PathVariable String nodeID, @PathVariable String nodeIP, @RequestBody String base64Content){
        services.setNewFile(filename, base64Content, Integer.parseInt(nodeID), nodeIP);
    }


    @GetMapping("/syncWithNeighbor")
    public Map<String,Boolean> syncWithNeighbor(){

    }
}