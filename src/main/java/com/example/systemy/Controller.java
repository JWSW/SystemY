package com.example.systemy;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/requestNode")
public class Controller {
    @Autowired
    private Services services;

    @PostMapping("/{filename}/{nodeID}/{nodeIP}/sendNewFile")
    public void sendNewFile (@PathVariable String filename, @PathVariable String nodeID, @PathVariable String nodeIP, @RequestBody String jsonData){
        String base64Content="";
        try {
            // Parse the JSON string to a JSON object
            JSONObject jsonObject = new JSONObject(jsonData);

            // Extract the file data field from the JSON object
            base64Content = jsonObject.getString("fileData");
        }catch (JSONException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
        services.setNewFile(filename, base64Content, Integer.parseInt(nodeID), nodeIP);
    }


    @GetMapping("/syncWithNeighbor")
    public Map<String,Boolean> syncWithNeighbor(){
        return services.getAgentFileList();
    }
}