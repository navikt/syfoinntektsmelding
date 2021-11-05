package no.nav.syfo.repository

import java.time.LocalDateTime
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.behandling.Historikk
import no.nav.syfo.dto.FeiletEntitet

class FeiletService(
    private val repository: FeiletRepository
) {

    fun finnHistorikk(arkivReferanse: String): Historikk {
        return Historikk(arkivReferanse, LocalDateTime.now(), finnTidligereFeilet(arkivReferanse))
    }

    private fun finnTidligereFeilet(arkivReferanse: String): List<FeiletEntitet> {
        return repository.findByArkivReferanse(arkivReferanse)
    }

    fun lagreFeilet(arkivReferanse: String, feiltype: Feiltype): FeiletEntitet {
        return repository.lagreInnteksmelding(FeiletEntitet(arkivReferanse = arkivReferanse, feiltype = feiltype))
    }
}
