package no.nav.syfo.bakgrunnsjobb

import log
import no.nav.syfo.dto.BakgrunnsjobbEntitet
import no.nav.syfo.dto.BakgrunnsjobbStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
import java.time.LocalDateTime

@Service
class BakgrunnsjobbService(val bakgrunnsjobbRepository: BakgrunnsjobbRepository) {
    private val prossesserere =  HashMap<String, BakgrunnsjobbProsesserer>()
    val log = log()

    @Scheduled(cron = "0/1 0/5 0 ? * * *")
    fun sjekkOgProsseserVentendeBakgrunnsjobber() {
        finnVentende()
            .also { log.info("Fant ${it.size} bakgrunnsjobber å kjøre") }
            .forEach(this::prosesser)
    }

    private fun prosesser(jobb: BakgrunnsjobbEntitet) {
        jobb.behandlet = LocalDateTime.now()
        jobb.forsoek++

        try {
            val prossessorForType = prossesserere[jobb.type]
                    ?: throw IllegalArgumentException("Det finnes ingen prossessor for typen '${jobb.type}'")

            prossessorForType.prosesser(jobb.data)

            jobb.status = BakgrunnsjobbStatus.OK
        } catch (ex: Exception) {
            jobb.status = if (jobb.forsoek >= jobb.maksAntallForsoek) BakgrunnsjobbStatus.STOPPET else BakgrunnsjobbStatus.FEILET
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
        bakgrunnsjobbRepository.findByKjoeretidBeforeAndStatusIn(LocalDateTime.now(), setOf(BakgrunnsjobbStatus.OPPRETTET, BakgrunnsjobbStatus.FEILET))
}


interface BakgrunnsjobbProsesserer {
    fun prosesser(jobbData: String)
}
