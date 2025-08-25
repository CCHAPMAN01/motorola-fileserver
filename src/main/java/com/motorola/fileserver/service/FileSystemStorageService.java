package com.motorola.fileserver.service;

import com.motorola.fileserver.config.StorageProperties;
import com.motorola.fileserver.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Store uploaded files in a simple directory structure on the server
 */
@Service
public class FileSystemStorageService implements StorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemStorageService.class);
    private final Path rootLocation;

    @Autowired
    public FileSystemStorageService(StorageProperties properties) {

        String defaultLocation = properties.getLocation();

        if (defaultLocation.trim().isBlank()) {
            throw new StorageException("File upload location can not be empty.");
        }

        this.rootLocation = Paths.get(defaultLocation);

        // create root location directory if it does not already exist
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void store(MultipartFile file) {
        try {

            String filename = getValidFileForUpload(file);
            LOGGER.debug("Filename to upload: {filename}");

            Path destinationFile = this.rootLocation.resolve(Paths.get(filename))
                    .normalize().toAbsolutePath();
            LOGGER.trace("Destination file absolute path: {destinationFile}");

            try (InputStream inputStream = file.getInputStream()) {
                Files.createDirectories(destinationFile.getParent());
                Files.copy(inputStream, destinationFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    private String getValidFileForUpload(MultipartFile file) {

        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new StorageException("Invalid filename.");
        }

        return filename;
    }
}
