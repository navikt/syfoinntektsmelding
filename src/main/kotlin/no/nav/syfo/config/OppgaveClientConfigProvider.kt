package no.nav.syfo.config

data class OppgaveConfig(val url: String, val tokenUrl: String, val username: String, val password: String)

class OppgaveClientConfigProvider(private val url: String,
                                  private val tokenUrl: String,
                                  private val username: String,
                                  private val password: String){

    fun getOppgaveClientConfig(): OppgaveConfig {
        return OppgaveConfig(url, tokenUrl, username, password)
    }
}


