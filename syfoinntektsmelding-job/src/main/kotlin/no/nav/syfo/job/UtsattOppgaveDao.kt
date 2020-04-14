package no.nav.syfo.job

import javax.sql.DataSource

internal class UtsattOppgaveDao(private val dataSource: DataSource) {

    fun finnUtgåtteOppgaver(): List<UtsattOppgave> {
        TODO()
    }
}
