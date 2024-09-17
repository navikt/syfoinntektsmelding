package no.nav.syfo.mapping

import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.util.JAXB
import no.seres.xsd.nav.inntektsmelding_m._20180924.XMLInntektsmeldingM
import java.time.LocalDateTime
import javax.xml.bind.JAXBElement

class XmlInntektsmeldingMapper {

    fun mapInntektsmelding(
        inntektsmeldingRAW: ByteArray,
        pdlClient: PdlClient,
        mottattDato: LocalDateTime,
        journalpostId: String,
        journalStatus: JournalStatus,
        arkivReferanse: String
    ): Inntektsmelding {
        val inntektsmelding = String(inntektsmeldingRAW)
        val jaxbInntektsmelding = JAXB.unmarshalInntektsmelding<JAXBElement<Any>>(inntektsmelding)
        return if (jaxbInntektsmelding.value is XMLInntektsmeldingM)
            InntektsmeldingArbeidsgiver20180924Mapper.fraXMLInntektsmelding(
                jaxbInntektsmelding,
                journalpostId,
                mottattDato,
                journalStatus,
                arkivReferanse
            )
        else {
            InntektsmeldingArbeidsgiverPrivat20181211Mapper.fraXMLInntektsmelding(
                jaxbInntektsmelding,
                journalpostId,
                mottattDato,
                journalStatus,
                arkivReferanse,
                pdlClient
            )
        }
    }
}
