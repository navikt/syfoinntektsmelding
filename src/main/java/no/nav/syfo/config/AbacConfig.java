//package no.nav.syfo.config;
//
//import no.nav.freg.abac.spring.consumer.AbacRequestMapper;
//import no.nav.freg.abac.spring.consumer.AbacResponseMapper;
//import no.nav.freg.abac.spring.consumer.AbacRestTemplateConsumer;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.client.support.BasicAuthorizationInterceptor;
//import org.springframework.web.client.RestTemplate;
//
//@Configuration
//@Import(no.nav.freg.abac.spring.config.AbacConfig.class)
//public class AbacConfig {
//    @Value("${ABAC_URL}")
//    private String url;
//    @Value("${SERVICEUSER_USERNAME}")
//    private String username;
//    @Value("${SERVICEUSER_PASSWORD}")
//    private String password;
//
//    @Bean
//    public AbacRestTemplateConsumer abacRestTemplateConsumer() {
//        return new AbacRestTemplateConsumer(abacRestTemplate(), url, new AbacRequestMapper(), new AbacResponseMapper());
//    }
//
//    private RestTemplate abacRestTemplate() {
//        RestTemplate template = new RestTemplate();
//        template.getInterceptors().add(new BasicAuthorizationInterceptor(username, password));
//        return template;
//    }
//}