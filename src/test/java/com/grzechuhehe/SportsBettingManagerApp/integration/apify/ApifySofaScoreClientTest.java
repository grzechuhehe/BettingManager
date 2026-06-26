package com.grzechuhehe.SportsBettingManagerApp.integration.apify;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ApifySofaScoreClientTest {

    private ApifySofaScoreClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        // connect/read timeout = 0 → klient nie nadpisuje request factory,
        // dzięki czemu MockRestServiceServer podpięty pod builder pozostaje aktywny.
        client = new ApifySofaScoreClient(
                builder,
                "https://api.apify.com/v2",
                "test-token",
                "abotapi~sofascore-scraper",
                true,
                0,
                0);
    }

    @Test
    void shouldReturnOnlyMatchRecords() {
        String json = """
            [
              {"type":"match","homeTeam":"Legia Warszawa","awayTeam":"Lech Poznan",
               "homeScore":2,"awayScore":1,"statusType":"finished","startTimestamp":1700000000,
               "url":"https://www.sofascore.com/x#id:1"},
              {"type":"team","name":"Legia Warszawa"}
            ]
            """;
        server.expect(requestTo(containsString("/acts/abotapi~sofascore-scraper/run-sync-get-dataset-items")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<SofaScoreEventDto> result = client.searchMatches("Legia Warszawa Lech Poznan");

        assertEquals(1, result.size());
        assertEquals("Legia Warszawa", result.get(0).getHomeTeam());
        assertEquals(2, result.get(0).getHomeScore());
        assertEquals("finished", result.get(0).getStatusType());
    }

    @Test
    void shouldBatchSearchInSingleRequest() {
        String json = """
            [{"type":"match","homeTeam":"A","awayTeam":"B","statusType":"finished"}]
            """;
        server.expect(requestTo(containsString("/acts/")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<SofaScoreEventDto> result = client.searchMatchesBatch(List.of("A vs B", "C vs D")).matches();

        assertEquals(1, result.size());
    }

    @Test
    void shouldFetchScheduledMatches() {
        String json = """
            [{"type":"match","homeTeam":"X","awayTeam":"Y","statusType":"finished"}]
            """;
        server.expect(requestTo(containsString("/acts/")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<SofaScoreEventDto> result = client.fetchScheduledMatches(
                java.time.LocalDate.of(2026, 6, 1), 2, List.of("football"), 100);

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnEmptyListOnError() {
        server.expect(requestTo(containsString("/acts/")))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        List<SofaScoreEventDto> result = client.searchMatches("whatever");

        assertTrue(result.isEmpty());
    }
}
