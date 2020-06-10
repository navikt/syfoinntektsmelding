package no.nav.syfo.kafkamottak

import no.nav.syfo.behandling.Feiltype

class InntektsmeldingConsumerException(ar:String, exception:Exception, feiltype: Feiltype) : RuntimeException(exception) {

}
