package no.nav.syfo

import log
import no.nav.migrator.DataType
import no.nav.migrator.Database
import no.nav.migrator.Destination
import no.nav.migrator.Migrator
import no.nav.migrator.MigratorListener
import no.nav.migrator.Source
import no.nav.syfo.util.Metrikk
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class CopyDatabase (
    private val metrikk: Metrikk
) {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    val INNTEKTSMELDING = "INNTEKTSMELDING"
    val ARBEIDSGIVERPERIODE = "ARBEIDSGIVERPERIODE"

    @Value("\${spring.gammel.datasource.url}")
    lateinit var oracleUrl : String
    @Value("\${spring.gammel.datasource.username}")
    lateinit var oracleUser: String
    @Value("\${spring.gammel.datasource.password}")
    lateinit var oraclePassword: String

    private val log = log()

    @EventListener(ApplicationReadyEvent::class)
    fun copy(){
        val oracle: Database = Database
            .build()
            .url(oracleUrl)
            .driver("oracle.jdbc.OracleDriver")
            .username(oracleUser)
            .password(oraclePassword)

        val postgres = Database
            .build()
            .dataSource(jdbcTemplate.dataSource)

        val sourceInntektsmelding: Source = Source.build()
            .table(INNTEKTSMELDING)
            .column(DataType.String)
            .column(DataType.String)
            .column(DataType.String)
            .column(DataType.String)
            .column(DataType.String)
            .column(DataType.Timestamp)
            .column(DataType.String)
            .database(oracle)

        val destinationInntektsmelding: Destination = Destination.build()
            .table(INNTEKTSMELDING)
            .database(postgres)

        val sourceArbeidsgiverperiode: Source = Source.build()
            .table(ARBEIDSGIVERPERIODE)
            .column(DataType.String)
            .column(DataType.String)
            .column(DataType.Date)
            .column(DataType.Date)
            .database(oracle)

        val destinationArbeidsgiverperiode: Destination = Destination.build()
            .table(ARBEIDSGIVERPERIODE)
            .database(postgres)

        var percentInntektsmelding = 0
        var percentArbeidsgiverperioder = 0

        val migrator = Migrator()
        migrator.addListener(object : MigratorListener {
            override fun starting(table: String, max: Int) {
                log.info("Starting copying $max rows from $table")
            }

            override fun rowCopied(rowIndex: Int, max: Int, table: String) {
                if (table == INNTEKTSMELDING){
                    val percent = rowIndex * 100 / max
                    if (percent > percentInntektsmelding){
                        log.info("Copied $percent% from $table ($rowIndex / $max)")
                        percentInntektsmelding = percent
                        metrikk.tellMigreringInntektsmelding()
                    }
                } else if (table == ARBEIDSGIVERPERIODE){
                    val percent = rowIndex * 100 / max
                    if (percent > percentArbeidsgiverperioder){
                        log.info("Copied $percent% from $table ($rowIndex / $max)")
                        percentArbeidsgiverperioder = percent
                        metrikk.tellMigreringArbeidsgiverperioder()
                    }
                }
            }

            override fun finished(table: String) {
                log.info("Finished copying from $table")
            }

            override fun failed(table: String, e: Exception) {
                log.info("Failed copying from $table")
            }
        })
        // Legger til en kommentar for å få deployet
//        migrator.copyTable(sourceInntektsmelding, destinationInntektsmelding)
        migrator.copyTable(sourceArbeidsgiverperiode, destinationArbeidsgiverperiode)
    }

}
