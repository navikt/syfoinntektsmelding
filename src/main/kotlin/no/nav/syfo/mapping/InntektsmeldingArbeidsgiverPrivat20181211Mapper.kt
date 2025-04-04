package no.nav.syfo.mapping

import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.AvsenderSystem
import no.nav.syfo.domain.inntektsmelding.EndringIRefusjon
import no.nav.syfo.domain.inntektsmelding.GjenopptakelseNaturalytelse
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Kontaktinformasjon
import no.nav.syfo.domain.inntektsmelding.MottaksKanal
import no.nav.syfo.domain.inntektsmelding.OpphoerAvNaturalytelse
import no.nav.syfo.domain.inntektsmelding.Refusjon
import no.nav.syfo.util.getAktørid
import no.seres.xsd.nav.inntektsmelding_m._20181211.XMLArbeidsforhold
import no.seres.xsd.nav.inntektsmelding_m._20181211.XMLGjenopptakelseNaturalytelseListe
import no.seres.xsd.nav.inntektsmelding_m._20181211.XMLInntektsmeldingM
import no.seres.xsd.nav.inntektsmelding_m._20181211.XMLOpphoerAvNaturalytelseListe
import no.seres.xsd.nav.inntektsmelding_m._20181211.XMLRefusjon
import java.time.LocalDate
import java.time.LocalDateTime
import javax.xml.bind.JAXBElement

internal object InntektsmeldingArbeidsgiverPrivat20181211Mapper {
    private val logger = this.logger()

    fun fraXMLInntektsmelding(
        jaxbInntektsmelding: JAXBElement<Any>,
        journalpostId: String,
        mottattDato: LocalDateTime,
        journalStatus: JournalStatus,
        arkivReferanse: String,
        pdlClient: PdlClient,
    ): Inntektsmelding {
        logger.info("Behandling inntektsmelding på 20181211 format")
        val skjemainnhold = (jaxbInntektsmelding.value as XMLInntektsmeldingM).skjemainnhold

        val arbeidsforholdId =
            skjemainnhold.arbeidsforhold.value.arbeidsforholdId
                ?.value
        val beregnetInntekt =
            skjemainnhold.arbeidsforhold.value.beregnetInntekt
                ?.value
                ?.beloep
                ?.value
        val arbeidsGiverAktørId =
            skjemainnhold.arbeidsgiverPrivat
                ?.value
                ?.arbeidsgiverFnr
                ?.let { fnr -> pdlClient.getAktørid(fnr) }

        val innsendingstidspunkt = skjemainnhold.avsendersystem?.innsendingstidspunkt?.value
        val bruttoUtbetalt =
            skjemainnhold.sykepengerIArbeidsgiverperioden
                ?.value
                ?.bruttoUtbetalt
                ?.value
        val årsakEndring =
            skjemainnhold.arbeidsforhold
                ?.value
                ?.beregnetInntekt
                ?.value
                ?.aarsakVedEndring
                ?.value

        val perioder =
            skjemainnhold
                ?.sykepengerIArbeidsgiverperioden
                ?.value
                ?.arbeidsgiverperiodeListe
                ?.value
                ?.arbeidsgiverperiode
                ?.filter { xmlPeriode -> xmlPeriode.fom != null && xmlPeriode.tom != null }
                ?.map { Periode(it.fom.value, it.tom.value) }
                ?: emptyList()

        return Inntektsmelding(
            fnr = skjemainnhold.arbeidstakerFnr,
            arbeidsgiverOrgnummer = skjemainnhold.arbeidsgiver?.value?.virksomhetsnummer,
            arbeidsgiverPrivatFnr = skjemainnhold.arbeidsgiverPrivat?.value?.arbeidsgiverFnr,
            arbeidsgiverPrivatAktørId = arbeidsGiverAktørId,
            arbeidsforholdId = arbeidsforholdId,
            journalpostId = journalpostId,
            arsakTilInnsending = skjemainnhold.aarsakTilInnsending,
            journalStatus = journalStatus,
            arbeidsgiverperioder = perioder,
            beregnetInntekt = beregnetInntekt,
            inntektsdato = null,
            refusjon = mapXmlRefusjon(skjemainnhold.refusjon),
            endringerIRefusjon = mapXmlEndringRefusjon(skjemainnhold.refusjon),
            opphørAvNaturalYtelse = mapXmlOpphørNaturalytelser(skjemainnhold.opphoerAvNaturalytelseListe),
            gjenopptakelserNaturalYtelse = mapXmlGjenopptakelseNaturalytelser(skjemainnhold.gjenopptakelseNaturalytelseListe),
            gyldighetsStatus = Gyldighetsstatus.GYLDIG,
            arkivRefereranse = arkivReferanse,
            feriePerioder = mapFerie(skjemainnhold.arbeidsforhold),
            førsteFraværsdag = mapFørsteFraværsdag(skjemainnhold.arbeidsforhold),
            mottattDato = mottattDato,
            begrunnelseRedusert =
                skjemainnhold.sykepengerIArbeidsgiverperioden.value.begrunnelseForReduksjonEllerIkkeUtbetalt
                    ?.value
                    ?: "",
            avsenderSystem = AvsenderSystem(skjemainnhold.avsendersystem.systemnavn, skjemainnhold.avsendersystem.systemversjon),
            nærRelasjon = skjemainnhold.isNaerRelasjon,
            kontaktinformasjon =
                Kontaktinformasjon(
                    skjemainnhold.arbeidsgiver
                        ?.value
                        ?.kontaktinformasjon
                        ?.kontaktinformasjonNavn,
                    skjemainnhold.arbeidsgiver
                        ?.value
                        ?.kontaktinformasjon
                        ?.telefonnummer,
                ),
            innsendingstidspunkt = innsendingstidspunkt,
            bruttoUtbetalt = bruttoUtbetalt,
            årsakEndring = årsakEndring,
            mottaksKanal = MottaksKanal.ALTINN,
        )
    }

