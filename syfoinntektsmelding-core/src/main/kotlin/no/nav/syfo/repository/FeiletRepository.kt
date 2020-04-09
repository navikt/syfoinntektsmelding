package no.nav.syfo.repository

import no.nav.syfo.dto.FeiletEntitet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FeiletRepository : JpaRepository<FeiletEntitet, String> {

    fun findByArkivReferanse(arkivReferanse: String): List<FeiletEntitet>

}
