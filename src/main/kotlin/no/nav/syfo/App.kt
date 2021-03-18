package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.helse.arbeidsgiver.system.AppEnv
import no.nav.helse.arbeidsgiver.system.getEnvironment
import no.nav.helse.fritakagp.koin.getAllOfType
import no.nav.helse.fritakagp.koin.selectModuleBasedOnProfile
import org.flywaydb.core.Flyway
import org.koin.core.KoinComponent
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get
import org.slf4j.LoggerFactory

class SpinnApplication(val port: Int = 8080) : KoinComponent {
    private val logger = LoggerFactory.getLogger(SpinnApplication::class.simpleName)
    //private var webserver: NettyApplicationEngine? = null
    private var appConfig: HoconApplicationConfig = HoconApplicationConfig(ConfigFactory.load())
    private val runtimeEnvironment = appConfig.getEnvironment()

    @KtorExperimentalAPI
    fun start() {
        if (runtimeEnvironment == AppEnv.PREPROD || runtimeEnvironment == AppEnv.PROD) {
            logger.info("Sover i 30s i påvente av SQL proxy sidecar")
            Thread.sleep(30000)
        }

        startKoin { modules(selectModuleBasedOnProfile(appConfig)) }
        migrateDatabase()

        configAndStartBackgroundWorker()
        autoDetectProbeableComponents()
        configAndStartWebserver()
    }

    fun shutdown() {
        //webserver?.stop(1000, 1000)
        get<BakgrunnsjobbService>().stop()
        stopKoin()
    }

    private fun configAndStartWebserver() {
      /*  webserver = embeddedServer(Netty, applicationEngineEnvironment {
            config = appConfig
            connector {
                port = this@FritakAgpApplication.port
            }

            module {
                if (runtimeEnvironment != AppEnv.PROD) {
                    localCookieDispenser(config)
                }

                nais()
                fritakModule(config)
            }
        })

        webserver!!.start(wait = false)*/
    }

    private fun configAndStartBackgroundWorker() {
      /*  if (appConfig.getString("run_background_workers") == "true") {
            get<BakgrunnsjobbService>().apply {
                registrer(get<GravidSoeknadProcessor>())
                registrer(get<GravidSoeknadKafkaProcessor>())
                registrer(get<GravidSoeknadKvitteringProcessor>())

                registrer(get<GravidKravProcessor>())
                registrer(get<GravidKravKafkaProcessor>())
                registrer(get<GravidKravKvitteringProcessor>())

                registrer(get<KroniskSoeknadProcessor>())
                registrer(get<KroniskSoeknadKafkaProcessor>())
                registrer(get<KroniskSoeknadKvitteringProcessor>())

                registrer(get<KroniskKravProcessor>())
                registrer(get<KroniskKravKafkaProcessor>())
                registrer(get<KroniskKravKvitteringProcessor>())

                registrer(get<BrukernotifikasjonProcessor>())

                startAsync(true)
            }
        }*/
    }

    private fun migrateDatabase() {
        logger.info("Starter databasemigrering")

        Flyway.configure().baselineOnMigrate(true)
            .dataSource(GlobalContext.get().koin.get())
            .load()
            .migrate()

        logger.info("Databasemigrering slutt")
    }

    private fun autoDetectProbeableComponents() {
        val kubernetesProbeManager = get<KubernetesProbeManager>()

        getKoin().getAllOfType<LivenessComponent>()
            .forEach { kubernetesProbeManager.registerLivenessComponent(it) }

        getKoin().getAllOfType<ReadynessComponent>()
            .forEach { kubernetesProbeManager.registerReadynessComponent(it) }

        logger.debug("La til probeable komponenter")
    }
}


@KtorExperimentalAPI
fun main() {
    val logger = LoggerFactory.getLogger("main")

    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        logger.error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }

    val application = SpinnApplication()
    application.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Fikk shutdown-signal, avslutter...")
        application.shutdown()
        logger.info("Avsluttet OK")
    })
}

