package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.helse.arbeidsgiver.system.AppEnv
import no.nav.helse.arbeidsgiver.system.getEnvironment
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.helsearbeidsgiver.utils.logger
import no.nav.syfo.integration.kafka.PollForUtsattOppgaveVarslingsmeldingJob
import no.nav.syfo.integration.kafka.journalpost.JournalpostHendelseConsumer
import no.nav.syfo.koin.selectModuleBasedOnProfile
import no.nav.syfo.prosesser.FinnAlleUtgaandeOppgaverProcessor
import no.nav.syfo.prosesser.FjernInntektsmeldingByBehandletProcessor
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import no.nav.syfo.util.logger
import no.nav.syfo.utsattoppgave.FeiletUtsattOppgaveMeldingProsessor
import no.nav.syfo.web.inntektsmeldingModule
import no.nav.syfo.web.nais.nais
import org.flywaydb.core.Flyway
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.concurrent.thread

class SpinnApplication(val port: Int = 8080) : KoinComponent {
    private val logger = this.logger()
    private var webserver: NettyApplicationEngine? = null
    private var appConfig: HoconApplicationConfig = HoconApplicationConfig(ConfigFactory.load())

    private val runtimeEnvironment = appConfig.getEnvironment()

    fun start() {
        if (runtimeEnvironment == AppEnv.PREPROD || runtimeEnvironment == AppEnv.PROD) {
            logger.info("Sover i 30s i påvente av SQL proxy sidecar")
            Thread.sleep(30000)
        }

        startKoin { modules(selectModuleBasedOnProfile(appConfig)) }
        migrateDatabase()
        configAndStartBackgroundWorkers()
        startKafkaConsumer()
        autoDetectProbeableComponents()
        configAndStartWebserver()
    }

    private fun startKafkaConsumer() {
        logger.info("Starter lytting for utsatt oppgave...")
        val utsattOppgavePoll = PollForUtsattOppgaveVarslingsmeldingJob(get(), get(), get(), get())
        utsattOppgavePoll.startAsync(retryOnFail = true)
        logger.info("Starter lytting for journalpost hendelser...")
        val journalpostHendelseConsumer = get<JournalpostHendelseConsumer>()
        thread(start = true) {
            journalpostHendelseConsumer.start()
        }
    }

    fun shutdown() {
        webserver?.stop(1000, 1000)
        get<BakgrunnsjobbService>().stop()
        stopKoin()
    }

    private fun configAndStartWebserver() {
        webserver = embeddedServer(
            Netty,
            applicationEngineEnvironment {
                config = appConfig
                connector {
                    port = this@SpinnApplication.port
                }

                module {
                    nais()
                    inntektsmeldingModule(config)
                }
            }
        )

        webserver!!.start(wait = false)
    }

    private fun configAndStartBackgroundWorkers() {
        if (appConfig.getString("run_background_workers") == "true") {
            get<FinnAlleUtgaandeOppgaverProcessor>().startAsync(true)

            get<BakgrunnsjobbService>().apply {

                registrer(get<FeiletUtsattOppgaveMeldingProsessor>())
                registrer(get<FjernInntektsmeldingByBehandletProcessor>())
                registrer(get<JoarkInntektsmeldingHendelseProsessor>())

                startAsync(true)
            }
        }
    }

    private fun migrateDatabase() {
        logger.info("Starter databasemigrering")

        Flyway.configure().baselineOnMigrate(true)
            .dataSource(GlobalContext.getKoinApplicationOrNull()?.koin?.get())
            .load()
            .migrate()

        logger.info("Databasemigrering slutt")
    }

    private fun autoDetectProbeableComponents() {
        logger.info("Starter registrering av helsesjekker...")
        val kubernetesProbeManager = get<KubernetesProbeManager>()

        getKoin().getAll<LivenessComponent>()
            .forEach {
                logger.info("Alive helsesjekk for: $it")
                kubernetesProbeManager.registerLivenessComponent(it)
            }

        getKoin().getAll<ReadynessComponent>()
            .forEach {
                logger.info("Ready helsesjekk for: $it")
                kubernetesProbeManager.registerReadynessComponent(it)
            }
        logger.info("Ferdig registrering av helsesjekker!")
    }
}

fun main() {
    val logger = logger("main")

    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        logger.error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }

    val application = SpinnApplication()
    application.start()

    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info("Fikk shutdown-signal, avslutter...")
            application.shutdown()
            logger.info("Avsluttet OK")
        }
    )
}
