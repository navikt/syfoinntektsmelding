package no.nav.syfo.client

import io.ktor.client.HttpClient
import no.nav.helsearbeidsgiver.oppgave.OppgaveClient
import no.nav.helsearbeidsgiver.oppgave.domain.Behandlingstema
import no.nav.helsearbeidsgiver.oppgave.domain.Behandlingstype
import no.nav.helsearbeidsgiver.oppgave.domain.HentOppgaverRequest
import no.nav.helsearbeidsgiver.oppgave.domain.OppgaveListeResponse
import no.nav.helsearbeidsgiver.oppgave.domain.Oppgavetype
import no.nav.helsearbeidsgiver.oppgave.domain.OpprettOppgaveRequest
import no.nav.helsearbeidsgiver.oppgave.domain.Prioritet
import no.nav.helsearbeidsgiver.oppgave.domain.Statuskategori
import no.nav.helsearbeidsgiver.oppgave.domain.Tema
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
    val oppgavebehandlingUrl: String,
    val httpClient: HttpClient,
    val metrikk: Metrikk,
    val getAccessToken: () -> String,
) {
    private val sikkerlogger = sikkerLogger()
    private val oppgaveClient = OppgaveClient(oppgavebehandlingUrl, getAccessToken)

    private suspend fun hentOppgave(
        oppgavetype: String,
        journalpostId: String,
    ): OppgaveListeResponse {
        return oppgaveClient
            .hentOppgaver(
                HentOppgaverRequest(
                    oppgavetype = oppgavetype,
                    tema = Tema.SYK,
                    statuskategori = Statuskategori.AAPEN,
                    journalpostId = journalpostId,
                ),
            ).also { sikkerlogger.info("Hentet oppgave for journalpostId {}", journalpostId) }
        /*
       //TODO: Legg til sorteringsfelt og sorteringsrekkefolge hvis de brukes
                parameter("sorteringsrekkefolge", "ASC")
                parameter("sorteringsfelt", "FRIST")
         */
    }

    private suspend fun hentHvisOppgaveFinnes(
        oppgavetype: String,
        journalpostId: String,
    ): OppgaveResultat? {
        try {
            val oppgaveResponse = hentOppgave(oppgavetype = oppgavetype, journalpostId = journalpostId)
            return if (oppgaveResponse.antallTreffTotalt > 0) {
                oppgaveResponse.oppgaver
                    .first()
                    .id
                    ?.let { OppgaveResultat(it, true, false) }
            } else {
                null
            }
        } catch (ex: Exception) {
            sikkerlogger.error("Feil ved sjekking av eksisterende oppgave", ex)
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
            sikkerlogger.info("Det finnes allerede journalføringsoppgave for journalpost $journalpostId")
            return eksisterendeOppgave
        }

        fun loggOppgave(type: String) {
            sikkerlogger.info("Oppretter oppgave: $type for journalpost $journalpostId")
        }

        val (behandlingstype, behandlingstema, utbetalingBruker) =
            when (behandlingsKategori) {
                BehandlingsKategori.SPEIL_RELATERT -> {
                    loggOppgave("Speil")
                    Triple(null, Behandlingstema.SPEIL, false)
                }

                BehandlingsKategori.UTLAND -> {
                    loggOppgave("Utland")
                    Triple(Behandlingstype.UTLAND, null, false)
                }

                BehandlingsKategori.BESTRIDER_SYKEMELDING -> {
                    loggOppgave("Bestrider sykmelding")
                    Triple(null, Behandlingstema.BESTRIDER_SYKEMELDING, false)
                }

                BehandlingsKategori.IKKE_REFUSJON,
                BehandlingsKategori.REFUSJON_MED_DATO,
                BehandlingsKategori.REFUSJON_LITEN_LØNN,
                -> {
                    loggOppgave("Utbetaling til bruker")
                    Triple(null, Behandlingstema.UTBETALING_TIL_BRUKER, true)
                }

                else -> {
                    loggOppgave("Normal")
                    Triple(null, Behandlingstema.NORMAL, false)
                }
            }

        val opprettOppgaveRequest =
            OpprettOppgaveRequest(
                aktoerId = aktoerId,
                journalpostId = journalpostId,
                behandlesAvApplikasjon = "FS22",
                beskrivelse = "Det har kommet en inntektsmelding på sykepenger.",
                tema = Tema.SYK,
                oppgavetype = Oppgavetype.INNTEKTSMELDING,
                behandlingstype = behandlingstype,
                behandlingstema = behandlingstema,
                aktivDato = LocalDate.now(),
                fristFerdigstillelse = leggTilEnVirkeuke(LocalDate.now()),
                prioritet = Prioritet.NORM,
            )
        sikkerlogger.info("Oppretter journalføringsoppgave")
        try {
            return OppgaveResultat(oppgaveClient.opprettOppgave(opprettOppgaveRequest).id, false, utbetalingBruker)
        } catch (ex: Exception) {
            throw OpprettOppgaveException(journalpostId, ex)
        }
    }

    suspend fun opprettFordelingsOppgave(journalpostId: String): OppgaveResultat {
        val eksisterendeOppgave = hentHvisOppgaveFinnes(Oppgavetype.FORDELINGSOPPGAVE, journalpostId)

        if (eksisterendeOppgave != null) {
            sikkerlogger.info("Det finnes allerede fordelingsoppgave for journalpost $journalpostId")
            return eksisterendeOppgave
        }

        val behandlingstype: String? = null

        val opprettOppgaveRequest =
            OpprettOppgaveRequest(
                journalpostId = journalpostId,
                behandlesAvApplikasjon = "FS22",
                beskrivelse = "Fordelingsoppgave for inntektsmelding på sykepenger",
                tema = Tema.SYK,
                oppgavetype = Oppgavetype.FORDELINGSOPPGAVE,
                behandlingstype = behandlingstype,
                aktivDato = LocalDate.now(),
                fristFerdigstillelse = leggTilEnVirkeuke(LocalDate.now()),
                prioritet = Prioritet.NORM,
            )
        sikkerlogger.info("Oppretter fordelingsoppgave for journalpost $journalpostId")
        try {
            return OppgaveResultat(oppgaveClient.opprettOppgave(opprettOppgaveRequest).id, false, false)
        } catch (ex: Exception) {
            throw OpprettFordelingsOppgaveException(journalpostId, ex)
        }
    }

    fun leggTilEnVirkeuke(dato: LocalDate): LocalDate =
        when (dato.dayOfWeek) {
            DayOfWeek.SATURDAY -> dato.plusDays(9)
            DayOfWeek.SUNDAY -> dato.plusDays(8)
            else -> dato.plusDays(7)
        }
}
