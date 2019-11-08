package no.nav.syfo.api

import no.nav.syfo.behandling.InntektsmeldingBehandler
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Profile("local")
@RestController
@RequestMapping("/api/v1/inntektsmelding")
class InntektsmeldingAPI (
        val inntektsmeldingBehandler : InntektsmeldingBehandler
){

    @PostMapping
    fun registrerInntektsmelding(@RequestBody request: InntektsmeldingRequest): ResponseEntity<String> {
        inntektsmeldingBehandler.behandle( request.arkivId, request.arkivReferanse )
        return ResponseEntity.status(HttpStatus.CREATED).body("Inntektsmelding mottatt.")
    }

}
