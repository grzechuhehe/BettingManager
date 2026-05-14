package com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.dto.OddsResponseDto;
import com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.dto.SportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

@RestClientTest(OddsApiClient.class)
class OddsApiClientTest {

    @Autowired
    private OddsApiClient oddsApiClient;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldFetchSportsFromApi() throws Exception {
        // Given
        SportDto sport1 = new SportDto();
        sport1.setKey("soccer_poland_ekstraklasa");
        sport1.setTitle("Ekstraklasa");
        sport1.setGroup("Soccer");
        
        List<SportDto> mockResponse = Arrays.asList(sport1);
        String jsonResponse = objectMapper.writeValueAsString(mockResponse);

        this.server.expect(requestTo(containsString("/sports")))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        // When
        List<SportDto> sports = oddsApiClient.getSports();

        // Then
        assertNotNull(sports);
        assertEquals(1, sports.size());
        assertEquals("Ekstraklasa", sports.get(0).getTitle());
        System.out.println("Pomyślnie przetestowano (MOCK) pobieranie sportów.");
    }

    @Test
    void shouldFetchOddsForSoccer() throws Exception {
        // Given
        String soccerKey = "soccer_poland_ekstraklasa";
        OddsResponseDto oddsResponse = new OddsResponseDto();
        oddsResponse.setHomeTeam("Legia Warszawa");
        oddsResponse.setAwayTeam("Lech Poznań");
        oddsResponse.setBookmakers(Arrays.asList());

        List<OddsResponseDto> mockResponse = Arrays.asList(oddsResponse);
        String jsonResponse = objectMapper.writeValueAsString(mockResponse);

        this.server.expect(requestTo(containsString("/odds")))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        // When
        List<OddsResponseDto> odds = oddsApiClient.getOdds(soccerKey, "eu", "h2h");

        // Then
        assertNotNull(odds);
        assertFalse(odds.isEmpty());
        assertEquals("Legia Warszawa", odds.get(0).getHomeTeam());
        System.out.println("Pomyślnie przetestowano (MOCK) pobieranie kursów.");
    }
}
