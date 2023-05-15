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

    }
}