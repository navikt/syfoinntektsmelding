package no.nav.syfo.consumer.rest;

import lombok.Getter;
import lombok.Value;

@Value
@Getter
class Token {
    private String access_token;
    private String token_type;
    private int expires_in;
}
