package com.example.systemy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/requestName")
public class Controller {
    @Autowired
    private Services services;

    @PostMapping("/addNode")
    public void addNode(@RequestBody Node node) {
        services.addNode(node);
    }

    @PostMapping("/{nodeName}/removeNode")
    public void removeNode(@PathVariable String nodeName) {
        services.removeNode(nodeName);
    }

    @PostMapping("/{nodeHashId}/removeNodeByHashId")
    public void removeNodeByHash(@PathVariable Integer nodeHashId) {
        services.removeNodeByHash(nodeHashId);
    }

    @GetMapping("/{filename}/getFileLocation")
    public Map<Integer,String> getFileLocation(@PathVariable String filename ) {
        return services.getFile(filename);
    }
}

