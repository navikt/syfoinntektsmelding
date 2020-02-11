package no.nav.syfo.behandling

import no.nav.syfo.dto.FeiletEntitet
import java.time.LocalDateTime

const val ANTALL_DAGER_FØR_ARKIVERING = 7L

data class Historikk(val arkivReferanse: String, val feiletList: List<FeiletEntitet>) {
    fun skalArkiveresForDato(dato: LocalDateTime = LocalDateTime.now()): Boolean {
        return skalArkiveresForDato(dato, feiletList)
    }
}

fun skalArkiveresForDato(dato: LocalDateTime, feiletList: List<FeiletEntitet>): Boolean {
    return feiletList.any { feil ->
        feil.tidspunkt.isBefore(dato.minusDays(ANTALL_DAGER_FØR_ARKIVERING))
    }
}
