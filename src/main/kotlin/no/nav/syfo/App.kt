package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.syfo.integration.kafka.UtsattOppgaveConsumer
import no.nav.syfo.integration.kafka.journalpost.JournalpostHendelseConsumer
import no.nav.syfo.koin.selectModuleBasedOnProfile
import no.nav.syfo.prosesser.FinnAlleUtgaandeOppgaverProcessor
import no.nav.syfo.prosesser.FjernInntektsmeldingByBehandletProcessor
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import no.nav.syfo.simba.InntektsmeldingConsumer
import no.nav.syfo.util.KubernetesProbeManager
import no.nav.syfo.util.getString
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

class SpinnApplication(
    val port: Int = 8080,
) : KoinComponent {
    private val logger = logger()
    private val appConfig = HoconApplicationConfig(ConfigFactory.load())
    private val runtimeEnvironment = appConfig.getString("koin.profile")

    private val webserver: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    init {
        logger.info("Environment: $runtimeEnvironment")
        if (runtimeEnvironment != "LOCAL" && runtimeEnvironment != "TEST") {
            logger.info("Sover i 30s i påvente av SQL proxy sidecar")
            Thread.sleep(30000)
        }
        startKoin { modules(selectModuleBasedOnProfile(appConfig)) }
        migrateDatabase()
        configAndStartBackgroundWorkers()
        startKafkaConsumer()

        webserver =
            createWebserver().also {
                it.start(wait = false)
            }
    }

    private fun startKafkaConsumer() {
        logger.info("Starter lytting for UtsattOppgave hendelser...")
        val utsattOppgaveConsumer = get<UtsattOppgaveConsumer>()
        thread(start = true) {
            utsattOppgaveConsumer.start()
        }
        logger.info("Starter lytting for journalpost hendelser...")
        val journalpostHendelseConsumer = get<JournalpostHendelseConsumer>()
        thread(start = true) {
            journalpostHendelseConsumer.start()
        }
        logger.info("Starter lytting for mottak fra simba...")
        val inntektsmeldingConsumer = get<InntektsmeldingConsumer>()
        logger.info("InntektsmeldingConsumer = $inntektsmeldingConsumer")
        thread(start = true) {
            logger.info("Starter inntektsmeldingConsumer-tråd")
            inntektsmeldingConsumer.start()
        }
        logger.info("Registrerer helsesjekker for kafka konsumentene...")
        val kubernetesProbeManager = get<KubernetesProbeManager>()
        val list = listOf(utsattOppgaveConsumer, journalpostHendelseConsumer, inntektsmeldingConsumer)
        list.forEach {
            logger.info("Registrerer helsesjekker for ${it.javaClass}")
            kubernetesProbeManager.registerLivenessComponent(it)
            kubernetesProbeManager.registerReadynessComponent(it)
        }
    }

    fun shutdown() {
        get<FinnAlleUtgaandeOppgaverProcessor>().stop()
        val service = get<BakgrunnsjobbService>()
        service.stop()
        // Et forsøk på at bakgrunnsjobber skal kunne ferdigstilles OK, før stopp. Ingen garantier..
        logger.info("Venter ${service.delayMillis} ms på at bakgrunnsjobbService stoppes!")
        Thread.sleep(service.delayMillis)
        webserver.stop(1000, 1000)
        stopKoin()
    }

    private fun createWebserver(): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> =
        embeddedServer(
            factory = Netty,
            environment =
                applicationEnvironment {
                    config = appConfig
                },
            configure = {
                connector {
                    port = this@SpinnApplication.port
                }
            },
            module = {
                nais()
                inntektsmeldingModule(appConfig)
            },
        )

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

        Flyway
            .configure()
            .baselineOnMigrate(true)
            .dataSource(GlobalContext.getKoinApplicationOrNull()?.koin?.get())
            .load()
            .migrate()

        logger.info("Databasemigrering slutt")
    }
}

fun main() {
    val logger = "main".logger()
    val sikkerlogger = sikkerLogger()

    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        "Uncaught exception in thread ${thread.name}: ${err.message}".also {
            logger.error(it)
            sikkerlogger.error(it, err)
        }
    }

    val application = SpinnApplication()

    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info("Fikk shutdown-signal, avslutter...")
            application.shutdown()
            logger.info("Avsluttet OK")
        },
    )
}
