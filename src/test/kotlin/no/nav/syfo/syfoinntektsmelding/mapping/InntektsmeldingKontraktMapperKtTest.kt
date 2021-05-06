package no.nav.syfo.syfoinntektsmelding.mapping

import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Refusjon
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.mapping.mapArbeidsgiverperioder
import no.nav.syfo.mapping.mapArbeidsgivertype
import no.nav.syfo.mapping.toInntektsmeldingEntitet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import no.nav.syfo.FØRSTE_JANUAR
import no.nav.syfo.FØRSTE_FEBRUAR
import no.nav.syfo.grunnleggendeInntektsmelding
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class InntektsmeldingKontraktMapperKtTest {

    @Test
    fun toInntektsmeldingDTO() {
        val inntektsmelding = Inntektsmelding(
            fnr = "fnr",
            arbeidsgiverPrivatFnr = "fnr",
            journalpostId = "journalpostId",
            arsakTilInnsending = "",
            journalStatus = JournalStatus.MIDLERTIDIG,
            arbeidsgiverperioder = ArrayList(),
            refusjon = Refusjon(BigDecimal.ONE),
            gyldighetsStatus = Gyldighetsstatus.GYLDIG,
            arkivRefereranse = "ar123",
            førsteFraværsdag = LocalDate.now(),
            mottattDato = LocalDateTime.of(2019, 10, 1, 5, 18, 45, 0),
            sakId = "sakId",
            aktorId = "aktorId"
        )
        val dto = toInntektsmeldingEntitet(inntektsmelding)
        assertThat(dto.journalpostId).isEqualTo("journalpostId")
        assertThat(dto.aktorId).isEqualTo("aktorId")
        assertThat(dto.sakId).isEqualTo("sakId")
        assertThat(dto.arbeidsgiverPrivat).isEqualTo("fnr")
        assertThat(dto.uuid).isNotNull()
        assertThat(dto.behandlet).isEqualTo(LocalDateTime.of(2019,10,1,5,18,45,0))
        assertThat(dto.arbeidsgiverperioder.size).isEqualTo(0)
    }

    @Test
    fun toInntektsmelding() {
        val dto = InntektsmeldingEntitet(
                journalpostId = "journalpostId",
                behandlet = LocalDateTime.of(2019, 10, 1, 5, 18, 45, 0),
                sakId = "sakId",
                orgnummer = "orgnummer",
                arbeidsgiverPrivat = "arbeidsgiverPrivat",
                aktorId = "aktorId"
        )
        val i = no.nav.syfo.mapping.toInntektsmelding(dto)
        assertThat(i.journalpostId).isEqualTo("journalpostId")
        assertThat(i.mottattDato).isEqualTo(LocalDateTime.of(2019,10,1,5,18,45,0))
        assertThat(i.sakId).isEqualTo("sakId")
        assertThat(i.arbeidsgiverOrgnummer).isEqualTo("orgnummer")
        assertThat(i.fnr).isEqualTo("arbeidsgiverPrivat")
        assertThat(i.aktorId).isEqualTo("aktorId")
        assertThat(i.arbeidsgiverperioder.size).isEqualTo(0)
    }

    @Test
    fun skal_mappe_enkel_periode() {
        val mappedePerioder = mapArbeidsgiverperioder(grunnleggendeInntektsmelding)
        assertThat(mappedePerioder.size).isEqualTo(1)
        assertThat(mappedePerioder[0].fom).isEqualTo( FØRSTE_JANUAR)
        assertThat(mappedePerioder[0].tom).isEqualTo( FØRSTE_FEBRUAR)
    }


    @Test
    fun skal_finne_arbeidsgivertype_virksomhet() {
        assertThat(mapArbeidsgivertype(grunnleggendeInntektsmelding)).isEqualTo(Arbeidsgivertype.VIRKSOMHET)
    }


    @Test
    fun skal_finne_arbeidsgivertype_privat() {
        assertThat(
            mapArbeidsgivertype(
                    grunnleggendeInntektsmelding.copy(arbeidsgiverOrgnummer = null, arbeidsgiverPrivatFnr = "00"))
        ).isEqualTo(Arbeidsgivertype.PRIVAT)
    }

}
