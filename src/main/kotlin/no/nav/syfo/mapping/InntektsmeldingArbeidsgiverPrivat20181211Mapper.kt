package no.nav.syfo.mapping

import log
import no.nav.syfo.client.aktor.AktorClient
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.*
import no.seres.xsd.nav.inntektsmelding_m._20181211.*
import java.time.LocalDate
import java.time.LocalDateTime
import javax.xml.bind.JAXBElement

internal object InntektsmeldingArbeidsgiverPrivat20181211Mapper {

    val log = log()

    fun tilXMLInntektsmelding(
        jaxbInntektsmelding: JAXBElement<Any>,
        journalpostId: String,
        mottattDato: LocalDateTime,
        journalStatus: JournalStatus,
        arkivReferanse: String,
        aktorClient: AktorClient
    ): Inntektsmelding {
        log.info("Behandling inntektsmelding på 20181211 format")
        val skjemainnhold = (jaxbInntektsmelding.value as XMLInntektsmeldingM).skjemainnhold

        val arbeidsforholdId = skjemainnhold.arbeidsforhold.value.arbeidsforholdId?.value
        val beregnetInntekt = skjemainnhold.arbeidsforhold.value.beregnetInntekt?.value?.beloep?.value
        val arbeidsGiverAktørId = skjemainnhold.arbeidsgiverPrivat?.value?.arbeidsgiverFnr?.let { ap -> aktorClient.getAktorId(ap) }

        val innsendingstidspunkt = skjemainnhold.avsendersystem?.innsendingstidspunkt?.value
        val bruttoUtbetalt = skjemainnhold.sykepengerIArbeidsgiverperioden?.value?.bruttoUtbetalt?.value
        val årsakEndring = skjemainnhold.arbeidsforhold?.value?.beregnetInntekt?.value?.aarsakVedEndring?.value

        val perioder = skjemainnhold
            ?.sykepengerIArbeidsgiverperioden?.value
            ?.arbeidsgiverperiodeListe?.value
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
            refusjon = mapXmlRefusjon(skjemainnhold.refusjon),
            endringerIRefusjon = mapXmlEndringRefusjon(skjemainnhold.refusjon),
            opphørAvNaturalYtelse = mapXmlOpphørNaturalytelser(skjemainnhold.opphoerAvNaturalytelseListe),
            gjenopptakelserNaturalYtelse = mapXmlGjenopptakelseNaturalytelser(skjemainnhold.gjenopptakelseNaturalytelseListe),
            gyldighetsStatus = Gyldighetsstatus.GYLDIG,
            arkivRefereranse = arkivReferanse,
            feriePerioder = mapFerie(skjemainnhold.arbeidsforhold),
            førsteFraværsdag = mapFørsteFraværsdag(skjemainnhold.arbeidsforhold),
            mottattDato = mottattDato,
            begrunnelseRedusert = skjemainnhold.sykepengerIArbeidsgiverperioden.value.begrunnelseForReduksjonEllerIkkeUtbetalt?.value
                ?: "",
            avsenderSystem = AvsenderSystem(skjemainnhold.avsendersystem.systemnavn, skjemainnhold.avsendersystem.systemversjon),
            nærRelasjon = skjemainnhold.isNaerRelasjon,
            kontaktinformasjon = Kontaktinformasjon(
                skjemainnhold.arbeidsgiver?.value?.kontaktinformasjon?.kontaktinformasjonNavn, skjemainnhold.arbeidsgiver?.value?.kontaktinformasjon?.telefonnummer
            ),
            innsendingstidspunkt = innsendingstidspunkt,
            bruttoUtbetalt = bruttoUtbetalt,
            årsakEndring = årsakEndring
        )
    }

    private fun mapFerie(arbeidsforhold: JAXBElement<XMLArbeidsforhold>): List<Periode> {
        return arbeidsforhold?.value?.avtaltFerieListe?.value?.avtaltFerie?.map { f -> Periode(f.fom.value, f.tom.value) }
            ?: emptyList()
    }

    private fun mapFørsteFraværsdag(arbeidsforhold: JAXBElement<XMLArbeidsforhold>?): LocalDate? {
        return arbeidsforhold?.value?.foersteFravaersdag?.value
    }

    private fun mapXmlGjenopptakelseNaturalytelser(xmlGjenopptakelseListe: JAXBElement<XMLGjenopptakelseNaturalytelseListe>?): List<GjenopptakelseNaturalytelse> {
        return xmlGjenopptakelseListe?.value?.naturalytelseDetaljer?.map { gjenopptakelse ->
            GjenopptakelseNaturalytelse(no.nav.syfo.consumer.ws.mapping.mapNaturalytelseType(gjenopptakelse.naturalytelseType), gjenopptakelse.fom?.value, gjenopptakelse.beloepPrMnd?.value)
        }
            ?: emptyList()
    }

    private fun mapXmlOpphørNaturalytelser(xmlOpphørsliste: JAXBElement<XMLOpphoerAvNaturalytelseListe>?): List<OpphoerAvNaturalytelse> {
        return xmlOpphørsliste?.value?.opphoerAvNaturalytelse?.map { opphør ->
            OpphoerAvNaturalytelse(no.nav.syfo.consumer.ws.mapping.mapNaturalytelseType(opphør.naturalytelseType), opphør.fom?.value, opphør.beloepPrMnd.value)
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
