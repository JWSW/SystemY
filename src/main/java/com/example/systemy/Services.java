package com.example.systemy;

import com.example.systemy.Agents.SyncAgent;
import com.example.systemy.Threads.MulticastReceiver;
import com.example.systemy.interfaces.MulticastObserver;
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
//            node.setOwnerFile(filename, nodeID,nodeIP);
        }catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }

    public void setFileNeighbors(String filename, Integer nodeID, ConcurrentHashMap<Integer,String> fileNodeLocationsMap){
        node.setFileLocations(filename, nodeID,fileNodeLocationsMap);
    }

    @Override
    public void onMessageReceived(String message) throws IOException {
        node.multicastHandlePacket(message);
    }

    public Map<String, Boolean> getAgentFileList() {
        return syncAgent.getAgentFileList();
    }
}
