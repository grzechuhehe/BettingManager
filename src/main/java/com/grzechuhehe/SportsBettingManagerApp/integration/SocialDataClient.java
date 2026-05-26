package com.grzechuhehe.SportsBettingManagerApp.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialDataClient {

    private final RestTemplate restTemplate;

    @Value("${socialdata.api.key}")
    private String apiKey;

    private static final String SEARCH_URL = "https://api.socialdata.tools/twitter/search";
    private static final String USER_URL = "https://api.socialdata.tools/twitter/user/";

    /**
     * Sprawdza czy dany użytkownik istnieje na platformie X.
     */
    public boolean checkProfileExists(String username) {
        try {
            String url = USER_URL + username;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.info("Weryfikuję istnienie profilu @{} w SocialData API...", username);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            boolean exists = response.getStatusCode().is2xxSuccessful() && 
                             response.getBody() != null && 
                             (response.getBody().containsKey("id_str") || response.getBody().containsKey("id"));
            
            if (!exists) {
                log.warn("SocialData zwróciło sukces, ale brak pól ID dla @{}. Body: {}", username, response.getBody());
            }

            return exists;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            log.info("Profil @{} nie istnieje na X (404 z API SocialData)", username);
            return false;
        } catch (Exception e) {
            log.error("KRYTYCZNY BŁĄD API SocialData podczas sprawdzania @{}: {}. Sprawdź klucz API i limity!", 
                      username, e.getMessage(), e);
            // Jeśli to błąd API (np. 401, 429), a nie brak usera (404), 
            // to chwilowo zwrócimy true, żeby nie blokować użytkownika przez błędy integracji? 
            // NIE - lepiej rzucić wyjątek, żeby kontroler mógł go obsłużyć.
            throw new RuntimeException("External API error: " + e.getMessage());
        }
    }

    /**
     * Pobiera ostatnie tweety użytkownika z X, pomijając odpowiedzi (replies).
     *
     * @param username Nazwa użytkownika bez @ (np. elonmusk).
     * @param sinceId ID tweeta, od którego (nie włącznie) chcemy pobierać nowe posty.
     * @return Lista map reprezentujących tweety.
     */
    public List<Map<String, Object>> fetchRecentTweets(String username, String sinceId) {
        try {
            // Budujemy zapytanie: from:username 
            StringBuilder queryBuilder = new StringBuilder("from:").append(username);
            
            if (sinceId != null && !sinceId.isEmpty()) {
                queryBuilder.append(" since_id:").append(sinceId);
            }
            
            String query = queryBuilder.toString();
            
            String url = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                    .queryParam("query", query)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.info("Pobieram tweety z SocialData. URL: {}, Query: {}", url, query);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody().get("tweets");
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Błąd podczas pobierania tweetów dla {}: {}", username, e.getMessage());
            return new ArrayList<>();
        }
    }
}
