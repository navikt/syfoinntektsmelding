package no.nav.syfo.service

import no.nav.syfo.consumer.SakConsumer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.util.Optional

@RunWith(MockitoJUnitRunner::class)
class EksisterendeSakServiceTest {

    @Mock
    private lateinit var sakConsumer: SakConsumer

    @InjectMocks
    private lateinit var eksisterendeSakService: EksisterendeSakService

    @Test
    fun tarSisteSaksnr() {
        given(sakConsumer.finnSisteSak("aktor", null, null)).willReturn("137662644")

        val sak = eksisterendeSakService.finnEksisterendeSak("aktor", null, null)
        assertThat(sak).isEqualTo("137662644")
    }
}
