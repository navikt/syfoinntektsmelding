package no.nav.syfo.consumer.rest;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Builder
@Getter
class Ident {
    private String ident;
    private String identgruppe;
    private Boolean gjeldende;
}
