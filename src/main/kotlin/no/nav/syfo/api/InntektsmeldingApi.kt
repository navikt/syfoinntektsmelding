package no.nav.syfo.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import no.nav.syfo.behandling.InntektsmeldingBehandler
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.ws.rs.Consumes
import javax.ws.rs.Produces

@Profile("local", "preprod-fss")
@RestController
@RequestMapping("/api/v1/inntektsmelding")
@Api(value="Inntektsmelding", description = "API for behandling av inntektsmelding")
class InntektsmeldingApi (
    val inntektsmeldingBehandler : InntektsmeldingBehandler
){

    @ApiOperation("Behandler en inntektsmelding som ligger i journal arkiv" )
    @ApiResponses(
        ApiResponse(code = 201, message = "Inntektsmeldingen ble behandlet")
    )
    @PostMapping("behandleJournal")
    @Produces(MediaType.TEXT_PLAIN_VALUE)
    @Consumes(MediaType.APPLICATION_JSON_VALUE)
    fun behandleInntektsmeldingJournal(
        @ApiParam("Referanser til journal arkiv og arkivreferansen", required = true)
        @RequestBody request: JournalInntektsmeldingRequest): ResponseEntity<Resultat> {
        val entitet = inntektsmeldingBehandler.behandle( request.arkivId, request.arkivReferanse )
        return ResponseEntity.status(HttpStatus.CREATED).body(Resultat(entitet!!.uuid))
    }

    @ApiOperation("Behandler en inntektsmelding uten å bruke journal arkiv")
    @ApiResponses(
        ApiResponse(code = 201, message = "Inntektsmeldingen ble behandlet")
    )
    @PostMapping("behandle")
    @Produces(MediaType.TEXT_PLAIN_VALUE)
    @Consumes(MediaType.APPLICATION_JSON_VALUE)
    fun behandleInntektsmelding(
        @ApiParam("Inntektsmelding")
        @RequestBody request: InntektsmeldingRequest): ResponseEntity<Resultat> {
        val entitet = inntektsmeldingBehandler.behandle( request.inntektsmelding.journalpostId, request.inntektsmelding.arkivRefereranse, request.inntektsmelding )
        return ResponseEntity.status(HttpStatus.CREATED).body(Resultat(entitet!!.uuid))
    }

}
