package com.grzechuhehe.SportsBettingManagerApp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

@Service
public class CurrencyConversionService {

    private static final String DEFAULT_CURRENCY = "PLN";

    private final Map<String, BigDecimal> plnPerUnit;

    @Autowired
    public CurrencyConversionService(
            @Value("${bet.currency.rate.PLN:1}") BigDecimal plnPerPln,
            @Value("${bet.currency.rate.USD:4.05}") BigDecimal plnPerUsd,
            @Value("${bet.currency.rate.EUR:4.30}") BigDecimal plnPerEur,
            @Value("${bet.currency.rate.GBP:5.10}") BigDecimal plnPerGbp) {
        this.plnPerUnit = Map.of(
                "PLN", plnPerPln,
                "USD", plnPerUsd,
                "EUR", plnPerEur,
                "GBP", plnPerGbp
        );
    }

    static CurrencyConversionService forRates(Map<String, BigDecimal> rates) {
        return new CurrencyConversionService(rates);
    }

    private CurrencyConversionService(Map<String, BigDecimal> plnPerUnit) {
        this.plnPerUnit = plnPerUnit;
    }

    public String resolveDisplayCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        return plnPerUnit.containsKey(normalized) ? normalized : DEFAULT_CURRENCY;
    }

    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        String from = resolveDisplayCurrency(fromCurrency);
        String to = resolveDisplayCurrency(toCurrency);
        if (from.equals(to)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal inPln = amount.multiply(rateFor(from));
        return inPln.divide(rateFor(to), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal rateFor(String currency) {
        return plnPerUnit.getOrDefault(currency, BigDecimal.ONE);
    }
}
