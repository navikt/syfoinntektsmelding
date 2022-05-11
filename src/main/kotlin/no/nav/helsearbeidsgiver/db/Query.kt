package no.nav.helsearbeidsgiver.db

class Query(val klass: Class<*>) {
    private var cols: MutableList<ColumnMatch> = mutableListOf()
    private var sort: MutableList<SortOrder> = mutableListOf()
    private var start: Int = 0
    private var limit: Int = 0

    fun sortAsc(path: String) : Query {
        this.sort.add(SortOrder(path, SortType.Asc))
        return this
    }

    fun add(path: String, type: MatchType, value: Any): Query {
        this.cols.add(ColumnMatch(path, type, value))
        return this
    }

    fun eq(path: String, value: Any): Query {
        return add(path, MatchType.Eq, value)
    }
    fun ne(path: String, value: Any): Query {
        return add(path, MatchType.Ne, value)
    }
    fun gt(path: String, value: Any): Query {
        return add(path, MatchType.Gt, value)
    }
    fun lt(path: String, value: Any): Query {
        return add(path, MatchType.Lt, value)
    }

    fun start(index: Int): Query {
        this.start = index
        return this
    }

    fun limit(limit: Int): Query {
        this.limit = limit
        return this
    }

    private fun getJsonColumn(): String {
        return "data"
    }

    fun toSQL(count: Boolean = false): String {
        val sql = StringBuffer(2000)
        sql.append("select * from ${getTableName()}")
        if (!cols.isEmpty()) {
            sql.append(" where")
            cols.forEach {
                sql.append(" " + it.toSQL(getJsonColumn()))
            }
        }
        if (start > 0) {
            sql.append(" offset $start")
        }
        if (limit > 0) {
            sql.append(" limit $limit")
        }
        if (!sort.isEmpty()) {
            sql.append(" order by")
            sort.forEach {
                sql.append(" " + it.toSQL())
            }
        }
        return sql.toString()
    }

    fun getTableName(): String {
        return getTable(klass)!!.table
    }
}


