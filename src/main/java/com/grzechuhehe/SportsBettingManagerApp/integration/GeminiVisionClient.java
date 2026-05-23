package com.grzechuhehe.SportsBettingManagerApp.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiVisionClient {

    private final RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.system.prompt}")
    private String systemPrompt;

    private static final String MODEL_NAME = "gemini-2.5-flash"; // Zgodnie z Twoją specyfikacją na maj 2026
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=";

    /**
     * Wysyła tekst posta i zdjęcie do Gemini w celu ekstrakcji danych o zakładzie.
     */
    public String analyzeBet(String postText, List<String> localImagePaths) {
        try {
            String url = API_URL + apiKey;

            // 1. Przygotowanie promptu z properties/env
            // 2. Przygotowanie części zapytania (tekst + obrazy w Base64)
            List<Map<String, Object>> parts = new ArrayList<>();
            
            // Część tekstowa
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", systemPrompt + "\n\nTekst posta: " + postText);
            parts.add(textPart);

            // Część obrazkowa
            if (localImagePaths != null) {
                for (String localImagePath : localImagePaths) {
                    Path path = Paths.get(localImagePath.replaceFirst("/images/profiles/", "uploads/profiles/"));
                    if (Files.exists(path)) {
                        byte[] imageBytes = Files.readAllBytes(path);
                        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                        Map<String, Object> inlineData = new HashMap<>();
                        inlineData.put("mime_type", "image/jpeg");
                        inlineData.put("data", base64Image);

                        Map<String, Object> imagePart = new HashMap<>();
                        imagePart.put("inline_data", inlineData);
                        parts.add(imagePart);
                    }
                }
            }

            // 3. Budowa ciała zapytania
            Map<String, Object> content = new HashMap<>();
            content.put("parts", parts);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", Collections.singletonList(content));
            
            // Konfiguracja wymuszenia formatu JSON i Schematu (Generation Config)
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("response_mime_type", "application/json");
            
            // Definiowanie schematu
            Map<String, Object> betSchema = new HashMap<>();
            betSchema.put("type", "OBJECT");
            
            Map<String, Object> betProperties = new HashMap<>();
            
            Map<String, Object> eventName = new HashMap<>(); eventName.put("type", "STRING"); eventName.put("description", "Name of the match/event.");
            Map<String, Object> selection = new HashMap<>(); selection.put("type", "STRING"); selection.put("description", "What was bet on (e.g., Over 2.5, Team A).");
            Map<String, Object> odds = new HashMap<>(); odds.put("type", "NUMBER"); odds.put("description", "Total odds of the bet.");
            Map<String, Object> bookmaker = new HashMap<>(); bookmaker.put("type", "STRING"); bookmaker.put("description", "Name of the bookmaker.");
            Map<String, Object> stake = new HashMap<>(); stake.put("type", "NUMBER"); stake.put("description", "Stake amount in currency. Null if not visible.");
            Map<String, Object> units = new HashMap<>(); units.put("type", "NUMBER"); units.put("description", "Stake in units (e.g., 1.5). Default to 1.0 if not specified.");
            Map<String, Object> sport = new HashMap<>(); sport.put("type", "STRING"); sport.put("description", "Sport category (e.g., Football, Basketball).");
            Map<String, Object> marketType = new HashMap<>(); marketType.put("type", "STRING"); marketType.put("description", "Market type (e.g., MATCH_ODDS, OVER_UNDER).");
            Map<String, Object> oddsType = new HashMap<>(); oddsType.put("type", "STRING"); oddsType.put("description", "Odds format (e.g., DECIMAL, AMERICAN). Default to DECIMAL.");
            Map<String, Object> status = new HashMap<>(); status.put("type", "STRING"); status.put("description", "Bet status: PENDING, WON, LOST, VOID. Default to PENDING.");
            
            // Dla kuponów AKO (Parlay)
            Map<String, Object> legs = new HashMap<>();
            legs.put("type", "ARRAY");
            legs.put("description", "List of individual selections if this is a parlay/accumulator bet.");
            Map<String, Object> legItems = new HashMap<>();
            legItems.put("type", "OBJECT");
            Map<String, Object> legProperties = new HashMap<>();
            legProperties.put("eventName", eventName);
            legProperties.put("selection", selection);
            legProperties.put("odds", odds);
            legItems.put("properties", legProperties);
            legs.put("items", legItems);
            
            betProperties.put("eventName", eventName);
            betProperties.put("selection", selection);
            betProperties.put("odds", odds);
            betProperties.put("bookmaker", bookmaker);
            betProperties.put("stake", stake);
            betProperties.put("units", units);
            betProperties.put("sport", sport);
            betProperties.put("marketType", marketType);
            betProperties.put("oddsType", oddsType);
            betProperties.put("status", status);
            betProperties.put("legs", legs);
            
            betSchema.put("properties", betProperties);
            generationConfig.put("response_schema", betSchema);

            requestBody.put("generationConfig", generationConfig);

            // 4. Wysyłka
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Wysyłam zapytanie do Gemini Vision dla modelu: {}", MODEL_NAME);
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            // 5. Wyciąganie tekstu z odpowiedzi Gemini
            // Struktura odpowiedzi Google: candidates[0].content.parts[0].text
            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> firstCandidate = candidates.get(0);
                    Map<String, Object> resContent = (Map<String, Object>) firstCandidate.get("content");
                    List<Map<String, Object>> resParts = (List<Map<String, Object>>) resContent.get("parts");
                    return (String) resParts.get(0).get("text");
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Błąd podczas komunikacji z Gemini API: {}", e.getMessage());
            return null;
        }
    }
}
