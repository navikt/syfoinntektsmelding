package no.nav.syfo.config;

import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class OidcTokenValidatorConfig {
    @Bean
    public VerificationKeyResolver verificationKeyResolver(@Value("${ISSO-JWKS_URL}") String issoJwksUrl) {
        return new HttpsJwksVerificationKeyResolver(new HttpsJwks(issoJwksUrl));
    }
}