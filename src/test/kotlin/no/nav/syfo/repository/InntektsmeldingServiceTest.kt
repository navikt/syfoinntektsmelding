package no.nav.syfo.repository

import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.*
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
    fun `Verifiserer at object mapper gir forventet JSON format`(){
        val im = Inntektsmelding(
            id = "id-abc",
            fnr = "fnr-123",
            aktorId = "aktør-123",
            refusjon = Refusjon(BigDecimal(333333333333), LocalDate.of(2020,2,20)),
            begrunnelseRedusert = "Grunn til reduksjon",
            sakId = "sak-123",
            mottattDato = LocalDateTime.of(2010,5,4, 3, 2, 1),
            arkivRefereranse = "ar-123",
            førsteFraværsdag = LocalDate.of(2010,2,10),
            arsakTilInnsending = "Ingen årsak",
            journalpostId = "jp-123",
            arbeidsforholdId = "arb-123",
            arbeidsgiverPrivatFnr =  "arb-priv-123",
            arbeidsgiverOrgnummer = "arb-org-123",
            arbeidsgiverPrivatAktørId = "arb-priv-aktør-123",
            beregnetInntekt = BigDecimal(999999999999),
            gyldighetsStatus = Gyldighetsstatus.GYLDIG,
            journalStatus = JournalStatus.MIDLERTIDIG,

            arbeidsgiverperioder = listOf(
                Periode(fom=LocalDate.of(2011, 11, 1), tom= LocalDate.of(2012,12,2)),
                Periode(fom=LocalDate.of(2013, 3, 3), tom= LocalDate.of(2014,4,4))
            ),
            endringerIRefusjon = listOf(
                EndringIRefusjon(LocalDate.of(2015,5,5), BigDecimal(555555555555)),
                EndringIRefusjon(LocalDate.of(2016,6,6), BigDecimal(666666666666))
            ),
            feriePerioder = listOf(
                Periode(fom=LocalDate.of(2017, 7, 7), tom= LocalDate.of(2018,8,8)),
                Periode(fom=LocalDate.of(2019, 9, 9), tom= LocalDate.of(2020,12,20))
            ),
            gjenopptakelserNaturalYtelse = listOf(
                GjenopptakelseNaturalytelse(Naturalytelse.BOLIG, LocalDate.of(2011,1,1), BigDecimal(111111111111)),
                GjenopptakelseNaturalytelse(Naturalytelse.KOSTDAGER, LocalDate.of(2012,2,2), BigDecimal(222222222222))
            ),
            opphørAvNaturalYtelse = listOf(
                OpphoerAvNaturalytelse(Naturalytelse.BIL, LocalDate.of(2015,5,5), BigDecimal(555555555555)),
                OpphoerAvNaturalytelse(Naturalytelse.TILSKUDDBARNEHAGEPLASS, LocalDate.of(2016,6,6), BigDecimal(666666666666))
            )
        )
        val json = inntektsmeldingService.mapString(im)

        val mapper = JacksonJsonConfig.objectMapperFactory.opprettObjectMapper()
        val node = mapper.readTree(json)

        assertThat(node.get("id").asText()).isEqualTo("id-abc")
        assertThat(node.get("fnr").asText()).isEqualTo("fnr-123")
        assertThat(node.get("aktorId").asText()).isEqualTo("aktør-123")
        assertThat(node.get("refusjon").get("beloepPrMnd").asLong()).isEqualTo(333333333333)
        assertThat(node.get("refusjon").get("opphoersdato").asText()).isEqualTo("2020-02-20")
        assertThat(node.get("begrunnelseRedusert").asText()).isEqualTo("Grunn til reduksjon")
        assertThat(node.get("sakId").asText()).isEqualTo("sak-123")
        assertThat(node.get("mottattDato").asText()).isEqualTo("2010-05-04T03:02:01")
        assertThat(node.get("arkivRefereranse").asText()).isEqualTo("ar-123")
        assertThat(node.get("førsteFraværsdag").asText()).isEqualTo("2010-02-10")
        assertThat(node.get("arsakTilInnsending").asText()).isEqualTo("Ingen årsak")
        assertThat(node.get("journalpostId").asText()).isEqualTo("jp-123")
        assertThat(node.get("arbeidsforholdId").asText()).isEqualTo("arb-123")
        assertThat(node.get("arbeidsgiverPrivatFnr").asText()).isEqualTo( "arb-priv-123")
        assertThat(node.get("arbeidsgiverOrgnummer").asText()).isEqualTo("arb-org-123")
        assertThat(node.get("arbeidsgiverPrivatAktørId").asText()).isEqualTo("arb-priv-aktør-123")
        assertThat(node.get("beregnetInntekt").asLong()).isEqualTo(999999999999)
        assertThat(node.get("gyldighetsStatus").asText()).isEqualTo("GYLDIG")
        assertThat(node.get("journalStatus").asText()).isEqualTo("MIDLERTIDIG")

        assertThat(node.get("arbeidsgiverperioder").size()).isEqualTo(2)
        assertThat(node.get("arbeidsgiverperioder")[0].get("fom").asText()).isEqualTo("2011-11-01")
        assertThat(node.get("arbeidsgiverperioder")[0].get("tom").asText()).isEqualTo("2012-12-02")
        assertThat(node.get("arbeidsgiverperioder")[1].get("fom").asText()).isEqualTo("2013-03-03")
        assertThat(node.get("arbeidsgiverperioder")[1].get("tom").asText()).isEqualTo("2014-04-04")

        assertThat(node.get("endringerIRefusjon").size()).isEqualTo(2)
        assertThat(node.get("endringerIRefusjon")[0].get("endringsdato").asText()).isEqualTo("2015-05-05")
        assertThat(node.get("endringerIRefusjon")[0].get("beloep").asLong()).isEqualTo(555555555555)
        assertThat(node.get("endringerIRefusjon")[1].get("endringsdato").asText()).isEqualTo("2016-06-06")
        assertThat(node.get("endringerIRefusjon")[1].get("beloep").asLong()).isEqualTo(666666666666)

        assertThat(node.get("feriePerioder").size()).isEqualTo(2)
        assertThat(node.get("feriePerioder")[0].get("fom").asText()).isEqualTo("2017-07-07")
        assertThat(node.get("feriePerioder")[0].get("tom").asText()).isEqualTo("2018-08-08")
        assertThat(node.get("feriePerioder")[1].get("fom").asText()).isEqualTo("2019-09-09")
        assertThat(node.get("feriePerioder")[1].get("tom").asText()).isEqualTo("2020-12-20")

        assertThat(node.get("gjenopptakelserNaturalYtelse").size()).isEqualTo(2)
        assertThat(node.get("gjenopptakelserNaturalYtelse")[0].get("naturalytelse").asText()).isEqualTo("BOLIG")
        assertThat(node.get("gjenopptakelserNaturalYtelse")[0].get("fom").asText()).isEqualTo("2011-01-01")
        assertThat(node.get("gjenopptakelserNaturalYtelse")[0].get("beloepPrMnd").asLong()).isEqualTo(111111111111)
        assertThat(node.get("gjenopptakelserNaturalYtelse")[1].get("naturalytelse").asText()).isEqualTo("KOSTDAGER")
        assertThat(node.get("gjenopptakelserNaturalYtelse")[1].get("fom").asText()).isEqualTo("2012-02-02")
        assertThat(node.get("gjenopptakelserNaturalYtelse")[1].get("beloepPrMnd").asLong()).isEqualTo(222222222222)

        assertThat(node.get("opphørAvNaturalYtelse").size()).isEqualTo(2)
        assertThat(node.get("opphørAvNaturalYtelse")[0].get("naturalytelse").asText()).isEqualTo("BIL")
        assertThat(node.get("opphørAvNaturalYtelse")[0].get("fom").asText()).isEqualTo("2015-05-05")
        assertThat(node.get("opphørAvNaturalYtelse")[0].get("beloepPrMnd").asLong()).isEqualTo(555555555555)
        assertThat(node.get("opphørAvNaturalYtelse")[1].get("naturalytelse").asText()).isEqualTo("TILSKUDDBARNEHAGEPLASS")
        assertThat(node.get("opphørAvNaturalYtelse")[1].get("fom").asText()).isEqualTo("2016-06-06")
        assertThat(node.get("opphørAvNaturalYtelse")[1].get("beloepPrMnd").asLong()).isEqualTo(666666666666)
    }

}

