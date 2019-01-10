package no.nav.syfo.consumer.rest.aktor;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.HashMap;
import java.util.List;

@Value
@Builder
@Getter
public class Ident {
    private String ident;
    private String identgruppe;
    private Boolean gjeldende;
}
