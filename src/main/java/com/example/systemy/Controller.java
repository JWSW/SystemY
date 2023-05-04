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
        System.out.println("node "+node.getNodeName()+ " has been added");
    }

    @PostMapping("/{nodeName}/removeNode")
    public void removeNode(@PathVariable String nodeName) {
        services.removeNode(nodeName);
        System.out.println("node "+nodeName+ " has been removed");
    }

    @PostMapping("/{nodeHashId}/removeNodeByHashId")
    public void removeNodeByHash(@PathVariable Integer nodeHashId) {
        services.removeNodeByHash(nodeHashId);
    }

    @GetMapping("/{nodeName}/getHash")
    public void getHash(@PathVariable String nodeName) {
        System.out.println("hash of "+nodeName+" is "+services.getHash(nodeName));
    }

    @GetMapping("/{filename}/getFileLocation")
    public Map<Integer,String> getFileLocation(@PathVariable String filename ) {
        return services.getFile(filename);
    }

    @GetMapping("/{nodeHashId}/getNext")
    public String getNext(@PathVariable Integer nodeHashId) {
        return services.getNext(nodeHashId);
    }

    @GetMapping("/{nodeHashId}/getPrevious")
    public String getPrevious(@PathVariable Integer nodeHashId) {
        return services.getPrevious(nodeHashId);
    }
}