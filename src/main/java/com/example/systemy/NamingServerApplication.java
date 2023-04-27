package com.example.systemy;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NamingServerApplication {

    @Autowired
    private MulticastReceive multicastReceive;

    public static void main(String[] args) {
        SpringApplication.run(NamingServerApplication.class, args);
    }

    @PostConstruct
    public void startMulticastReceiver() {
        multicastReceive.start();
    }
}
