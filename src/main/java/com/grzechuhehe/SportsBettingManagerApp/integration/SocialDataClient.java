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

    /**
     * Pobiera ostatnie tweety użytkownika z X, pomijając odpowiedzi (replies).
     * @param username Nazwa użytkownika bez @ (np. elonmusk).
     * @return Lista map reprezentujących tweety.
     */
    public List<Map<String, Object>> fetchRecentTweets(String username) {
        try {
            // Budujemy zapytanie: from:username 
            // Pobieramy wszystko od tego użytkownika (w tym jego odpowiedzi do własnych wątków).
            // Niestety API Twittera/SocialData nie ma idealnego filtra "tylko odpowiedzi do siebie", 
            // ale pobierając wszystko, w Orchestratorze możemy odfiltrować posty, które są
            // odpowiedziami do innych użytkowników, sprawdzając pole 'in_reply_to_screen_name'.
            String query = "from:" + username;
            
            String url = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                    .queryParam("query", query)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.info("Pobieram tweety z SocialData dla profilu: {}", username);
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
