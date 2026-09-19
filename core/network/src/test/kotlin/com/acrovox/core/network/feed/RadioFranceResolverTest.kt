package com.acrovox.core.network.feed

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RadioFranceResolverTest {
    @Test
    fun radioFrancePageUrl() {
        assertThat(RadioFranceResolver.pageUrl("Affaires étrangères", "France Culture"))
            .isEqualTo("https://www.radiofrance.fr/franceculture/podcasts/affaires-etrangeres")
        assertThat(RadioFranceResolver.pageUrl("L'Heure bleue", "France Inter"))
            .isEqualTo("https://www.radiofrance.fr/franceinter/podcasts/l-heure-bleue")
        assertThat(RadioFranceResolver.pageUrl("Underscore_", "Micode")).isNull()
    }
}
