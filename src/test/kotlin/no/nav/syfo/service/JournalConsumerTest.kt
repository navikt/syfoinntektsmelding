package no.nav.syfo.service

import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.behandling.HentDokumentFeiletException
import no.nav.syfo.client.aktor.AktorClient
import no.nav.syfo.client.saf.ResponseError
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.client.saf.SafDokumentClient
import no.nav.syfo.client.saf.SafJournalData
import no.nav.syfo.client.saf.SafJournalResponse
import no.nav.syfo.client.saf.SafJournalpostClient
import no.nav.syfo.client.saf.model.Dokument
import no.nav.syfo.client.saf.model.Journalpost
import no.nav.syfo.syfoinntektsmelding.consumer.ws.inntektsmeldingArbeidsgiver
import no.nav.syfo.syfoinntektsmelding.consumer.ws.inntektsmeldingArbeidsgiverPrivat
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import java.time.LocalDate
import java.time.LocalDateTime

class JournalConsumerTest {

    private val aktør = mockk<AktorClient>(relaxed = true)
    private val dokumentClient = mockk<SafDokumentClient>(relaxed = true)
    private val safJournalpostClient = mockk<SafJournalpostClient>(relaxed = true)
    private val journalConsumer = JournalConsumer(dokumentClient, safJournalpostClient, aktør)

    // Todo Lage test for når safJournalpostClient returnerer errrors

    @Test
    fun hentInntektsmelding() {
        val periode = Periode(
            LocalDate.of(2021,7,1),
            LocalDate.of(2021,7,19)
        )
        val journalresponse = SafJournalResponse(
            data = SafJournalData(
                journalpost = Journalpost(
                    JournalStatus.MIDLERTIDIG,
                    LocalDateTime.now(),
                    dokumenter = listOf(Dokument(dokumentInfoId="dokumentId"))
                )
            ), errors = emptyList()
        )
        every {
            safJournalpostClient.getJournalpostMetadata(any())
        } returns journalresponse
        val fakeInntekt = inntektsmeldingArbeidsgiver( listOf(periode), "18018522868")
        every {
            dokumentClient.hentDokument(any(), any())
        } returns fakeInntekt.toByteArray()
        val inntektsmelding = journalConsumer.hentInntektsmelding("12345", "AR-123")
        assertThat(inntektsmelding.fnr).isEqualTo("18018522868")
        assertThat(inntektsmelding.journalpostId).isEqualTo("12345")
    }

    @Test
    fun parserInntektsmeldingUtenPerioder() {
        val journalresponse = SafJournalResponse(
            data = SafJournalData(
                journalpost = Journalpost(
                    JournalStatus.MIDLERTIDIG,
                    LocalDateTime.now(),
                    dokumenter = listOf(Dokument(dokumentInfoId="dokumentId"))
                )
            )
            , errors = emptyList()
        )
        every {
            safJournalpostClient.getJournalpostMetadata(any())
        } returns journalresponse
        val fakeInntekt = inntektsmeldingArbeidsgiver( emptyList(), "18018522868")
        every {
            dokumentClient.hentDokument(any(), any())
        } returns fakeInntekt.toByteArray()
        val inntektsmelding = journalConsumer.hentInntektsmelding("12345", "AR-123")
        assertThat(inntektsmelding.fnr).isEqualTo("18018522868")
        assertThat(inntektsmelding.arbeidsgiverperioder.isEmpty())
    }

    @Test
    fun parseInntektsmeldingV7() {
        val periode = Periode(
            LocalDate.of(2021,7,1),
            LocalDate.of(2021,7,19)
        )
        val journalresponse = SafJournalResponse(
            data = SafJournalData(
                journalpost = Journalpost(
                    JournalStatus.MIDLERTIDIG,
                    LocalDateTime.now(),
                    dokumenter = listOf(Dokument(dokumentInfoId="dokumentId"))
                )
            )
            , errors = emptyList()
        )
        every {
            safJournalpostClient.getJournalpostMetadata(any())
        } returns journalresponse
        val fakeInntekt = inntektsmeldingArbeidsgiverPrivat()
        every {
            dokumentClient.hentDokument(any(), any())
        } returns fakeInntekt.toByteArray()

        val (_, _, _, arbeidsgiverPrivat, _, _, _, _, _, arbeidsgiverperioder) = journalConsumer.hentInntektsmelding(
            "journalpostId",
            "AR-123"
        )

        assertThat(arbeidsgiverperioder.isEmpty()).isFalse
        assertThat(arbeidsgiverPrivat != null).isTrue
    }

    @Test
    fun parseInntektsmelding0924() {
        val journalresponse = SafJournalResponse(
            data = SafJournalData(
                journalpost = Journalpost(
                    JournalStatus.MIDLERTIDIG,
                    LocalDateTime.now(),
                    dokumenter = listOf(Dokument(dokumentInfoId="dokumentId"))
                )
            )
            , errors = emptyList()
        )
        every {
            safJournalpostClient.getJournalpostMetadata(any())
        } returns journalresponse

        every {
            dokumentClient.hentDokument(any(), any())
        } returns inntektsmeldingArbeidsgiver(
            listOf(
                Periode(
                    LocalDate.of(2019, 2, 1),
                    LocalDate.of(2019, 2, 16)
                )
            )
        ).toByteArray()

        val (_, _, arbeidsgiverOrgnummer, arbeidsgiverPrivat) = journalConsumer.hentInntektsmelding(
            "journalpostId",
            "AR-123"
        )
        assertThat(arbeidsgiverOrgnummer).isNotNull()
        assertThat(arbeidsgiverPrivat != null).isFalse
    }

    @Test
    fun feil_i_graphql_spørring() {
        val journalresponse = SafJournalResponse(
            data = SafJournalData(
                journalpost = Journalpost(
                    JournalStatus.MIDLERTIDIG,
                    LocalDateTime.now(),
                    dokumenter = listOf(Dokument(dokumentInfoId="dokumentId"))
                )
            )
            , errors = listOf(ResponseError("Feil", locations = emptyList(), path= emptyList(), extensions = null))
        )
        every {
            safJournalpostClient.getJournalpostMetadata(any())
        } returns journalresponse
        assertThrows<HentDokumentFeiletException> {
            journalConsumer.hentInntektsmelding("12345", "AR-123")
        }
    }

}
