package com.grzechuhehe.SportsBettingManagerApp.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ImageStorageServiceTest {

    private final ImageStorageService service = new ImageStorageService(new RestTemplate());

    @Test
    void savesImageAndReturnsServablePath() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "image", "kupon.png", "image/png", new byte[]{1, 2, 3, 4});

        String path = service.saveUploadedImage(file, "manual/john");

        assertTrue(path.startsWith("/images/profiles/manual/john/"));
        assertTrue(path.endsWith(".png"));
        Path physical = Paths.get(path.replaceFirst("/images/profiles/", "uploads/profiles/"));
        assertTrue(Files.exists(physical));
        Files.deleteIfExists(physical);
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile(
                "image", "x.png", "image/png", new byte[]{});
        assertThrows(IllegalArgumentException.class,
                () -> service.saveUploadedImage(empty, "manual/john"));
    }

    @Test
    void rejectsNonImage() {
        MockMultipartFile pdf = new MockMultipartFile(
                "image", "x.pdf", "application/pdf", new byte[]{1, 2});
        assertThrows(IllegalArgumentException.class,
                () -> service.saveUploadedImage(pdf, "manual/john"));
    }
}
