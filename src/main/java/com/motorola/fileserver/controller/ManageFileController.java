package com.motorola.fileserver.controller;

import com.motorola.fileserver.service.StorageService;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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
    private final StorageService storageService;

    public ManageFileController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Upload a file to the server
     *
     * @param file The file to be uploaded/stored
     */
    @PostMapping("/")
    public ResponseEntity<String> uploadFile(@RequestParam("file") @Nonnull MultipartFile file) {
        LOGGER.trace("Enter uploadFile");

        storageService.store(file);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Successfully uploaded: " + file.getOriginalFilename());
    }


}
