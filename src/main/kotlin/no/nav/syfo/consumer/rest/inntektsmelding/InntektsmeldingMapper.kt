package no.nav.syfo.consumer.rest.inntektsmelding

import no.nav.syfo.client.aktor.AktorConsumer
import no.nav.syfo.consumer.ws.mapping.InntektsmeldingArbeidsgiver20180924Mapper
import no.nav.syfo.consumer.ws.mapping.InntektsmeldingArbeidsgiverPrivat20181211Mapper
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.util.JAXB
import no.seres.xsd.nav.inntektsmelding_m._20180924.XMLInntektsmeldingM
import java.time.LocalDateTime
import javax.xml.bind.JAXBElement

class InntektsmeldingMapper {

    fun mapInntektsmelding(inntektsmeldingRAW: ByteArray, aktorConsumer: AktorConsumer, mottattDato: LocalDateTime, journalpostId: String, journalStatus: JournalStatus, arkivReferanse: String): Inntektsmelding {
        val inntektsmelding = String(inntektsmeldingRAW)
        val jaxbInntektsmelding = JAXB.unmarshalInntektsmelding<JAXBElement<Any>>(inntektsmelding)
        return if (jaxbInntektsmelding.value is XMLInntektsmeldingM)
            InntektsmeldingArbeidsgiver20180924Mapper.tilXMLInntektsmelding(jaxbInntektsmelding, journalpostId, mottattDato, journalStatus, arkivReferanse)
        else {
            InntektsmeldingArbeidsgiverPrivat20181211Mapper.tilXMLInntektsmelding(jaxbInntektsmelding, journalpostId, mottattDato, journalStatus, arkivReferanse, aktorConsumer)
        }
    }

}
