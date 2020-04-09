package no.nav.syfo.job

import no.nav.syfo.utsattoppgave.UtsattOppgaveDTO
import javax.sql.DataSource

internal class UtsattOppgaveDao(private val dataSource: DataSource) {

    fun finnUtgåtteOppgaver(): List<UtsattOppgave> {
        TODO()
    }
}
