package com.motorola.fileserver.service;

import com.motorola.fileserver.config.StorageProperties;
import com.motorola.fileserver.exception.DownloadException;
import com.motorola.fileserver.exception.StorageException;
import com.motorola.fileserver.util.FileValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

/**
 * Store uploaded files in a simple directory structure on the server
 */
@Service
public class FileSystemStorageService implements IStorageService {

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

    /**
     * Stores a multipart file to the root directory
     *
     * @param file MultipartFile received in the request. Must not be empty and must have a valid filename
     */
    @Override
    public void store(MultipartFile file) {
        try {
            String filename = FileValidator.getValidFileForUpload(file);
            LOGGER.debug("Filename to upload: " + filename);

            // use .normalize() to sanitise the the filepath and avoid directory traversal attacks
            Path destinationFile = this.rootLocation.resolve(Paths.get(filename))
                    .normalize().toAbsolutePath();
            LOGGER.trace("Destination file absolute path: " + destinationFile);

            try (InputStream inputStream = file.getInputStream()) {
                Files.createDirectories(destinationFile.getParent());
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    /**
     * Process the request to download a given file by filename.
     * Response content-type to be derived from the filename (if found) - otherwise defaults to
     * "application/octet-stream" so the browser does not try to render or execute the file, instead prompts for
     * download as per the content-disposition=attachment header
     *
     * @param filename String representing the name of the file to be downloaded
     * @return a response entity wrapper containing the file (resource) to be downloaded
     */
    @Override
    public ResponseEntity<Resource> download(String filename) {

        try {
            FileValidator.validateFilename(filename);
            LOGGER.debug("Filename to download: " + filename);

            Path filePath = this.rootLocation.resolve(filename).normalize().toAbsolutePath();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new DownloadException("File " + filename + " does not exist.");
            }

            String contentType = Files.probeContentType(resource.getFile().toPath());

            if (contentType == null) {
                LOGGER.info("Unable to determine MIME type - setting default content-type");
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                            resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            throw new DownloadException("Filename provided cannot be found.", e);
        } catch (IOException e) {
            throw new DownloadException("Unable to download file.", e);
        }
    }

    /**
     * Process the request to delete a given file by filename.
     *
     * @param filename String representing the name of the file to be deleted
     */
    @Override
    public void delete(String filename) {
        try {
            FileValidator.validateFilename(filename);
            LOGGER.debug("Filename to delete: " + filename);

            Path filePath = this.rootLocation.resolve(filename).normalize().toAbsolutePath();
            Files.delete(filePath);

        } catch (IOException e) {
            throw new StorageException("Unable to delete file", e);
        }
    }

    /**
     * List all file paths starting from the root directory
     *
     * @return a list of String representations of all file paths
     */
    @Override
    public List<String> retrieveFilesList() {
        Path root = this.rootLocation.normalize().toAbsolutePath();

        try (Stream<Path> filesStream = Files.list(root)) {
            return filesStream.map(file -> file.getFileName().toString()).toList();
        } catch (IOException e) {
            throw new StorageException("Unable to retrieve files list", e);
        }
    }

}
