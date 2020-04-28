package no.nav.syfo.consumer.ws.mapping

import log
import no.nav.syfo.domain.InngaaendeJournal
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.*
import no.seres.xsd.nav.inntektsmelding_m._20180924.*
import java.time.LocalDate
import javax.xml.bind.JAXBElement


internal object InntektsmeldingArbeidsgiver20180924Mapper {

    val log = log()

    fun tilXMLInntektsmelding(jabxInntektsmelding: JAXBElement<Any>, journalpostId: String, inngaaendeJournal: InngaaendeJournal, arkivReferanse: String): Inntektsmelding {
        log.info("Behandling inntektsmelding på 20180924 format")
        val skjemainnhold = (jabxInntektsmelding.value as XMLInntektsmeldingM).skjemainnhold

        val arbeidsforholdId = skjemainnhold.arbeidsforhold.value.arbeidsforholdId?.value
        val beregnetInntekt = skjemainnhold.arbeidsforhold.value.beregnetInntekt?.value?.beloep?.value

        val innsendingstidspunkt = skjemainnhold.avsendersystem?.innsendingstidspunkt?.value
        val bruttoUtbetalt = skjemainnhold.sykepengerIArbeidsgiverperioden?.value?.bruttoUtbetalt?.value
        val årsakEndring = skjemainnhold.arbeidsforhold?.value?.beregnetInntekt?.value?.aarsakVedEndring?.value

        val perioder = skjemainnhold.sykepengerIArbeidsgiverperioden.value.arbeidsgiverperiodeListe
            .value
            ?.arbeidsgiverperiode
            ?.filter { xmlPeriode -> xmlPeriode.fom != null && xmlPeriode.tom != null }
            ?.map { Periode(it.fom.value, it.tom.value) }
            ?: emptyList()

        return Inntektsmelding(
            fnr = skjemainnhold.arbeidstakerFnr,
            arbeidsgiverOrgnummer = skjemainnhold.arbeidsgiver?.virksomhetsnummer,
            arbeidsgiverPrivatFnr = null,
            arbeidsgiverPrivatAktørId = null,
            arbeidsforholdId = arbeidsforholdId,
            journalpostId = journalpostId,
            arsakTilInnsending = skjemainnhold.aarsakTilInnsending,
            journalStatus = inngaaendeJournal.status,
            arbeidsgiverperioder = perioder,
            beregnetInntekt = beregnetInntekt,
            refusjon = mapXmlRefusjon(skjemainnhold.refusjon),
            endringerIRefusjon = mapXmlEndringRefusjon(skjemainnhold.refusjon),
            opphørAvNaturalYtelse = mapXmlOpphørNaturalytelser(skjemainnhold.opphoerAvNaturalytelseListe),
            gjenopptakelserNaturalYtelse = mapXmlGjenopptakelseNaturalytelser(skjemainnhold.gjenopptakelseNaturalytelseListe),
            gyldighetsStatus = Gyldighetsstatus.GYLDIG,
            arkivRefereranse = arkivReferanse,
            feriePerioder = mapFerie(skjemainnhold.arbeidsforhold),
            førsteFraværsdag = mapFørsteFraværsdag(skjemainnhold.arbeidsforhold),
            mottattDato = mapXmlGregorianTilLocalDate(inngaaendeJournal.mottattDato),
            begrunnelseRedusert = skjemainnhold.sykepengerIArbeidsgiverperioden.value.begrunnelseForReduksjonEllerIkkeUtbetalt?.value
                ?: "",
            avsenderSystem = AvsenderSystem(skjemainnhold.avsendersystem.systemnavn, skjemainnhold.avsendersystem.systemversjon),
            nærRelasjon = skjemainnhold.isNaerRelasjon,
            kontaktinformasjon = Kontaktinformasjon(
                skjemainnhold.arbeidsgiver?.kontaktinformasjon?.kontaktinformasjonNavn, skjemainnhold.arbeidsgiver?.kontaktinformasjon?.telefonnummer
            ),
            innsendingstidspunkt = innsendingstidspunkt,
            bruttoUtbetalt = bruttoUtbetalt,
            årsakEndring = årsakEndring
        )
    }

    private fun mapFerie(arbeidsforhold: JAXBElement<XMLArbeidsforhold>): List<Periode> {
        return arbeidsforhold?.value?.avtaltFerieListe?.value?.avtaltFerie?.filter { f -> f.fom != null }?.map { f -> Periode(f.fom.value, f.tom.value) }
            ?: emptyList()
    }

    private fun mapFørsteFraværsdag(arbeidsforhold: JAXBElement<XMLArbeidsforhold>?): LocalDate? {
        return arbeidsforhold?.value?.foersteFravaersdag?.value
    }

    private fun mapXmlGjenopptakelseNaturalytelser(xmlGjenopptakelseListe: JAXBElement<XMLGjenopptakelseNaturalytelseListe>?): List<GjenopptakelseNaturalytelse> {
        return xmlGjenopptakelseListe?.value?.naturalytelseDetaljer?.map { gjenopptakelse ->
            GjenopptakelseNaturalytelse(mapNaturalytelseType(gjenopptakelse.naturalytelseType), gjenopptakelse.fom?.value, gjenopptakelse.beloepPrMnd?.value)
        }
            ?: emptyList()
    }

    private fun mapXmlOpphørNaturalytelser(xmlOpphørsliste: JAXBElement<XMLOpphoerAvNaturalytelseListe>?): List<OpphoerAvNaturalytelse> {
        return xmlOpphørsliste?.value?.opphoerAvNaturalytelse?.map { opphør ->
            OpphoerAvNaturalytelse(mapNaturalytelseType(opphør.naturalytelseType), opphør.fom?.value, opphør.beloepPrMnd.value)
        }
            ?: emptyList()

    }

    private fun mapXmlEndringRefusjon(xmlRefusjon: JAXBElement<XMLRefusjon>?): List<EndringIRefusjon> {
        return xmlRefusjon?.value?.endringIRefusjonListe?.value?.endringIRefusjon?.map { endring -> EndringIRefusjon(endring.endringsdato?.value, endring.refusjonsbeloepPrMnd?.value) }
            ?: emptyList()
    }

    private fun mapXmlRefusjon(refusjon: JAXBElement<XMLRefusjon>?): Refusjon {
        return Refusjon(refusjon?.value?.refusjonsbeloepPrMnd?.value, refusjon?.value?.refusjonsopphoersdato?.value)
    }


}
