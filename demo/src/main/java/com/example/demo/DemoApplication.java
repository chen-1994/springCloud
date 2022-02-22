package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@Slf4j
public class DemoApplication {

    public static void main(String[] args) {
        /*SpringApplication.run(DemoApplication.class, args);*/
        ConfigurableApplicationContext run = SpringApplication.run(DemoApplication.class, args);
        log.info("swagger地址1 http://{}:{}{}/doc.html","127.0.0.1", run.getEnvironment().getProperty("server.port"), run.getEnvironment().getProperty("server.servlet.context-path"));
        log.info("swagger地址2 http://{}:{}{}/swagger-ui/index.html","127.0.0.1", run.getEnvironment().getProperty("server.port"), run.getEnvironment().getProperty("server.servlet.context-path"));
    }

}
