package no.nav.syfo.koin

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnOrganisasjon
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostRequest
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.JournalpostResponse
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.*
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.*
import no.nav.helse.arbeidsgiver.utils.loadFromResources
import no.nav.helse.arbeidsgiver.web.auth.AltinnOrganisationsRepository
import no.nav.syfo.client.norg.ArbeidsfordelingRequest
import no.nav.syfo.client.norg.ArbeidsfordelingResponse
import no.nav.syfo.client.norg.Norg2Client
import org.koin.core.module.Module
import org.koin.dsl.bind
import java.time.LocalDate
import java.time.LocalDate.now

fun Module.mockExternalDependecies() {
    //single { MockAltinnRepo(get()) } bind AltinnOrganisationsRepository::class

    single {
        object : AccessTokenProvider {
            override fun getToken(): String {
                return "token";
            }
        }
    } bind AccessTokenProvider::class

    single {
        object : DokarkivKlient {
            override fun journalførDokument(
                journalpost: JournalpostRequest,
                forsoekFerdigstill: Boolean,
                callId: String
            ): JournalpostResponse {
                return JournalpostResponse("arkiv-ref", true, "J", null, emptyList())
            }
        }
    } bind DokarkivKlient::class

    single {
        object : PdlClient {
            override fun fullPerson(ident: String) =
                PdlHentFullPerson(
                    PdlHentFullPerson.PdlFullPersonliste(
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList()
                    ),

                    PdlHentFullPerson.PdlIdentResponse(listOf(PdlIdent("aktør-id", PdlIdent.PdlIdentGruppe.AKTORID))),

                    PdlHentFullPerson.PdlGeografiskTilknytning(
                        PdlHentFullPerson.PdlGeografiskTilknytning.PdlGtType.UTLAND,
                        null,
                        null,
                        "SWE"
                    )
                )

            override fun personNavn(ident: String) =
                PdlHentPersonNavn.PdlPersonNavneliste(
                    listOf(
                        PdlHentPersonNavn.PdlPersonNavneliste.PdlPersonNavn(
                            "Ola",
                            "M",
                            "Avsender",
                            PdlPersonNavnMetadata("freg")
                        )
                    )
                )
        }

    } bind PdlClient::class

    single {
        object : OppgaveKlient {
            override suspend fun opprettOppgave(
                opprettOppgaveRequest: OpprettOppgaveRequest,
                callId: String
            ): OpprettOppgaveResponse = OpprettOppgaveResponse(
                1234,
                "321",
            "awdawd",
            "SYK",
            1,
            now(),
            Prioritet.NORM,
            Status.OPPRETTET)
        }
    } bind OppgaveKlient::class

    single {
        object : Norg2Client("",
            object : AccessTokenProvider {
                override fun getToken(): String {
                    return "token";
                }
            },
            get()) {
            override suspend fun hentAlleArbeidsfordelinger(
                request: ArbeidsfordelingRequest, callId: String?
            ): List<ArbeidsfordelingResponse> = listOf(ArbeidsfordelingResponse(
                behandlingstema = "string",
                behandlingstype= "string",
                diskresjonskode= "string",
                enhetId= 0,
                enhetNavn= "string",
                enhetNr= "string",
                geografiskOmraade= "string",
                gyldigFra= LocalDate.of(2021,1,1),
                gyldigTil= LocalDate.of(2021,11,1),
                id= 0,
                oppgavetype= "string",
                skalTilLokalkontor= true,
                tema= "string",
                temagruppe= "string"
            ))
        }
    } bind Norg2Client::class
}

class MockAltinnRepo(om: ObjectMapper) : AltinnOrganisationsRepository {
    private val mockList = "altinn-mock/organisasjoner-med-rettighet.json".loadFromResources()
    private val mockAcl = om.readValue<Set<AltinnOrganisasjon>>(mockList)
    override fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon> = mockAcl
}
