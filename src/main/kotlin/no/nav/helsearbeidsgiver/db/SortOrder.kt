package no.nav.helsearbeidsgiver.db

class SortOrder(val column: String, val order: SortType) {
    fun toSQL(): String {
        return "$column ${order.sort}"
    }
}

enum class SortType(val sort: String) {
    Asc("asc"), Desc("desc")
}
