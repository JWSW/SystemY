package SystemY;

import SystemY.Agents.FailureAgent;
import SystemY.Threads.MulticastReceiver;
import SystemY.interfaces.MulticastObserver;
import SystemY.Agents.SyncAgent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class Services implements MulticastObserver {
    private Node node;

    @Autowired
    MulticastReceiver multicastReceiver;
    SyncAgent syncAgent;



    @PostConstruct
    public void init() throws Exception {
        node = new Node(InetAddress.getLocalHost().getHostName(), InetAddress.getLocalHost().getHostAddress());
        syncAgent = new SyncAgent(node);
        Thread syncAgent1 = new Thread(syncAgent);
        syncAgent1.start();
        multicastReceiver.setObserver(this);
        multicastReceiver.start();
    }

    @PreDestroy
    public void destroy() throws IOException, InterruptedException {
        node.shutDown();
        node.killProcess();
    }

    public void setNewFile(String filename, String base64Content, int nodeID, String nodeIP) {
        byte[] fileContent = Base64.getDecoder().decode(base64Content);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream("/home/Dist/SystemY/replicatedFiles/" + filename);
            fileOutputStream.write(fileContent);
            fileOutputStream.close();

            System.out.println("File saved successfully.");
            node.setOwnerFile(filename, nodeID,nodeIP);
        }catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }

    public void setFileNeighbors(String filename, Integer nodeID, ConcurrentHashMap<Integer,String> fileNodeLocationsMap){
        node.setFileLocations(filename, nodeID,fileNodeLocationsMap);
    }

    public void notifyTerminated(String filename, Integer nodeID){
        node.isTerminated(filename,nodeID);
    }

    @Override
    public void onMessageReceived(String message) throws IOException {
        node.multicastHandlePacket(message);
    }

    public Map<String, Boolean> getAgentFileList() {
        return syncAgent.getAgentFileList();
    }

    public Node getNode() {
        return node;
    }


    public void processFailureAgent(String jsonFailureAgent) throws IOException {
        System.out.println("processFailureAgent");
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Integer> failureAgentParams = objectMapper.readValue(jsonFailureAgent, new TypeReference<>() {});
        int failingID = failureAgentParams.get("failingID");
        int initiatingNodeID = failureAgentParams.get("initiatingNodeID");

        // Create a new FailureAgent instance with the extracted parameters
        FailureAgent failureAgent = new FailureAgent(failingID, node.getCurrentID(),initiatingNodeID, node);

        Thread FailureAgent1 = new Thread(failureAgent);
        FailureAgent1.start();
    }



}
