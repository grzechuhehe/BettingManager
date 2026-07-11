package com.grzechuhehe.SportsBettingManagerApp.service.resolution.importing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BetStakeExtractionNormalizerTest {

    private BetStakeExtractionNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new BetStakeExtractionNormalizer(new BigDecimal("0.88"));
    }

    @Test
    void usdStake_keepsCurrencyAndDerivesUnits() {
        BetStakeNormalizationResult result = normalizer.normalize(
                new BigDecimal("50"), null, "USD", null, "DraftKings", new BigDecimal("2.00"));

        assertEquals(0, new BigDecimal("50").compareTo(result.stake()));
        assertEquals("USD", result.currency());
        assertEquals(0, new BigDecimal("5.00").compareTo(result.units()));
    }

    @Test
    void plnStake_setsUnitsFromTenPlnPerUnit() {
        BetStakeNormalizationResult result = normalizer.normalize(
                new BigDecimal("50"), null, "PLN", null, "STS", new BigDecimal("1.85"));

        assertEquals(0, new BigDecimal("50").compareTo(result.stake()));
        assertEquals("PLN", result.currency());
        assertEquals(0, new BigDecimal("5.00").compareTo(result.units()));
    }

    @Test
    void polishBookmaker_defaultsCurrencyToPln() {
        BetStakeNormalizationResult result = normalizer.normalize(
                new BigDecimal("50"), null, null, null, "Fortuna", new BigDecimal("2.00"));

        assertEquals("PLN", result.currency());
    }

    @Test
    void potentialWinMatchingStake_derivesStakeFromGrossWin() {
        BetStakeNormalizationResult result = normalizer.normalize(
                new BigDecimal("120"), null, "PLN", new BigDecimal("120"), "STS", new BigDecimal("2.40"));

        assertEquals(0, new BigDecimal("50").compareTo(result.stake()));
        assertEquals(0, new BigDecimal("5.00").compareTo(result.units()));
    }

    @Test
    void polishNetPotentialWin_derivesStakeFromNetWin() {
        BetStakeNormalizationResult result = normalizer.normalize(
                null, null, null, new BigDecimal("105.60"), "Superbet", new BigDecimal("2.40"));

        assertEquals(0, new BigDecimal("50.00").compareTo(result.stake()));
        assertEquals("PLN", result.currency());
        assertEquals(0, new BigDecimal("5.00").compareTo(result.units()));
    }

    @Test
    void explicitUnits_only_setsStakeFromUnits() {
        BetStakeNormalizationResult result = normalizer.normalize(
                null, new BigDecimal("3"), null, null, null, new BigDecimal("2.10"));

        assertEquals(0, new BigDecimal("30").compareTo(result.stake()));
        assertEquals("PLN", result.currency());
        assertEquals(0, new BigDecimal("3").compareTo(result.units()));
    }

    @Test
    void defaultsToOneUnitWhenNothingExtracted() {
        BetStakeNormalizationResult result = normalizer.normalize(
                null, null, null, null, null, new BigDecimal("2.00"));

        assertEquals(0, new BigDecimal("10").compareTo(result.stake()));
        assertEquals("PLN", result.currency());
        assertEquals(0, BigDecimal.ONE.compareTo(result.units()));
    }
}
