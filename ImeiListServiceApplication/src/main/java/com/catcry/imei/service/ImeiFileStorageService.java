package com.catcry.imei.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

@Service
public class ImeiFileStorageService {

    private final Path storageDir;

    public ImeiFileStorageService(@Value("${imei.storage.path}") String storagePath) {
        this.storageDir = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    private static final DateTimeFormatter DATE_PART = DateTimeFormatter.ofPattern("uuuuDDD");
    private static final DateTimeFormatter TIME_PART = DateTimeFormatter.ofPattern("HHmmss");
    public Path save(String operation, String batchId, List<String> imeis) {
        try {
            Files.createDirectories(storageDir);

            String safeBatch = batchId.replaceAll("[^A-Za-z0-9-]", "_");
            LocalDateTime now = LocalDateTime.now();
            String fileName = operation + "_" + safeBatch
                    + "_" + now.format(DATE_PART)
                    + "_" + now.format(TIME_PART) + ".csv";

            Path target = storageDir.resolve(fileName);
            Path temp = storageDir.resolve(fileName + ".tmp");

            Files.write(temp, imeis, StandardCharsets.UTF_8);
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save IMEI file", e);
        }
    }
}
