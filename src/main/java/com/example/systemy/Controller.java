package com.example.systemy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class Controller {
    @Autowired
    private Services services;

//    @PostMapping("/addNode")
//    public void addNode(@RequestBody Node node) {
//        services.addNode(node);
//        System.out.println("node "+node.getNodeName()+ " has been added");
//    }
}