package no.nav.syfo.api

import no.nav.syfo.behandling.InntektsmeldingBehandler
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/inntektsmelding")
class InntektsmeldingAPI (
        val inntektsmeldingBehandler : InntektsmeldingBehandler
){

    @PostMapping
    fun create(@RequestBody request: InntektsmeldingRequest): ResponseEntity<String> {
        inntektsmeldingBehandler.behandle( request.arkivId, request.arkivReferanse )
        return ResponseEntity.status(HttpStatus.CREATED).body("Takker!")
    }

}
