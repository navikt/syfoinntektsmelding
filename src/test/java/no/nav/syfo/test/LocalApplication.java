package no.nav.syfo.test;

import lombok.extern.slf4j.*;
import no.nav.syfo.*;
import org.h2.tools.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.*;

import java.sql.*;

@SpringBootApplication(exclude = FlywayAutoConfiguration.class)
@Slf4j
public class LocalApplication {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @Profile("local")
    public Server server() {
        try {
            return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "8081").start();
        } catch (SQLException e) {
            log.error("Klarte ikke starte databasekobling", e);
            throw new RuntimeException("Klarte ikke starte databasekobling", e);
        }
    }
}
