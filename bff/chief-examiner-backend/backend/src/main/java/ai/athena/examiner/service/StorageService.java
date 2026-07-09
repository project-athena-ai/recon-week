package ai.athena.examiner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Local filesystem storage (spec: "Local filesystem or MinIO").
 * Swappable for MinIO later without touching the pipeline.
 */
@Service
public class StorageService {

    private final Path root;

    public StorageService(@Value("${athena.storage-dir}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage dir " + root, e);
        }
    }

    public Path save(UUID scriptId, MultipartFile file) {
        Path target = root.resolve(scriptId + ".pdf");
        try {
            file.transferTo(target);
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store " + file.getOriginalFilename(), e);
        }
    }

    public Path resolve(String storagePath) {
        return Path.of(storagePath);
    }
}
