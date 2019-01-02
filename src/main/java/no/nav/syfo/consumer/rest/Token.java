package no.nav.syfo.consumer.rest;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Builder
@Getter
class Token {
    private String access_token;
    private String token_type;
    private int expires_in;
}
