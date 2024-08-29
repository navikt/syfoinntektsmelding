package no.nav.syfo.domain.inntektsmelding

import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.simba.Avsender.NAV_NO_SELVBESTEMT
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Inntektsmelding(
    val id: String = "",
    var fnr: String,
    val arbeidsgiverOrgnummer: String? = null,
    val arbeidsgiverPrivatFnr: String? = null,
    val arbeidsgiverPrivatAktørId: String? = null,
    val arbeidsforholdId: String? = null,
    val journalpostId: String,
    val arsakTilInnsending: String,
    var journalStatus: JournalStatus,
    val arbeidsgiverperioder: List<Periode> = emptyList(),
    val beregnetInntekt: BigDecimal? = null,
    val inntektsdato: LocalDate? = null,
    val refusjon: Refusjon = Refusjon(),
    val endringerIRefusjon: List<EndringIRefusjon> = emptyList(),
    val opphørAvNaturalYtelse: List<OpphoerAvNaturalytelse> = emptyList(),
    val gjenopptakelserNaturalYtelse: List<GjenopptakelseNaturalytelse> = emptyList(),
    val gyldighetsStatus: Gyldighetsstatus = Gyldighetsstatus.GYLDIG,
    val arkivRefereranse: String,
    val feriePerioder: List<Periode> = emptyList(),
    val førsteFraværsdag: LocalDate?,
    val mottattDato: LocalDateTime,
    var sakId: String? = null,
    var aktorId: String? = null,
    val begrunnelseRedusert: String = "",
    val avsenderSystem: AvsenderSystem = AvsenderSystem(),
    val nærRelasjon: Boolean? = null,
    val kontaktinformasjon: Kontaktinformasjon = Kontaktinformasjon(),
    val innsendingstidspunkt: LocalDateTime? = null,
    val bruttoUtbetalt: BigDecimal? = null,
    val årsakEndring: String? = null,
    val rapportertInntekt: RapportertInntekt? = null,
    val vedtaksperiodeId: UUID? = null
) {
    fun isDuplicate(inntektsmelding: Inntektsmelding): Boolean {
        return this.equals(
            inntektsmelding.copy(
                id = this.id,
                fnr = this.fnr,
                mottattDato = this.mottattDato,
                journalpostId = this.journalpostId,
                journalStatus = this.journalStatus,
                arkivRefereranse = this.arkivRefereranse,
                aktorId = this.aktorId,
                sakId = this.sakId,
                innsendingstidspunkt = this.innsendingstidspunkt,
                avsenderSystem = this.avsenderSystem
            )
        )
    }

    fun isDuplicateExclusiveArsakInnsending(inntektsmelding: Inntektsmelding): Boolean {
        return this.equals(
            inntektsmelding.copy(
                id = this.id,
                fnr = this.fnr,
                mottattDato = this.mottattDato,
                journalpostId = this.journalpostId,
                journalStatus = this.journalStatus,
                arkivRefereranse = this.arkivRefereranse,
                aktorId = this.aktorId,
                sakId = this.sakId,
                innsendingstidspunkt = this.innsendingstidspunkt,
                avsenderSystem = this.avsenderSystem,
                arsakTilInnsending = this.arsakTilInnsending
            )
        )
    }

    // TODO: kanskje kan vi fjerne dette, hvis vedtaksperiodeId *alltid* skal komme i selvbestemtIM
    fun matcherSpleis(): Boolean {
        return !(avsenderSystem.navn == NAV_NO_SELVBESTEMT && vedtaksperiodeId == null)
    }
}
