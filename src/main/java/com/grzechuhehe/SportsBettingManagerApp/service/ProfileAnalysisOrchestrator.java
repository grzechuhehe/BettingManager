package com.grzechuhehe.SportsBettingManagerApp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.integration.GeminiVisionClient;
import com.grzechuhehe.SportsBettingManagerApp.integration.SocialDataClient;
import com.grzechuhehe.SportsBettingManagerApp.model.Bet;
import com.grzechuhehe.SportsBettingManagerApp.model.User;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetStatus;
import com.grzechuhehe.SportsBettingManagerApp.model.enum_model.BetType;
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
                    betRepository.save(existingBet);
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
            
            if (newBet.getEventName() != null || newBet.getOdds() != null) {
                 betRepository.save(newBet);
                 log.info("Zapisano nowy zakład wyciągnięty przez AI dla użytkownika {}", xUsername);
            }
        }
    }

    private void updateBetDataFromAI(Bet bet, String text, List<String> imagePaths) {
        String aiResponseJson = geminiVisionClient.analyzeBet(text, imagePaths);
        
        if (aiResponseJson != null && !aiResponseJson.isEmpty()) {
            try {
                // Usuwamy ewentualne formatowanie markdown (```json ... ```) od Gemini
                String cleanJson = aiResponseJson.replaceAll("```json", "").replaceAll("```", "").trim();
                JsonNode jsonNode = objectMapper.readTree(cleanJson);

                if (jsonNode.has("eventName")) bet.setEventName(jsonNode.get("eventName").asText());
                if (jsonNode.has("selection")) bet.setSelection(jsonNode.get("selection").asText());
                if (jsonNode.has("bookmaker")) bet.setBookmaker(jsonNode.get("bookmaker").asText());
                
                if (jsonNode.has("odds") && !jsonNode.get("odds").isNull()) {
                    bet.setOdds(new BigDecimal(jsonNode.get("odds").asText()));
                }
                
                if (jsonNode.has("units") && !jsonNode.get("units").isNull()) {
                    bet.setUnits(new BigDecimal(jsonNode.get("units").asText()));
                } else {
                    bet.setUnits(BigDecimal.ONE); // Domyślnie 1u
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
