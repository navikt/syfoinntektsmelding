package no.nav.syfo.consumer.rest;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.List;

@Value
@Builder
@Getter
class Ident {
    private String ident;
    private String identgruppe;
    private Boolean gjeldende;
}

@Value
@Builder
@Getter
class AktorResponse {
    private List<Ident> identer;
    private String feilmelding;
}
