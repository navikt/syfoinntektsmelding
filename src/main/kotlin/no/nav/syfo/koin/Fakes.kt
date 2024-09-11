package no.nav.syfo.koin

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveResponse
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveRequest
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OpprettOppgaveResponse
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.Prioritet
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.Status
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentPersonNavn
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlIdent
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlPersonNavnMetadata
import no.nav.helsearbeidsgiver.pdl.Behandlingsgrunnlag
import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.helsearbeidsgiver.pdl.domene.FullPerson
import no.nav.helsearbeidsgiver.pdl.domene.PersonNavn
import no.nav.syfo.client.dokarkiv.DokArkivClient
import no.nav.syfo.client.norg.ArbeidsfordelingRequest
import no.nav.syfo.client.norg.ArbeidsfordelingResponse
import no.nav.syfo.client.norg.Norg2Client
import org.koin.core.module.Module
import org.koin.dsl.bind
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime

fun Module.mockExternalDependecies() {
    single {
         DokArkivClient("",get()) { "" }
    } bind DokArkivClient::class
    single {
        mockk<PdlClient> {
            coEvery { personNavn(any()) } returns PersonNavn("Ola", "M", "Avsender")
            coEvery { fullPerson(any()) } returns FullPerson(
                navn = PersonNavn(fornavn = "Per", mellomnavn = "", etternavn = "Ulv"),
                foedselsdato = LocalDate.of(1900, 1, 1),
                ident = "aktør-id",
                diskresjonskode = "SPSF",
                geografiskTilknytning = "SWE"
            )
        }

  /*  single {
          PdlClient("", Behandlingsgrunnlag.INNTEKTSMELDING, { "" }) {
            override fun fullPerson(ident: String) =
                PdlHentFullPerson(
                    PdlHentFullPerson.PdlFullPersonliste(
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                    ),
                    PdlHentFullPerson.PdlIdentResponse(listOf(PdlIdent("aktør-id", PdlIdent.PdlIdentGruppe.AKTORID))),
                    PdlHentFullPerson.PdlGeografiskTilknytning(
                        PdlHentFullPerson.PdlGeografiskTilknytning.PdlGtType.UTLAND,
                        null,
                        null,
                        "SWE",
                    ),
                )

            override fun personNavn(ident: String) =
                PdlHentPersonNavn.PdlPersonNavneliste(
                    listOf(
                        PdlHentPersonNavn.PdlPersonNavneliste.PdlPersonNavn(
                            "Ola",
                            "M",
                            "Avsender",
                            PdlPersonNavnMetadata("freg"),
                        ),
                    ),
                )
        }
    } bind PdlClient::class
*/
    single {
        object : OppgaveKlient {
            override suspend fun hentOppgave(
                oppgaveId: Int,
                callId: String,
            ): OppgaveResponse =
                OppgaveResponse(
                    oppgaveId,
                    1,
                    oppgavetype = "SYK",
                    aktivDato = LocalDateTime.now().minusDays(3).toLocalDate(),
                    prioritet = Prioritet.NORM.toString(),
                )

            override suspend fun opprettOppgave(
                opprettOppgaveRequest: OpprettOppgaveRequest,
                callId: String,
            ): OpprettOppgaveResponse =
                OpprettOppgaveResponse(
                    1234,
                    "321",
                    "awdawd",
                    "SYK",
                    1,
                    now(),
                    Prioritet.NORM,
                    Status.OPPRETTET,
                )
        }
    } bind OppgaveKlient::class

    single {
        object : Norg2Client(
            "",
            get(),
        ) {
            override suspend fun hentAlleArbeidsfordelinger(
                request: ArbeidsfordelingRequest,
                callId: String?,
            ): List<ArbeidsfordelingResponse> =
                listOf(
                    ArbeidsfordelingResponse(
                        aktiveringsdato = LocalDate.of(2020, 11, 31),
                        antallRessurser = 0,
                        enhetId = 123456789,
                        enhetNr = "1234",
                        kanalstrategi = null,
                        navn = "NAV Område",
                        nedleggelsesdato = null,
                        oppgavebehandler = false,
                        orgNivaa = "SPESEN",
                        orgNrTilKommunaltNavKontor = "",
                        organisasjonsnummer = null,
                        sosialeTjenester = "",
                        status = "Aktiv",
                        type = "KO",
                        underAvviklingDato = null,
                        underEtableringDato = LocalDate.of(2020, 11, 30),
                        versjon = 1,
                    ),
                )
        }
    } bind Norg2Client::class
}
