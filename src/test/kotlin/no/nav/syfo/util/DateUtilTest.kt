package no.nav.syfo.util

import java.time.LocalDate
import no.nav.syfo.domain.Periode
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DateUtilTest {

    @Test
    fun overlapperPerioder() {

        Assertions.assertThat(
            DateUtil.overlapperPerioder(
                Periode(
                    fom = LocalDate.of(2019, 1, 1),
                    tom = LocalDate.of(2019, 1, 10)
                ),
                Periode(
                    fom = LocalDate.of(2019, 1, 12),
                    tom = LocalDate.of(2019, 1, 15)
                )
            )
        )
            .isFalse

        assertThat(
            DateUtil.overlapperPerioder(
                Periode(
                    fom = LocalDate.of(2019, 1, 1),
                    tom = LocalDate.of(2019, 1, 10)
                ),
                Periode(
                    fom = LocalDate.of(2019, 1, 1),
                    tom = LocalDate.of(2019, 1, 10)
                )
            )
        )
            .isTrue

        assertThat(
            DateUtil.overlapperPerioder(
                Periode(
                    fom = LocalDate.of(2019, 1, 1),
                    tom = LocalDate.of(2019, 1, 10)
                ),
                Periode(
                    fom = LocalDate.of(2019, 1, 4),
                    tom = LocalDate.of(2019, 1, 9)
                )
            )
        )
            .isTrue

        assertThat(
            DateUtil.overlapperPerioder(
                Periode(
                    fom = LocalDate.of(2019, 1, 1),
                    tom = LocalDate.of(2019, 1, 10)
                ),
                Periode(
                    fom = LocalDate.of(2019, 1, 4),
                    tom = LocalDate.of(2019, 1, 15)
                )
            )
        )
            .isTrue

        assertThat(
            DateUtil.overlapperPerioder(
                Periode(
                    fom = LocalDate.of(2019, 1, 10),
                    tom = LocalDate.of(2019, 1, 15)
                ),
                Periode(
                    fom = LocalDate.of(2019, 1, 1),
                    tom = LocalDate.of(2019, 1, 12)
                )
            )
        )
            .isTrue

        assertThat(
            DateUtil.overlapperPerioder(
                Periode(
                    fom = LocalDate.of(2019, 1, 1),
                    tom = LocalDate.of(2019, 1, 1)
                ),
                Periode(
                    fom = LocalDate.of(2019, 1, 1),
                    tom = LocalDate.of(2019, 1, 2)
                )
            )
        )
            .isTrue

        assertThat(
            DateUtil.overlapperPerioder(
                Periode(
                    fom = LocalDate.of(2019, 1, 1),
                    tom = LocalDate.of(2019, 1, 2)
                ),
                Periode(
                    fom = LocalDate.of(2019, 1, 1),
                    tom = LocalDate.of(2019, 1, 1)
                )
            )
        )
            .isTrue

        assertThat(
            DateUtil.overlapperPerioder(
                Periode(
                    fom = LocalDate.of(2019, 1, 10),
                    tom = LocalDate.of(2019, 1, 15)
                ),
                Periode(
                    fom = LocalDate.of(2019, 1, 1),
                    tom = LocalDate.of(2019, 1, 5)
                )
            )
        )
            .isFalse
    }
}
