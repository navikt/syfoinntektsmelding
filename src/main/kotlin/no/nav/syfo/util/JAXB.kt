package no.nav.syfo.util
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.ObjectFactory
import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException

object JAXB {
    private var FORSENDELSESINFORMASJON: JAXBContext? = null
    private var INNTEKTSMELDING: JAXBContext? = null

    init {
        try {
            FORSENDELSESINFORMASJON = JAXBContext.newInstance(
                ObjectFactory::class.java
            )
            INNTEKTSMELDING = JAXBContext.newInstance(
                no.seres.xsd.nav.inntektsmelding_m._20180924.ObjectFactory::class.java,
                no.seres.xsd.nav.inntektsmelding_m._20181211.ObjectFactory::class.java
            )
        } catch (e: JAXBException) {
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    fun <T> unmarshalInntektsmelding(melding: String?): T {
        return try {
            INNTEKTSMELDING!!.createUnmarshaller().unmarshal(StringReader(melding)) as T
        } catch (e: JAXBException) {
            throw RuntimeException(e)
        }
    }
}
