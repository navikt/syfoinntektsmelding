package no.nav.syfo.consumer.rest.aktor

data class Ident (
    var ident: String? = null,
    var identgruppe: String? = null,
    var gjeldende: Boolean? = null,
)
