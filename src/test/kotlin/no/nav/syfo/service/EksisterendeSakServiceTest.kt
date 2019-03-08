package no.nav.syfo.service

import no.nav.syfo.consumer.SakConsumer
import no.nav.syfo.consumer.rest.EksisterendeSakConsumer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class EksisterendeSakServiceTest {

    @Mock
    private lateinit var eksisterendeSakConsumer: EksisterendeSakConsumer

    @Mock
    private lateinit var sakConsumer: SakConsumer

    @InjectMocks
    private lateinit var eksisterendeSakService: EksisterendeSakService

    @Test
    fun tarSisteSaksnr() {
        given(eksisterendeSakConsumer.finnEksisterendeSaksId("aktor", "orgnummer")).willReturn(Optional.of("137662643"))
        given(sakConsumer.finnSisteSak("aktor")).willReturn("137662644")

        val sak = eksisterendeSakService.finnEksisterendeSak("aktor", "orgnummer")
        assertThat(sak).isEqualTo("137662644")
    }

    @Test
    fun tarSaksnrSomIkkeErNull() {
        given(eksisterendeSakConsumer.finnEksisterendeSaksId("aktor", "orgnummer")).willReturn(Optional.of("137662644"))
        given(sakConsumer.finnSisteSak("aktor")).willReturn(null)

        val sak = eksisterendeSakService.finnEksisterendeSak("aktor", "orgnummer")
        assertThat(sak).isEqualTo("137662644")
    }

    @Test
    fun returnererManglendeSaksnrSomNull() {
        given(eksisterendeSakConsumer.finnEksisterendeSaksId("aktor", "orgnummer")).willReturn(Optional.ofNullable(null))
        given(sakConsumer.finnSisteSak("aktor")).willReturn(null)

        val sak = eksisterendeSakService.finnEksisterendeSak("aktor", "orgnummer")
        assertThat(sak).isEqualTo(null)
    }
}
