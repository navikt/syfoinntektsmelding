package no.nav.syfo.syfoinntektsmelding.util

import no.nav.syfo.datapakke.DatapakkePublisherJob
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.grunnleggendeInntektsmelding
import no.nav.syfo.repository.LPSStats
import no.nav.syfo.util.DatapakkeUtil
import no.nav.syfo.util.validerInntektsmelding
import org.junit.Assert
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DatapakkeUtilTest {

    @Test
    fun `countSAP teller riktig antall versjoner og inntektsmeldinger`() {

        val lpsStats = listOf(
            LPSStats("SAP [SID:QOA/510]", 1, 5),
            LPSStats("SAP [SID:QOA/511]", 1, 4),
            LPSStats("SAP [SID:QOA/514][BUILD:20210807]",1,6),
            LPSStats("SAP [SID:QOA/514][BUILD:20210818]", 1, 3),
            LPSStats("SAP [SID:QOA/514][BUILD:20210818]", 1, 4)
        )

        val expected = LPSStats("SAP", 3, 22)

        assertEquals(expected,DatapakkeUtil.countSAP(lpsStats),)
    }

    @Test
    fun `countSAP returnerer null hvis listen er tom`() {
        val lpsStats = emptyList<LPSStats>()

        assertEquals(null, DatapakkeUtil.countSAP(lpsStats))
    }

    @Test
    fun `countSAP returnerer riktig hvis ingen Build`() {
        val lpsStats = listOf(
            LPSStats("SAP [SID:QOA/510]", 1, 5),
            LPSStats("SAP [SID:QOA/511]", 1, 4),
            LPSStats("SAP [SID:QOA/514]",1,6),
            LPSStats("SAP [SID:QOA/515]", 1, 3),
        )

        val expected = LPSStats("SAP", 1, 18)

        assertEquals(expected,DatapakkeUtil.countSAP(lpsStats),)
    }
}
