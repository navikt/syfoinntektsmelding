package no.nav.melding.virksomhet.dokumentnotifikasjon.v1

import javax.xml.bind.JAXBElement
import javax.xml.bind.annotation.XmlElementDecl
import javax.xml.bind.annotation.XmlRegistry
import javax.xml.namespace.QName

@XmlRegistry
class ObjectFactory {

    private val _forsendelsesinformasjon_QNAME = QName("", "forsendelsesinformasjon")
    private val emptyString = ""
    /**
     * Create an instance of {@link Tema }
     *
     */
    fun createXMLTema(): XMLTema {
        return XMLTema(emptyString, emptyString, null)
    }

    /**
     * Create an instance of {@link Kodeverdi }
     *
     */
    fun createXMLKodeverdi(): XMLKodeverdi {
        return XMLKodeverdi(emptyString, emptyString)
    }

    /**
     * Create an instance of {@link Behandlingstema }
     *
     */
    fun createXMLBehandlingstema(): XMLBehandlingstema {
        return XMLBehandlingstema(emptyString, emptyString, null)
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link forsendelsesinformasjon }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "", name = "forsendelsesinformasjon")
    fun createforsendelsesinformasjon(value: XMLForsendelsesinformasjon): JAXBElement<XMLForsendelsesinformasjon> {
        return JAXBElement<XMLForsendelsesinformasjon>(
            _forsendelsesinformasjon_QNAME,
            XMLForsendelsesinformasjon::class.java,
            null,
            value
        )
    }

    fun createXMLForsendelsesinformasjon(): XMLForsendelsesinformasjon {
        return XMLForsendelsesinformasjon()
    }
}
