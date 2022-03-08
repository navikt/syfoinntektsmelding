package no.nav.syfo.repository

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
    val antallInntektsmeldinger: Int
)

data class ArsakStats(
    val arsak: String,
    val antall: Int
)

data class IMWeeklyQualityStats(
    val weekNumber: Int,

    val total: Int,
    val ingen_arbeidsforhold_id: Int,
    val har_arbeidsforhold_id: Int,
    val en_periode: Int,
    val to_perioder: Int,
    val over_to_perioder: Int,
    val riktig_ff: Int,
    val feil_ff: Int,
    val ingen_fravaer: Int,
    val ingen_fravaer_med_refusjon: Int
)

data class ForsinkelseStats(
    val antall_med_forsinkelsen_altinn: Int,
    val antall_med_forsinkelsen_lps: Int,
    val dager_etter_ff: Int
)

data class OppgaveStats(
    val antall_forkastet: Int,
    val antall_utsatt: Int,
    val antall_opprettet: Int,
    val antall_opprettet_timeout: Int,
    val dato: String
)

data class ForsinkelseWeeklyStats(
    val antall_med_forsinkelsen_altinn: Int,
    val antall_med_forsinkelsen_lps: Int,
    val bucket: Int,
    val uke: Int,
    val year: Int
)

interface IMStatsRepo {
    fun getWeeklyStats(): List<IMWeeklyStats>
    fun getLPSStats(): List<LPSStats>
    fun getArsakStats(): List<ArsakStats>
    fun getWeeklyQualityStats(): List<IMWeeklyQualityStats>
    fun getFeilFFPerLPS(): List<LPSStats>
    fun getIngenFravaerPerLPS(): List<LPSStats>
    fun getBackToBackPerLPS(): List<LPSStats>
    fun getForsinkelseStats(): List<ForsinkelseStats>
    fun getOppgaveStats(): List<OppgaveStats>
    fun getForsinkelseWeeklyStats(): List<ForsinkelseWeeklyStats>
}

/**
 * Implementasjon av https://confluence.adeo.no/display/PH/Inntektsmeldingen+-+datapakke
 */
