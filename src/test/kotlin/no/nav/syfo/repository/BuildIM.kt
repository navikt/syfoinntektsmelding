package no.nav.syfo.repository

import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.EndringIRefusjon
import no.nav.syfo.domain.inntektsmelding.GjenopptakelseNaturalytelse
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Naturalytelse
import no.nav.syfo.domain.inntektsmelding.OpphoerAvNaturalytelse
import no.nav.syfo.domain.inntektsmelding.Refusjon
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

fun buildIM(): Inntektsmelding =
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
                OpphoerAvNaturalytelse(
                    Naturalytelse.TILSKUDDBARNEHAGEPLASS,
                    LocalDate.of(2016, 6, 6),
                    BigDecimal(666666666666),
                ),
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
