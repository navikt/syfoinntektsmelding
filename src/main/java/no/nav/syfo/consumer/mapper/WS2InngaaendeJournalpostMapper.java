package no.nav.syfo.consumer.mapper;

import no.nav.syfo.domain.InngaaendeJournalpost;
import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.WSInngaaendeJournalpost;

import java.util.function.Function;

public abstract class WS2InngaaendeJournalpostMapper {

    public static final Function<WSInngaaendeJournalpost, InngaaendeJournalpost> ws2InngaaendeJouralpost =
            wsInngaaendeJournalpost ->
                    new InngaaendeJournalpost(wsInngaaendeJournalpost.getHoveddokument().getDokumentId());

}
