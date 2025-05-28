package no.nav.syfo.repository

import com.zaxxer.hikari.HikariDataSource
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.EndringIRefusjon
import no.nav.syfo.domain.inntektsmelding.GjenopptakelseNaturalytelse
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Naturalytelse
import no.nav.syfo.domain.inntektsmelding.OpphoerAvNaturalytelse
import no.nav.syfo.domain.inntektsmelding.Refusjon
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.slowtests.SystemTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

open class InntektsmeldingRepositorySpec : SystemTestBase() {
    lateinit var repository: InntektsmeldingRepository

    @BeforeAll
    internal fun setUp() {
        val ds = HikariDataSource(createTestHikariConfig())
        repository = InntektsmeldingRepositoryImp(ds)
        repository.deleteAll()
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll()
    }

    @Test
    fun findByJournalpostId() {
        val inntektsmelding =
            InntektsmeldingEntitet(
                uuid = UUID.randomUUID().toString(),
                journalpostId = "jp-123-987",
                behandlet = LocalDateTime.of(2019, 10, 1, 5, 18, 45, 0),
                orgnummer = "orgnummer",
                arbeidsgiverPrivat = "arbeidsgiverPrivat",
                fnr = Fnr("28014026691"),
                aktorId = "aktorId1",
            )
        repository.lagreInnteksmelding(inntektsmelding)
        assertNotNull(repository.findByJournalpost("jp-123-987"))
    }

    @Test
    fun findByAktorId() {
        val behandlet = LocalDateTime.of(2019, 10, 1, 5, 18, 45, 0)
        val inntektsmelding =
            InntektsmeldingEntitet(
                uuid = UUID.randomUUID().toString(),
                journalpostId = "journalpostId",
                behandlet = behandlet,
                orgnummer = "orgnummer",
                arbeidsgiverPrivat = "arbeidsgiverPrivat",
                fnr = Fnr("28014026691"),
                aktorId = "aktorId1",
            )
        inntektsmelding.leggtilArbeidsgiverperiode(fom = LocalDate.of(2019, 10, 5), tom = LocalDate.of(2019, 10, 25))
        repository.lagreInnteksmelding(inntektsmelding)
        val inntektsmeldinger = repository.findByAktorId("aktorId1")
        assertThat(inntektsmeldinger.size).isEqualTo(1)
        val i = inntektsmeldinger[0]
        assertThat(i.uuid).isNotNull
        assertThat(i.journalpostId).isEqualTo("journalpostId")
        assertThat(i.orgnummer).isEqualTo("orgnummer")
        assertThat(i.arbeidsgiverPrivat).isEqualTo("arbeidsgiverPrivat")
        assertThat(i.aktorId).isEqualTo("aktorId1")
        assertThat(i.behandlet).isEqualTo(LocalDateTime.of(2019, 10, 1, 5, 18, 45, 0))
        assertThat(i.arbeidsgiverperioder.size).isEqualTo(1)
        //  assertThat(i.arbeidsgiverperioder[0].inntektsmelding).isEqualTo(i)
        assertThat(i.arbeidsgiverperioder[0].uuid).isNotNull
        assertThat(i.arbeidsgiverperioder[0].fom).isEqualTo(LocalDate.of(2019, 10, 5))
        assertThat(i.arbeidsgiverperioder[0].tom).isEqualTo(LocalDate.of(2019, 10, 25))
    }

    @Test
    fun lagre_flere_arbeidsgiverperioder() {
        val behandlet = LocalDateTime.of(2019, 10, 1, 5, 18, 45, 0)
        val inntektsmelding =
            InntektsmeldingEntitet(
                uuid = UUID.randomUUID().toString(),
                journalpostId = "journalpostId",
                behandlet = behandlet,
                orgnummer = "orgnummer",
                arbeidsgiverPrivat = "arbeidsgiverPrivat",
                fnr = Fnr("28014026691"),
                aktorId = "aktorId2",
            )
        inntektsmelding.leggtilArbeidsgiverperiode(fom = LocalDate.of(2019, 10, 5), tom = LocalDate.of(2019, 10, 25))
        inntektsmelding.leggtilArbeidsgiverperiode(fom = LocalDate.of(2018, 10, 5), tom = LocalDate.of(2018, 10, 25))
        repository.lagreInnteksmelding(inntektsmelding)
        val inntektsmeldinger = repository.findByAktorId("aktorId2")
        assertThat(inntektsmeldinger.size).isEqualTo(1)
        val i = inntektsmeldinger[0]
        assertThat(i.arbeidsgiverperioder.size).isEqualTo(2)
        // assertThat(i.arbeidsgiverperioder[0].inntektsmelding).isEqualTo(i)
        assertThat(i.arbeidsgiverperioder[0].uuid).isNotNull
        assertThat(i.arbeidsgiverperioder[0].fom).isEqualTo(LocalDate.of(2019, 10, 5))
        assertThat(i.arbeidsgiverperioder[0].tom).isEqualTo(LocalDate.of(2019, 10, 25))
    }

