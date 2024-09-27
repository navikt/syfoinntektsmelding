package no.nav.syfo.koin

import io.ktor.server.config.ApplicationConfig
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.helsearbeidsgiver.tokenprovider.AccessTokenProvider
import no.nav.syfo.client.OppgaveService
import no.nav.syfo.client.dokarkiv.DokArkivClient
import no.nav.syfo.client.norg.Norg2Client
import no.nav.syfo.client.saf.SafDokumentClient
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.util.AppEnv
import no.nav.syfo.util.Metrikk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import javax.sql.DataSource
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
class KoinProfilesKtTest : KoinTest {
    private val config = mockk<ApplicationConfig>(relaxed = true)
    private val clientConfig = mockk<ApplicationConfig>(relaxed = true)
    private val accessTokenProvider = mockk<AccessTokenProvider>(relaxed = true)
    private val metrikk = mockk<Metrikk>(relaxed = true)

    private val dataSource = mockk<DataSource>(relaxed = true)
    private val inntektsmeldingRepository: InntektsmeldingRepository by inject()
    private val pdlClient: PdlClient by inject()
    private val safDokumentClient: SafDokumentClient by inject()
    private val safJournalpostClient: SafDokumentClient by inject()
    private val dokArkivClient: DokArkivClient by inject()
    private val norg2Client: Norg2Client by inject()
    private val oppgaveService: OppgaveService by inject()

    @Test
    fun `test prodConfig`() {
        mockConfig(AppEnv.PROD)

        startKoin {
            modules(selectModuleBasedOnProfile(config) + getTestModules())
        }

        assertKoin()
        stopKoin()
    }

    @Test
    fun `test devConfig`() {
        mockConfig(AppEnv.DEV)

        startKoin {
            modules(selectModuleBasedOnProfile(config) + getTestModules())
        }

        assertKoin()
        stopKoin()
    }

    @Test
    fun `test localConfig`() {
        mockConfig(AppEnv.LOCAL)

        startKoin {
            modules(selectModuleBasedOnProfile(config) + getTestModules())
        }

        assertKoin()
        stopKoin()
    }

    private fun assertKoin() {
        assertNotNull(dataSource)
        assertNotNull(inntektsmeldingRepository)
        assertNotNull(pdlClient)
        assertNotNull(safDokumentClient)
        assertNotNull(dokArkivClient)
        assertNotNull(norg2Client)
        assertNotNull(oppgaveService)
        assertNotNull(safJournalpostClient)
    }

    private fun getTestModules(): Module {
        val testModule =
            module {
                single { dataSource } bind DataSource::class
                single(named(AccessScope.PDL)) { accessTokenProvider } bind AccessTokenProvider::class
                single(named(AccessScope.SAF)) { accessTokenProvider } bind AccessTokenProvider::class
                single(named(AccessScope.OPPGAVE)) { accessTokenProvider } bind AccessTokenProvider::class
                single(named(AccessScope.DOKARKIV)) { accessTokenProvider } bind AccessTokenProvider::class
                single { metrikk }
            }
        return testModule
    }

    private fun mockConfig(appEnv: AppEnv) {
        every { config.property("koin.profile").getString() } returns appEnv.name
        every { config.configList("no.nav.security.jwt.client.registration.clients") } returns listOf(clientConfig)
        every { config.property("saf_dokument_url").getString() } returns "saf_url"
        every { config.property("dokarkiv_url").getString() } returns "dokarkiv_url"
    }
}
