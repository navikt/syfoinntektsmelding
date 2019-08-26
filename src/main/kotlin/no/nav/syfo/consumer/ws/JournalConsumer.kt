package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.domain.InngaaendeJournal
import no.nav.syfo.domain.Inntektsmelding
import no.nav.syfo.util.JAXB
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentDokumentIkkeFunnet
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2
import no.nav.tjeneste.virksomhet.journal.v2.informasjon.Variantformater
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentRequest
import org.springframework.stereotype.Component

import javax.xml.bind.JAXBElement

@Component
class JournalConsumer(private val journalV2: JournalV2) {

    var log = log()

    fun hentInntektsmelding(journalpostId: String, inngaaendeJournal: InngaaendeJournal): Inntektsmelding {

        val format = Variantformater()
        format.value = "ORIGINAL"

        val request = HentDokumentRequest()
        request.journalpostId = journalpostId
        request.dokumentId = inngaaendeJournal.dokumentId
        request.variantformat = format

        try {
            val inntektsmeldingRAW = journalV2.hentDokument(request).dokument
            val inntektsmelding = String(inntektsmeldingRAW)

            val jaxbInntektsmelding = JAXB.unmarshalInntektsmelding<JAXBElement<Any>>(inntektsmelding)

            val (arbeidsforholdId, perioder, arbeidstakerFnr, virksomhetsnummer, arbeidsgiverPrivat, aarsakTilInnsending) = if (jaxbInntektsmelding.value is no.seres.xsd.nav.inntektsmelding_m._20180924.XMLInntektsmeldingM)
                InntektsmeldingArbeidsgiver20180924Mapper.tilXMLInntektsmelding(jaxbInntektsmelding)
            else
                InntektsmeldingArbeidsgiverPrivat20181211Mapper.tilXMLInntektsmelding(jaxbInntektsmelding)

            return Inntektsmelding(
                arbeidstakerFnr,
                virksomhetsnummer,
                arbeidsgiverPrivat,
                journalpostId,
                arbeidsforholdId,
                aarsakTilInnsending,
                inngaaendeJournal.status,
                perioder
            )
        } catch (e: HentDokumentSikkerhetsbegrensning) {
            log.error("Feil ved henting av dokument: Sikkerhetsbegrensning!")
            throw RuntimeException("Feil ved henting av dokument: Sikkerhetsbegrensning!", e)
        } catch (e: HentDokumentDokumentIkkeFunnet) {
            log.error("Feil ved henting av dokument: Dokument ikke funnet!")
            throw RuntimeException("Feil ved henting av journalpost: Dokument ikke funnet!", e)
        } catch (e: RuntimeException) {
            log.error(
                "Klarte ikke å hente inntektsmelding med journalpostId: {} og dokumentId: {}",
                journalpostId,
                inngaaendeJournal.dokumentId,
                e
            )
            throw RuntimeException(e)
        }

    }
}
