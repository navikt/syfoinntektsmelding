package no.nav.syfo.behandling

import java.time.LocalDateTime
import no.nav.syfo.dto.FeiletEntitet

const val ANTALL_DAGER_FØR_ARKIVERING = 7L

data class Historikk(val arkivReferanse: String, val dato: LocalDateTime, val feiletList: List<FeiletEntitet>) {
    fun skalArkiveresForDato(): Boolean {
        return skalArkiveresForDato(dato, feiletList)
    }
}

fun skalArkiveresForDato(dato: LocalDateTime, feiletList: List<FeiletEntitet>): Boolean {
    return feiletList.any { feil ->
        feil.tidspunkt.isBefore(dato.minusDays(ANTALL_DAGER_FØR_ARKIVERING))
    }
}
