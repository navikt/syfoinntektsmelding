package no.nav.syfo.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
)

val norskDatoFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun LocalDate.tilNorskFormat(): String = format(norskDatoFormat)

fun List<Periode>.tilKortFormat(): String? =
    if (isEmpty()) {
        null
    } else if (size == 1) {
        "${first().fom.tilNorskFormat()} - ${first().tom.tilNorskFormat()}"
    } else {
        "${first().fom.tilNorskFormat()} - [...] - ${last().tom.tilNorskFormat()}"
    }
