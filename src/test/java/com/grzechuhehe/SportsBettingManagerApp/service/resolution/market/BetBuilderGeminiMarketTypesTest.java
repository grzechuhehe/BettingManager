package com.grzechuhehe.SportsBettingManagerApp.service.resolution.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionNameTranslator;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.DoublesNameNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BetBuilderGeminiMarketTypesTest {

    private BetBuilderMarketResolver resolver;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ResolutionNameTranslator translator = new ResolutionNameTranslator(new DoublesNameNormalizer());
        CompositeSelectionParser parser = new CompositeSelectionParser();
        StandardMarketResolver standard = new StandardMarketResolver(translator);
        HandicapMarketResolver handicap = new HandicapMarketResolver(translator);
        StatisticsMarketResolver statistics = new StatisticsMarketResolver(translator);
        objectMapper = new ObjectMapper();
        resolver = new BetBuilderMarketResolver(parser, standard, handicap, statistics, objectMapper);
    }

    @Test
    void loadsGeminiBuilderJsonWithoutFallback() throws Exception {
        String json = """
                [{"selection": "Senegal Over 1.5", "marketType": "TEAM_TOTAL_GOALS"},
                 {"selection": "Iraq Over 4.5", "marketType": "TEAM_TOTAL_SHOTS"}]
                """;
        Bet bet = Bet.builder().id(244L).selection("Bet Builder").builderConditionsJson(json).build();

        List<AtomicCondition> conditions = resolver.loadConditions(bet);

        assertEquals(2, conditions.size());
        assertEquals(MarketType.TEAM_TOTAL_GOALS, conditions.get(0).marketType());
        assertEquals(MarketType.TEAM_TOTAL_SHOTS, conditions.get(1).marketType());
    }

    @Test
    void resolvesDoubleChanceAndOverUnderFromGeminiJson() throws Exception {
        String json = """
                [{"selection": "Chorwacja lub remis", "marketType": "DOUBLE_CHANCE"},
                 {"line": "1.5", "selection": "Poniżej 1.5", "marketType": "OVER_UNDER"}]
                """;
        Bet bet = Bet.builder()
                .eventName("Chorwacja - Ghana")
                .selection("BetBuilder")
                .builderConditionsJson(json)
                .build();
        SofaScoreEventDto event = finished("Croatia", "Ghana", 1, 0);

        assertTrue(resolver.supports(bet));
        assertEquals(BetStatus.WON, resolver.resolve(bet, event).orElseThrow());
    }

    @Test
    void teamTotalGoalsConditionWon() throws Exception {
        String json = """
                [{"selection": "Senegal Over 1.5", "marketType": "TEAM_TOTAL_GOALS"},
                 {"selection": "YES", "marketType": "BOTH_TEAMS_TO_SCORE"}]
                """;
        Bet bet = Bet.builder()
                .eventName("Senegal - Iraq")
                .selection("Bet Builder")
                .builderConditionsJson(json)
                .build();
        SofaScoreEventDto event = finished("Senegal", "Iraq", 2, 1);

        assertEquals(BetStatus.WON, resolver.resolve(bet, event).orElseThrow());
    }

    private static SofaScoreEventDto finished(String home, String away, int homeScore, int awayScore) {
        SofaScoreEventDto e = new SofaScoreEventDto();
        e.setStatusType("finished");
        e.setHomeTeam(home);
        e.setAwayTeam(away);
        e.setHomeScore(homeScore);
        e.setAwayScore(awayScore);
        return e;
    }
}
