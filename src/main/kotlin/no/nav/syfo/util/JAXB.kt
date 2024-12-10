package no.nav.syfo.util
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.ObjectFactory
import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException

object JAXB {
    private var forsendelsesinformasjon: JAXBContext? = null
    private var inntektsmelding: JAXBContext? = null

    init {
        try {
            forsendelsesinformasjon =
                JAXBContext.newInstance(
                    ObjectFactory::class.java,
                )
            inntektsmelding =
                JAXBContext.newInstance(
                    no.seres.xsd.nav.inntektsmelding_m._20180924.ObjectFactory::class.java,
                    no.seres.xsd.nav.inntektsmelding_m._20181211.ObjectFactory::class.java,
                )
        } catch (e: JAXBException) {
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    fun <T> unmarshalInntektsmelding(melding: String?): T {
        return try {
            inntektsmelding!!.createUnmarshaller().unmarshal(StringReader(melding)) as T
        } catch (e: JAXBException) {
            throw RuntimeException(e)
        }
    }
}
