package no.nav.melding.virksomhet.dokumentnotifikasjon.v1

import javax.xml.bind.annotation.*

/**
 * Kodeverk for behandlingstema
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Behandlingstema")
class XMLBehandlingstema : XMLKodeverdi {
    @XmlAttribute(name = "kodeverksRef")
    @XmlSchemaType(name = "anyURI")
    var kodeverksRef: String? = null
        get() {
            return if (kodeverksRef == null) {
                "http://nav.no/kodeverk/Kodeverk/Behandlingstema"
            } else {
                kodeverksRef
            }
        }

    constructor(value: String, kodeRef: String, kodeverksRef: String?) : super(value, kodeRef) {
        this.kodeverksRef = kodeverksRef
    }

    fun withKodeverksRef(value: String): XMLBehandlingstema {
        this.kodeverksRef = value
        return this
    }

    override fun withValue(value: String): XMLBehandlingstema {
        super.withValue(value)
        return this
    }

    override fun withKodeRef(value: String): XMLBehandlingstema {
        super.withKodeRef(value)
        return this
    }
}
