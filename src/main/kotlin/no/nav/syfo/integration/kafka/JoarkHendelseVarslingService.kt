package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import org.slf4j.LoggerFactory

interface VarslingService{
    fun handleMessage(jsonMessageString: String)
}

class JoarkHendelseVarslingService (
        private val om: ObjectMapper
) : VarslingService {

    val logger = LoggerFactory.getLogger(JoarkHendelseVarslingService::class.java)

    override fun handleMessage(jsonMessageString: String) {
        val msg = om.readValue(jsonMessageString, InngaaendeJournalpostDTO::class.java)
        logger.debug("Fikk en melding fra kafka på journalpost id ${msg.journalpostId} med ${msg.behandlingstema}")
    }


    companion object {
        val VENTETID_I_DAGER = 21L
    }
}
