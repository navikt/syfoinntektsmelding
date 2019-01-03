package no.nav.syfo.util;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.GregorianCalendar;

import static java.time.temporal.ChronoUnit.DAYS;
import static javax.xml.datatype.DatatypeFactory.newInstance;

public class DateUtil {

    public static LocalDate tidligsteAv(final LocalDate date1, final LocalDate date2) {
        return date1.isBefore(date2) ? date1 : date2;
    }

    public static LocalDate senesteAv(final LocalDate date1, final LocalDate date2) {
        return date1.isAfter(date2) ? date1 : date2;
    }

    public static int antallDager(LocalDate fom, LocalDate tom) {
        return (int) DAYS.between(fom, tom);
    }

    public static int antallDagerMellom(LocalDate tidligst, LocalDate eldst) {
        return (int) DAYS.between(tidligst, eldst) - 1;
    }

    public static boolean erDatoIPerioden(LocalDate dato, LocalDate fom, LocalDate tom) {
        return dato.isEqual(fom) || dato.isEqual(tom) || iDatoIntervall(dato, tom, fom);
    }

    private static boolean iDatoIntervall(LocalDate dato, LocalDate tom, LocalDate fom) {
        return dato.isAfter(fom) && dato.isBefore(tom);
    }

    public static LocalDate convertToLocalDate(final XMLGregorianCalendar xmlGregorianCalendar) {
        return xmlGregorianCalendar.toGregorianCalendar().toZonedDateTime().toLocalDate();
    }

    public static XMLGregorianCalendar convertToXmlGregorianCalendar(LocalDate dato) {
        try {
            GregorianCalendar gregorianCalendar = GregorianCalendar.from(dato.atStartOfDay().atZone(ZoneId.systemDefault()));
            return newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException dce) {
            throw new IllegalArgumentException("Feil ved konvertering fra LocalDate til XmlGregorianCalendar", dce);
        }
    }

    public static XMLGregorianCalendar convertToXmlGregorianCalendar(LocalDateTime dato) {
        try {
            GregorianCalendar gregorianCalendar = GregorianCalendar.from(dato.atZone(ZoneId.systemDefault()));
            return newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException dce) {
            throw new IllegalArgumentException("Feil ved konvertering fra LocalDateTime til XmlGregorianCalendar", dce);
        }
    }

    public static LocalDateComparator compare(LocalDate localDate) {
        return new LocalDateComparator(localDate);
    }

    public static class LocalDateComparator {
        private LocalDate localDate;

        private LocalDateComparator(LocalDate localDate) {
            this.localDate = localDate;
        }

        public boolean isEqualOrAfter(LocalDate other) {
            return !localDate.isBefore(other);
        }

        public boolean isBeforeOrEqual(LocalDate other) {
            return !localDate.isAfter(other);
        }

        public boolean isNotBetween(LocalDate fom, LocalDate tom) {
            return localDate.isBefore(fom) || localDate.isAfter(tom);
        }

        public boolean isBetweenOrEqual(LocalDate fom, LocalDate tom) {
            return !isNotBetween(fom, tom);
        }

        public boolean isBetween(LocalDate fom, LocalDate tom) {
            return localDate.isAfter(fom) && localDate.isBefore(tom);
        }
    }
}
