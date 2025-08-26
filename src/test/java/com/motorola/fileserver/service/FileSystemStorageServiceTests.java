package com.motorola.fileserver.service;

import com.motorola.fileserver.config.StorageProperties;
import com.motorola.fileserver.exception.DownloadException;
import com.motorola.fileserver.exception.FileValidationException;
import com.motorola.fileserver.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
public class FileSystemStorageServiceTests {

    private FileSystemStorageService storageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // simulate injected configuration properties
        StorageProperties props = new StorageProperties();
        props.setLocation(tempDir.toString());

        // instantiate service with properties
        storageService = new FileSystemStorageService(props);
    }

    @Test
    public void testStoreHappyPath() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.txt");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("Test file".getBytes()));

        storageService.store(file);

        Path storedFile = Files.walk(tempDir)
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow();
        String content = Files.readString(storedFile);

        assertThat(content).isEqualTo("Test file");
    }

    @Test
    public void testStoreEmptyFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("empty file");
    }

    @Test
    public void testStoreInvalidFilename() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn(null);

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("Invalid filename");
    }

    @Test
    public void testStoreIOException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("fail.txt");
        when(file.getInputStream()).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to store file");
    }

    @Test
    public void testOverwritesExistingFile() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.txt");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("Test file".getBytes()));

        storageService.store(file);

        Path storedFile = Files.walk(tempDir)
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow();
        String content = Files.readString(storedFile);

        assertThat(content).isEqualTo("Test file");

        // update the file content and re-upload (expect content to be overwritten)
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("New file content".getBytes()));
        storageService.store(file);
        assertThat(Files.readString(storedFile)).isEqualTo("New file content");
    }

    @Test
    public void testDownloadFile() throws IOException {
        String filename = "test.txt";

        Path filepath = tempDir.resolve(filename);
        List<String> fileContent = List.of("Test file");
        Files.write(filepath, fileContent);

        ResponseEntity<Resource> response = storageService.download(filename);
        assertThat(response.getBody()).isNotNull();

        File file = response.getBody().getFile();
        assertThat(file.isFile()).isTrue();
        assertThat(file.getName()).isEqualTo(filename);
        assertThat(Files.readAllLines(file.toPath())).isEqualTo(fileContent);
    }

    @Test
    public void testDownloadFileNotFound() {
        String filename = "test.txt";

        assertThatExceptionOfType(DownloadException.class)
                .isThrownBy(() -> storageService.download(filename));
    }

    @Test
    public void testDeleteFile() throws IOException {
        String filename = "test.txt";

        Path filepath = tempDir.resolve(filename);
        List<String> fileContent = List.of("Test file");
        Files.write(filepath, fileContent);

        assertThat(Files.exists(filepath)).isTrue();

        storageService.delete(filename);

        assertThat(Files.exists(filepath)).isFalse();
    }

    @Test
    public void testDelete_fileNotFound() throws IOException {
        String filename = "test.txt";

        Path filepath = tempDir.resolve(filename);

        assertThat(Files.exists(filepath)).isFalse();

        assertThatExceptionOfType(StorageException.class).isThrownBy(() ->  storageService.delete(filename));
    }

}
