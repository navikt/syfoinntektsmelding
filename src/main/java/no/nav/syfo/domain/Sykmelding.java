package no.nav.syfo.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Sykmelding {

    private String uuid;
    private String status;

}
