package no.nav.syfo

import java.time.LocalDateTime

fun LocalDateTime?.isEqualNullSafe(other: LocalDateTime?): Boolean =
    when {
        this == null && other == null -> true
        this == null || other == null -> false
        else -> this.isEqual(other)
    }
