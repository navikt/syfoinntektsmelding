package no.nav.syfo.integration.kafka

import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord

data class JoarkHendelse(val journalpostId: String, val record: JournalfoeringHendelseRecord)
