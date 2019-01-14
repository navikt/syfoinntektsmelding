package no.nav.syfo.util;

import java.time.LocalDate;

public class DateUtil {

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

        public boolean isBetweenOrEqual(LocalDate fom, LocalDate tom) {
            return !isNotBetween(fom, tom);
        }
    }
}
