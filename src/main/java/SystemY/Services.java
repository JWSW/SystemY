package SystemY;

import SystemY.Agents.FailureAgent;
import SystemY.Threads.MulticastReceiver;
import SystemY.interfaces.MulticastObserver;
import SystemY.Agents.SyncAgent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
        ObjectMapper objectMapper = new ObjectMapper();
        FailureAgent failureAgent = objectMapper.readValue(jsonFailureAgent, FailureAgent.class);
        System.out.println(failureAgent); ///test


        /*Thread FailureAgent1 = new Thread(failureAgent);
        FailureAgent1.start();
        Thread thread = new Thread(() -> {
            FailureAgent failureAgent = new FailureAgent(failureAgent.getInitiatedNodeId());
            // Perform actions with the newFailureAgent
            newFailureAgent.start(); // Replace with your own method name or logic

            // Access the properties of the newFailureAgent object
            String property = newFailureAgent.getProperty(); // Replace with your own property name

            // Perform any necessary operations with the newFailureAgent
            // ...
        });
        thread.start();

         */
    }



}
