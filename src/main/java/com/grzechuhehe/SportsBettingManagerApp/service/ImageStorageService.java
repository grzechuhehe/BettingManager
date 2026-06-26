package com.grzechuhehe.SportsBettingManagerApp.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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
     * Pobiera obraz z podanego URL i zapisuje go lokalnie w podfolderze użytkownika.
     * @param imageUrl Adres URL obrazu (z SocialData API).
     * @param xUsername Nazwa użytkownika z X (Twitter).
     * @return Względna ścieżka do zapisanego pliku, którą odczyta frontend, lub null.
     */
    public String downloadAndSaveImage(String imageUrl, String xUsername) {
        if (imageUrl == null || imageUrl.isEmpty() || xUsername == null || xUsername.isEmpty()) {
            return null;
        }

        try {
            byte[] imageBytes = restTemplate.getForObject(imageUrl, byte[].class);
            if (imageBytes == null) {
                log.warn("Nie udało się pobrać obrazu, pusty wynik z URL: {}", imageUrl);
                return null;
            }

            // Tworzenie podfolderu dla użytkownika
            String userDirPath = UPLOAD_DIR + xUsername + "/";
            File userDir = new File(userDirPath);
            if (!userDir.exists()) {
                userDir.mkdirs();
            }

            String fileName = UUID.randomUUID().toString() + ".jpg";
            Path filePath = Paths.get(userDirPath + fileName);

            Files.write(filePath, imageBytes);
            log.info("Pomyślnie zapisano dowód kuponu do: {}", filePath);

            return "/images/profiles/" + xUsername + "/" + fileName;

        } catch (Exception e) {
            log.error("Błąd podczas pobierania lub zapisu obrazu z {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Zapisuje wgrany obraz kuponu do uploads/profiles/<subdir>/ i zwraca ścieżkę
     * w formacie /images/profiles/... (serwowaną przez WebMvcConfig).
     */
    public String saveUploadedImage(MultipartFile file, String subdir) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Pusty plik obrazu");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Nieobsługiwany typ pliku: " + contentType);
        }
        try {
            String safeSub = (subdir == null || subdir.isBlank())
                    ? "manual"
                    : subdir.replaceAll("[^a-zA-Z0-9_/-]", "_");
            String dirPath = UPLOAD_DIR + safeSub + "/";
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String fileName = UUID.randomUUID() + resolveExtension(contentType);
            Path filePath = Paths.get(dirPath + fileName);
            Files.write(filePath, file.getBytes());
            log.info("Zapisano wgrany dowód kuponu do: {}", filePath);
            return "/images/profiles/" + safeSub + "/" + fileName;
        } catch (IOException e) {
            log.error("Błąd zapisu wgranego obrazu: {}", e.getMessage());
            throw new RuntimeException("Nie udało się zapisać obrazu", e);
        }
    }

    private String resolveExtension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
