package no.nav.syfo.util;

import no.nav.syfo.domain.Periode;

import java.time.LocalDate;

public class DateUtil {

    public static boolean overlapperPerioder(Periode a, Periode b) {
        return compare(a.getFom()).isBetweenOrEqual(b)
                || compare(a.getTom()).isBetweenOrEqual(b)
                || compare(b.getFom()).isBetweenOrEqual(a)
                || compare(b.getTom()).isBetweenOrEqual(a);
    }

    public static LocalDateComparator compare(LocalDate localDate) {
        return new LocalDateComparator(localDate);
    }

    public static class LocalDateComparator {
        private LocalDate localDate;

        private LocalDateComparator(LocalDate localDate) {
            this.localDate = localDate;
        }

        public boolean isNotBetween(LocalDate fom, LocalDate tom) {
            return localDate.isBefore(fom) || localDate.isAfter(tom);
        }

        public boolean isBetweenOrEqual(Periode p) {
            return !isNotBetween(p.getFom(), p.getTom());
        }
    }
}