    @Test
    fun lagre_uten_arbeidsgiverperioder() {
        val behandlet = LocalDateTime.of(2019, 10, 1, 5, 18, 45, 0)
        val inntektsmelding =
            InntektsmeldingEntitet(
                uuid = UUID.randomUUID().toString(),
                journalpostId = "journalpostId",
                behandlet = behandlet,
                orgnummer = "orgnummer",
                arbeidsgiverPrivat = "arbeidsgiverPrivat",
                fnr = Fnr("28014026691"),
                aktorId = "aktorId3",
            )
        repository.lagreInnteksmelding(inntektsmelding)
        val inntektsmeldinger = repository.findByAktorId("aktorId3")
        val i = inntektsmeldinger[0]
        assertThat(inntektsmeldinger.size).isEqualTo(1)
        assertThat(i.arbeidsgiverperioder.size).isEqualTo(0)
    }

    @Test
    fun lagre_inntektsmelding_som_json() {
        val im =
            Inntektsmelding(
                id = "id-abc",
                fnr = "fnr-123",
                arbeidsgiverOrgnummer = "arb-org-123",
                arbeidsgiverPrivatFnr = "arb-priv-123",
                arbeidsgiverPrivatAktørId = "arb-priv-aktør-123",
                arbeidsforholdId = "arb-123",
                journalpostId = "jp-123",
                arsakTilInnsending = "Ingen årsak",
                journalStatus = JournalStatus.MOTTATT,
                arbeidsgiverperioder =
                    listOf(
                        Periode(fom = LocalDate.of(2011, 11, 1), tom = LocalDate.of(2012, 12, 2)),
                        Periode(fom = LocalDate.of(2013, 3, 3), tom = LocalDate.of(2014, 4, 4)),
                    ),
                beregnetInntekt = BigDecimal(999999999999),
                refusjon = Refusjon(BigDecimal(333333333333), LocalDate.of(2020, 2, 20)),
                endringerIRefusjon =
                    listOf(
                        EndringIRefusjon(LocalDate.of(2015, 5, 5), BigDecimal(555555555555)),
                        EndringIRefusjon(LocalDate.of(2016, 6, 6), BigDecimal(666666666666)),
                    ),
                opphørAvNaturalYtelse =
                    listOf(
                        OpphoerAvNaturalytelse(Naturalytelse.BIL, LocalDate.of(2015, 5, 5), BigDecimal(555555555555)),
                        OpphoerAvNaturalytelse(Naturalytelse.TILSKUDDBARNEHAGEPLASS, LocalDate.of(2016, 6, 6), BigDecimal(666666666666)),
                    ),
                gjenopptakelserNaturalYtelse =
                    listOf(
                        GjenopptakelseNaturalytelse(Naturalytelse.BOLIG, LocalDate.of(2011, 1, 1), BigDecimal(111111111111)),
                        GjenopptakelseNaturalytelse(Naturalytelse.KOSTDAGER, LocalDate.of(2012, 2, 2), BigDecimal(222222222222)),
                    ),
                gyldighetsStatus = Gyldighetsstatus.GYLDIG,
                arkivRefereranse = "ar-123",
                feriePerioder =
                    listOf(
                        Periode(fom = LocalDate.of(2017, 7, 7), tom = LocalDate.of(2018, 8, 8)),
                        Periode(fom = LocalDate.of(2019, 9, 9), tom = LocalDate.of(2020, 12, 20)),
                    ),
                førsteFraværsdag = LocalDate.of(2010, 2, 10),
                mottattDato = LocalDateTime.of(2010, 5, 4, 3, 2, 1),
                sakId = "sak-123",
                aktorId = "aktør-123",
                begrunnelseRedusert = "Grunn til reduksjon",
            )
        val mapper = JacksonJsonConfig.objectMapperFactory.opprettObjectMapper()
        val inntektsmelding =
            InntektsmeldingEntitet(
                uuid = UUID.randomUUID().toString(),
                journalpostId = "journalpostId",
                behandlet = LocalDateTime.now(),
                orgnummer = "orgnummer",
                arbeidsgiverPrivat = "arbeidsgiverPrivat",
                aktorId = "aktorId-repo-test",
                fnr = Fnr("28014026691"),
                data = mapper.writeValueAsString(im),
            )
        repository.lagreInnteksmelding(inntektsmelding)
        val inntektsmeldinger = repository.findByAktorId("aktorId-repo-test")
        val i = inntektsmeldinger[0]
        assertThat(inntektsmeldinger.size).isEqualTo(1)

        val data = mapper.readTree(i.data)

        assertThat(data.get("id").asText()).isEqualTo("id-abc")
        assertThat(data.get("fnr").asText()).isEqualTo("fnr-123")
        assertThat(data.get("aktorId").asText()).isEqualTo("aktør-123")
        assertThat(data.get("journalpostId").asText()).isEqualTo("jp-123")
        assertThat(data.get("refusjon").get("beloepPrMnd").asText()).isEqualTo("333333333333")
        assertThat(data.get("refusjon").get("opphoersdato").asText()).isEqualTo("2020-02-20")
        assertThat(data.get("begrunnelseRedusert").asText()).isEqualTo("Grunn til reduksjon")
        assertThat(data.get("sakId").asText()).isEqualTo("sak-123")
        assertThat(data.get("mottattDato").asText()).isEqualTo("2010-05-04T03:02:01")
        assertThat(data.get("arkivRefereranse").asText()).isEqualTo("ar-123")
        assertThat(data.get("førsteFraværsdag").asText()).isEqualTo("2010-02-10")
        assertThat(data.get("arsakTilInnsending").asText()).isEqualTo("Ingen årsak")
    }

