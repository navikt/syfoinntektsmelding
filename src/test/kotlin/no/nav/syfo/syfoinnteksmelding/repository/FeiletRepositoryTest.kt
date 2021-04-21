package no.nav.syfo.syfoinnteksmelding.repository

import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.dto.FeiletEntitet
import no.nav.syfo.repository.FeiletRepository
import no.nav.syfo.repository.FeiletRepositoryMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

open class FeiletRepositoryTest {

    private var respository: FeiletRepository = FeiletRepositoryMock()

    val NOW = LocalDateTime.now()
    val DAYS_1 = NOW.minusDays(1)
    val DAYS_8 = NOW.minusDays(8)
    val DAYS_14 = NOW.minusDays(14)

    @Test
    fun `Skal finne alle entiteter med arkivReferansen og rette verdier`(){
        val ARKIV_REFERANSE = "ar-123"
        respository.lagreInnteksmelding(FeiletEntitet(
            arkivReferanse = ARKIV_REFERANSE,
            tidspunkt = DAYS_1,
            feiltype = Feiltype.AKTØR_IKKE_FUNNET
        ))
        respository.lagreInnteksmelding(FeiletEntitet(
            arkivReferanse = ARKIV_REFERANSE,
            tidspunkt = DAYS_1,
            feiltype = Feiltype.AKTØR_IKKE_FUNNET
        ))
        respository.lagreInnteksmelding(FeiletEntitet(
            arkivReferanse = "ar-222",
            tidspunkt = DAYS_1,
            feiltype = Feiltype.JMS
        ))
        respository.lagreInnteksmelding(FeiletEntitet(
            arkivReferanse = "ar-555",
            tidspunkt = DAYS_1,
            feiltype = Feiltype.USPESIFISERT
        ))
        respository.lagreInnteksmelding(FeiletEntitet(
            arkivReferanse = ARKIV_REFERANSE,
            tidspunkt = DAYS_8,
            feiltype = Feiltype.BEHANDLENDE_IKKE_FUNNET
        ))
        val liste = respository.findByArkivReferanse(ARKIV_REFERANSE)
        assertThat(liste.size).isEqualTo(2)
        assertThat(liste[0].arkivReferanse).isEqualTo(ARKIV_REFERANSE)
        assertThat(liste[0].feiltype).isEqualTo(Feiltype.AKTØR_IKKE_FUNNET)
        assertThat(liste[0].tidspunkt).isEqualTo(DAYS_1)
        assertThat(liste[1].arkivReferanse).isEqualTo(ARKIV_REFERANSE)
        assertThat(liste[1].feiltype).isEqualTo(Feiltype.BEHANDLENDE_IKKE_FUNNET)
        assertThat(liste[1].tidspunkt).isEqualTo(DAYS_8)
    }

    @Test
    fun `Skal ikke finne entitet dersom arkivReferansen ikke er lagret tidligere`(){
        val ARKIV_REFERANSE = "ar-finnes-ikke"
        respository.lagreInnteksmelding(FeiletEntitet(
            arkivReferanse = "ar-333",
            tidspunkt = DAYS_8,
            feiltype = Feiltype.BEHANDLENDE_IKKE_FUNNET
        ))
        respository.lagreInnteksmelding(FeiletEntitet(
            arkivReferanse = "ar-444",
            tidspunkt = DAYS_14,
            feiltype = Feiltype.BEHANDLENDE_IKKE_FUNNET
        ))
        val liste = respository.findByArkivReferanse(ARKIV_REFERANSE)
        assertThat(liste.size).isEqualTo(0)
    }


}

