package com.example.systemy;

import com.example.systemy.Agents.SyncAgent;
import com.example.systemy.interfaces.MulticastObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

@Service
public class Services implements MulticastObserver {
    private Node node;


    @Autowired
    MulticastReceiver multicastReceiver;
    SyncAgent syncAgent;

    @PostConstruct
    public void init() throws Exception {
        node = new Node(InetAddress.getLocalHost().getHostName(), InetAddress.getLocalHost().getHostAddress());
        new SyncAgent(node);
        multicastReceiver.setObserver(this);
        multicastReceiver.start();
    }

    @PreDestroy
    public void destroy() throws IOException, InterruptedException {
        node.shutDown();
        node.killProcess();
    }


    @Override
    public void onMessageReceived(String message) throws IOException {
        node.multicastHandlePacket(message);
    }

    public Map<String, Boolean> getAgentFileList() {
        return syncAgent.getAgentFileList();
    }
}
