package no.nav.syfo.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.every
import io.mockk.mockk
import no.nav.helsearbeidsgiver.utils.logger
import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.mapping.toInntektsmelding
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.repository.buildIM
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InntektsmeldingServiceTest {

    val objectMapper = ObjectMapper().registerModules(KotlinModule(), JavaTimeModule())
    val AKTOR_ID_FOUND = "aktør-123"
    val logger = this.logger()

    @Test
    fun `Skal ikke være duplikat dersom ingen tidligere inntektsmeldinger`() {
        val im = lag(0, 0)
        assertFalse(isDuplicateWithLatest(logger, im, emptyList()))
    }

    @Test
    fun `Skal ikke være duplikat dersom nest siste eller tidligere var lik`() {
        val inntekt = 100
        val im = lag(0, inntekt)
        val list = listOf(
            lag(1, inntekt + 300),
            lag(2, inntekt),
            lag(3, inntekt + 200)
        )
        assertFalse(isDuplicateWithLatest(logger, im, list))
        assertFalse(isDuplicateWithLatest(logger, im, list.asReversed()))
    }

    @Test
    fun `Skal være duplikat av sist innsendt`() {
        val inntekt = 100
        val im = lag(0, inntekt)
        val list = listOf(
            lag(1, inntekt),
            lag(2, inntekt + 100),
            lag(3, inntekt + 200),
        )
        assertTrue(isDuplicateWithLatest(logger, im, list))
        assertTrue(isDuplicateWithLatest(logger, im, list.asReversed()))
    }

    @Test
    fun `Skal finne inntektsmeldinger og mappe de om til domene objekter`() {
        val repository = mockk<InntektsmeldingRepository>(relaxed = true)
        val service = InntektsmeldingService(repository, objectMapper)
        every { repository.findByAktorId(any()) } returns listOf(buildEntitet(AKTOR_ID_FOUND, buildIM()))
        assertEquals(1, service.finnBehandledeInntektsmeldinger(AKTOR_ID_FOUND).size)
    }

    @Test
    fun `isDuplicate - Skal detektere eksisterende inntektsmeldingene som allerede er lagret`() {
        val repository = mockk<InntektsmeldingRepository>(relaxed = true)
        val service = InntektsmeldingService(repository, objectMapper)
        val ent = buildEntitet(AKTOR_ID_FOUND, buildIM())
        val im = toInntektsmelding(ent, objectMapper)
        every { repository.findByAktorId(any()) } returns listOf(ent)
        assertTrue(service.isDuplicate(im))
    }

    @Test
    fun `isDuplicate - Skal detektere at de er litt ulike`() {
        val repository = mockk<InntektsmeldingRepository>(relaxed = true)
        val service = InntektsmeldingService(repository, objectMapper)
        val ent = buildEntitet(AKTOR_ID_FOUND, buildIM())
        val im = toInntektsmelding(ent, objectMapper).copy(arbeidsforholdId = "abc")
        every { repository.findByAktorId(any()) } returns listOf(ent)
        assertFalse(service.isDuplicate(im))
    }

    @Test
    fun `isDuplicate - Skal ikke finne når inntektsmeldinger ikke finnes`() {
        val repository = mockk<InntektsmeldingRepository>(relaxed = true)
        val service = InntektsmeldingService(repository, objectMapper)
        every { repository.findByAktorId(any()) } returns emptyList()
        assertFalse(service.isDuplicate(buildIM()))
    }

    @Test
    fun `Skal gi forventet JSON format`() {
        val im = buildIM()
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

    fun buildEntitet(aktorId: String, im: Inntektsmelding): InntektsmeldingEntitet {
        return InntektsmeldingEntitet(
            aktorId = aktorId,
            behandlet = LocalDateTime.now(),
            orgnummer = "arb-org-123",
            journalpostId = "jp-123",
            data = objectMapper.writeValueAsString(im)
        )
    }

    fun lag(dager: Long, inntekt: Int): Inntektsmelding {
        return buildIM().copy(innsendingstidspunkt = LocalDateTime.now().minusDays(dager), beregnetInntekt = BigDecimal(inntekt))
    }
}
