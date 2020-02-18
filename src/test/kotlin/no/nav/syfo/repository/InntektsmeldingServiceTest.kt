package no.nav.syfo.repository

import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Refusjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(SpringRunner::class)
@TestPropertySource(locations = ["classpath:application-test.properties"])
@SpringBootTest
class InntektsmeldingServiceTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            System.setProperty("SECURITYTOKENSERVICE_URL", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_USERNAME", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_PASSWORD", "joda")
        }
    }

    @Autowired
    private lateinit var inntektsmeldingService: InntektsmeldingService

    @Test
    fun `Skal lagre hele inntektsmelding som JSON`(){
        val id = "id-abc"
        val fnr = "fnr-123"
        val aktørId = "aktør-123"
        val saksId = "sak-123"
        val arkivReferanse = "ar-123"
        val arsakTilInnsending = "Ingen årsak"
        val journalpostId = "jp-123"
        val arbeidsforholdId = "arb-123"
        val arbeidsgiverPrivatFnr = "arb-priv-123"
        val arbeidsgiverOrgnummer = "arb-org-123"
        val arbeidsgiverPrivatAktørId = "arb-priv-aktør-123"
        val refusjon = 200
        val beregnetInntekt = 300

        val im = Inntektsmelding(
            id = id,
            fnr = fnr,
            aktorId = aktørId,
            refusjon = Refusjon(BigDecimal(refusjon), LocalDate.of(2020,2,20)),
            begrunnelseRedusert = "Grunn til reduksjon",
            sakId = saksId,
            mottattDato = LocalDateTime.of(2010,5,4, 3, 2, 1),
            arkivRefereranse = arkivReferanse,
            førsteFraværsdag = LocalDate.of(2010,2,10),
            arsakTilInnsending = arsakTilInnsending,
            journalpostId = journalpostId,
            arbeidsforholdId = arbeidsforholdId,
            arbeidsgiverPrivatFnr = arbeidsgiverPrivatFnr,
            arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
            arbeidsgiverPrivatAktørId = arbeidsgiverPrivatAktørId,
            beregnetInntekt = BigDecimal(beregnetInntekt),
            gyldighetsStatus = Gyldighetsstatus.GYLDIG,
            journalStatus = JournalStatus.MIDLERTIDIG,




            arbeidsgiverperioder = emptyList(),
            endringerIRefusjon = emptyList(),
            feriePerioder = emptyList(),
            gjenopptakelserNaturalYtelse = emptyList(),
            opphørAvNaturalYtelse = emptyList()
        )
        val json = inntektsmeldingService.mapString(im)

        val mapper = JacksonJsonConfig.objectMapperFactory.opprettObjectMapper()
        val node = mapper.readTree(json)

        assertThat(node.get("id").asText()).isEqualTo(id)
        assertThat(node.get("fnr").asText()).isEqualTo(fnr)
        assertThat(node.get("aktorId").asText()).isEqualTo(aktørId)
        assertThat(node.get("refusjon").get("beloepPrMnd").asInt()).isEqualTo(refusjon)
        assertThat(node.get("refusjon").get("opphoersdato").asText()).isEqualTo("2020-02-20")
        assertThat(node.get("begrunnelseRedusert").asText()).isEqualTo("Grunn til reduksjon")
        assertThat(node.get("sakId").asText()).isEqualTo(saksId)
        assertThat(node.get("mottattDato").asText()).isEqualTo("2010-05-04T03:02:01")
        assertThat(node.get("arkivRefereranse").asText()).isEqualTo(arkivReferanse)
        assertThat(node.get("førsteFraværsdag").asText()).isEqualTo("2010-02-10")
        assertThat(node.get("arsakTilInnsending").asText()).isEqualTo(arsakTilInnsending)
        assertThat(node.get("journalpostId").asText()).isEqualTo(journalpostId)
        assertThat(node.get("arbeidsforholdId").asText()).isEqualTo(arbeidsforholdId)
        assertThat(node.get("arbeidsgiverPrivatFnr").asText()).isEqualTo(arbeidsgiverPrivatFnr)
        assertThat(node.get("arbeidsgiverOrgnummer").asText()).isEqualTo(arbeidsgiverOrgnummer)
        assertThat(node.get("arbeidsgiverPrivatAktørId").asText()).isEqualTo(arbeidsgiverPrivatAktørId)
        assertThat(node.get("beregnetInntekt").asInt()).isEqualTo(beregnetInntekt)
        assertThat(node.get("gyldighetsStatus").asText()).isEqualTo(Gyldighetsstatus.GYLDIG.toString())
        assertThat(node.get("journalStatus").asText()).isEqualTo(JournalStatus.MIDLERTIDIG.toString())



    }

}

