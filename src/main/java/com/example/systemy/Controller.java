package com.example.systemy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/requestName")
public class Controller {
    @Autowired
    private Services services;

    @PostMapping("/{nodeName}/{ipAddr}/addNode")
    public void addMoney(@PathVariable String name, @PathVariable String ipAddr) {
        services.addNode(name, ipAddr);
    }

    @PostMapping("/{nodeName}/{ipAddr}/removeNode")
    public void removeMoney(@PathVariable String name, @PathVariable String ipAddr) {
        services.removeNode(name, ipAddr);
    }
}

