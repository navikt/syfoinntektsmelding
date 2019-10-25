package no.nav.syfo.mapping

import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.syfo.dto.InntektsmeldingDto
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Refusjon
import org.assertj.core.api.Assertions
import org.junit.Assert.assertEquals
import org.junit.Test
import testutil.FØRSTE_FEBRUAR
import testutil.FØRSTE_JANUAR
import testutil.grunnleggendeInntektsmelding
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class InntektsmeldingKontraktMapperKtTest {

    @Test
    fun toInntektsmeldingDTO() {
        val inntektsmelding = Inntektsmelding(
                journalpostId = "journalpostId",
                mottattDato = LocalDateTime.of(2019,10,1,5,18,45,0),
                sakId = "sakId",
                fnr = "fnr",
                aktorId = "aktorId",
                arbeidsgiverperioder = ArrayList(),
                arsakTilInnsending = "",
                journalStatus = JournalStatus.MIDLERTIDIG,
                refusjon = Refusjon(BigDecimal.ONE),
                gyldighetsStatus = Gyldighetsstatus.GYLDIG,
                arkivRefereranse = "ar123",
                førsteFraværsdag = LocalDate.now()
        )
        val dto = toInntektsmeldingDTO(inntektsmelding)
        Assertions.assertThat(dto.journalpostId).isEqualTo("journalpostId")
        Assertions.assertThat(dto.aktorId).isEqualTo("aktorId")
        Assertions.assertThat(dto.sakId).isEqualTo("sakId")
        Assertions.assertThat(dto.arbeidsgiverPrivat).isEqualTo("fnr")
        Assertions.assertThat(dto.uuid).isNotNull()
        Assertions.assertThat(dto.behandlet).isEqualTo(LocalDateTime.of(2019,10,1,5,18,45,0))
        Assertions.assertThat(dto.orgnummer).isEqualTo("abc")
        Assertions.assertThat(dto.arbeidsgiverperioder.size).isEqualTo(1)
    }

    @Test
    fun toInntektsmelding() {
        val dto = InntektsmeldingDto(
                journalpostId = "journalpostId",
                behandlet = LocalDateTime.of(2019, 10, 1, 5, 18, 45, 0),
                sakId = "sakId",
                orgnummer = "orgnummer",
                arbeidsgiverPrivat = "arbeidsgiverPrivat",
                aktorId = "aktorId"
        )
        val i = toInntektsmelding(dto)
        Assertions.assertThat(i.journalpostId).isEqualTo("journalpostId")
        Assertions.assertThat(i.mottattDato).isEqualTo(LocalDateTime.of(2019,10,1,5,18,45,0))
        Assertions.assertThat(i.sakId).isEqualTo("sakId")
        Assertions.assertThat(i.arbeidsgiverOrgnummer).isEqualTo("orgnummer")
        Assertions.assertThat(i.fnr).isEqualTo("arbeidsgiverPrivat")
        Assertions.assertThat(i.aktorId).isEqualTo("aktorId")
        Assertions.assertThat(i.arbeidsgiverperioder.size).isEqualTo(1)
    }

    @Test
    fun skal_mappe_enkel_periode() {
        val mappedePerioder = mapArbeidsgiverperioder(grunnleggendeInntektsmelding)
        assertEquals(mappedePerioder.size, 1)
        assertEquals(mappedePerioder[0].fom, FØRSTE_JANUAR)
        assertEquals(mappedePerioder[0].tom, FØRSTE_FEBRUAR)
    }


    @Test
    fun skal_finne_arbeidsgivertype_virksomhet() {
        assertEquals(mapArbeidsgivertype(grunnleggendeInntektsmelding), Arbeidsgivertype.VIRKSOMHET)
    }


    @Test
    fun skal_finne_arbeidsgivertype_privat() {
        assertEquals(
                mapArbeidsgivertype(grunnleggendeInntektsmelding.copy(arbeidsgiverOrgnummer = null, arbeidsgiverPrivatFnr = "00")),
                Arbeidsgivertype.PRIVAT)
    }

}
