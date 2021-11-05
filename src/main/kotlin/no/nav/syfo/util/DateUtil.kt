package no.nav.syfo.util

import java.time.LocalDate
import no.nav.syfo.domain.Periode

object DateUtil {
    fun overlapperPerioder(a: Periode, b: Periode): Boolean {
        return (compare(a.fom).isBetweenOrEqual(b)
            || compare(a.tom).isBetweenOrEqual(b)
            || compare(b.fom).isBetweenOrEqual(a)
            || compare(b.tom).isBetweenOrEqual(a))
    }

    fun compare(localDate: LocalDate): LocalDateComparator {
        return LocalDateComparator(localDate)
    }

    class LocalDateComparator internal constructor(private val localDate: LocalDate) {
        private fun isNotBetween(fom: LocalDate?, tom: LocalDate?): Boolean {
            return localDate.isBefore(fom) || localDate.isAfter(tom)
        }

        fun isBetweenOrEqual(p: Periode): Boolean {
            return !isNotBetween(p.fom, p.tom)
        }
    }
}
