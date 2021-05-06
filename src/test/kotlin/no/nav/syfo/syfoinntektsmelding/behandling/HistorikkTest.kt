package no.nav.syfo.syfoinntektsmelding.behandling

import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.behandling.Historikk
import no.nav.syfo.dto.FeiletEntitet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import java.time.LocalDateTime

class HistorikkTest {

    val IDAG: LocalDateTime = LocalDateTime.of(2019,12,24,18,0)
    val ELDRE: LocalDateTime = IDAG.minusDays(8)
    val IGÅR: LocalDateTime = IDAG.minusDays(1)
    val EN_TIME_SIDEN: LocalDateTime = IDAG.minusDays(0)

    @Test
    fun `Skal arkiveres dersom inntektsmeldingen kom inn før 1 uke siden`() {
        val AR = "AR-123"
        val feiletList: List<FeiletEntitet> = listOf(
            FeiletEntitet(arkivReferanse =  AR,feiltype = Feiltype.JMS, tidspunkt = EN_TIME_SIDEN),
            FeiletEntitet(arkivReferanse =  AR,feiltype = Feiltype.JMS, tidspunkt = IGÅR),
            FeiletEntitet(arkivReferanse =  AR,feiltype = Feiltype.JMS, tidspunkt = ELDRE)
        )
        val historikk = Historikk( arkivReferanse = AR, feiletList=feiletList, dato = IDAG )
        assertThat(historikk.skalArkiveresForDato()).isTrue()
    }

    @Test
    fun `Skal ikke arkiveres fordi det inntektsmelding kom inn akkurat nå`() {
        val AR = "AR-123"
        val feiletList: List<FeiletEntitet> = listOf(FeiletEntitet(arkivReferanse =  AR,feiltype = Feiltype.JMS, tidspunkt = IDAG.minusHours(2)))
        val historikk = Historikk( arkivReferanse = AR, feiletList=feiletList, dato = IDAG )
        assertThat(historikk.skalArkiveresForDato()).isFalse()
    }

    @Test
    fun `Skal ikke arkiveres fordi det ikke har gått mer enn 1 uke siden inntektsmelding kom inn første gang`() {
        val AR = "AR-123"
        val feiletList: List<FeiletEntitet> = listOf(FeiletEntitet(arkivReferanse =  AR,feiltype = Feiltype.JMS, tidspunkt = IGÅR))
        val historikk = Historikk( arkivReferanse = AR, feiletList=feiletList, dato = IDAG )
        assertThat(historikk.skalArkiveresForDato()).isFalse()
    }

    @Test
    fun `Skal ikke arkiveres fordi inntektsmeldingen ikke har kommet inn før`() {
        val AR = "AR-123"
        val feiletList: List<FeiletEntitet> = listOf()
        val historikk = Historikk( arkivReferanse = AR, feiletList=feiletList, dato = IDAG )
        assertThat(historikk.skalArkiveresForDato()).isFalse()
    }

}
