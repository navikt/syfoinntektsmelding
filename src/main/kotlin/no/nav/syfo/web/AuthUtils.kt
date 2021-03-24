package no.nav.syfo.web.auth

import io.ktor.config.*
import io.ktor.request.*
import io.ktor.util.*
import no.nav.security.token.support.core.jwt.JwtToken
import java.time.Instant
import java.util.*

//TODO: Sjekk om dette er noe vi trenger, kopiert fra fritakagp
@KtorExperimentalAPI
fun hentIdentitetsnummerFraLoginToken(config: ApplicationConfig, request: ApplicationRequest): String {
    val tokenString = getTokenString(config, request)
    return JwtToken(tokenString).subject
}

@KtorExperimentalAPI
fun hentUtløpsdatoFraLoginToken(config: ApplicationConfig, request: ApplicationRequest): Date {
    val tokenString = getTokenString(config, request)
    return JwtToken(tokenString).jwtTokenClaims.expirationTime ?: Date.from(Instant.MIN)
}

private fun getTokenString(config: ApplicationConfig, request: ApplicationRequest): String {
    val cookieName = config.configList("no.nav.security.jwt.issuers")[0].property("cookie_name").getString()

    return request.cookies[cookieName]
        ?: request.headers["Authorization"]?.replaceFirst("Bearer ", "")
        ?: throw IllegalAccessException("Du må angi et identitetstoken som cookieen $cookieName eller i Authorization-headeren")
}