    private fun mapFerie(arbeidsforhold: JAXBElement<XMLArbeidsforhold>): List<Periode> =
        arbeidsforhold.value
            ?.avtaltFerieListe
            ?.value
            ?.avtaltFerie
            ?.map { f -> Periode(f.fom.value, f.tom.value) }
            ?: emptyList()

    private fun mapFørsteFraværsdag(arbeidsforhold: JAXBElement<XMLArbeidsforhold>?): LocalDate? = arbeidsforhold?.value?.foersteFravaersdag?.value

    private fun mapXmlGjenopptakelseNaturalytelser(
        xmlGjenopptakelseListe: JAXBElement<XMLGjenopptakelseNaturalytelseListe>?,
    ): List<GjenopptakelseNaturalytelse> =
        xmlGjenopptakelseListe?.value?.naturalytelseDetaljer?.map { gjenopptakelse ->
            GjenopptakelseNaturalytelse(
                mapNaturalytelseType(gjenopptakelse.naturalytelseType),
                gjenopptakelse.fom?.value,
                gjenopptakelse.beloepPrMnd?.value,
            )
        }
            ?: emptyList()

    private fun mapXmlOpphørNaturalytelser(xmlOpphørsliste: JAXBElement<XMLOpphoerAvNaturalytelseListe>?): List<OpphoerAvNaturalytelse> =
        xmlOpphørsliste?.value?.opphoerAvNaturalytelse?.map { opphør ->
            OpphoerAvNaturalytelse(mapNaturalytelseType(opphør.naturalytelseType), opphør.fom?.value, opphør.beloepPrMnd.value)
        }
            ?: emptyList()

    private fun mapXmlEndringRefusjon(xmlRefusjon: JAXBElement<XMLRefusjon>?): List<EndringIRefusjon> =
        xmlRefusjon?.value?.endringIRefusjonListe?.value?.endringIRefusjon?.map { endring ->
            EndringIRefusjon(endring.endringsdato?.value, endring.refusjonsbeloepPrMnd?.value)
        }
            ?: emptyList()

    private fun mapXmlRefusjon(refusjon: JAXBElement<XMLRefusjon>?): Refusjon = Refusjon(refusjon?.value?.refusjonsbeloepPrMnd?.value, refusjon?.value?.refusjonsopphoersdato?.value)
}
