package no.nav.syfo

import java.time.LocalDateTime

fun LocalDateTime?.isEqualNullSafe(other: LocalDateTime?): Boolean =
    when (listOfNotNull(this, other).size) {
        0 -> true
        1 -> false
        else -> this!!.isEqual(other)
    }
