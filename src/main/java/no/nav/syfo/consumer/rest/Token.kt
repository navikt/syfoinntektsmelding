package no.nav.syfo.consumer.rest

data class Token (val access_token: String, val token_type: String, var expires_in :Int = 0)

