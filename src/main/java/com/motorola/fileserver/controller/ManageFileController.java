package com.motorola.fileserver.controller;

import com.motorola.fileserver.exception.DownloadException;
import com.motorola.fileserver.exception.FileValidationException;
import com.motorola.fileserver.exception.StorageException;
import com.motorola.fileserver.service.IStorageService;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Exposes REST API endpoints/operations for managing files
 */
@Controller
public class ManageFileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManageFileController.class);

    @Autowired
    private final IStorageService storageService;

    public ManageFileController(IStorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Upload a file to the server
     *
     * @param file The file to be uploaded/stored
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") @Nonnull MultipartFile file) {
        LOGGER.trace("Enter uploadFile");

        storageService.store(file);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Successfully uploaded: " + file.getOriginalFilename());
    }

    /**
     * Download a given file by name from the server
     *
     * @param filename String representing the name of the file to be downloaded - includes regex to allow for '.' chars
     */
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable @Nonnull String filename) {
        LOGGER.trace("Enter downloadFile");

        try {
            return storageService.download(filename);
        } catch (DownloadException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (FileValidationException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

    /**
     * Delete a file by name from the server
     *
     * @param filename String representing the name of the file to be deleted - includes regex to allow for '.' chars
     */
    @DeleteMapping("/delete/{filename:.+}")
    public ResponseEntity<String> deleteFile(@PathVariable @Nonnull String filename) {
        LOGGER.trace("Enter deleteFile");

        try {
            storageService.delete(filename);
            return ResponseEntity.ok().body("Successfully deleted: " + filename);
        } catch (StorageException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (FileValidationException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }


}
