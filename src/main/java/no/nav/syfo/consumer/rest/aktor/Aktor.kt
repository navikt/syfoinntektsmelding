package no.nav.syfo.consumer.rest.aktor;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.List;

@Value
@Builder
@Getter
public class Aktor {
    private List<Ident> identer;
    private String feilmelding;
}
