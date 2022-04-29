package no.nav.syfo.slowtests.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.EndringIRefusjon
import no.nav.syfo.domain.inntektsmelding.GjenopptakelseNaturalytelse
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Naturalytelse
import no.nav.syfo.domain.inntektsmelding.OpphoerAvNaturalytelse
import no.nav.syfo.domain.inntektsmelding.Refusjon
import no.nav.syfo.service.asJsonString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class InntektsmeldingServiceTest {
    private val objectMapper = ObjectMapper().registerModules(KotlinModule(), JavaTimeModule())

    @Test
    fun `Verifiserer at object mapper gir forventet JSON format`() {
        val im = Inntektsmelding(
            id = "id-abc",
            fnr = "fnr-123",
            arbeidsgiverOrgnummer = "arb-org-123",
            arbeidsgiverPrivatFnr = "arb-priv-123",
            arbeidsgiverPrivatAktørId = "arb-priv-aktør-123",
            arbeidsforholdId = "arb-123",
            journalpostId = "jp-123",
            arsakTilInnsending = "Ingen årsak",
            journalStatus = JournalStatus.MOTTATT,
            arbeidsgiverperioder = listOf(
                Periode(fom = LocalDate.of(2011, 11, 1), tom = LocalDate.of(2012, 12, 2)),
                Periode(fom = LocalDate.of(2013, 3, 3), tom = LocalDate.of(2014, 4, 4))
            ),
            beregnetInntekt = BigDecimal(999999999999),
            refusjon = Refusjon(BigDecimal(333333333333), LocalDate.of(2020, 2, 20)),
            endringerIRefusjon = listOf(
                EndringIRefusjon(LocalDate.of(2015, 5, 5), BigDecimal(555555555555)),
                EndringIRefusjon(LocalDate.of(2016, 6, 6), BigDecimal(666666666666))
            ),
            opphørAvNaturalYtelse = listOf(
                OpphoerAvNaturalytelse(Naturalytelse.BIL, LocalDate.of(2015, 5, 5), BigDecimal(555555555555)),
                OpphoerAvNaturalytelse(
                    Naturalytelse.TILSKUDDBARNEHAGEPLASS,
                    LocalDate.of(2016, 6, 6),
                    BigDecimal(666666666666)
                )
            ),
            gjenopptakelserNaturalYtelse = listOf(
                GjenopptakelseNaturalytelse(Naturalytelse.BOLIG, LocalDate.of(2011, 1, 1), BigDecimal(111111111111)),
                GjenopptakelseNaturalytelse(Naturalytelse.KOSTDAGER, LocalDate.of(2012, 2, 2), BigDecimal(222222222222))
            ),
            gyldighetsStatus = Gyldighetsstatus.GYLDIG,
            arkivRefereranse = "ar-123",
            feriePerioder = listOf(
                Periode(fom = LocalDate.of(2017, 7, 7), tom = LocalDate.of(2018, 8, 8)),
                Periode(fom = LocalDate.of(2019, 9, 9), tom = LocalDate.of(2020, 12, 20))
            ),

            førsteFraværsdag = LocalDate.of(2010, 2, 10),
            mottattDato = LocalDateTime.of(2010, 5, 4, 3, 2, 1),
            sakId = "sak-123",
            aktorId = "aktør-123",
            begrunnelseRedusert = "Grunn til reduksjon"
        )
        val json = im.asJsonString(objectMapper)

        val mapper = JacksonJsonConfig.objectMapperFactory.opprettObjectMapper()
        val node = mapper.readTree(json)

        assertThat(node.get("id").asText()).isEqualTo("id-abc")
        assertThat(node.get("fnr").asText()).isNullOrEmpty() // Skal ikke lagre fødselsnummer
        assertThat(node.get("aktorId").asText()).isEqualTo("aktør-123")
        assertThat(node.get("refusjon").get("beloepPrMnd").asLong()).isEqualTo(333333333333)
        assertThat(node.get("refusjon").get("opphoersdato").toString()).isEqualTo("[2020,2,20]")
        assertThat(node.get("begrunnelseRedusert").asText()).isEqualTo("Grunn til reduksjon")
        assertThat(node.get("sakId").asText()).isEqualTo("sak-123")
        assertThat(node.get("mottattDato").toString()).isEqualTo("[2010,5,4,3,2,1]")
        assertThat(node.get("arkivRefereranse").asText()).isEqualTo("ar-123")
        assertThat(node.get("førsteFraværsdag").toString()).isEqualTo("[2010,2,10]")
        assertThat(node.get("arsakTilInnsending").asText()).isEqualTo("Ingen årsak")
        assertThat(node.get("journalpostId").asText()).isEqualTo("jp-123")
        assertThat(node.get("arbeidsforholdId").asText()).isEqualTo("arb-123")
        assertThat(node.get("arbeidsgiverPrivatFnr").asText()).isEqualTo("arb-priv-123")
        assertThat(node.get("arbeidsgiverOrgnummer").asText()).isEqualTo("arb-org-123")
        assertThat(node.get("arbeidsgiverPrivatAktørId").asText()).isEqualTo("arb-priv-aktør-123")
        assertThat(node.get("beregnetInntekt").asLong()).isEqualTo(999999999999)
        assertThat(node.get("gyldighetsStatus").asText()).isEqualTo("GYLDIG")
        assertThat(node.get("journalStatus").asText()).isEqualTo("MOTTATT")

        assertThat(node.get("arbeidsgiverperioder").size()).isEqualTo(2)
        assertThat(node.get("arbeidsgiverperioder")[0].get("fom").toString()).isEqualTo("[2011,11,1]")
        assertThat(node.get("arbeidsgiverperioder")[0].get("tom").toString()).isEqualTo("[2012,12,2]")
        assertThat(node.get("arbeidsgiverperioder")[1].get("fom").toString()).isEqualTo("[2013,3,3]")
        assertThat(node.get("arbeidsgiverperioder")[1].get("tom").toString()).isEqualTo("[2014,4,4]")

        assertThat(node.get("endringerIRefusjon").size()).isEqualTo(2)
        assertThat(node.get("endringerIRefusjon")[0].get("endringsdato").toString()).isEqualTo("[2015,5,5]")
        assertThat(node.get("endringerIRefusjon")[0].get("beloep").asLong()).isEqualTo(555555555555)
        assertThat(node.get("endringerIRefusjon")[1].get("endringsdato").toString()).isEqualTo("[2016,6,6]")
        assertThat(node.get("endringerIRefusjon")[1].get("beloep").asLong()).isEqualTo(666666666666)

        assertThat(node.get("feriePerioder").size()).isEqualTo(2)
        assertThat(node.get("feriePerioder")[0].get("fom").toString()).isEqualTo("[2017,7,7]")
        assertThat(node.get("feriePerioder")[0].get("tom").toString()).isEqualTo("[2018,8,8]")
        assertThat(node.get("feriePerioder")[1].get("fom").toString()).isEqualTo("[2019,9,9]")
        assertThat(node.get("feriePerioder")[1].get("tom").toString()).isEqualTo("[2020,12,20]")

        assertThat(node.get("gjenopptakelserNaturalYtelse").size()).isEqualTo(2)
        assertThat(node.get("gjenopptakelserNaturalYtelse")[0].get("naturalytelse").asText()).isEqualTo("BOLIG")
        assertThat(node.get("gjenopptakelserNaturalYtelse")[0].get("fom").toString()).isEqualTo("[2011,1,1]")
        assertThat(node.get("gjenopptakelserNaturalYtelse")[0].get("beloepPrMnd").asLong()).isEqualTo(111111111111)
        assertThat(node.get("gjenopptakelserNaturalYtelse")[1].get("naturalytelse").asText()).isEqualTo("KOSTDAGER")
        assertThat(node.get("gjenopptakelserNaturalYtelse")[1].get("fom").toString()).isEqualTo("[2012,2,2]")
        assertThat(node.get("gjenopptakelserNaturalYtelse")[1].get("beloepPrMnd").asLong()).isEqualTo(222222222222)

        assertThat(node.get("opphørAvNaturalYtelse").size()).isEqualTo(2)
        assertThat(node.get("opphørAvNaturalYtelse")[0].get("naturalytelse").asText()).isEqualTo("BIL")
        assertThat(node.get("opphørAvNaturalYtelse")[0].get("fom").toString()).isEqualTo("[2015,5,5]")
        assertThat(node.get("opphørAvNaturalYtelse")[0].get("beloepPrMnd").asLong()).isEqualTo(555555555555)
        assertThat(
            node.get("opphørAvNaturalYtelse")[1].get("naturalytelse").asText()
        ).isEqualTo("TILSKUDDBARNEHAGEPLASS")
        assertThat(node.get("opphørAvNaturalYtelse")[1].get("fom").toString()).isEqualTo("[2016,6,6]")
        assertThat(node.get("opphørAvNaturalYtelse")[1].get("beloepPrMnd").asLong()).isEqualTo(666666666666)
    }
}
