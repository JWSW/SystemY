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
        System.out.println("node with "+nodeHashId+" has been removed");
    }

    @GetMapping("/{nodeName}/getHash")
    public void getHash(@PathVariable String nodeName) {
        System.out.println("hash of "+nodeName+" is "+services.getHash(nodeName));
    }

    @GetMapping("/{filename}/getFileLocation")
    public Map<Integer,String> getFileLocation(@PathVariable String filename ) {
        return services.getFile(filename);
    }

    @GetMapping("/getNext")
    public String getNext(@RequestBody Node node) {
        return services.getNext(services.getHash(node.getNodeName()));
    }

    @GetMapping("/getPrevious")
    public String getPrevious(@RequestBody Node node) {
        return services.getPrevious(services.getHash(node.getNodeName()));
    }
}