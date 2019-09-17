package no.nav.syfo.consumer.ws

import no.nav.syfo.util.Metrikk
import no.nav.tjeneste.virksomhet.behandlesak.v2.*
import no.nav.tjeneste.virksomhet.behandlesak.v2.WSOpprettSakRequest
import no.nav.tjeneste.virksomhet.behandlesak.v2.WSOpprettSakResponse
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.test.context.TestPropertySource

import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@RunWith(MockitoJUnitRunner::class)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class BehandleSakConsumerTest {
    @Mock
    private val behandleSak: BehandleSakV2? = null

    @Mock
    private val metrikk: Metrikk? = null

    @InjectMocks
    private val behandleSakConsumer: BehandleSakConsumer? = null

    @Test
    @Throws(Exception::class)
    fun opprettSak() {
        `when`(behandleSak!!.opprettSak(any())).thenReturn(WSOpprettSakResponse().withSakId("1"))
        val captor = ArgumentCaptor.forClass(WSOpprettSakRequest::class.java)

        val sakId = behandleSakConsumer!!.opprettSak("12345678910")

        verify(behandleSak).opprettSak(captor.capture())
        val sak = captor.value.sak

        assertThat(sakId).isEqualTo("1")
        assertThat(sak.fagomrade).isEqualTo("SYK")
        assertThat(sak.fagsystem).isEqualTo("FS22")
        assertThat(sak.saktype).isEqualTo("GEN")
        assertThat<WSAktor>(sak.gjelderBrukerListe).contains( WSAktor().withIdent("12345678910") )
    }
}
