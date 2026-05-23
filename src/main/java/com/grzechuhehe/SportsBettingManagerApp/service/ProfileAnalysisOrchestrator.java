package com.grzechuhehe.SportsBettingManagerApp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.integration.GeminiVisionClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.SocialDataClient;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.OddsType;
import com.grzechuhehe.SportsBettingManagerApp.repository.BetRepository;
import com.grzechuhehe.SportsBettingManagerApp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileAnalysisOrchestrator {

    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final SocialDataClient socialDataClient;
    private final GeminiVisionClient geminiVisionClient;
    private final ImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;

    // Uruchamia się co 15 minut
    @Scheduled(fixedRate = 900000)
    @Transactional
    public void processTrackedProfiles() {
        log.info("Rozpoczynam analizę profili z X (SocialData + Gemini)...");
        
        // Szukamy użytkowników, którzy mają xUsername i nie byli sprawdzani od ponad godziny
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        List<User> profilesToUpdate = userRepository.findProfilesToUpdate(threshold);

        for (User user : profilesToUpdate) {
            try {
                analyzeProfile(user);
                user.setLastXCheckAt(LocalDateTime.now());
                userRepository.save(user);
            } catch (Exception e) {
                log.error("Błąd podczas analizy profilu {} : {}", user.getXUsername(), e.getMessage());
            }
        }
    }

    private void analyzeProfile(User user) {
        String xUsername = user.getXUsername();
        List<Map<String, Object>> recentTweets = socialDataClient.fetchRecentTweets(xUsername);

        for (Map<String, Object> tweet : recentTweets) {
            String tweetId = (String) tweet.get("id_str");
            String fullText = (String) tweet.get("full_text");
            
            // Ignoruj odpowiedzi do INNYCH użytkowników (jeśli in_reply_to_screen_name istnieje i nie jest to sam "guru")
            String inReplyTo = (String) tweet.get("in_reply_to_screen_name");
            if (inReplyTo != null && !inReplyTo.equalsIgnoreCase(xUsername)) {
                continue;
            }

            Optional<Bet> existingBetOpt = betRepository.findBySourcePostId(tweetId);
            
            // Szukamy zdjęć
            List<String> imageUrls = extractAllImageUrlsFromTweet(tweet);
            List<String> localImagePaths = new ArrayList<>();
            for (String url : imageUrls) {
                String path = imageStorageService.downloadAndSaveImage(url, xUsername);
                if (path != null) localImagePaths.add(path);
            }

            if (existingBetOpt.isPresent()) {
                Bet existingBet = existingBetOpt.get();
                if (!localImagePaths.isEmpty() && existingBet.getImageProofPath() == null) {
                    log.info("Wykryto dodane zdjęcia do istniejącego posta {} od {}", tweetId, xUsername);
                    existingBet.setImageProofPath(localImagePaths.get(0)); // Uproszczenie: zapisujemy ścieżkę pierwszego
                    
                    updateBetDataFromAI(existingBet, fullText, localImagePaths);
                    
                    // Defense-in-depth: Validate before saving to prevent rollback
                    if (existingBet.getOdds() != null) {
                        betRepository.save(existingBet);
                    } else {
                        log.warn("Pomijam zapis zaktualizowanego zakładu dla {} - AI nie zwróciło kursu (odds)", xUsername);
                    }
                }
                continue;
            }

            Bet newBet = Bet.builder()
                    .user(user)
                    .sourcePostId(tweetId)
                    .imageProofPath(localImagePaths.isEmpty() ? null : localImagePaths.get(0))
                    .isAiExtracted(true)
                    .betType(BetType.SINGLE)
                    .status(BetStatus.PENDING)
                    .placedAt(LocalDateTime.now())
                    .build();

            updateBetDataFromAI(newBet, fullText, localImagePaths);
            
            // Defense-in-depth: Validate before saving
            if (newBet.getOdds() != null && newBet.getEventName() != null) {
                 betRepository.save(newBet);
                 log.info("Zapisano nowy zakład wyciągnięty przez AI dla użytkownika {}", xUsername);
            } else {
                 log.warn("Odrzucono nowy zakład z posta {} dla {} - brak wymaganych danych od AI (odds: {}, event: {})", 
                          tweetId, xUsername, newBet.getOdds(), newBet.getEventName());
            }
        }
    }

    private void updateBetDataFromAI(Bet bet, String text, List<String> imagePaths) {
        String aiResponseJson = geminiVisionClient.analyzeBet(text, imagePaths);
        
        if (aiResponseJson != null && !aiResponseJson.isEmpty()) {
            try {
                // Usuwamy ewentualne formatowanie markdown od Gemini
                String cleanJson = aiResponseJson.replaceAll("```json", "").replaceAll("```", "").trim();
                JsonNode jsonNode = objectMapper.readTree(cleanJson);

                if (jsonNode.has("eventName") && !jsonNode.get("eventName").isNull()) bet.setEventName(jsonNode.get("eventName").asText());
                if (jsonNode.has("selection") && !jsonNode.get("selection").isNull()) bet.setSelection(jsonNode.get("selection").asText());
                if (jsonNode.has("bookmaker") && !jsonNode.get("bookmaker").isNull()) bet.setBookmaker(jsonNode.get("bookmaker").asText());
                if (jsonNode.has("sport") && !jsonNode.get("sport").isNull()) bet.setSport(jsonNode.get("sport").asText());
                if (jsonNode.has("marketType") && !jsonNode.get("marketType").isNull()) {
                    try {
                        bet.setMarketType(com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType.valueOf(jsonNode.get("marketType").asText().toUpperCase()));
                    } catch (IllegalArgumentException ignored) {}
                }
                
                if (jsonNode.has("oddsType") && !jsonNode.get("oddsType").isNull()) {
                    try {
                        bet.setOddsType(OddsType.valueOf(jsonNode.get("oddsType").asText().toUpperCase()));
                    } catch (IllegalArgumentException ignored) {}
                }
                
                if (jsonNode.has("status") && !jsonNode.get("status").isNull()) {
                    try {
                        bet.setStatus(BetStatus.valueOf(jsonNode.get("status").asText().toUpperCase()));
                    } catch (IllegalArgumentException ignored) {}
                }
                
                if (jsonNode.has("odds") && !jsonNode.get("odds").isNull()) {
                    bet.setOdds(new BigDecimal(jsonNode.get("odds").asText()));
                }
                
                if (jsonNode.has("units") && !jsonNode.get("units").isNull()) {
                    bet.setUnits(new BigDecimal(jsonNode.get("units").asText()));
                } else {
                    bet.setUnits(BigDecimal.ONE); // Domyślnie 1u
                }
                
                if (jsonNode.has("stake") && !jsonNode.get("stake").isNull()) {
                    bet.setStake(new BigDecimal(jsonNode.get("stake").asText()));
                } else {
                    bet.setStake(bet.getUnits()); // Fallback dla wyliczeń, jeśli nie ma jawnej waluty
                }
                
                bet.calculatePotentialWinnings();
                
                // Obsługa Parlay (AKO) z nowej struktury JSON
                if (jsonNode.has("legs") && jsonNode.get("legs").isArray() && jsonNode.get("legs").size() > 0) {
                    bet.setBetType(BetType.PARLAY);
                    if (bet.getEventName() == null || bet.getEventName().isEmpty() || bet.getEventName().equals("null")) {
                        bet.setEventName("Parlay Bet (" + jsonNode.get("legs").size() + " legs)");
                    }
                    if (bet.getSelection() == null || bet.getSelection().isEmpty() || bet.getSelection().equals("null")) {
                        bet.setSelection("Multiple Selections");
                    }
                    
                    java.util.Set<Bet> childBets = new java.util.HashSet<>();
                    for (JsonNode legNode : jsonNode.get("legs")) {
                        Bet childBet = Bet.builder()
                            .user(bet.getUser())
                            .betType(BetType.SINGLE)
                            .status(bet.getStatus()) // Dziedziczy status po rodzicu na etapie tworzenia
                            .placedAt(bet.getPlacedAt())
                            .parentBet(bet)
                            .build();
                            
                        if (legNode.has("eventName") && !legNode.get("eventName").isNull()) childBet.setEventName(legNode.get("eventName").asText());
                        if (legNode.has("selection") && !legNode.get("selection").isNull()) childBet.setSelection(legNode.get("selection").asText());
                        if (legNode.has("odds") && !legNode.get("odds").isNull()) childBet.setOdds(new BigDecimal(legNode.get("odds").asText()));
                        
                        childBets.add(childBet);
                    }
                    bet.setChildBets(childBets);
                }
                
            } catch (Exception e) {
                log.error("Nie udało się sparsować odpowiedzi JSON od Gemini: {}", e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractAllImageUrlsFromTweet(Map<String, Object> tweet) {
        List<String> imageUrls = new ArrayList<>();
        try {
            Map<String, Object> entities = (Map<String, Object>) tweet.get("entities");
            if (entities != null && entities.containsKey("media")) {
                List<Map<String, Object>> media = (List<Map<String, Object>>) entities.get("media");
                if (media != null) {
                    for (Map<String, Object> item : media) {
                        if ("photo".equals(item.get("type"))) {
                            imageUrls.add((String) item.get("media_url_https"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Błąd podczas szukania obrazków w tweecie: {}", e.getMessage());
        }
        return imageUrls;
    }
}
