package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.behandling.HentDokumentFeiletException
import no.nav.syfo.behandling.HentDokumentIkkeFunnetException
import no.nav.syfo.behandling.HentDokumentSikkerhetsbegrensningException
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.consumer.ws.mapping.InntektsmeldingArbeidsgiver20180924Mapper
import no.nav.syfo.consumer.ws.mapping.InntektsmeldingArbeidsgiverPrivat20181211Mapper
import no.nav.syfo.domain.InngaaendeJournal
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.util.JAXB
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentDokumentIkkeFunnet
import no.nav.tjeneste.virksomhet.journal.v2.binding.HentDokumentSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2
import no.nav.tjeneste.virksomhet.journal.v2.informasjon.Variantformater
import no.nav.tjeneste.virksomhet.journal.v2.meldinger.HentDokumentRequest
import no.seres.xsd.nav.inntektsmelding_m._20180924.XMLInntektsmeldingM
import javax.xml.bind.JAXBElement

class JournalConsumer(private val journalV2: JournalV2,
                      private val aktorConsumer: AktorConsumer) {

    var log = log()

    fun hentInntektsmelding(journalpostId: String, inngaaendeJournal: InngaaendeJournal, arkivReferanse: String): Inntektsmelding {

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

            return if (jaxbInntektsmelding.value is XMLInntektsmeldingM)
                InntektsmeldingArbeidsgiver20180924Mapper.tilXMLInntektsmelding(jaxbInntektsmelding, journalpostId, inngaaendeJournal, arkivReferanse)
            else
                InntektsmeldingArbeidsgiverPrivat20181211Mapper.tilXMLInntektsmelding(jaxbInntektsmelding, journalpostId, inngaaendeJournal, aktorConsumer, arkivReferanse)
        } catch (e: HentDokumentSikkerhetsbegrensning) {
            log.error("Feil ved henting av dokument: Sikkerhetsbegrensning!")
            throw HentDokumentSikkerhetsbegrensningException(journalpostId, e)
        } catch (e: HentDokumentDokumentIkkeFunnet) {
            log.error("Feil ved henting av dokument: Dokument ikke funnet!")
            throw HentDokumentIkkeFunnetException(journalpostId, e)
        } catch (e: RuntimeException) {
            log.error(
                    "Klarte ikke å hente inntektsmelding med journalpostId: {} og dokumentId: {}",
                    journalpostId,
                    inngaaendeJournal.dokumentId,
                    e
            )
            throw HentDokumentFeiletException(journalpostId, e)
        }

    }

}
