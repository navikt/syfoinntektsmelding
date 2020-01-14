package no.nav.syfo;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(exclude = FlywayAutoConfiguration.class)
public class Application {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class)
            .profiles(ClusterAwareSpringProfileResolver.profiles())
            .main(Application.class)
            .run(args);
    }
}
