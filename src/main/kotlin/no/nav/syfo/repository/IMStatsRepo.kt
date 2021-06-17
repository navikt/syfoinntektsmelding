package no.nav.syfo.repository

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

data class IMWeeklyStats(
    val weekNumber: Int,

    val total: Int,
    val fraAltinnPortal: Int,
    val fraLPS: Int,

    val fullRefusjon: Int,
    val delvisRefusjon: Int,
    val ingenRefusjon: Int,

    val fravaer: Int,
    val ikkeFravaer: Int,

    val arsakNy: Int,
    val arsakEndring: Int
)

data class LPSStats(
    val lpsNavn: String,
    val antallVersjoner: Int,
    val antallInntektsmeldinger: Int,
    val fraDato: LocalDateTime
)

data class ArsakStats(
    val arsak: String,
    val antall: Int,
    val fraDato: LocalDateTime
)


interface IMStatsRepo {
    fun getWeeklyStats(): List<IMWeeklyStats>
    fun getLPSStats(): List<LPSStats>
    fun getArsakStats(): List<ArsakStats>
}

/**
 * Implementasjon av https://confluence.adeo.no/display/PH/Inntektsmeldingen+-+datapakke
 */
class IMStatsRepoImpl (
    private val ds: DataSource
) : IMStatsRepo {

    val postgresStringFormat = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss")

    override fun getWeeklyStats(): List<IMWeeklyStats> {
        val query = """
            SELECT
            	extract('week' from behandlet) as uke,
                count(*) as total,
                count(*) filter (where data -> 'avsenderSystem' ->> 'navn' = 'AltinnPortal') as fra_altinn, -- O1A
                count(*) filter (where data -> 'avsenderSystem' ->> 'navn' != 'AltinnPortal') as fra_lps, -- O1B
                count(*) filter (where data -> 'refusjon' ->> 'beloepPrMnd' = data ->> 'beregnetInntekt' and data ->> 'begrunnelseRedusert' != 'IkkeFravaer') as full_refusjon, -- 03A
                count(*) filter (where data -> 'refusjon' ->> 'beloepPrMnd' < data ->> 'beregnetInntekt' and data ->> 'begrunnelseRedusert' != 'IkkeFravaer') as delvis_refusjon, -- 03A
                count(*) filter (where (data -> 'refusjon' -> 'beloepPrMnd') is null and data ->> 'begrunnelseRedusert' != 'IkkeFravaer') as ingen_refusjon, -- 03C
                count(*) filter (where data ->> 'begrunnelseRedusert' != 'IkkeFravaer') as med_fravaer, -- 04A
                count(*) filter (where data ->> 'begrunnelseRedusert' = 'IkkeFravaer') as ikke_fravaer, -- 04B
                count(*) filter (where data ->> 'arsakTilInnsending' = 'Ny') as arsak_ny, -- 05A
                count(*) filter (where data ->> 'arsakTilInnsending' = 'Endring') as arsak_endring -- 05B
            from inntektsmelding
            group by extract('week' from behandlet);
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<IMWeeklyStats>()
            while (res.next()) {
                returnValue.add(
                    IMWeeklyStats(
                        res.getInt("uke"),
                        res.getInt("total"),
                        res.getInt("fra_altinn"),
                        res.getInt("fra_lps"),
                        res.getInt("full_refusjon"),
                        res.getInt("delvis_refusjon"),
                        res.getInt("ingen_refusjon"),
                        res.getInt("med_fravaer"),
                        res.getInt("ikke_fravaer"),
                        res.getInt("arsak_ny"),
                        res.getInt("arsak_endring"),
                    )
                )
            }

            return returnValue
        }
    }

    override fun getLPSStats(): List<LPSStats> {
        val query = """
            select
            	min(behandlet) as fra_dato,
            	count(*) as antall_im,  -- 02A
            	count(distinct data -> 'avsenderSystem' ->> 'versjon') as antall_versjoner, --02B
            	data -> 'avsenderSystem' ->> 'navn'  as lps_navn
            from inntektsmelding i
            where
            	behandlet > NOW()::DATE-EXTRACT(DOW FROM NOW())::INTEGER-7
            group by
            	data -> 'avsenderSystem' ->> 'navn';
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<LPSStats>()
            while (res.next()) {
                returnValue.add(
                    LPSStats(
                        res.getString("lps_navn"),
                        res.getInt("antall_versjoner"),
                        res.getInt("antall_im"),
                        LocalDateTime.parse(res.getString("fra_dato"), postgresStringFormat)
                    )
                )
            }

            return returnValue
        }
    }

    override fun getArsakStats(): List<ArsakStats> {
        val query = """
            select
                min(behandlet) as fra_dato,
                count(*) as antall_im,  -- 06A + 06B
                data ->> 'begrunnelseRedusert' as begrunnelse
            from inntektsmelding i
            where
                behandlet > NOW()::DATE-EXTRACT(DOW FROM NOW())::INTEGER-7
            group by
                data ->> 'begrunnelseRedusert';
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<ArsakStats>()
            while (res.next()) {
                returnValue.add(
                    ArsakStats(
                        res.getString("begrunnelse"),
                        res.getInt("antall_im"),
                        LocalDateTime.parse(res.getString("fra_dato"), postgresStringFormat)
                    )
                )
            }

            return returnValue
        }
    }
}

