package no.nav.syfo.mapping

import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.syfo.FØRSTE_FEBRUAR
import no.nav.syfo.FØRSTE_JANUAR
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.RapportertInntekt
import no.nav.syfo.domain.inntektsmelding.Refusjon
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.grunnleggendeInntektsmelding
import no.nav.syfo.koin.buildObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class InntektsmeldingKontraktMapperKtTest {
    val om = buildObjectMapper()

    @Test
    fun toInntektsmeldingDTO() {
        val inntektsmelding =
            Inntektsmelding(
                fnr = "fnr",
                arbeidsgiverPrivatFnr = "fnr",
                journalpostId = "journalpostId",
                arsakTilInnsending = "",
                journalStatus = JournalStatus.MOTTATT,
                arbeidsgiverperioder = ArrayList(),
                refusjon = Refusjon(BigDecimal.ONE),
                gyldighetsStatus = Gyldighetsstatus.GYLDIG,
                arkivRefereranse = "ar123",
                førsteFraværsdag = LocalDate.now(),
                mottattDato = LocalDateTime.of(2019, 10, 1, 5, 18, 45, 0),
                sakId = "sakId",
                aktorId = "aktorId",
            )
        val dto = toInntektsmeldingEntitet(inntektsmelding)
        assertThat(dto.journalpostId).isEqualTo("journalpostId")
        assertThat(dto.aktorId).isEqualTo("aktorId")
        assertThat(dto.arbeidsgiverPrivat).isEqualTo("fnr")
        assertThat(dto.uuid).isNotNull
        assertThat(dto.behandlet).isEqualTo(LocalDateTime.of(2019, 10, 1, 5, 18, 45, 0))
        assertThat(dto.arbeidsgiverperioder.size).isEqualTo(0)
    }

    @Test
    fun toInntektsmelding() {
        val dto =
            InntektsmeldingEntitet(
                uuid = UUID.randomUUID().toString(),
                journalpostId = grunnleggendeInntektsmelding.journalpostId,
                behandlet = LocalDateTime.of(2019, 10, 25, 0, 0, 0, 0),
                orgnummer = grunnleggendeInntektsmelding.arbeidsgiverOrgnummer,
                arbeidsgiverPrivat = "arbeidsgiverPrivat",
                aktorId = grunnleggendeInntektsmelding.aktorId.toString(),
                data = om.writeValueAsString(grunnleggendeInntektsmelding),
            )
        val i = toInntektsmelding(dto, om)
        assertThat(i.journalpostId).isEqualTo("123")
        assertThat(i.sakId).isEqualTo("sakId")
        assertThat(i.arbeidsgiverOrgnummer).isEqualTo("1234")
        assertThat(i.fnr).isEqualTo(grunnleggendeInntektsmelding.fnr)
        assertThat(i.aktorId).isEqualTo("aktorId")
        assertThat(i.arbeidsgiverperioder.size).isEqualTo(1)
    }

    @Test
    fun skal_mappe_enkel_periode() {
        val mappedePerioder = mapArbeidsgiverperioder(grunnleggendeInntektsmelding)
        assertThat(mappedePerioder.size).isEqualTo(1)
        assertThat(mappedePerioder[0].fom).isEqualTo(FØRSTE_JANUAR)
        assertThat(mappedePerioder[0].tom).isEqualTo(FØRSTE_FEBRUAR)
    }

    @Test
    fun skal_finne_arbeidsgivertype_virksomhet() {
        assertThat(mapArbeidsgivertype(grunnleggendeInntektsmelding)).isEqualTo(Arbeidsgivertype.VIRKSOMHET)
    }

    @Test
    fun skal_finne_arbeidsgivertype_privat() {
        assertThat(
            mapArbeidsgivertype(
                grunnleggendeInntektsmelding.copy(arbeidsgiverOrgnummer = null, arbeidsgiverPrivatFnr = "00"),
            ),
        ).isEqualTo(Arbeidsgivertype.PRIVAT)
    }

    @Test
    fun `RapportertInntekt uten endringAarsakerData blir desirialisert riktig `() {
        val rapportertInntektJson =
            """
            {
                "bekreftet": true,
                "endringAarsak": null,
                "beregnetInntekt": 49000.0,
                "manueltKorrigert": false,
                "endringAarsakData": {
                    "aarsak": "NyStillingsprosent",
                    "bleKjent": null,
                    "perioder": null,
                    "gjelderFra": "2024-11-01"
                  }
              }
            """.trimIndent()
        val rapportertInntekt = om.readValue(rapportertInntektJson, RapportertInntekt::class.java)
        assertThat(rapportertInntekt.endringAarsakerData).isNotEmpty()
        assertThat(rapportertInntekt.endringAarsakerData.first().aarsak).isEqualTo("NyStillingsprosent")
        assertThat(rapportertInntekt.endringAarsakerData.first().gjelderFra).isEqualTo("2024-11-01")
        assertThat(rapportertInntekt.endringAarsakerData.first().bleKjent).isNull()
        assertThat(rapportertInntekt.endringAarsakerData.first().perioder).isNull()
    }
}
