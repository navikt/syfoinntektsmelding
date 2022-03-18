package no.nav.syfo.repository

import javax.sql.DataSource

interface DuplikatRepository {
    fun findByHendelsesId(value: String): Boolean
}

class DuplikatRepositoryImpl(private val ds: DataSource) : DuplikatRepository {

    override fun findByHendelsesId(value: String): Boolean {
        val sql = "select * from bakgrunnsjobb where data ->> 'hendelsesId' = ? limit 1"
        var found: Boolean
        ds.connection.use {
            val res = it.prepareStatement(sql).apply {
                setString(1, value)
            }.executeQuery()
            found = res.next()
        }
        return found
    }
}
