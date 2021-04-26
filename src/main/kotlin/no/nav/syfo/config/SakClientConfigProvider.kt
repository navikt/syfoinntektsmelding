package no.nav.syfo.config


data class SakClientConfig(val url: String, val tokenUrl: String, val username: String, val password: String)

class SakClientConfigProvider(
    private val url: String,
    private val tokenUrl: String,
    private val username: String,
    private val password: String
) {

    fun getSakClientConfig(): SakClientConfig {
        return SakClientConfig(url, tokenUrl, username, password)
    }
}
