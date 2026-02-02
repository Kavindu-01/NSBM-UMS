package lk.nsbm.university.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class MediaStorageService {

    private final Path mediaRoot;

    public MediaStorageService(@Value("${storage.media-root:uploads/media}") String mediaRootLocation) {
        this.mediaRoot = Paths.get(mediaRootLocation).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.mediaRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize media directory", ex);
        }
    }

    public String store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String safeDirectory = StringUtils.hasText(subDirectory) ? sanitizeDirectory(subDirectory) : "general";
        String filename = buildFileName(file.getOriginalFilename());
        Path destination = mediaRoot.resolve(safeDirectory).resolve(filename);
        try {
            Files.createDirectories(destination.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store uploaded file", ex);
        }
        return "/media/" + safeDirectory + "/" + filename;
    }

    private String sanitizeDirectory(String directory) {
        String cleaned = directory.trim().replaceAll("[^A-Za-z0-9-_]", "");
        return cleaned.isBlank() ? "general" : cleaned.toLowerCase();
    }

    private String buildFileName(String originalFilename) {
        String extension = "";
        if (StringUtils.hasText(originalFilename)) {
            String cleaned = Paths.get(originalFilename).getFileName().toString();
            int dotIndex = cleaned.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < cleaned.length() - 1) {
                extension = cleaned.substring(dotIndex).toLowerCase();
            }
        }
        return UUID.randomUUID().toString().replaceAll("-", "") + extension;
    }
}
