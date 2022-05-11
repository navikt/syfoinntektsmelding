package no.nav.helsearbeidsgiver.db

interface Mapper {
    fun toObject() : Object
    fun toJson() : String
}
