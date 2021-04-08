package no.nav.syfo.syfoinnteksmelding.service

import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.consumer.SakConsumer
import no.nav.syfo.service.EksisterendeSakService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class EksisterendeSakServiceTest {

    var sakConsumer = mockk<SakConsumer>(relaxed = true)
    var eksisterendeSakService = EksisterendeSakService(sakConsumer)

    @Test
    fun tarSisteSaksnr() {
        every { sakConsumer.finnSisteSak("aktor", null, null) } returns "137662644"

        val sak = eksisterendeSakService.finnEksisterendeSak("aktor", null, null)
        assertThat(sak).isEqualTo("137662644")
    }
}
