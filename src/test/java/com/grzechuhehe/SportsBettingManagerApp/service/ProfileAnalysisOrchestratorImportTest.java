package com.grzechuhehe.SportsBettingManagerApp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.integration.GeminiVisionClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.SocialDataClient;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.importing.BetImportResolutionEnricher;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.importing.MarketTypeInferrer;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.market.CompositeSelectionParser;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.ResolutionNameTranslator;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.DoublesNameNormalizer;
import com.grzechuhehe.SportsBettingManagerApp.service.resolution.matching.TennisNameNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileAnalysisOrchestratorImportTest {

    @Mock private UserRepository userRepository;
    @Mock private BetRepository betRepository;
    @Mock private SocialDataClient socialDataClient;
    @Mock private GeminiVisionClient geminiVisionClient;
    @Mock private ImageStorageService imageStorageService;

    private ProfileAnalysisOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        ResolutionNameTranslator nameTranslator = new ResolutionNameTranslator(new DoublesNameNormalizer(), new TennisNameNormalizer());
        BetImportResolutionEnricher enricher = new BetImportResolutionEnricher(
                new CompositeSelectionParser(),
                objectMapper,
                new MarketTypeInferrer(nameTranslator));
        orchestrator = new ProfileAnalysisOrchestrator(
                userRepository, betRepository, socialDataClient,
                geminiVisionClient, imageStorageService, objectMapper, enricher);
    }

    @Test
    void importBuildsAndSavesBetFromGeminiJson() {
        String json = "{\"isPlacedBet\":true,\"eventName\":\"Real Madrid - Barcelona\","
                + "\"selection\":\"Over 2.5\",\"odds\":1.85,\"stake\":50,"
                + "\"sport\":\"Football\",\"marketType\":\"TOTALS_OVER_UNDER\",\"status\":\"PENDING\"}";
        when(geminiVisionClient.analyzeBet(any(), anyList())).thenReturn(json);
        when(betRepository.save(any(Bet.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = new User();
        Optional<Bet> result = orchestrator.importBetFromImages(
                user, List.of("/images/profiles/manual/john/abc.png"), null);

        assertTrue(result.isPresent());
        Bet bet = result.get();
        assertEquals("Real Madrid - Barcelona", bet.getEventName());
        assertEquals(0, new java.math.BigDecimal("1.85").compareTo(bet.getOdds()));
        assertTrue(bet.isAiExtracted());
        assertEquals("/images/profiles/manual/john/abc.png", bet.getImageProofPath());
        assertEquals(BetStatus.PENDING, bet.getStatus());
        assertSame(user, bet.getUser());
    }

    @Test
    void importReturnsEmptyWhenExtractionInvalid() {
        String json = "{\"isPlacedBet\":true,\"selection\":\"???\"}";
        when(geminiVisionClient.analyzeBet(any(), anyList())).thenReturn(json);

        Optional<Bet> result = orchestrator.importBetFromImages(
                new User(), List.of("/images/profiles/manual/john/abc.png"), null);

        assertTrue(result.isEmpty());
    }

    @Test
    void importAppliesDefaultsWhenAiOmitsOptionalFields() {
        String json = "{\"isPlacedBet\":true,\"eventName\":\"Team A - Team B\","
                + "\"selection\":\"Team A\",\"odds\":2.10}";
        when(geminiVisionClient.analyzeBet(any(), anyList())).thenReturn(json);
        when(betRepository.save(any(Bet.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Bet> result = orchestrator.importBetFromImages(
                new User(), List.of("/images/profiles/manual/john/abc.png"), null);

        assertTrue(result.isPresent());
        Bet bet = result.get();
        assertNotNull(bet.getEventDate());
        assertEquals(MarketType.MONEYLINE_1X2, bet.getMarketType());
        assertEquals("Unknown", bet.getBookmaker());
        assertEquals("Other", bet.getSport());
        assertEquals(0, new BigDecimal("10").compareTo(bet.getStake()));
    }

    @Test
    void importStoresRootBuilderConditionsForSingleBet() {
        String json = "{\"isPlacedBet\":true,\"eventName\":\"Maroko - Norwegia\","
                + "\"selection\":\"BetBuilder: over 1.5\",\"odds\":2.5,\"status\":\"PENDING\","
                + "\"builderConditions\":["
                + "{\"marketType\":\"TOTALS_OVER_UNDER\",\"selection\":\"over 1.5\",\"line\":\"1.5\"}"
                + "]}";
        when(geminiVisionClient.analyzeBet(any(), anyList())).thenReturn(json);
        when(betRepository.save(any(Bet.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Bet> result = orchestrator.importBetFromImages(
                new User(), List.of("/images/profiles/manual/john/bb.png"), null);

        assertTrue(result.isPresent());
        assertNotNull(result.get().getBuilderConditionsJson());
        assertTrue(result.get().getBuilderConditionsJson().contains("TOTALS_OVER_UNDER"));
    }

    @Test
    void importFromGeminiWithWonStatusMarksRetroactiveAtImport() {
        String json = "{\"isPlacedBet\":true,\"eventName\":\"Team A - Team B\","
                + "\"selection\":\"1\",\"odds\":2.0,\"status\":\"WON\"}";
        when(geminiVisionClient.analyzeBet(any(), anyList())).thenReturn(json);
        when(betRepository.save(any(Bet.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Bet> result = orchestrator.importBetFromImages(
                new User(), List.of("/images/profiles/manual/john/won.png"), null);

        assertTrue(result.isPresent());
        Bet bet = result.get();
        assertTrue(bet.isRetroactiveAtImport());
        assertFalse(bet.isPreMatch());
        assertEquals(BetStatus.WON, bet.getStatus());
    }

    @Test
    void importFromGeminiWithPendingStatusIsNotRetroactive() {
        String json = "{\"isPlacedBet\":true,\"eventName\":\"Team A - Team B\","
                + "\"selection\":\"1\",\"odds\":2.0,\"status\":\"PENDING\"}";
        when(geminiVisionClient.analyzeBet(any(), anyList())).thenReturn(json);
        when(betRepository.save(any(Bet.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Bet> result = orchestrator.importBetFromImages(
                new User(), List.of("/images/profiles/manual/john/pending.png"), null);

        assertTrue(result.isPresent());
        Bet bet = result.get();
        assertFalse(bet.isRetroactiveAtImport());
        assertTrue(bet.isPreMatch());
    }

    @Test
    void importAcceptsManualUploadWhenGeminiMarksIsPlacedBetFalse() {
        String json = "{\"isPlacedBet\":false,\"eventName\":\"Norwegia - Francja\","
                + "\"selection\":\"Francja wygra mecz i Francja wykona więcej rz.rożnych\","
                + "\"odds\":3.25,\"stake\":0,\"status\":\"PENDING\","
                + "\"marketType\":\"BET_BUILDER\",\"bookmaker\":\"Superbet\","
                + "\"legs\":["
                + "{\"eventName\":\"Norwegia - Francja\",\"marketType\":\"MATCH_ODDS\",\"selection\":\"Francja wygra mecz\"},"
                + "{\"eventName\":\"Norwegia - Francja\",\"marketType\":\"TEAM_CORNERS\",\"selection\":\"Francja wykona więcej rz.rożnych\"}"
                + "]}";
        when(geminiVisionClient.analyzeBet(any(), anyList())).thenReturn(json);
        when(betRepository.save(any(Bet.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Bet> result = orchestrator.importBetFromImages(
                new User(), List.of("/images/profiles/manual/grz/slip.png"), null);

        assertTrue(result.isPresent());
        Bet bet = result.get();
        assertEquals("Norwegia - Francja", bet.getEventName());
        assertEquals(BetType.SINGLE, bet.getBetType());
        assertNotNull(bet.getBuilderConditionsJson());
        assertTrue(bet.getBuilderConditionsJson().contains("MATCH_ODDS"));
        assertEquals(0, new java.math.BigDecimal("3.25").compareTo(bet.getOdds()));
        assertEquals(0, new java.math.BigDecimal("10").compareTo(bet.getStake()));
        assertEquals(0, BigDecimal.ONE.compareTo(bet.getUnits()));
    }
}