class IMStatsRepoImpl(
    private val ds: DataSource
) : IMStatsRepo {

    override fun getWeeklyStats(): List<IMWeeklyStats> {
        val query = """
            SELECT
                extract('week' from date_trunc('week',behandlet)) as uke,
                extract('year' from date_trunc('week',behandlet)) as year,
                count(*) as total,
                count(*) filter (where data -> 'avsenderSystem' ->> 'navn' = 'AltinnPortal') as fra_altinn, -- O1A
                count(*) filter (where data -> 'avsenderSystem' ->> 'navn' != 'AltinnPortal') as fra_lps, -- O1B
                count(*) filter (where data -> 'refusjon' ->> 'beloepPrMnd' = data ->> 'beregnetInntekt' and data ->> 'begrunnelseRedusert' != 'IkkeFravaer') as full_refusjon, -- 03A
                count(*) filter (where data -> 'refusjon' ->> 'beloepPrMnd' < data ->> 'beregnetInntekt' and data ->> 'begrunnelseRedusert' != 'IkkeFravaer') as delvis_refusjon, -- 03A
                count(*) filter (where ((data -> 'refusjon' -> 'beloepPrMnd') is null or data -> 'refusjon' ->> 'beloepPrMnd' = '0') and data ->> 'begrunnelseRedusert' != 'IkkeFravaer') as ingen_refusjon, -- 03C
                count(*) filter (where data ->> 'begrunnelseRedusert' != 'IkkeFravaer') as med_fravaer, -- 04A
                count(*) filter (where data ->> 'begrunnelseRedusert' = 'IkkeFravaer') as ikke_fravaer, -- 04B
                count(*) filter (where data ->> 'arsakTilInnsending' = 'Ny') as arsak_ny, -- 05A
                count(*) filter (where data ->> 'arsakTilInnsending' = 'Endring') as arsak_endring -- 05B
            from inntektsmelding
            group by  extract('year' from date_trunc('week',behandlet)), extract('week' from date_trunc('week',behandlet))
            order by  extract('year' from date_trunc('week',behandlet)), extract('week' from date_trunc('week',behandlet));
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
            	behandlet > NOW()::DATE - INTERVAL '6 DAYS'
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
                        res.getInt("antall_im")
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
                behandlet > NOW()::DATE - INTERVAL '6 DAYS'
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
                        res.getInt("antall_im")
                    )
                )
            }

            return returnValue
        }
    }

    override fun getWeeklyQualityStats(): List<IMWeeklyQualityStats> {
        val query = """
            select
                extract('week' from date_trunc('week',behandlet)) as uke,
                extract('year' from date_trunc('week',behandlet)) as year,
                count(*) as total,
                count(*) filter (where (data ->> 'arbeidsforholdId') is null or (data ->> 'arbeidsforholdId') = '') as ingen_arbeidsforhold_id, -- K1A
                    count(*) filter (where (data ->> 'arbeidsforholdId') is not null and (data ->> 'arbeidsforholdId') != '') as har_arbeidsforhold_id, -- K1B
                    count(*) filter (where JSONB_ARRAY_LENGTH(data -> 'arbeidsgiverperioder') = 1 and data ->> 'begrunnelseRedusert' != 'IkkeFravaer') as en_periode, -- K2A
                    count(*) filter (where JSONB_ARRAY_LENGTH(data -> 'arbeidsgiverperioder') = 2 and data ->> 'begrunnelseRedusert' != 'IkkeFravaer') as to_perioder, -- K2B
                    count(*) filter (where JSONB_ARRAY_LENGTH(data -> 'arbeidsgiverperioder') > 2 and data ->> 'begrunnelseRedusert' != 'IkkeFravaer') as over_to_perioder, -- K2C
                    count(*) filter (
                    where (
                        date(data ->> 'førsteFraværsdag') - date((data -> 'arbeidsgiverperioder' ->> -1)::jsonb ->> 'tom') < 2 and
                        data ->> 'begrunnelseRedusert' != 'IkkeFravaer' and
                        date(data ->> 'førsteFraværsdag') = date((data -> 'arbeidsgiverperioder' ->> -1)::jsonb ->> 'fom')
                      )
                    ) as riktig_ff,-- K4A
                    count(*) filter (
                    where (
                        date(data ->> 'førsteFraværsdag') - date((data -> 'arbeidsgiverperioder' ->> -1)::jsonb ->> 'tom') < 2 and
                        data ->> 'begrunnelseRedusert' != 'IkkeFravaer' and
                        date(data ->> 'førsteFraværsdag') != date((data -> 'arbeidsgiverperioder' ->> -1)::jsonb ->> 'fom')
                      )
                    ) as feil_ff,-- K4B
                    count(*) filter (where (data -> 'refusjon' ->> 'beloepPrMnd')::numeric = 0 and data ->> 'begrunnelseRedusert' = 'IkkeFravaer') as ingen_fravaer, -- K6A
                    count(*) filter (where (data -> 'refusjon' ->> 'beloepPrMnd')::numeric > 0 and data ->> 'begrunnelseRedusert' = 'IkkeFravaer') as ingen_fravaer_med_refusjon -- K6B
            from inntektsmelding
            group by  extract('year' from date_trunc('week',behandlet)), extract('week' from date_trunc('week',behandlet))
            order by  extract('year' from date_trunc('week',behandlet)), extract('week' from date_trunc('week',behandlet));
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<IMWeeklyQualityStats>()
            while (res.next()) {
                returnValue.add(
                    IMWeeklyQualityStats(
                        res.getInt("uke"),
                        res.getInt("total"),
                        res.getInt("ingen_arbeidsforhold_id"),
                        res.getInt("har_arbeidsforhold_id"),
                        res.getInt("en_periode"),
                        res.getInt("to_perioder"),
                        res.getInt("over_to_perioder"),
                        res.getInt("riktig_ff"),
                        res.getInt("feil_ff"),
                        res.getInt("ingen_fravaer"),
                        res.getInt("ingen_fravaer_med_refusjon"),
                    )
                )
            }

            return returnValue
        }
    }

    override fun getFeilFFPerLPS(): List<LPSStats> {
        val query = """
            select
                count(*) as antall_im,  -- K4C
                count(data -> 'avsenderSystem' ->> 'versjon') as antall_versjoner,
                data -> 'avsenderSystem' ->> 'navn'  as lps_navn
            from inntektsmelding i
            where (
                behandlet > NOW()::DATE - INTERVAL '6 DAYS' and
                date(data ->> 'førsteFraværsdag') - date((data -> 'arbeidsgiverperioder' ->> -1)::jsonb ->> 'tom') < 2 and
                data ->> 'begrunnelseRedusert' != 'IkkeFravaer' and
                date(data ->> 'førsteFraværsdag') != date((data -> 'arbeidsgiverperioder' ->> -1)::jsonb ->> 'fom')
                )
            group by data -> 'avsenderSystem' ->> 'navn';
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<LPSStats>()
            while (res.next()) {
                returnValue.add(
                    LPSStats(
                        res.getString("lps_navn"),
                        res.getInt("antall_versjoner"),
                        res.getInt("antall_im")
                    )
                )
            }

            return returnValue
        }
    }

    override fun getIngenFravaerPerLPS(): List<LPSStats> {
        val query = """
            select
                count(*) as antall_im,  -- K6C
                count(data -> 'avsenderSystem' ->> 'versjon') as antall_versjoner,
                data -> 'avsenderSystem' ->> 'navn'  as lps_navn
            from inntektsmelding i
            where (
                  behandlet > NOW()::DATE - INTERVAL '27 DAYS'
              and
                  (data -> 'refusjon' ->> 'beloepPrMnd')::numeric > 0
              and
                  data ->> 'begrunnelseRedusert' = 'IkkeFravaer'
          )
            group by data -> 'avsenderSystem' ->> 'navn';
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<LPSStats>()
            while (res.next()) {
                returnValue.add(
                    LPSStats(
                        res.getString("lps_navn"),
                        res.getInt("antall_versjoner"),
                        res.getInt("antall_im")
                    )
                )
            }

            return returnValue
        }
    }

    override fun getBackToBackPerLPS(): List<LPSStats> {
        val query = """
            select
                   count(*) as antall_im,
                   count(data -> 'avsenderSystem' ->> 'versjon') as antall_versjoner,
                   data -> 'avsenderSystem' ->> 'navn' as lps_navn
            from inntektsmelding i
            where
                    JSONB_ARRAY_LENGTH(data -> 'arbeidsgiverperioder') = 2 and
                    (date(data -> 'arbeidsgiverperioder' -> 0 ->> 'tom') - date(data -> 'arbeidsgiverperioder' -> 1 ->> 'fom')) = -1 and
                                data -> 'arbeidsgiverperioder' -> 1 ->> 'fom'  = data ->> 'førsteFraværsdag'
            group by DATA -> 'avsenderSystem' ->> 'navn';
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<LPSStats>()
            while (res.next()) {
                returnValue.add(
                    LPSStats(
                        res.getString("lps_navn"),
                        res.getInt("antall_versjoner"),
                        res.getInt("antall_im")
                    )
                )
            }

            return returnValue
        }
    }

    override fun getForsinkelseStats(): List<ForsinkelseStats> {

        val query = """
            select
                count(*) filter (where data -> 'avsenderSystem' ->> 'navn' = 'AltinnPortal') as antall_med_forsinkelsen_altinn, -- K3A
                count(*) filter (where data -> 'avsenderSystem' ->> 'navn' != 'AltinnPortal') as antall_med_forsinkelsen_lps, --K3B
                DATE_PART('day', behandlet - DATE(data ->> 'førsteFraværsdag')) as dager_etter_ff
            from
                inntektsmelding i2
            where
                    data ->> 'førsteFraværsdag' = data -> 'arbeidsgiverperioder' -> 0 ->> 'fom' and
                    (date(data -> 'arbeidsgiverperioder' -> 0 ->> 'tom') - date(data -> 'arbeidsgiverperioder' -> 0 ->> 'fom')) = 15 and
                    behandlet > NOW()::DATE - INTERVAL '89 DAYS'
            GROUP BY
                DATE_PART('day', behandlet - DATE(data ->> 'førsteFraværsdag'));
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<ForsinkelseStats>()
            while (res.next()) {
                returnValue.add(
                    ForsinkelseStats(
                        res.getInt("antall_med_forsinkelsen_altinn"),
                        res.getInt("antall_med_forsinkelsen_lps"),
                        res.getInt("dager_etter_ff")
                    )
                )
            }

            return returnValue
        }
    }

    override fun getOppgaveStats(): List<OppgaveStats> {
        val query = """
            select
                count(*) filter ( where tilstand = 'Forkastet') as antall_forkastet,
                count(*) filter ( where tilstand = 'Utsatt') as antall_utsatt,
                count(*) filter ( where tilstand = 'Opprettet') as antall_opprettet,
                count(*) filter ( where tilstand = 'OpprettetTimeout') as antall_opprettet_timeout,
                Date(oppdatert) as dato
            from utsatt_oppgave
            where oppdatert > NOW()::DATE - INTERVAL '29 DAYS'
            group by Date(oppdatert)
            order by Date(oppdatert);
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<OppgaveStats>()
            while (res.next()) {
                returnValue.add(
                    OppgaveStats(
                        res.getInt("antall_forkastet"),
                        res.getInt("antall_utsatt"),
                        res.getInt("antall_opprettet"),
                        res.getInt("antall_opprettet_timeout"),
                        res.getDate("dato").toString()
                    )
                )
            }

            return returnValue
        }
    }

    override fun getForsinkelseWeeklyStats(): List<ForsinkelseWeeklyStats> {

        val query = """
            select
                count(*) filter (where data -> 'avsenderSystem' ->> 'navn' = 'AltinnPortal') as antall_med_forsinkelsen_altinn, -- K3A
                count(*) filter (where data -> 'avsenderSystem' ->> 'navn' != 'AltinnPortal') as antall_med_forsinkelsen_lps, --K3B
                extract('week' from date_trunc('week',behandlet)) as uke,
                extract('year' from date_trunc('week',behandlet)) as year,
                width_bucket(DATE_PART('day', behandlet - DATE(data ->> 'førsteFraværsdag'))::int, array[15, 30, 90]) as bucket
            from
                inntektsmelding
            where
                        data ->> 'førsteFraværsdag' = data -> 'arbeidsgiverperioder' -> 0 ->> 'fom' and
                    (date(data -> 'arbeidsgiverperioder' -> 0 ->> 'tom') - date(data -> 'arbeidsgiverperioder' -> 0 ->> 'fom')) = 15 and
                    behandlet > NOW()::DATE - INTERVAL '89 DAYS'
            GROUP BY
                width_bucket(DATE_PART('day', behandlet - DATE(data ->> 'førsteFraværsdag'))::int, array[15, 30, 90]),
                extract('year' from date_trunc('week',behandlet)),
                extract('week' from date_trunc('week',behandlet));
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<ForsinkelseWeeklyStats>()
            while (res.next()) {
                returnValue.add(
                    ForsinkelseWeeklyStats(
                        res.getInt("antall_med_forsinkelsen_altinn"),
                        res.getInt("antall_med_forsinkelsen_lps"),
                        res.getInt("bucket"),
                        res.getInt("uke"),
                        res.getInt("year"),
                    )
                )
            }
            return returnValue
        }
    }
}
