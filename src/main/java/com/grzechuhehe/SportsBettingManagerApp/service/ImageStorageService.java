package com.grzechuhehe.SportsBettingManagerApp.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private final RestTemplate restTemplate;
    
    // Katalog docelowy na zdjęcia kuponów w głównym folderze aplikacji (obok src)
    private static final String UPLOAD_DIR = "uploads/profiles/";

    @PostConstruct
    public void init() {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("Utworzono katalog na zdjęcia dowodów: {}", UPLOAD_DIR);
            } else {
                log.error("Nie udało się utworzyć katalogu: {}", UPLOAD_DIR);
            }
        }
    }

    /**
     * Pobiera obraz z podanego URL i zapisuje go lokalnie.
     * @param imageUrl Adres URL obrazu (z SocialData API).
     * @return Względna ścieżka do zapisanego pliku, którą odczyta frontend (np. /images/profiles/uuid.jpg),
     *         lub null w przypadku błędu.
     */
    public String downloadAndSaveImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        try {
            // 1. Pobieranie obrazu jako tablica bajtów
            byte[] imageBytes = restTemplate.getForObject(imageUrl, byte[].class);
            if (imageBytes == null) {
                log.warn("Nie udało się pobrać obrazu, pusty wynik z URL: {}", imageUrl);
                return null;
            }

            // 2. Generowanie unikalnej nazwy pliku (tymczasowo zapisujemy jako standardowy JPG, 
            // w przyszłości można dodać libkę Scrimage do konwersji na WebP)
            String fileName = UUID.randomUUID().toString() + ".jpg";
            Path filePath = Paths.get(UPLOAD_DIR + fileName);

            // 3. Zapis pliku na dysk
            Files.write(filePath, imageBytes);
            log.info("Pomyślnie zapisano dowód kuponu do: {}", filePath);

            // 4. Zwracanie ścieżki dostępnej publicznie przez Spring MVC
            return "/images/profiles/" + fileName;

        } catch (Exception e) {
            log.error("Błąd podczas pobierania lub zapisu obrazu z {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }
}
