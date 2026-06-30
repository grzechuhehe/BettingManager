package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.DoublesNameNormalizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResolutionNameTranslatorTest {

    private final ResolutionNameTranslator translator = new ResolutionNameTranslator(new DoublesNameNormalizer());

    @Test
    void shouldBuildEnglishSearchQueryFromPolishAbbreviations() {
        assertEquals(
                "South Africa South Korea",
                translator.toSearchQuery("RPA - Korea Płd."));
    }

    @Test
    void shouldTranslateNationalTeamNames() {
        assertEquals("Germany Norway", translator.toSearchQuery("Niemcy - Norwegia"));
        assertEquals("Ecuador Curacao", translator.toSearchQuery("Ekwador - Curacao"));
    }

    @Test
    void shouldAddEnglishTokensForMatching() {
        var base = new java.util.HashSet<>(java.util.Set.of("rpa", "korea", "pld"));
        var expanded = translator.matchingTokens("RPA - Korea Płd.", base);
        assertTrue(expanded.contains("south"));
        assertTrue(expanded.contains("africa"));
        assertTrue(expanded.contains("korea"));
    }

    @Test
    void shouldRejectSingleWordAbbreviation() {
        assertFalse(translator.isSearchableEventName("ucl"));
        assertFalse(translator.isSearchableEventName("ucll"));
    }

    @Test
    void shouldAcceptMatchWithSeparator() {
        assertTrue(translator.isSearchableEventName("RPA - Korea Płd."));
    }

    @Test
    void shouldRejectParlayAndMultiEventNames() {
        assertFalse(translator.isSearchableEventName("Mistrzostwa Świata w Hokeju - Kupon Akumulacyjny"));
        assertFalse(translator.isSearchableEventName("Alaves - Vallecano; Norwegia - Szwecja"));
        assertFalse(translator.isSearchableEventName("Real Madryt - Athletic Bilbao | Ajax - Utrecht"));
        assertFalse(translator.isSearchableEventName("1. VfL Wolfsburg - SC Paderborn; 2. Darderi, Luciano"));
        assertFalse(translator.isSearchableEventName("AKO (2): Mlada Boleslav U19 vs Zbrojovka Brno U19 & Mushuc Runa vs Guayaquil City"));
        assertFalse(translator.isSearchableEventName("Kl. końcowa - Mistrzostwa Świata 2026"));
    }

    @Test
    void shouldRejectUnknownShortAbbreviation() {
        assertTrue(translator.resolveQueryForApify("Niemcy - Norwegia").isPresent());
        assertTrue(translator.resolveQueryForApify("Niemcy - WKS").isEmpty());
    }

    @Test
    void shouldTranslateWorldCupNationalTeamAliases() {
        ResolutionNameTranslator translator = new ResolutionNameTranslator(new DoublesNameNormalizer());
        assertEquals("Cape Verde", translator.translateSegment("Republika Zielonego Przylądka"));
        assertEquals("DR Congo", translator.translateSegment("DR Kongo"));
        assertEquals("Algeria", translator.translateSegment("Algieria"));
        assertEquals("New Zealand", translator.translateSegment("Nowa Zelandia"));
        assertEquals("Senegal", translator.translateSegment("Senegal"));
        assertEquals("Austria", translator.translateSegment("Austria"));
        assertEquals("Dominican Republic", translator.translateSegment("Dominikana"));
        assertEquals("Bulgaria", translator.translateSegment("Bułgaria"));
    }

    @Test
    void shouldAllowFuzzyClubNamesWithoutFullTranslation() {
        assertEquals(
                "Brighton Manchester United",
                translator.resolveQueryForApify("Brighton - Manchester United").orElseThrow());
        assertEquals(
                "Jil Teichmann Petra Marcinko",
                translator.resolveQueryForApify("Jil Teichmann - Petra Marcinko").orElseThrow());
    }
}
