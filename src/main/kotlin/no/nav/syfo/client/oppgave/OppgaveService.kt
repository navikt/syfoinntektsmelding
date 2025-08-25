package no.nav.syfo.client.oppgave

import no.nav.helsearbeidsgiver.oppgave.OppgaveClient
import no.nav.helsearbeidsgiver.oppgave.domain.HentOppgaverRequest
import no.nav.helsearbeidsgiver.oppgave.domain.OppgaveListeResponse
import no.nav.helsearbeidsgiver.oppgave.domain.Oppgavetype
import no.nav.helsearbeidsgiver.oppgave.domain.OpprettOppgaveRequest
import no.nav.helsearbeidsgiver.oppgave.domain.Prioritet
import no.nav.helsearbeidsgiver.oppgave.domain.Statuskategori
import no.nav.helsearbeidsgiver.oppgave.domain.Tema
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.syfo.behandling.HentOppgaveException
import no.nav.syfo.behandling.OpprettFordelingsOppgaveException
import no.nav.syfo.behandling.OpprettOppgaveException
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.BehandlingsKategori
import java.time.DayOfWeek
import java.time.LocalDate

class OppgaveService(
    val oppgaveClient: OppgaveClient,
    val metrikk: Metrikk,
) {
    private val logger = logger()
    private val sikkerlogger = sikkerLogger()

    private suspend fun hentOppgave(
        oppgavetype: String,
        journalpostId: String,
    ): OppgaveListeResponse =
        oppgaveClient
            .hentOppgaver(
                HentOppgaverRequest(
                    oppgavetype = oppgavetype,
                    tema = Tema.SYK,
                    statuskategori = Statuskategori.AAPEN,
                    journalpostId = journalpostId,
                    sorteringsrekkefolge = "ASC",
                    sorteringsfelt = "FRIST",
                ),
            ).also { logger.info("Hentet oppgave for journalpostId {}", journalpostId) }

    private suspend fun hentHvisOppgaveFinnes(
        oppgavetype: String,
        journalpostId: String,
    ): OppgaveResultat? {
        try {
            val oppgaveResponse = hentOppgave(oppgavetype = oppgavetype, journalpostId = journalpostId)
            return oppgaveResponse.oppgaver
                .firstOrNull()
                ?.id
                ?.let { OppgaveResultat(oppgaveId = it, duplikat = true, utbetalingBruker = false) }
        } catch (ex: Exception) {
            "Feil ved sjekking av eksisterende oppgave".also {
                logger.error(it)
                sikkerlogger.error(it, ex)
            }
            throw HentOppgaveException(journalpostId, oppgavetype, ex)
        }
    }

    suspend fun opprettOppgave(
        journalpostId: String,
        aktoerId: String,
        behandlingsKategori: BehandlingsKategori,
    ): OppgaveResultat {
        val eksisterendeOppgave = hentHvisOppgaveFinnes(Oppgavetype.INNTEKTSMELDING, journalpostId)
        metrikk.tellOpprettOppgave(eksisterendeOppgave != null)
        if (eksisterendeOppgave != null) {
            logger.info("Det finnes allerede journalføringsoppgave for journalpost $journalpostId")
            return eksisterendeOppgave
        }

        logger.info("Oppretter oppgave for journalpost $journalpostId")
        sikkerlogger.info("Oppretter oppgave: ${behandlingsKategori.oppgaveBeskrivelse} for journalpost $journalpostId")

        val opprettOppgaveRequest =
            OpprettOppgaveRequest(
                aktoerId = aktoerId,
                journalpostId = journalpostId,
                behandlesAvApplikasjon = "FS22",
                beskrivelse = "Det har kommet en inntektsmelding på sykepenger.",
                tema = Tema.SYK,
                oppgavetype = Oppgavetype.INNTEKTSMELDING,
                behandlingstype = behandlingsKategori.getBehandlingsType(),
                behandlingstema = behandlingsKategori.getBehandlingstema(),
                aktivDato = LocalDate.now(),
                fristFerdigstillelse = leggTilEnVirkeuke(LocalDate.now()),
                prioritet = Prioritet.NORM,
            )

        logger.info("Oppretter journalføringsoppgave")
        try {
            return OppgaveResultat(oppgaveClient.opprettOppgave(opprettOppgaveRequest).id, false, behandlingsKategori.getUtbetalingBruker())
        } catch (ex: Exception) {
            throw OpprettOppgaveException(journalpostId, ex)
        }
    }

    suspend fun opprettFordelingsOppgave(journalpostId: String): OppgaveResultat {
        val eksisterendeOppgave = hentHvisOppgaveFinnes(Oppgavetype.FORDELINGSOPPGAVE, journalpostId)

        if (eksisterendeOppgave != null) {
            logger.info("Det finnes allerede fordelingsoppgave for journalpost $journalpostId")
            return eksisterendeOppgave
        }

        val opprettOppgaveRequest =
            OpprettOppgaveRequest(
                journalpostId = journalpostId,
                behandlesAvApplikasjon = "FS22",
                beskrivelse = "Fordelingsoppgave for inntektsmelding på sykepenger",
                tema = Tema.SYK,
                oppgavetype = Oppgavetype.FORDELINGSOPPGAVE,
                aktivDato = LocalDate.now(),
                fristFerdigstillelse = leggTilEnVirkeuke(LocalDate.now()),
                prioritet = Prioritet.NORM,
            )
        logger.info("Oppretter fordelingsoppgave for journalpost $journalpostId")
        try {
            return OppgaveResultat(oppgaveClient.opprettOppgave(opprettOppgaveRequest).id, false, false)
        } catch (ex: Exception) {
            throw OpprettFordelingsOppgaveException(journalpostId, ex)
        }
    }

    private fun leggTilEnVirkeuke(dato: LocalDate): LocalDate =
        when (dato.dayOfWeek) {
            DayOfWeek.SATURDAY -> dato.plusDays(9)
            DayOfWeek.SUNDAY -> dato.plusDays(8)
            else -> dato.plusDays(7)
        }
}
