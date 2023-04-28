package com.example.systemy;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NamingServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NamingServerApplication.class, args);
    }

}
