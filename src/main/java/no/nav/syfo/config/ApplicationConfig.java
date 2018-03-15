package no.nav.syfo.config;

import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.web.EnablePrometheusTiming;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@EnablePrometheusEndpoint
@EnablePrometheusTiming
@Configuration
@EnableTransactionManagement
public class ApplicationConfig {
}


