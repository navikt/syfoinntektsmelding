package no.nav.syfo.repository

import lombok.extern.slf4j.Slf4j
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.behandling.Historikk
import no.nav.syfo.dto.FeiletEntitet
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.transaction.Transactional

@Service
@Slf4j
class FeiletService (
    private val repository: FeiletRepository
) {

    fun finnHistorikk(arkivReferanse: String): Historikk{
        return Historikk(arkivReferanse, LocalDateTime.now(), finnTidligereFeilet(arkivReferanse))
    }

    fun finnTidligereFeilet(arkivReferanse: String): List<FeiletEntitet> {
        return repository.findByArkivReferanse(arkivReferanse)
    }

    @Transactional(Transactional.TxType.REQUIRED)
    @org.springframework.transaction.annotation.Transactional("transactionManager")
    fun lagreFeilet(arkivReferanse: String, feiltype: Feiltype): FeiletEntitet {
        return repository.saveAndFlush(FeiletEntitet( arkivReferanse=arkivReferanse, feiltype = feiltype ))
    }

}
