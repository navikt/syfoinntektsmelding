package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import no.nav.syfo.utsattoppgave.UtsattOppgaveDTO
import org.slf4j.LoggerFactory

class UtsattOppgaveVarslingService (
        private val om: ObjectMapper
) : VarslingService {

    val logger = LoggerFactory.getLogger(UtsattOppgaveVarslingService::class.java)

    override fun handleMessage(jsonMessageString: String) {
        val msg = om.readValue(jsonMessageString, UtsattOppgaveDTO::class.java)
        logger.debug("Fikk en melding fra kafka på dokument id ${msg.dokumentId} med dokumenttype ${msg.dokumentType}")
    }

    companion object {
        const val VENTETID_I_DAGER = 21L
    }
}
