package no.nav.syfo.repository

import javax.sql.DataSource

interface DuplikatRepository {
    fun findByHendelsesId(value: String): Boolean
}

class DuplikatRepositoryImpl(private val ds: DataSource) : DuplikatRepository {

    override fun findByHendelsesId(value: String): Boolean {
        val sql = "select count(*) from bakgrunnsjobb where data ->> 'hendelsesId' = ?"
        var found: Boolean
        ds.connection.use {
            val res = it.prepareStatement(sql).apply {
                setString(1, value)
            }.executeQuery()
            res.next()
            found = res.getInt(1) > 0
        }
        return found
    }
}
