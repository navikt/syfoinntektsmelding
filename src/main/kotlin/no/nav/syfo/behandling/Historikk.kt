package no.nav.syfo.behandling

import no.nav.syfo.dto.FeiletEntitet
import java.time.LocalDateTime

data class Historikk(val arkivReferanse: String, val feiletList: List<FeiletEntitet>) {

    val ANTALL_DAGER_FØR_ARKIVERING = 7

    fun skalArkiveresForDato(dato: LocalDateTime = LocalDateTime.now()): Boolean {
        if (feiletList.isEmpty()){
            return false
        }
        val fe = feiletList.sortedByDescending { feiletEntitet -> feiletEntitet.tidspunkt }.last()
        return fe.tidspunkt.isBefore(dato.minusDays(ANTALL_DAGER_FØR_ARKIVERING.toLong()))
    }

}
