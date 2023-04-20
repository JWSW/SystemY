package com.example.systemy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/requestName")
public class Controller {
    @Autowired
    private Services services;

    @PostMapping("/{nodeName}/{ipAddr}/addNode")
    public void addMoney(@PathVariable String nodeName, @PathVariable String ipAddr) {
        services.addNode(nodeName, ipAddr);
    }

    @PostMapping("/{nodeName}/removeNode")
    public void removeMoney(@PathVariable String nodeName) {
        services.removeNode(nodeName);
    }
    @GetMapping("/{filename}/getFileLocation")
    public int getFileLocation(@PathVariable String filename ) {
        return services.getHash(filename);
    }
}

