package no.nav.syfo.domain.inntektsmelding

import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime


data class Inntektsmelding(
        val id: String = "",
        val fnr: String,
        val arbeidsgiverOrgnummer: String? = null,
        val arbeidsgiverPrivatFnr: String? = null,
        val arbeidsgiverPrivatAktørId: String? = null,
        val arbeidsforholdId: String? = null,
        val journalpostId: String,
        val arsakTilInnsending: String,
        val journalStatus: JournalStatus,
        val arbeidsgiverperioder: List<Periode> = emptyList(),
        val beregnetInntekt: BigDecimal? = null,
        val refusjon: Refusjon = Refusjon(),
        val endringerIRefusjon: List<EndringIRefusjon> = emptyList(),
        val opphørAvNaturalYtelse: List<OpphoerAvNaturalytelse> = emptyList(),
        val gjenopptakelserNaturalYtelse: List<GjenopptakelseNaturalytelse> = emptyList(),
        val gyldighetsStatus: Gyldighetsstatus = Gyldighetsstatus.GYLDIG,
        val arkivRefereranse: String,
        val feriePerioder: List<Periode> = emptyList(),
        val førsteFraværsdag: LocalDate,
        val mottattDato: LocalDateTime,
        var sakId: String? = null,
        var aktorId: String? = null
)

