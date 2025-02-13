package no.nav.syfo.util

import no.nav.syfo.grunnleggendeInntektsmelding
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DokumentbeskrivelseUtilsTest {
    @Test
    fun `1 agp i dokumentbeskrivelse`() {
        assertThat(grunnleggendeInntektsmelding.tilDokumentbeskrivelse())
            .isEqualTo("Inntektsmelding-01.01.2019 - 01.02.2019")
    }

    @Test
    fun `2 agp i dokumentbeskrivelse`() {
        val periode = grunnleggendeInntektsmelding.arbeidsgiverperioder.first()
        assertThat(grunnleggendeInntektsmelding.copy(arbeidsgiverperioder = listOf(periode, periode)).tilDokumentbeskrivelse())
            .isEqualTo("Inntektsmelding-01.01.2019 - [...] - 01.02.2019")
    }

    @Test
    fun `ingen agp i dokumentbeskrivelse`() {
        assertThat(grunnleggendeInntektsmelding.copy(arbeidsgiverperioder = emptyList()).tilDokumentbeskrivelse())
            .isEqualTo("Inntektsmelding-(ingen agp)")
    }

    @Test
    fun `ingen orgnr i dokumentbeskrivelse`() {
        assertThat(grunnleggendeInntektsmelding.copy(arbeidsgiverOrgnummer = null).tilDokumentbeskrivelse())
            .isEqualTo("Inntektsmelding-01.01.2019 - 01.02.2019")
    }

    @Test
    fun `tom string orgnr i dokumentbeskrivelse`() {
        assertThat(grunnleggendeInntektsmelding.copy(arbeidsgiverOrgnummer = "").tilDokumentbeskrivelse())
            .isEqualTo("Inntektsmelding-01.01.2019 - 01.02.2019")
    }
}
