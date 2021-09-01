package no.nav.syfo.util

import no.nav.syfo.repository.LPSStats

object DatapakkeUtil {
    fun countSAP(sapList: List<LPSStats>): LPSStats? {

        if(sapList.isEmpty()) return null

        val noBuild = sapList.filter { !it.lpsNavn.contains("BUILD") }
            .reduce{ s1, s2 ->
                LPSStats("SAP",
                    s1.antallVersjoner,
                    s1.antallInntektsmeldinger + s2.antallInntektsmeldinger
                )
            }

        val withBuild = sapList.filter { it.lpsNavn.contains("BUILD") }.groupBy { it.lpsNavn }.map{it.value.reduce{ s1, s2 ->
            LPSStats(s1.lpsNavn,
                s1.antallVersjoner,
                s1.antallInntektsmeldinger + s2.antallInntektsmeldinger
            )
        }}

        val tempList = withBuild.toMutableList()
        tempList.add(noBuild)

        return tempList.reduce{ s1, s2 ->
            LPSStats("SAP",
                s1.antallVersjoner + s2.antallVersjoner,
                s1.antallInntektsmeldinger + s2.antallInntektsmeldinger
            )
        }
    }

}
