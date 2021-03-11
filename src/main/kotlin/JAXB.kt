package no.nav.syfo.util
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.ObjectFactory
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.transform.stream.StreamResult
import javax.xml.bind.JAXBException
import java.lang.RuntimeException
import javax.xml.bind.Marshaller

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

    fun marshallForsendelsesinformasjon(element: Any?): String {
        return try {
            val writer = StringWriter()
            val marshaller = FORSENDELSESINFORMASJON!!.createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8")
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true)
            marshaller.marshal(element, StreamResult(writer))
            writer.toString()
        } catch (e: JAXBException) {
            throw RuntimeException(e)
        }
    }

    fun <T> unmarshalForsendelsesinformasjon(melding: String?): T {
        return try {
            FORSENDELSESINFORMASJON!!.createUnmarshaller().unmarshal(StringReader(melding)) as T
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
