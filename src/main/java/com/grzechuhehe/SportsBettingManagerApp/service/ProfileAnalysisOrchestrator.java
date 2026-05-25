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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

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
    public void processTrackedProfiles() {
        log.info("Rozpoczynam analizę profili z X (SocialData + Gemini)...");
        
        // Szukamy użytkowników, którzy mają xUsername i nie byli sprawdzani od ponad godziny
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        List<User> profilesToUpdate = userRepository.findProfilesToUpdate(threshold);

        for (User user : profilesToUpdate) {
            try {
                // Wywołujemy w oddzielnej transakcji, aby błąd u jednego użytkownika nie psuł reszty
                processSingleProfile(user);
            } catch (Exception e) {
                log.error("KRYTYCZNY BŁĄD podczas analizy profilu {} : {}", user.getXUsername(), e.getMessage());
            }
        }
    }

    @Transactional
    public void processSingleProfile(User user) {
        analyzeProfile(user);
        user.setLastXCheckAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private void analyzeProfile(User user) {
        String xUsernameRaw = user.getXUsername();
        if (xUsernameRaw == null) return;
        
        // Czyścimy nazwę użytkownika z '@' dla poprawnego porównywania w filtrach
        String xUsername = xUsernameRaw.replace("@", "").trim();
        
        log.info("Pobieranie najnowszych postów dla profilu @{} (od ID: {})", xUsername, user.getLastScrapedTweetId());
        List<Map<String, Object>> recentTweets = socialDataClient.fetchRecentTweets(xUsername, user.getLastScrapedTweetId());
        log.info("Pobrano {} NOWYCH postów dla profilu @{}", recentTweets.size(), xUsername);

        if (recentTweets.isEmpty()) return;

        // Przetwarzamy od najstarszych do najnowszych, aby najpierw zapisać zakład, 
        // a potem móc dokleić do niego link z komentarza (reply).
        java.util.Collections.reverse(recentTweets);
        
        // Śledzimy najnowszy ID w tej paczce, aby zaktualizować lastScrapedTweetId
        String newestTweetId = user.getLastScrapedTweetId();

        int processed = 0;
        int skippedNonBets = 0;
        int skippedReplies = 0;
        int savedCount = 0;
        int errorCount = 0;

        // Mapa do śledzenia tekstów tweetów w bieżącej paczce dla kontekstu wątków
        Map<String, String> tweetIdToTextMap = new HashMap<>();
        for (Map<String, Object> t : recentTweets) {
            String id = (String) t.get("id_str");
            String txt = (String) t.get("full_text");
            if (id != null && txt != null) tweetIdToTextMap.put(id, txt);
        }

        for (Map<String, Object> tweet : recentTweets) {
            String tweetId = (String) tweet.get("id_str");
            // Aktualizujemy newestTweetId (tweety są posortowane chronologicznie po odwróceniu, 
            // więc ostatni w pętli będzie najnowszy).
            newestTweetId = tweetId;

            try {
                String fullText = (String) tweet.get("full_text");
                
                // Ignoruj odpowiedzi do INNYCH użytkowników
                String inReplyTo = (String) tweet.get("in_reply_to_screen_name");
                if (inReplyTo != null && !inReplyTo.equalsIgnoreCase(xUsername)) {
                    skippedReplies++;
                    continue;
                }
                
                // HEURYSTYKA: Sprawdź czy post w ogóle wygląda na zakład przed wysłaniem do Gemini
                List<String> imageUrls = extractAllImageUrlsFromTweet(tweet);
                if (!isLikelyABet(fullText, imageUrls)) {
                    skippedNonBets++;
                    continue;
                }

                processed++;
                String sharingUrl = extractSharingUrl(tweet);
                String replyToId = (String) tweet.get("in_reply_to_status_id_str");

                // ... reszta logiki bez zmian ...
                String threadContext = null;
                if (replyToId != null) {
                    threadContext = tweetIdToTextMap.get(replyToId);
                    if (threadContext == null) {
                        // Jeśli nie ma w paczce, szukamy w bazie
                        Optional<Bet> parentBetFromDb = betRepository.findBySourcePostId(replyToId);
                        if (parentBetFromDb.isPresent()) {
                            Bet pb = parentBetFromDb.get();
                            threadContext = String.format("PARENT CONTEXT: Event: %s, Sport: %s, Selection: %s", 
                                pb.getEventName(), pb.getSport(), pb.getSelection());
                        }
                    }
                }
                
                // Jeśli to jest odpowiedź (komentarz) i zawiera link, to może być link do kuponu w wątku
                if (replyToId != null && sharingUrl != null) {
                    Optional<Bet> parentBetOpt = betRepository.findBySourcePostId(replyToId);
                    if (parentBetOpt.isPresent()) {
                        Bet parentBet = parentBetOpt.get();
                        if (parentBet.getSharingUrl() == null) {
                            parentBet.setSharingUrl(sharingUrl);
                            betRepository.save(parentBet);
                            log.info("🔗 Doklejono link do kuponu z komentarza {} do zakładu {}", tweetId, replyToId);
                        }
                    }
                }

                Optional<Bet> existingBetOpt = betRepository.findBySourcePostId(tweetId);
                
                // Szukamy lokalnych ścieżek dla obrazków (już pobranych w kroku heurystyki)
                List<String> localImagePaths = new ArrayList<>();
                for (String url : imageUrls) {
                    String path = imageStorageService.downloadAndSaveImage(url, xUsername);
                    if (path != null) localImagePaths.add(path);
                }

                if (existingBetOpt.isPresent()) {
                    Bet existingBet = existingBetOpt.get();
                    
                    // Uzupełnij link, jeśli wcześniej go nie było
                    if (sharingUrl != null && existingBet.getSharingUrl() == null) {
                        existingBet.setSharingUrl(sharingUrl);
                        betRepository.save(existingBet);
                    }
                    
                    if (!localImagePaths.isEmpty() && existingBet.getImageProofPath() == null) {
                        log.info("Wykryto dodane zdjęcia do istniejącego posta {} od {}", tweetId, xUsername);
                        existingBet.setImageProofPath(localImagePaths.get(0));
                        updateBetDataFromAI(existingBet, fullText, localImagePaths, threadContext);
                        
                        if (isBetValid(existingBet)) {
                            betRepository.save(existingBet);
                            log.info("Zaktualizowano istniejący zakład z posta {}", tweetId);
                        }
                    }
                    continue;
                }

                log.info("Analizuję nowy post {} dla użytkownika @{}", tweetId, xUsername);
                Bet newBet = Bet.builder()
                        .user(user)
                        .sourcePostId(tweetId)
                        .imageProofPath(localImagePaths.isEmpty() ? null : localImagePaths.get(0))
                        .isAiExtracted(true)
                        .betType(BetType.SINGLE)
                        .status(BetStatus.PENDING)
                        .sharingUrl(sharingUrl)
                        .placedAt(LocalDateTime.now())
                        .build();

                updateBetDataFromAI(newBet, fullText, localImagePaths, threadContext);
                
                if (isBetValid(newBet)) {
                     betRepository.save(newBet);
                     savedCount++;
                     log.info("✅ Zapisano nowy zakład z posta {} (@{})", tweetId, xUsername);
                } else {
                     log.warn("❌ Odrzucono post {} (@{}) - nie-zakład lub błąd danych (odds: {})", 
                              tweetId, xUsername, newBet.getOdds());
                }
            } catch (Exception e) {
                errorCount++;
                log.error("Błąd podczas przetwarzania konkretnego posta {} dla {}: {}", tweetId, xUsername, e.getMessage());
                // NIE rzucamy wyjątku dalej, aby pętla mogła sprawdzić następne tweety
            }
        }
        
        // Aktualizacja ID ostatnio zescrapowanego tweeta
        if (newestTweetId != null && !newestTweetId.equals(user.getLastScrapedTweetId())) {
            user.setLastScrapedTweetId(newestTweetId);
            log.info("Zaktualizowano lastScrapedTweetId dla @{} na {}", xUsername, newestTweetId);
        }

        log.info("Zakończono analizę @{}. Nowych: {}, Przetworzono: {}, Zapisano: {}, Pominięto (nie-zakłady): {}, Pominięto (replies): {}, Błędy: {}", 
                xUsername, recentTweets.size(), processed, savedCount, skippedNonBets, skippedReplies, errorCount);
    }

    private boolean isLikelyABet(String text, List<String> imageUrls) {
        // Jeśli są zdjęcia, jest duża szansa że to kupon
        if (imageUrls != null && !imageUrls.isEmpty()) return true;
        
        if (text == null || text.isEmpty()) return false;
        
        String lowerText = text.toLowerCase();
        // Lista słów kluczowych sugerujących zakład
        List<String> keywords = Arrays.asList(
            "kurs", "stawka", "typ", "kupon", "jednostki", "bet", "ako", "singiel", 
            "sts", "fortuna", "superbet", "betclic", "totalbet", "forbet", "etoto", "lvbet"
        );
        
        for (String keyword : keywords) {
            if (lowerText.contains(keyword)) return true;
        }
        
        return false;
    }

    private boolean isBetValid(Bet bet) {
        if (bet.getOdds() == null || bet.getEventName() == null) return false;
        if (bet.getEventName().equalsIgnoreCase("null") || bet.getEventName().isEmpty()) return false;
        
        // Minimalny kurs musi być większy niż 1.00 (walidacja w encji Bet ma @DecimalMin("1.01"))
        return bet.getOdds().compareTo(new BigDecimal("1.00")) > 0;
    }

    private void updateBetDataFromAI(Bet bet, String text, List<String> imagePaths, String threadContext) {
        // Zabezpieczenie przed halucynacjami: jeśli nie ma zdjęć i tekst to tylko URL, odrzucamy.
        boolean isOnlyUrl = false;
        if ((imagePaths == null || imagePaths.isEmpty()) && text != null) {
            String sharingUrl = bet.getSharingUrl();
            String trimmedText = text.trim();
            // Jeśli tekst jest bardzo krótki i zawiera URL, uznajemy go za "pusty" post
            if (sharingUrl != null && trimmedText.contains(sharingUrl) && trimmedText.length() < sharingUrl.length() + 15) {
                isOnlyUrl = true;
            }
        }

        if (isOnlyUrl) {
            log.warn("⚠️ Post {} zawiera tylko link i brak zdjęć. Pomijam analizę AI, aby uniknąć halucynacji.", bet.getSourcePostId());
            bet.setEventName(null);
            return;
        }

        String fullPromptText = text;
        if (threadContext != null) {
            fullPromptText = "CONTEXT FROM PREVIOUS POST (The post below is a reply to this): \n" + threadContext + "\n\nCURRENT POST TEXT: \n" + text;
        }

        log.info("🔍 Wysyłam post (id: {}) do analizy przez Gemini (zdjęcia: {}). Tekst: [{}]", 
                 bet.getSourcePostId(), imagePaths.size(), text != null ? text.replace("\n", " ") : "brak");
        String aiResponseJson = geminiVisionClient.analyzeBet(fullPromptText, imagePaths);
        
        if (aiResponseJson != null && !aiResponseJson.isEmpty()) {
            log.debug("Otrzymano odpowiedź z Gemini dla posta {}: {}", bet.getSourcePostId(), aiResponseJson);
            try {
                // Usuwamy ewentualne formatowanie markdown od Gemini
                String cleanJson = aiResponseJson.replaceAll("```json", "").replaceAll("```", "").trim();
                JsonNode jsonNode = objectMapper.readTree(cleanJson);

                if (jsonNode.has("eventName")) bet.setEventName(sanitizeAiString(jsonNode.get("eventName")));
                if (jsonNode.has("selection")) bet.setSelection(sanitizeAiString(jsonNode.get("selection")));
                if (jsonNode.has("bookmaker")) bet.setBookmaker(sanitizeAiString(jsonNode.get("bookmaker")));
                if (jsonNode.has("sport")) bet.setSport(sanitizeAiString(jsonNode.get("sport")));
                
                if (jsonNode.has("marketType") && !jsonNode.get("marketType").isNull()) {
                    String mt = sanitizeAiString(jsonNode.get("marketType"));
                    if (mt != null) {
                        try {
                            bet.setMarketType(com.grzechuhehe.SportsBettingManagerApp.model.enum_model.MarketType.valueOf(mt.toUpperCase()));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
                
                if (jsonNode.has("oddsType") && !jsonNode.get("oddsType").isNull()) {
                    String ot = sanitizeAiString(jsonNode.get("oddsType"));
                    if (ot != null) {
                        try {
                            bet.setOddsType(OddsType.valueOf(ot.toUpperCase()));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
                
                if (jsonNode.has("status") && !jsonNode.get("status").isNull()) {
                    String st = sanitizeAiString(jsonNode.get("status"));
                    if (st != null) {
                        try {
                            BetStatus status = BetStatus.valueOf(st.toUpperCase());
                            bet.setStatus(status);
                            
                            // Jeśli zakład jest już rozliczony w momencie pierwszego wykrycia przez AI,
                            // oznaczamy go jako retroactive (nie-pre-match), aby nie psuć statystyk.
                            if (status != BetStatus.PENDING && bet.getId() == null) {
                                bet.setPreMatch(false);
                                log.info("Detected retroactive bet (status: {}) for post {}. Marking isPreMatch = false.", 
                                         status, bet.getSourcePostId());
                            }
                        } catch (IllegalArgumentException ignored) {}
                    }
                }

                // Sprawdzamy czy AI uznało to za prawdziwy zakład, a nie promocję
                if (jsonNode.has("isPlacedBet") && !jsonNode.get("isPlacedBet").asBoolean()) {
                    log.warn("Gemini uznało post {} za promocję lub nie-zakład (isPlacedBet: false)", bet.getSourcePostId());
                    bet.setEventName(null); // To spowoduje odrzucenie przez isBetValid
                    return;
                }
                
                if (jsonNode.has("odds") && !jsonNode.get("odds").isNull()) {
                    bet.setOdds(new BigDecimal(jsonNode.get("odds").asText()));
                }
                
                BigDecimal extractedUnits = jsonNode.has("units") && !jsonNode.get("units").isNull() ? new BigDecimal(jsonNode.get("units").asText()) : null;
                BigDecimal extractedStake = jsonNode.has("stake") && !jsonNode.get("stake").isNull() ? new BigDecimal(jsonNode.get("stake").asText()) : null;

                if (extractedStake != null) {
                    bet.setStake(extractedStake);
                    // Zawsze synchronizujemy jednostki: 10 PLN = 1u
                    bet.setUnits(extractedStake.divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP));
                } else if (extractedUnits != null) {
                    bet.setUnits(extractedUnits);
                    // Jeśli mamy tylko jednostki, przeliczamy na PLN: 1u = 10 PLN
                    bet.setStake(extractedUnits.multiply(new BigDecimal("10")));
                } else {
                    bet.setUnits(BigDecimal.ONE);
                    bet.setStake(new BigDecimal("10"));
                }
                
                bet.calculatePotentialWinnings();

                // Obliczanie finalProfit dla zakładów już rozliczonych (WON/LOST)
                if (bet.getStatus() != BetStatus.PENDING) {
                    if (bet.getStatus() == BetStatus.WON && bet.getPotentialWinnings() != null) {
                        bet.setFinalProfit(bet.getPotentialWinnings().subtract(bet.getStake()));
                    } else if (bet.getStatus() == BetStatus.LOST) {
                        bet.setFinalProfit(bet.getStake().negate());
                    } else if (bet.getStatus() == BetStatus.VOID) {
                        bet.setFinalProfit(BigDecimal.ZERO);
                    }
                    if (bet.getSettledAt() == null) {
                        bet.setSettledAt(LocalDateTime.now());
                    }
                }
                
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
                            
                        if (legNode.has("eventName")) childBet.setEventName(sanitizeAiString(legNode.get("eventName")));
                        if (legNode.has("selection")) childBet.setSelection(sanitizeAiString(legNode.get("selection")));
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

    private String sanitizeAiString(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String text = node.asText().trim();
        if (text.equalsIgnoreCase("null") || text.equalsIgnoreCase("undefined") || text.isEmpty()) {
            return null;
        }
        return text;
    }

    @SuppressWarnings("unchecked")
    private String extractSharingUrl(Map<String, Object> tweet) {
        // 1. Szukamy w encjach (najpewniejsze źródło)
        try {
            Map<String, Object> entities = (Map<String, Object>) tweet.get("entities");
            if (entities != null && entities.containsKey("urls")) {
                List<Map<String, Object>> urls = (List<Map<String, Object>>) entities.get("urls");
                if (urls != null && !urls.isEmpty()) {
                    return (String) urls.get(0).get("expanded_url");
                }
            }
        } catch (Exception ignored) {}

        // 2. Fallback: szukamy w tekście
        String text = (String) tweet.get("full_text");
        if (text != null) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("https?://\\S+");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
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
