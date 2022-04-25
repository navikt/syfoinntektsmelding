package no.nav.syfo

import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import java.time.LocalDate
import java.time.LocalDateTime

val FØRSTE_JANUAR: LocalDate = LocalDate.of(2019, 1, 1)
val FØRSTE_FEBRUAR: LocalDate = LocalDate.of(2019, 2, 1)
const val validIdentitetsnummer = "20015001543"
const val validOrgNr = "917404437"
val BEHANDLET_DATO: LocalDateTime = LocalDateTime.of(2021, 6, 23, 12, 0, 0)

val grunnleggendeInntektsmelding = Inntektsmelding(
    id = "ID",
    fnr = "12345678901",
    arbeidsgiverOrgnummer = "1234",
    journalpostId = "123",
    arsakTilInnsending = "TEST",
    journalStatus = JournalStatus.FERDIGSTILT,
    arbeidsgiverperioder = listOf(Periode(FØRSTE_JANUAR, FØRSTE_FEBRUAR)),
    arkivRefereranse = "AR123",
    førsteFraværsdag = LocalDate.of(2019, 10, 5),
    mottattDato = LocalDate.of(2019, 10, 25).atStartOfDay()
)

val inntektsmeldingEntitet = InntektsmeldingEntitet(
    uuid = "UUID",
    aktorId = validIdentitetsnummer,
    sakId = "987",
    journalpostId = "",
    orgnummer = validOrgNr,
    arbeidsgiverPrivat = null,
    behandlet = BEHANDLET_DATO,
    data = """
        {
          "id": "",
          "fnr": "",
          "sakId": null,
          "aktorId": null,
          "refusjon": {
            "beloepPrMnd": 39968,
            "opphoersdato": null
          },
          "mottattDato": "2021-06-15T12:42:37",
          "nærRelasjon": false,
          "feriePerioder": [],
          "journalStatus": "MOTTATT",
          "journalpostId": "5406",
          "årsakEndring": null,
          "avsenderSystem": {
            "navn": "AltinnPortal",
            "versjon": "1.455"
          },
          "bruttoUtbetalt": 2216,
          "beregnetInntekt": 3968,
          "arbeidsforholdId": null,
          "arkivRefereranse": "AR434",
          "gyldighetsStatus": "GYLDIG",
          "arsakTilInnsending": "Ny",
          "endringerIRefusjon": [],
          "førsteFraværsdag": "2021-05-01",
          "kontaktinformasjon": {
            "navn": "Pedersen",
            "telefon": "3232233"
          },
          "begrunnelseRedusert": "",
          "arbeidsgiverperioder": [
            {
              "fom": "2021-01-26",
              "tom": "2021-02-10"
            }
          ],
          "innsendingstidspunkt": null,
          "arbeidsgiverOrgnummer": "9999999",
          "arbeidsgiverPrivatFnr": null,
          "opphørAvNaturalYtelse": [],
          "arbeidsgiverPrivatAktørId": null,
          "gjenopptakelserNaturalYtelse": []
        }
    """.trimIndent()
)
