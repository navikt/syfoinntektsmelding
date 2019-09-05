package no.nav.syfo.consumer.ws.mapping

import log
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.*
import no.seres.xsd.nav.inntektsmelding_m._20181211.XMLGjenopptakelseNaturalytelseListe
import no.seres.xsd.nav.inntektsmelding_m._20181211.XMLInntektsmeldingM
import no.seres.xsd.nav.inntektsmelding_m._20181211.XMLOpphoerAvNaturalytelseListe
import no.seres.xsd.nav.inntektsmelding_m._20181211.XMLRefusjon
import javax.xml.bind.JAXBElement

internal object InntektsmeldingArbeidsgiverPrivat20181211Mapper {

    val log = log()

    fun tilXMLInntektsmelding(jaxbInntektsmelding: JAXBElement<Any>, journalpostId: String, status: JournalStatus, aktorConsumer: AktorConsumer): Inntektsmelding {
        log.info("Behandling inntektsmelding på 20181211 format")
        val skjemainnhold = (jaxbInntektsmelding.value as XMLInntektsmeldingM).skjemainnhold

        val arbeidsforholdId = skjemainnhold.arbeidsforhold.value.arbeidsforholdId?.value
        val beregnetInntekt = skjemainnhold.arbeidsforhold.value.beregnetInntekt?.value?.beloep?.value
        val arbeidsGiverAktørId = skjemainnhold.arbeidsgiverPrivat?.value?.arbeidsgiverFnr?.let { ap -> aktorConsumer.getAktorId(ap) }
        val perioder = skjemainnhold.sykepengerIArbeidsgiverperioden.value.arbeidsgiverperiodeListe
                .value
                ?.arbeidsgiverperiode
                ?.filter { xmlPeriode -> xmlPeriode.fom != null && xmlPeriode.tom != null }
                ?.map { Periode(it.fom.value, it.tom.value) }
                ?: emptyList()

        return Inntektsmelding(
                skjemainnhold.arbeidstakerFnr,
                skjemainnhold.arbeidsgiver?.value?.virksomhetsnummer,
                skjemainnhold.arbeidsgiverPrivat?.value?.arbeidsgiverFnr,
                arbeidsGiverAktørId,
                arbeidsforholdId,
                journalpostId,
                skjemainnhold.aarsakTilInnsending,
                status,
                perioder,
                beregnetInntekt,
                mapXmlRefusjon(skjemainnhold.refusjon),
                mapXmlEndringRefusjon(skjemainnhold.refusjon),
                mapXmlOpphørNaturalytelser(skjemainnhold.opphoerAvNaturalytelseListe),
                mapXmlGjenopptakelseNaturalytelser(skjemainnhold.gjenopptakelseNaturalytelseListe),
                Gyldighetsstatus.GYLDIG)
    }

    private fun mapXmlGjenopptakelseNaturalytelser(xmlGjenopptakelseListe: JAXBElement<XMLGjenopptakelseNaturalytelseListe>?): List<GjenopptakelseNaturalytelse> {
        return xmlGjenopptakelseListe?.value?.naturalytelseDetaljer?.map { go ->
            GjenopptakelseNaturalytelse(mapNaturalytelseType(go.naturalytelseType), go.fom?.value, go.beloepPrMnd?.value)
        }
                ?: emptyList()
    }

    private fun mapXmlOpphørNaturalytelser(xmlOpphørsliste: JAXBElement<XMLOpphoerAvNaturalytelseListe>): List<OpphoerAvNaturalytelse> {
        return xmlOpphørsliste?.value?.opphoerAvNaturalytelse?.map { o ->
            OpphoerAvNaturalytelse(mapNaturalytelseType(o.naturalytelseType), o.fom?.value, o.beloepPrMnd.value)
        }
                ?: emptyList()

    }

    private fun mapXmlEndringRefusjon(xmlRefusjon: JAXBElement<XMLRefusjon>): List<EndringIRefusjon> {
        return xmlRefusjon?.value?.endringIRefusjonListe?.value?.endringIRefusjon?.map { e -> EndringIRefusjon(e.endringsdato?.value, e.refusjonsbeloepPrMnd?.value) }
                ?: emptyList()
    }

    private fun mapXmlRefusjon(refusjon: JAXBElement<XMLRefusjon>): Refusjon {
        return Refusjon(refusjon?.value?.refusjonsbeloepPrMnd?.value, refusjon?.value?.refusjonsopphoersdato?.value)
    }
}
