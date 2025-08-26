package com.motorola.fileserver.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    void store(MultipartFile file);

    ResponseEntity<Resource> download(String filename, HttpServletRequest request);
}
