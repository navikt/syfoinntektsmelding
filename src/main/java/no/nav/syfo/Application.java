package no.nav.syfo;

import org.h2.tools.Server;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.sql.SQLException;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class)
            .main(Application.class)
            .run(args);
    }

    @Bean
    @Profile("local")
    public Server server() {
        try {
            return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "8081").start();
        } catch (SQLException e) {
            throw new RuntimeException("Klarte ikke starte databasekobling", e);
        }
    }

}