    @Test
    fun `skal kun slette inntektsmeldinger eldre enn gitt dato`() {
        // Lagre 5 inntektsmeldinger i 2019
        val imMedPeriodeGammel = lagInntektsmelding(LocalDate.of(2019, 12, 31).atStartOfDay())
        imMedPeriodeGammel.leggtilArbeidsgiverperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1))
        repository.lagreInnteksmelding(imMedPeriodeGammel)
        repository.lagreInnteksmelding(lagInntektsmelding(LocalDate.of(2019, 12, 30).atStartOfDay()))
        repository.lagreInnteksmelding(lagInntektsmelding(LocalDate.of(2019, 11, 25).atStartOfDay()))
        repository.lagreInnteksmelding(lagInntektsmelding(LocalDate.of(2019, 10, 14).atStartOfDay()))
        repository.lagreInnteksmelding(lagInntektsmelding(LocalDate.of(2019, 3, 15).atStartOfDay()))

        // Lagre 5 inntektsmeldinger i 2020
        val imMedPeriodeNy = lagInntektsmelding(LocalDate.of(2020, 1, 1).atStartOfDay())
        imMedPeriodeNy.leggtilArbeidsgiverperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1))
        repository.lagreInnteksmelding(imMedPeriodeNy)
        repository.lagreInnteksmelding(lagInntektsmelding(LocalDate.of(2020, 1, 5).atStartOfDay()))
        repository.lagreInnteksmelding(lagInntektsmelding(LocalDate.of(2020, 2, 25).atStartOfDay()))
        repository.lagreInnteksmelding(lagInntektsmelding(LocalDate.of(2020, 4, 14).atStartOfDay()))
        repository.lagreInnteksmelding(lagInntektsmelding(LocalDate.of(2020, 5, 15).atStartOfDay()))

        // 10 tilsammen
        assertThat(repository.findAll().size).isEqualTo(10)
        // 5 før 2020
        assertThat(repository.findFirst100ByBehandletBefore(LocalDate.of(2020, 1, 1).atStartOfDay()).size).isEqualTo(5)

        // Slett alle før 2020
        repository.deleteByBehandletBefore(LocalDate.of(2020, 1, 1).atStartOfDay())
        // Nå skal det bare være 5 treff
        assertThat(repository.findAll().size).isEqualTo(5)
    }

    @Test
    fun `skal hente inntektsmeldinger for gitt fnr i periode`() {
        val im1 = lagInntektsmelding(LocalDate.of(2020, 1, 1).atStartOfDay())
        val fnr = "07025032327"
        im1.fnr = Fnr(fnr)
        repository.lagreInnteksmelding(im1)

        val im2 = lagInntektsmelding(LocalDate.of(2020, 2, 1).atStartOfDay())
        im2.fnr = Fnr(fnr)
        repository.lagreInnteksmelding(im2)

        val im3 = lagInntektsmelding(LocalDate.of(2020, 3, 1).atStartOfDay())
        im3.fnr = Fnr(fnr)
        repository.lagreInnteksmelding(im3)

        val inntektsmeldinger =
            repository.findByFnrInPeriod(fnr = fnr, fom = LocalDate.of(2020, 1, 15), tom = LocalDate.of(2020, 5, 15))

        assertThat(inntektsmeldinger.size).isEqualTo(2)
    }

    private fun lagInntektsmelding(behandlet: LocalDateTime): InntektsmeldingEntitet =
        InntektsmeldingEntitet(
            uuid = UUID.randomUUID().toString(),
            journalpostId = "journalpostId",
            behandlet = behandlet,
            orgnummer = "orgnummer",
            arbeidsgiverPrivat = "arbeidsgiverPrivat",
            fnr = Fnr("28014026691"),
            aktorId = "aktorId1",
        )
}
