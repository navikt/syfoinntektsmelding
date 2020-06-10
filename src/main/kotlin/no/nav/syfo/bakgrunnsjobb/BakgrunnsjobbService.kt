package no.nav.syfo.bakgrunnsjobb

import log
import no.nav.syfo.dto.BakgrunnsjobbEntitet
import no.nav.syfo.dto.BakgrunnsjobbStatus
import no.nav.syfo.kafkamottak.JoarkInntektsmeldingHendelseProsessor
import no.nav.syfo.repository.BakgrunnsjobbRepository
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.FeiletUtsattOppgaveMeldingProsessor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import java.time.LocalDateTime.now

@Component
class BakgrunnsjobbService(
    val bakgrunnsjobbRepository: BakgrunnsjobbRepository,
    feiletUtsattOppgaveMeldingProsessor: FeiletUtsattOppgaveMeldingProsessor,
    joarkInntektsmeldingHendelseProsessor: JoarkInntektsmeldingHendelseProsessor,
    private val metrikk: Metrikk

    ) {
    private val prossesserere =  HashMap<String, BakgrunnsjobbProsesserer>()
    val log = log()

    init {
        // konfigurasjon av hvilke prosessorer som er kjente for tjenesten. Dette kan puttes et annet sted om ønskelig
        registrerJobbProsesserer(FeiletUtsattOppgaveMeldingProsessor.JOBB_TYPE, feiletUtsattOppgaveMeldingProsessor)
        registrerJobbProsesserer(JoarkInntektsmeldingHendelseProsessor.JOBB_TYPE, joarkInntektsmeldingHendelseProsessor)
    }

    @Scheduled(fixedRate = 60000)
    fun sjekkOgProsseserVentendeBakgrunnsjobber() {
        finnVentende()
            .also { log.info("Fant ${it.size} bakgrunnsjobber å kjøre") }
            .forEach(this::prosesser)
    }

    private fun prosesser(jobb: BakgrunnsjobbEntitet) {
        jobb.behandlet = now()
        jobb.forsoek++

        try {
            val prossessorForType = prossesserere[jobb.type]
                    ?: throw IllegalArgumentException("Det finnes ingen prossessor for typen '${jobb.type}'. Dette må konfigureres.")

            jobb.kjoeretid = prossessorForType.nesteForsoek(jobb.forsoek, now())
            prossessorForType.prosesser(jobb.opprettet, jobb.forsoek, jobb.data)

            jobb.status = BakgrunnsjobbStatus.OK
        } catch (ex: Exception) {
            jobb.status = if (jobb.forsoek >= jobb.maksAntallForsoek) BakgrunnsjobbStatus.STOPPET else BakgrunnsjobbStatus.FEILET

            if (jobb.status == BakgrunnsjobbStatus.STOPPET) {
                log.error("Jobb ${jobb.uuid} feilet permanent", ex)
                metrikk.tellStoppetBakgrunnsjobb()
            } else {
                log.error("Jobb ${jobb.uuid} feilet, forsøker igjen ${jobb.kjoeretid}", ex)
                metrikk.tellFeiletBakgrunnsjobb()
            }
        } finally {
            lagre(jobb)
        }
    }

    fun registrerJobbProsesserer(type: String, prossessor: BakgrunnsjobbProsesserer) {
        prossesserere[type] = prossessor
    }

    fun opprett(bakgrunnsjobb: BakgrunnsjobbEntitet): String {
        return bakgrunnsjobbRepository.saveAndFlush(bakgrunnsjobb).uuid
    }

    fun finn(id: String) =
        bakgrunnsjobbRepository.findById(id)

    fun lagre(jobb: BakgrunnsjobbEntitet) {
        bakgrunnsjobbRepository.saveAndFlush(jobb)
    }

    fun finnVentende(): List<BakgrunnsjobbEntitet> =
        bakgrunnsjobbRepository.findByKjoeretidBeforeAndStatusIn(now(), setOf(BakgrunnsjobbStatus.OPPRETTET, BakgrunnsjobbStatus.FEILET))
}

/**
 * Interface for en klasse som kan prosessere en bakgrunnsjobbstype
 */
interface BakgrunnsjobbProsesserer {
    fun prosesser(jobbOpprettet: LocalDateTime, forsoek: Int, jobbData: String)
    fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime
}
