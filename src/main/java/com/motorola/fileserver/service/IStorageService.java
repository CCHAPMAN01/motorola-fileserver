package com.motorola.fileserver.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IStorageService {

    void store(MultipartFile file);

    ResponseEntity<Resource> download(String filename);

    void delete(String filename);

    List<String> retrieveFilesList();
}
