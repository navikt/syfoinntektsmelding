package no.nav.syfo;

import lombok.extern.slf4j.Slf4j;
import org.h2.tools.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.sql.SQLException;

@SpringBootApplication
@Slf4j
public class LocalApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocalApplication.class, args);
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
