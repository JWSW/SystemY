package com.example.systemy;

import com.example.systemy.interfaces.MulticastObserver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class Services implements MulticastObserver {
    private Node node;


    @Autowired
    MulticastReceive multicastReceive;

    @PostConstruct
    public void init() throws Exception {
        node = new Node(InetAddress.getLocalHost().getHostName(), InetAddress.getLocalHost().getHostAddress());
        multicastReceive.setObserver(this);
        multicastReceive.start();
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


}
