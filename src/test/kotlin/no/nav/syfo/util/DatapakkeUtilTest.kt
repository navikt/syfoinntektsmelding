package no.nav.syfo.util

import no.nav.syfo.repository.LPSStats
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

        assertEquals(expected,DatapakkeUtil.countSAP(lpsStats))
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

        assertEquals(expected,DatapakkeUtil.countSAP(lpsStats))
    }

	@Test
	fun `countSAP returnerer riktig hvis bare et SAP element i listen`() {
		val lpsStats = listOf(
			LPSStats(
				"SAP [SID:QHR/002]",
				1,
				6
			)
		)

		val expected = LPSStats("SAP", 1, 6)

		assertEquals(expected,DatapakkeUtil.countSAP(lpsStats))
	}

	@Test
	fun `countSAP returnerer riktig hvis bare et BUILD element i listen`() {
		val lpsStats = listOf(
			LPSStats(
				"SAP [SID:QHR/002][BUILD:20210820]",
				1,
				6
			)
		)

		val expected = LPSStats("SAP", 1, 6)

		assertEquals(expected,DatapakkeUtil.countSAP(lpsStats))
	}
}
