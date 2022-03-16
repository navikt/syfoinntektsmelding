package no.nav.syfo.integration.kafka

import org.apache.avro.generic.GenericRecord

data class JoarkHendelse(val journalpostId: String, val record: GenericRecord)
