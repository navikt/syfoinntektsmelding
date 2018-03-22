package no.nav.syfo.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Sykepengesoknad {
    private String uuid;
    private String status;
}
