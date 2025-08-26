package com.motorola.fileserver.util;

import com.motorola.fileserver.exception.FileValidationException;
import org.springframework.web.multipart.MultipartFile;

public class FileValidator {

    public static String getValidFileForUpload(MultipartFile file) {

        if (file.isEmpty()) {
            throw new FileValidationException("Failed to store empty file.");
        }

        String filename = file.getOriginalFilename();
        validateFilename(filename);

        // additional validation checks could be implemented here for file type/size

        return filename;
    }

    public static void validateFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new FileValidationException("Invalid filename.");
        }
    }
}
