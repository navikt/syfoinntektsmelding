package no.nav.syfo.utsattoppgave

import no.nav.helsearbeidsgiver.oppgave.domain.Behandlingstema
import no.nav.helsearbeidsgiver.oppgave.domain.Behandlingstype
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.UtsattOppgaveEntitet
import java.math.BigDecimal

enum class BehandlingsKategori(
    val oppgaveBeskrivelse: String? = null,
) {
    SPEIL_RELATERT("Speil"),
    UTLAND("Utland"),
    IKKE_REFUSJON("Utbetaling til bruker"),
    REFUSJON_UTEN_DATO("Normal"),
    REFUSJON_MED_DATO("Utbetaling til bruker"),
    REFUSJON_LITEN_LØNN("Utbetaling til bruker"),
    BESTRIDER_SYKEMELDING("Bestrider sykmelding"),
    IKKE_FRAVAER("Normal"),
    ;

    fun getBehandlingsType(): String? = if (this == UTLAND) Behandlingstype.UTLAND else null

    fun getBehandlingstema(): String? =
        when (this) {
            SPEIL_RELATERT -> Behandlingstema.SPEIL
            BESTRIDER_SYKEMELDING -> Behandlingstema.BESTRIDER_SYKEMELDING
            IKKE_REFUSJON, REFUSJON_MED_DATO, REFUSJON_LITEN_LØNN -> Behandlingstema.UTBETALING_TIL_BRUKER
            UTLAND -> null
            else -> Behandlingstema.NORMAL
        }

    fun getUtbetalingBruker(): Boolean = this == IKKE_REFUSJON || this == REFUSJON_MED_DATO || this == REFUSJON_LITEN_LØNN
}

fun finnBehandlingsKategori(
    inntektsmelding: Inntektsmelding,
    speilRelatert: Boolean,
    gjelderUtland: Boolean,
): BehandlingsKategori =
    when {
        inntektsmelding.begrunnelseRedusert == "IkkeFravaer" -> BehandlingsKategori.IKKE_FRAVAER
        speilRelatert -> BehandlingsKategori.SPEIL_RELATERT
        gjelderUtland -> BehandlingsKategori.UTLAND
        inntektsmelding.begrunnelseRedusert == "BetvilerArbeidsufoerhet" -> BehandlingsKategori.BESTRIDER_SYKEMELDING
        inntektsmelding.refusjon.beloepPrMnd == null || inntektsmelding.refusjon.beloepPrMnd <
            BigDecimal(
                1,
            )
        -> BehandlingsKategori.IKKE_REFUSJON
        inntektsmelding.refusjon.opphoersdato != null -> BehandlingsKategori.REFUSJON_MED_DATO
        inntektsmelding.refusjon.beloepPrMnd < inntektsmelding.beregnetInntekt -> BehandlingsKategori.REFUSJON_LITEN_LØNN
        else -> BehandlingsKategori.REFUSJON_UTEN_DATO
    }

fun utledBehandlingsKategori(
    oppgave: UtsattOppgaveEntitet,
    inntektsmelding: Inntektsmelding,
    gjelderUtland: Boolean,
): BehandlingsKategori = finnBehandlingsKategori(inntektsmelding, oppgave.speil, gjelderUtland)
