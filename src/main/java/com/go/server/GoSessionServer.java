package com.go.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class GoSessionServer {
    public static void main(final String[] args) {
        SpringApplication.run(GoSessionServer.class, args);
    }
}
