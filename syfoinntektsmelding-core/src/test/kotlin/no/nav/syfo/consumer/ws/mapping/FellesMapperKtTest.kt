package no.nav.syfo.consumer.ws.mapping

import junit.framework.Assert.assertEquals
import no.nav.syfo.domain.inntektsmelding.Naturalytelse
import org.junit.Test
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName

class FellesMapperKtTest {

    @Test
    fun skal_mappe_ytelse_til_riktig_verdi() {
        assertEquals(mapNaturalytelseType(opprettJaxbElement("kostDoegn")), Naturalytelse.KOSTDOEGN);
        assertEquals(mapNaturalytelseType(opprettJaxbElement("tull")), Naturalytelse.ANNET);
        assertEquals(mapNaturalytelseType(opprettJaxbElement("skattepliktigDelForsikringer")), Naturalytelse.SKATTEPLIKTIGDELFORSIKRINGER);
    }

    private fun opprettJaxbElement(verdi: String) = JAXBElement(QName.valueOf("naturalYtelse"), String::class.java, verdi)

}