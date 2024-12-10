package no.nav.syfo.koin

import io.mockk.coEvery
import io.mockk.mockk
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

fun Module.mockExternalDependecies() {
    single {
        DokArkivClient("", get()) { "" }
    } bind DokArkivClient::class
    single {
        mockk<PdlClient> {
            coEvery { personNavn(any()) } returns PersonNavn("Ola", "M", "Avsender")
            coEvery { fullPerson(any()) } returns
                FullPerson(
                    navn = PersonNavn(fornavn = "Per", mellomnavn = "", etternavn = "Ulv"),
                    foedselsdato = LocalDate.of(1900, 1, 1),
                    ident = "aktør-id",
                    diskresjonskode = "SPSF",
                    geografiskTilknytning = "SWE",
                )
        }
    }

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
