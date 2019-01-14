package no.nav.syfo.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Getter
@Builder
public class InngaaendeJournal {
    public static final String MIDLERTIDIG = "MIDLERTIDIG";

    String dokumentId;
    String status;
}
