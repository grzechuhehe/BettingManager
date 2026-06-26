package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BetBuilderMarketResolver implements MarketResolver {

    private final CompositeSelectionParser compositeSelectionParser;
    private final StandardMarketResolver standardMarketResolver;
    private final HandicapMarketResolver handicapMarketResolver;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(Bet bet) {
        if (bet.getBuilderConditionsJson() != null && !bet.getBuilderConditionsJson().isBlank()) {
            return !loadConditions(bet).isEmpty();
        }
        String selection = bet.getSelection() == null ? "" : bet.getSelection().toLowerCase(Locale.ROOT);
        if (!selection.contains("bet builder") && !selection.contains("betbuilder")) {
            return false;
        }
        return compositeSelectionParser.parse(bet.getSelection()).size() >= 2;
    }

    @Override
    public Optional<BetStatus> resolve(Bet bet, SofaScoreEventDto event) {
        List<AtomicCondition> conditions = loadConditions(bet);
        if (conditions.isEmpty()) {
            return Optional.empty();
        }
        for (AtomicCondition condition : conditions) {
            Bet synthetic = syntheticBet(bet, condition);
            Optional<BetStatus> result = resolveCondition(synthetic, event);
            if (result.isEmpty()) {
                return Optional.empty();
            }
            if (result.get() == BetStatus.LOST) {
                return Optional.of(BetStatus.LOST);
            }
            if (result.get() == BetStatus.VOID) {
                return Optional.empty();
            }
        }
        return Optional.of(BetStatus.WON);
    }

    List<AtomicCondition> loadConditions(Bet bet) {
        if (bet.getBuilderConditionsJson() != null && !bet.getBuilderConditionsJson().isBlank()) {
            try {
                return objectMapper.readValue(
                        bet.getBuilderConditionsJson(), new TypeReference<List<AtomicCondition>>() {});
            } catch (Exception e) {
                log.warn("Bet {}: invalid builderConditionsJson — fallback to parser", bet.getId());
            }
        }
        return compositeSelectionParser.parse(bet.getSelection());
    }

    private Bet syntheticBet(Bet parent, AtomicCondition condition) {
        return Bet.builder()
                .eventName(parent.getEventName())
                .sport(parent.getSport())
                .marketType(condition.marketType())
                .selection(condition.selection())
                .line(condition.line())
                .build();
    }

    private Optional<BetStatus> resolveCondition(Bet synthetic, SofaScoreEventDto event) {
        if (synthetic.getMarketType() == MarketType.HANDICAP
                || synthetic.getMarketType() == MarketType.ASIAN_HANDICAP) {
            return handicapMarketResolver.resolve(synthetic, event);
        }
        return standardMarketResolver.resolve(synthetic, event);
    }
}
