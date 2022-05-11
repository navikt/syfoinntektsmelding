package no.nav.helsearbeidsgiver.db

enum class MatchType(val sql: String) {
    Eq("="),
    Ne("!="),
    Gt(">"),
    Lt("<")
}

class ColumnMatch(val column: String, val type: MatchType, val value: Any){
    fun toSQL(columnName: String): String {
        if (column.startsWith("/")){
            val arr = column.substring(1).split("/")
            val buf = StringBuffer()
            buf.append(columnName)
            arr.forEachIndexed { index, s ->
                if (index == arr.size - 1) {
                    buf.append(" ->> '$s' = ?")
                } else {
                    buf.append(" -> '$s'")
                }
            }
            return buf.toString()
        } else {
            return "$column$type?"
        }
    }
}
