package no.nav.helsearbeidsgiver.db

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import javax.sql.DataSource

@Retention(RetentionPolicy.RUNTIME)
annotation class Table(val table: String)

@Retention(RetentionPolicy.RUNTIME)
annotation class IdColumn(val column: String)

@Retention(RetentionPolicy.RUNTIME)
annotation class ValueColumn(val column: String)

@Retention(RetentionPolicy.RUNTIME)
annotation class JsonColumn(val column: String)

class Db(val ds: DataSource, val mapper: Mapper) {

    fun findBySql(sql: String): List<Inntekt> {
        return listOf()
    }

    fun count(query: Query) : Long {
        val sql = query.toSQL(true)
        ds.connection.prepareStatement(sql).use { st ->
            //st.setObject(1, getPrimaryKeyValue(instance))
            st.executeUpdate()
        }
        return 15
    }

    fun find(query: Query): List<Inntekt> {
        return listOf()
    }

    fun find(uuid: String, klass: Class<Inntekt>): Inntekt? {
        return null
    }

    fun add(inntekt: Inntekt) {
    }

    fun update(inntekt: Inntekt) {
    }

    fun remove(inntekt: Inntekt) {
        val sql = "DELETE FROM ? WHERE ID=?"
        ds.connection.prepareStatement(sql).use { st ->
            st.setObject(1, getTable(inntekt.javaClass))
            st.setObject(2, )
            st.executeUpdate()
        }
    }
}

fun getTable(klass: Class<*>): Table? {
    if (klass.isAnnotationPresent(Table::class.java)) {
        return klass.getAnnotation(Table::class.java) as Table
    }
    return null
}

fun getIdColumn(klass: Class<*>): Table? {
    if (klass.isAnnotationPresent(Table::class.java)) {
        return klass.getAnnotation(Table::class.java) as Table
    }
    return null
}
