package SystemY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    @PostMapping("/{filename}/{nodeID}/sendFileLocations")
    public void sendNewFile (@PathVariable String filename,@PathVariable String nodeID, @RequestBody String jsonData){
        String base64Content="";
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ConcurrentHashMap<Integer, String> fileNodeLocations = objectMapper.readValue(jsonData, new TypeReference<ConcurrentHashMap<Integer, String>>() {});
            services.setFileNeighbors(filename, Integer.valueOf(nodeID),fileNodeLocations);
        }catch (JsonMappingException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


    @GetMapping("/syncWithNeighbor")
    public Map<String,Boolean> syncWithNeighbor(){
        return services.getAgentFileList();
    }
}