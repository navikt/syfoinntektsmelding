package no.nav.syfo.simba

import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.BegrunnelseIngenEllerRedusertUtbetalingKode
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.FullLonnIArbeidsgiverPerioden
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.InntektsmeldingDokument
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.Naturalytelse
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.NaturalytelseKode
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.Periode
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.Refusjon
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.RefusjonEndring
import no.nav.helsearbeidsgiver.felles.inntektsmelding.felles.models.ÅrsakInnsending
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

internal class MapInntektsmeldingKtTest {

    @Test
    fun mapInntektsmeldingMedNaturalytelser() {
        val dato1 = LocalDate.now().minusDays(7)
        val dato2 = LocalDate.now().minusDays(5)
        val dato3 = LocalDate.now().minusDays(3)
        val periode1 = listOf(Periode(dato1, dato1))
        val periode2 = listOf(Periode(dato1, dato1), Periode(dato2, dato2))
        val periode3 = listOf(Periode(dato1, dato3))
        val naturalytelseListe = NaturalytelseKode.values().map { Naturalytelse(it, dato1, BigDecimal.ONE) }
        val antallNaturalytelser = naturalytelseListe.count()
        val refusjonEndring = listOf(RefusjonEndring(123.toBigDecimal(), LocalDate.now()))
        val refusjon = Refusjon(true, BigDecimal.TEN, LocalDate.of(2025, 12, 12), refusjonEndring)

        val imDokumentFraSimba = InntektsmeldingDokument(
            orgnrUnderenhet = "123456789",
            identitetsnummer = "12345678901",
            fulltNavn = "Test testesen",
            virksomhetNavn = "Blåbærsyltetøy A/S",
            behandlingsdager = listOf(dato1),
            egenmeldingsperioder = periode1,
            bestemmendeFraværsdag = dato2,
            fraværsperioder = periode2,
            arbeidsgiverperioder = periode3,
            beregnetInntekt = BigDecimal.valueOf(100_000L),
            fullLønnIArbeidsgiverPerioden = FullLonnIArbeidsgiverPerioden(utbetalerFullLønn = false, begrunnelse = BegrunnelseIngenEllerRedusertUtbetalingKode.BESKJED_GITT_FOR_SENT),
            refusjon = refusjon,
            naturalytelser = naturalytelseListe,
            tidspunkt = OffsetDateTime.now(),
            årsakInnsending = ÅrsakInnsending.NY,
            innsenderNavn = "Peppes Pizza",
            telefonnummer = "22555555"
        )
        val mapped = mapInntektsmelding("1323", "sdfds", "134", imDokumentFraSimba)
        val naturalytelse = mapped.opphørAvNaturalYtelse.get(0)
        assertEquals(mapped.refusjon.opphoersdato, refusjon.refusjonOpphører)
        assertEquals(mapped.endringerIRefusjon.size, 1)
        assertEquals(antallNaturalytelser, mapped.opphørAvNaturalYtelse.size)
        assertEquals(no.nav.syfo.domain.inntektsmelding.Naturalytelse.AKSJERGRUNNFONDSBEVISTILUNDERKURS, naturalytelse.naturalytelse)
    }
}
