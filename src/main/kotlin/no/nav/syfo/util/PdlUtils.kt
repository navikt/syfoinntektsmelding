package no.nav.syfo.util

import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlIdent

fun PdlClient.getAktørid(fnr: String): String? = this.fullPerson(fnr)?.hentIdenter?.trekkUtIdent(PdlIdent.PdlIdentGruppe.AKTORID)
