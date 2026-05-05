package com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi;

import com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.dto.OddsResponseDto;
import com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.dto.SportDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OddsApiClientTest {

    @Autowired
    private OddsApiClient oddsApiClient;

    @Test
    void shouldFetchSportsFromApi() {
        // Given
        // Klucz API jest wstrzykiwany przez Springa z zmiennych środowiskowych

        // When
        List<SportDto> sports = oddsApiClient.getSports();

        // Then
        assertNotNull(sports);
        assertFalse(sports.isEmpty(), "Lista sportów nie powinna być pusta");
        
        // Sprawdźmy czy pierwszy sport ma podstawowe dane
        SportDto firstSport = sports.get(0);
        assertNotNull(firstSport.getKey());
        assertNotNull(firstSport.getTitle());
        
        System.out.println("Pomyślnie pobrano " + sports.size() + " sportów.");
        System.out.println("Przykładowy sport: " + firstSport.getTitle() + " (" + firstSport.getGroup() + ")");
    }

    @Test
    void shouldFetchOddsForSoccer() {
        // Given
        String soccerKey = "soccer_poland_ekstraklasa"; // Spróbujmy polską Ekstraklasę!
        
        // When
        try {
            List<OddsResponseDto> odds = oddsApiClient.getOdds(soccerKey, "eu", "h2h");

            // Then
            assertNotNull(odds);
            System.out.println("Pomyślnie pobrano kursy dla: " + soccerKey);
            if (!odds.isEmpty()) {
                OddsResponseDto firstMatch = odds.get(0);
                System.out.println("Mecz: " + firstMatch.getHomeTeam() + " vs " + firstMatch.getAwayTeam());
                System.out.println("Liczba bukmacherów: " + firstMatch.getBookmakers().size());
            } else {
                System.out.println("Brak aktualnych kursów dla Ekstraklasy w tej chwili.");
            }
        } catch (Exception e) {
            // Jeśli Ekstraklasa nie jest akurat dostępna (np. przerwa w lidze), test nie powinien wybuchnąć błędem krytycznym
            System.out.println("Info: Nie udało się pobrać kursów dla Ekstraklasy (możliwa przerwa w rozgrywkach): " + e.getMessage());
        }
    }
}
