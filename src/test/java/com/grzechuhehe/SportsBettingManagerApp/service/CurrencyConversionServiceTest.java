package com.grzechuhehe.SportsBettingManagerApp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrencyConversionServiceTest {

    private CurrencyConversionService service;

    @BeforeEach
    void setUp() {
        service = CurrencyConversionService.forRates(Map.of(
                "PLN", new BigDecimal("1"),
                "USD", new BigDecimal("4.00"),
                "EUR", new BigDecimal("4.30")
        ));
    }

    @Test
    void convertsUsdToPln() {
        assertEquals(0, new BigDecimal("200.00").compareTo(
                service.convert(new BigDecimal("50"), "USD", "PLN")));
    }

    @Test
    void convertsPlnToUsd() {
        assertEquals(0, new BigDecimal("50.00").compareTo(
                service.convert(new BigDecimal("200"), "PLN", "USD")));
    }

    @Test
    void nullCurrencyTreatedAsPln() {
        assertEquals(0, new BigDecimal("100.00").compareTo(
                service.convert(new BigDecimal("100"), null, "PLN")));
    }

    @Test
    void sameCurrencyReturnsAmount() {
        assertEquals(0, new BigDecimal("75.50").compareTo(
                service.convert(new BigDecimal("75.50"), "EUR", "EUR")));
    }
}
