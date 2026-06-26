package com.grzechuhehe.SportsBettingManagerApp.service.resolution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.market.*;

public final class ResolutionTestFixtures {

    private ResolutionTestFixtures() {}

    public static ResolutionComponents components() {
        ResolutionNameTranslator nameTranslator = new ResolutionNameTranslator();
        CompositeSelectionParser parser = new CompositeSelectionParser(nameTranslator);
        StandardMarketResolver standard = new StandardMarketResolver(nameTranslator);
        HandicapMarketResolver handicap = new HandicapMarketResolver(nameTranslator);
        ObjectMapper objectMapper = new ObjectMapper();
        BetBuilderMarketResolver betBuilder =
                new BetBuilderMarketResolver(parser, standard, handicap, objectMapper);
        MarketResolverRegistry registry = new MarketResolverRegistry(betBuilder, handicap, standard);
        BetOutcomeEvaluator evaluator = new BetOutcomeEvaluator(registry);
        SelectionResolvabilityChecker resolvabilityChecker =
                new SelectionResolvabilityChecker(betBuilder, handicap);
        BetMatcher matcher = new BetMatcher(nameTranslator);
        return new ResolutionComponents(
                nameTranslator, parser, handicap, betBuilder, evaluator, resolvabilityChecker, matcher);
    }

    public static BetResolutionTransactionService transactionService(BetRepository betRepository) {
        ResolutionComponents c = components();
        return new BetResolutionTransactionService(
                betRepository,
                c.matcher(),
                c.evaluator(),
                c.nameTranslator(),
                c.resolvabilityChecker());
    }

    public record ResolutionComponents(
            ResolutionNameTranslator nameTranslator,
            CompositeSelectionParser parser,
            HandicapMarketResolver handicap,
            BetBuilderMarketResolver betBuilder,
            BetOutcomeEvaluator evaluator,
            SelectionResolvabilityChecker resolvabilityChecker,
            BetMatcher matcher) {}
}
