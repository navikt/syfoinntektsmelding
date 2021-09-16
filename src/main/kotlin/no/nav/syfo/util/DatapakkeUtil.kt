package no.nav.syfo.util

import no.nav.syfo.repository.LPSStats

object DatapakkeUtil {
    fun countSAP(sapList: List<LPSStats>): LPSStats? {

        if(sapList.isEmpty()) return null

        val withBuild = sapList.filter { it.lpsNavn.contains("BUILD") }.groupBy { it.lpsNavn }.map{it.value.reduce{ s1, s2 ->
            LPSStats(s1.lpsNavn,
                s1.antallVersjoner,
                s1.antallInntektsmeldinger + s2.antallInntektsmeldinger
            )
        }}

        val tempList = withBuild.toMutableList()
		val noBuildList = sapList.filter { !it.lpsNavn.contains("BUILD") }
		if (noBuildList.isNotEmpty()) {
			val noBuild = noBuildList.reduce { s1, s2 ->
				LPSStats(
					"SAP",
					s1.antallVersjoner,
					s1.antallInntektsmeldinger + s2.antallInntektsmeldinger
				)
			}
        	tempList.add(noBuild)
		}
		if (tempList.size == 1) {
			val lpsObj = tempList.get(0)
			return LPSStats("SAP", lpsObj.antallVersjoner, lpsObj.antallInntektsmeldinger)
		}
        return tempList.reduce{ s1, s2 ->
            LPSStats("SAP",
                s1.antallVersjoner + s2.antallVersjoner,
                s1.antallInntektsmeldinger + s2.antallInntektsmeldinger
            )
        }
    }

}
