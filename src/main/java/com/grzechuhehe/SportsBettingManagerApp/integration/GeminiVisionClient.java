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

    private static final String MODEL_NAME = "gemini-2.5-flash"; // Zgodnie z Twoją specyfikacją na maj 2026
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=";

    /**
     * Wysyła tekst posta i zdjęcie do Gemini w celu ekstrakcji danych o zakładzie.
     */
    public String analyzeBet(String postText, List<String> localImagePaths) {
        try {
            String url = API_URL + apiKey;

            // 1. Przygotowanie promptu
            String prompt = "Jesteś ekspertem od zakładów bukmacherskich. Przeanalizuj tekst posta i ZAŁĄCZONE ZDJĘCIA (mogą być częściami jednego kuponu). " +
                    "Zadaniem jest scalenie informacji ze wszystkich zdjęć w jeden spójny kupon. " +
                    "Sprawdź: czy iloczyn kursów na zdjęciach zgadza się z końcowym kursem? Czy są jakieś bonusy (np. 'boosted odds')? " +
                    "Zwróć odpowiedź WYŁĄCZNIE w formacie JSON, np: " +
                    "{\"eventName\": \"Real - Barca\", \"selection\": \"1\", \"odds\": 1.85, \"bookmaker\": \"STS\", \"units\": 5.0}";

            // 2. Przygotowanie części zapytania (tekst + obrazy w Base64)
            List<Map<String, Object>> parts = new ArrayList<>();
            
            // Część tekstowa
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt + "\n\nTekst posta: " + postText);
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
            
            // Konfiguracja wymuszenia formatu JSON (Generation Config)
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("response_mime_type", "application/json");
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
