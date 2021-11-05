package no.nav.melding.virksomhet.dokumentnotifikasjon.v1

import javax.xml.bind.annotation.*

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Kodeverdi", namespace = "", propOrder = ["value"])
open class XMLKodeverdi {
    @XmlValue
    protected lateinit var value: String
    @XmlAttribute(name = "kodeRef")
    @XmlSchemaType(name = "anyURI")
    protected lateinit var kodeRef: String

    constructor(value: String, kodeRef: String) {
        this.value = value
        this.kodeRef = kodeRef
    }

    open fun withValue(value: String): XMLKodeverdi {
        this.value = value
        return this
    }

    open fun withKodeRef(value: String): XMLKodeverdi {
        this.kodeRef = value
        return this
    }
}
