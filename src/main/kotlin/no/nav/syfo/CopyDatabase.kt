package no.nav.syfo

import no.nav.migrator.DataType
import no.nav.migrator.Database
import no.nav.migrator.Destination
import no.nav.migrator.Migrator
import no.nav.migrator.MigratorListener
import no.nav.migrator.Source
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class CopyDatabase (
    @Value("gammel.spring.datasource")
    val oracleUrl : String,
    @Value("gammel.spring.datasource.username")
    val oracleUser: String,
    @Value("gammel.spring.datasource.password")
    val oraclePassword: String
){

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    val INNTEKTSMELDING = "INNTEKTSMELDING"
    val ARBEIDSGIVERPERIODE = "ARBEIDSGIVERPERIODE"

    fun copy(){
        val oracle: Database = Database
            .build()
            .url(oracleUrl)
            .driver("oracle.jdbc.driver.OracleDriver")
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
                println("Starting copying $max rows from $table")
            }

            override fun rowCopied(rowIndex: Int, max: Int, table: String) {
                if (table == INNTEKTSMELDING){
                    val percent = rowIndex / max
                    if (percent > percentInntektsmelding){
                        println("Copied $percent% from $table ($rowIndex / $max)")
                        percentInntektsmelding = percent
                    }
                } else if (table == ARBEIDSGIVERPERIODE){
                    val percent = rowIndex / max
                    if (percent > percentArbeidsgiverperioder){
                        println("Copied $percent% from $table ($rowIndex / $max)")
                        percentArbeidsgiverperioder = percent
                    }
                }
            }

            override fun finished(table: String) {
                println("Finished copying from $table")
            }

            override fun failed(table: String, e: Exception) {
                println("Failed copying from $table")
            }
        })
        migrator.copyTable(sourceInntektsmelding, destinationInntektsmelding)
        migrator.copyTable(sourceArbeidsgiverperiode, destinationArbeidsgiverperiode)
    }

}
