package no.nav.syfo.repository

import com.zaxxer.hikari.HikariDataSource
import no.nav.syfo.SystemTestBase
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.dto.FeiletEntitet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

open class FeiletRepositorySpec : SystemTestBase() {

    private lateinit var respository: FeiletRepository

    @BeforeAll
    internal fun setUp() {
        val ds = HikariDataSource(createTestHikariConfig())
        respository = FeiletRepositoryImp(ds)
        respository.deleteAll()
    }

    @AfterEach
    internal fun tearDown() {
        respository.deleteAll()
    }

    private val NOW: LocalDateTime = LocalDateTime.now()
    private val DAYS_1: LocalDateTime = NOW.minusDays(1)
    private val DAYS_8: LocalDateTime = NOW.minusDays(8)
    private val DAYS_14: LocalDateTime = NOW.minusDays(14)

    @Test
    fun `Skal finne alle entiteter med arkivReferansen og rette verdier`() {
        val ARKIV_REFERANSE = "ar-123"
        respository.lagreInnteksmelding(
            FeiletEntitet(
                id = 2,
                arkivReferanse = ARKIV_REFERANSE,
                tidspunkt = DAYS_1,
                feiltype = Feiltype.AKTØR_IKKE_FUNNET
            )
        )
        respository.lagreInnteksmelding(
            FeiletEntitet(
                id = 3,
                arkivReferanse = "ar-222",
                tidspunkt = DAYS_1,
                feiltype = Feiltype.JMS
            )
        )
        respository.lagreInnteksmelding(
            FeiletEntitet(
                id = 4,
                arkivReferanse = "ar-555",
                tidspunkt = DAYS_1,
                feiltype = Feiltype.USPESIFISERT
            )
        )
        respository.lagreInnteksmelding(
            FeiletEntitet(
                id = 5,
                arkivReferanse = ARKIV_REFERANSE,
                tidspunkt = DAYS_8,
                feiltype = Feiltype.BEHANDLENDE_IKKE_FUNNET
            )
        )
        val liste = respository.findByArkivReferanse(ARKIV_REFERANSE)
        assertThat(liste.size).isEqualTo(2)
        assertThat(liste[0].arkivReferanse).isEqualTo(ARKIV_REFERANSE)
        assertThat(liste[0].feiltype).isEqualTo(Feiltype.AKTØR_IKKE_FUNNET)
        assertThat(liste[0].tidspunkt).isEqualTo(DAYS_1)
        assertThat(liste[1].arkivReferanse).isEqualTo(ARKIV_REFERANSE)
        assertThat(liste[1].feiltype).isEqualTo(Feiltype.BEHANDLENDE_IKKE_FUNNET)
        assertThat(liste[1].tidspunkt).isEqualTo(DAYS_8)
    }

    @Test
    fun `Skal ikke finne entitet dersom arkivReferansen ikke er lagret tidligere`() {
        val ARKIV_REFERANSE = "ar-finnes-ikke"
        respository.lagreInnteksmelding(
            FeiletEntitet(
                id = 3,
                arkivReferanse = "ar-333",
                tidspunkt = DAYS_8,
                feiltype = Feiltype.BEHANDLENDE_IKKE_FUNNET
            )
        )
        respository.lagreInnteksmelding(
            FeiletEntitet(
                id = 4,
                arkivReferanse = "ar-444",
                tidspunkt = DAYS_14,
                feiltype = Feiltype.BEHANDLENDE_IKKE_FUNNET
            )
        )
        val liste = respository.findByArkivReferanse(ARKIV_REFERANSE)
        assertThat(liste.size).isEqualTo(0)
    }
}
