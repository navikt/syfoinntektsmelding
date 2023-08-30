package no.nav.melding.virksomhet.dokumentnotifikasjon.v1

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlSchemaType
import javax.xml.bind.annotation.XmlType
import javax.xml.bind.annotation.XmlValue

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Kodeverdi", namespace = "", propOrder = ["value"])
open class XMLKodeverdi {
    @XmlValue
    protected var value: String
    @XmlAttribute(name = "kodeRef")
    @XmlSchemaType(name = "anyURI")
    protected var kodeRef: String

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
