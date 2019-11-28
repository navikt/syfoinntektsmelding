package no.nav.syfo

import no.nav.migrator.DataType
import no.nav.migrator.Database
import no.nav.migrator.Destination
import no.nav.migrator.Migrator
import no.nav.migrator.MigratorListener
import no.nav.migrator.Source

class CopyDatabase {

    val INNTEKTSMELDING = "INNTEKTSMELDING"
    val ARBEIDSGIVERPERIODE = "ARBEIDSGIVERPERIODE"

    fun copy(){
        val oracle: Database = Database
            .build()
            .url("jdbc:oracle:thin:@localhost:49161:XE")
            .driver("oracle.jdbc.driver.OracleDriver")
            .username("user")
            .password("secret")

        val postgres = Database
            .build()
            .url("jdbc:postgresql://localhost:5433/spinn")
            .driver("org.postgresql.Driver")
            .username("user")
            .password("secret")

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

        var maxInntektsmelding = 0
        var maxArbeidsgiverperioder = 0

        var percentInntektsmelding = 0
        var percentArbeidsgiverperioder = 0

        val migrator = Migrator()
        migrator.addListener(object : MigratorListener {
            override fun starting(table: String, max: Int) {
                println("Starting copying $max rows from $table")
                if (table == INNTEKTSMELDING){
                    maxInntektsmelding = max
                } else if (table == ARBEIDSGIVERPERIODE){
                    maxArbeidsgiverperioder = max
                }
            }

            override fun rowCopied(rowIndex: Int, table: String) {
                if (table == INNTEKTSMELDING){
                    val percent = rowIndex / maxInntektsmelding
                    if (percent > percentInntektsmelding){
                        println("Copied $percent% from $table ($rowIndex / $maxInntektsmelding)")
                        percentInntektsmelding = percent
                    }
                } else if (table == ARBEIDSGIVERPERIODE){
                    val percent = rowIndex / maxArbeidsgiverperioder
                    if (percent > percentArbeidsgiverperioder){
                        println("Copied $percent% from $table ($rowIndex / $maxArbeidsgiverperioder)")
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
