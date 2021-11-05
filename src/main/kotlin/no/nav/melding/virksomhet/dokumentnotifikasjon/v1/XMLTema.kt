package no.nav.melding.virksomhet.dokumentnotifikasjon.v1

import javax.xml.bind.annotation.*

/**
 * Kodeverk for tema
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Tema")
class XMLTema : XMLKodeverdi {
    @XmlAttribute(name = "kodeverksRef")
    @XmlSchemaType(name = "anyURI")
    var kodeverksRef: String? = null
        get() {
            return if (kodeverksRef == null) {
                "http://nav.no/kodeverk/Kodeverk/Tema"
            } else {
                kodeverksRef
            }
        }

    constructor(value: String, kodeRef: String, kodeverksRef: String?) : super(value, kodeRef) {
        this.kodeverksRef = kodeverksRef
    }

    fun withKodeverksRef(value: String): XMLTema {
        this.kodeverksRef = value
        return this
    }

    override fun withValue(value: String): XMLTema {
        super.withValue(value)
        return this
    }

    override fun withKodeRef(value: String): XMLTema {
        super.withKodeRef(value)
        return this
    }
}
