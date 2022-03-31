package no.nav.syfo.repository

import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

const val validIdentitetsnummer = "20015001543"
const val validOrgNr = "917404437"

fun getRandomNumber(fra: Int, til: Int) = (fra..til).shuffled().first()
fun getRandomFeiltype() = Feiltype.values().toList().shuffled().first()
fun getRandomTilstand() = Tilstand.values().toList().shuffled().first()
fun addLeadingZeroIfLessThanTen(num: Int): String = if (num < 10) String.format("%02d", num) else num.toString()

fun getRandomDate(before: LocalDateTime?): String {
    return if (before == null)
        "${(2000..LocalDate.now().year).shuffled().first()}-${addLeadingZeroIfLessThanTen((1..12).shuffled().first())}-${addLeadingZeroIfLessThanTen((1..28).shuffled().first())}"
    else
        "${(2000 until before.year).shuffled().first()}-${addLeadingZeroIfLessThanTen((1 until before.monthValue).shuffled().first())}-${addLeadingZeroIfLessThanTen((1 until before.dayOfMonth).shuffled().first())}"
}

fun getRandomTime(before: LocalDateTime?): String {
    return if (before == null)
        "${addLeadingZeroIfLessThanTen((0..24).shuffled().first())}:${addLeadingZeroIfLessThanTen((0..60).shuffled().first())}:${addLeadingZeroIfLessThanTen((0..60).shuffled().first())}"
    else
        "${addLeadingZeroIfLessThanTen((0 until before.hour).shuffled().first())}:${addLeadingZeroIfLessThanTen((0 until before.minute).shuffled().first())}:${addLeadingZeroIfLessThanTen((0 until before.second).shuffled().first())}"
}

fun getRandonDateTime(before: LocalDateTime?) = "${getRandomDate(before)}T${getRandomTime(before)}"

fun getRandonUtsattOppgaveEntitet(
    id: Int = getRandomNumber(100, 1000),
    inntektsmeldingId: String = "INNTEKTSMELDING_ID",
    arkivreferanse: String = "ARKIVREFERANSE",
    fnr: String = "FNR",
    aktørId: String = "AKTOR_ID",
    sakId: String = "SAK_ID",
    journalpostId: String = "JOURNALPOST_ID",
    timeout: LocalDateTime = LocalDateTime.parse(getRandonDateTime(null)),
    tilstand: Tilstand = getRandomTilstand()
): UtsattOppgaveEntitet {
    return UtsattOppgaveEntitet(
        id = id,
        inntektsmeldingId = inntektsmeldingId,
        arkivreferanse = arkivreferanse,
        fnr = fnr,
        aktørId = aktørId,
        sakId = sakId,
        journalpostId = journalpostId,
        timeout = timeout,
        tilstand = tilstand,
        gosysOppgaveId = null,
        oppdatert = null,
        speil = false,
        utbetalingBruker = null
    )
}

fun getRandonInntektsmeldingEntitet(
    uuid: String = UUID.randomUUID().toString(),
    aktorId: String = validIdentitetsnummer,
    sakId: String = getRandomNumber(1000, 5000).toString(),
    journalpostId: String = getRandomNumber(10, 50).toString(),
    orgnummer: String = validOrgNr,
    arbeidsgiverPrivat: String? = null,
    behandlet: LocalDateTime = LocalDateTime.parse(getRandonDateTime(null)),
    data: String? = null
): InntektsmeldingEntitet {
    return InntektsmeldingEntitet(
        uuid = uuid,
        aktorId = aktorId,
        sakId = sakId,
        journalpostId = journalpostId,
        orgnummer = orgnummer,
        arbeidsgiverPrivat = arbeidsgiverPrivat,
        behandlet = behandlet,
        data = data
    )
}
