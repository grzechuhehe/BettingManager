package com.grzechuhehe.SportsBettingManagerApp.integration.apify;

import com.grzechuhehe.SportsBettingManagerApp.integration.apify.dto.SofaScoreEventDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(ApifySofaScoreClient.class)
class ApifySofaScoreClientTest {

    @Autowired
    private ApifySofaScoreClient client;

    @Autowired
    private MockRestServiceServer server;

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
    void shouldReturnEmptyListOnError() {
        server.expect(requestTo(containsString("/acts/")))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        List<SofaScoreEventDto> result = client.searchMatches("whatever");

        assertTrue(result.isEmpty());
    }
}
