package no.nav.syfo.util;

import no.nav.syfo.domain.Periode;
import org.junit.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class DateUtilTest {

    @Test
    public void overlapperPerioder() {

        assertThat(DateUtil.overlapperPerioder(
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 1))
                        .tom(LocalDate.of(2019, 1, 10)).build(),
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 12))
                        .tom(LocalDate.of(2019, 1, 15)).build()))
                .isFalse();

        assertThat(DateUtil.overlapperPerioder(
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 1))
                        .tom(LocalDate.of(2019, 1, 10)).build(),
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 1))
                        .tom(LocalDate.of(2019, 1, 10)).build()))
                .isTrue();

        assertThat(DateUtil.overlapperPerioder(
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 1))
                        .tom(LocalDate.of(2019, 1, 10)).build(),
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 4))
                        .tom(LocalDate.of(2019, 1, 9)).build()))
                .isTrue();

        assertThat(DateUtil.overlapperPerioder(
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 1))
                        .tom(LocalDate.of(2019, 1, 10)).build(),
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 4))
                        .tom(LocalDate.of(2019, 1, 15)).build()))
                .isTrue();

        assertThat(DateUtil.overlapperPerioder(
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 10))
                        .tom(LocalDate.of(2019, 1, 15)).build(),
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 1))
                        .tom(LocalDate.of(2019, 1, 12)).build()))
                .isTrue();

        assertThat(DateUtil.overlapperPerioder(
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 1))
                        .tom(LocalDate.of(2019, 1, 1)).build(),
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 1))
                        .tom(LocalDate.of(2019, 1, 2)).build()))
                .isTrue();

        assertThat(DateUtil.overlapperPerioder(
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 1))
                        .tom(LocalDate.of(2019, 1, 2)).build(),
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 1))
                        .tom(LocalDate.of(2019, 1, 1)).build()))
                .isTrue();

        assertThat(DateUtil.overlapperPerioder(
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 10))
                        .tom(LocalDate.of(2019, 1, 15)).build(),
                Periode.builder()
                        .fom(LocalDate.of(2019, 1, 1))
                        .tom(LocalDate.of(2019, 1, 5)).build()))
                .isFalse();
    }
}
