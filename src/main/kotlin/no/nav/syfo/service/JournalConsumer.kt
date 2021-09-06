package no.nav.syfo.service

import kotlinx.coroutines.runBlocking
import log
import no.nav.syfo.behandling.HentDokumentFeiletException
import no.nav.syfo.client.aktor.AktorConsumer
import no.nav.syfo.mapping.InntektsmeldingArbeidsgiver20180924Mapper
import no.nav.syfo.mapping.InntektsmeldingArbeidsgiverPrivat20181211Mapper
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.client.saf.SafDokumentClient
import no.nav.syfo.client.saf.SafJournalpostClient
import no.nav.syfo.util.JAXB
import no.seres.xsd.nav.inntektsmelding_m._20180924.XMLInntektsmeldingM
import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import javax.xml.bind.JAXBElement

class JournalConsumer(
    private val safDokumentClient: SafDokumentClient,
    private val safJournalpostClient: SafJournalpostClient,
    private val aktorConsumer: AktorConsumer)
{
    var log = log()

    /**
     * 1 - Henter inntektsmelding fra journalpost i byteArray
     * 2 - Gjør om bytearray til XML
     * 2 - mapper om i to separate format
     * 3 - privat mapper henter ut aktørID
     */
    fun hentInntektsmelding(journalpostId: String, arkivReferanse: String): Inntektsmelding {
        try {
            val response = safJournalpostClient.getJournalpostMetadata(journalpostId)
            if (!response.errors.isNullOrEmpty()){
                throw IllegalArgumentException("Feil i spørring")
            }
            val journalpost = response.data.journalpost
            val inntektsmeldingRAW = runBlocking {
                safDokumentClient.hentDokument(journalpostId, journalpost.dokumenter[0].dokumentInfoId)
            }
            val jaxbInntektsmelding = JAXB.unmarshalInntektsmelding<JAXBElement<Any>>(inntektsmeldingRAW?.decodeToString())
            val mottattDato: LocalDateTime = journalpost.datoOpprettet
            val journalStatus: JournalStatus = journalpost.journalstatus
            return if (jaxbInntektsmelding.value is XMLInntektsmeldingM)
                InntektsmeldingArbeidsgiver20180924Mapper.tilXMLInntektsmelding(jaxbInntektsmelding, journalpostId, mottattDato, journalStatus, arkivReferanse)
            else
                InntektsmeldingArbeidsgiverPrivat20181211Mapper.tilXMLInntektsmelding(jaxbInntektsmelding, journalpostId, mottattDato, journalStatus, arkivReferanse, aktorConsumer)
        } catch (e: RuntimeException) {
            log.error( "Klarte ikke å hente inntektsmelding med journalpostId: $journalpostId", e )
            throw HentDokumentFeiletException(journalpostId, e)
        }
    }
}
