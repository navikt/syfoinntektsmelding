package no.nav.syfo.util

import no.nav.syfo.domain.Periode

fun sammenslattPeriode(arbeidsgiverperioder: List<Periode>): Periode? {
    val tidligsteFom = arbeidsgiverperioder.map { it.fom }.min()
    val senesteTom = arbeidsgiverperioder.map { it.tom }.max()

    return if (tidligsteFom != null && senesteTom != null) Periode(tidligsteFom, senesteTom) else null
}
