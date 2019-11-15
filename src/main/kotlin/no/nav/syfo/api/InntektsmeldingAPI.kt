package no.nav.syfo.api

import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.repository.InntektsmeldingService
import org.springframework.beans.factory.annotation.Autowired
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

    @Autowired
    lateinit var inntektsmeldingService: InntektsmeldingService

    @PostMapping
    fun registrerInntektsmelding(@RequestBody request: InntektsmeldingRequest): ResponseEntity<String> {
        inntektsmeldingService.lagre("123", request.arkivId, "987")

            inntektsmeldingBehandler.behandle( request.arkivId, request.arkivReferanse )
        return ResponseEntity.status(HttpStatus.CREATED).body("Inntektsmelding mottatt.")
    }

}
