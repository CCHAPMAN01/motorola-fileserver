package com.motorola.fileserver.service;

import com.motorola.fileserver.config.StorageProperties;
import com.motorola.fileserver.exception.FileValidationException;
import com.motorola.fileserver.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    public void testFileSystemStorageService_happyPath() throws IOException {
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
    public void testFileSystemStorageService_emptyFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("empty file");
    }

    @Test
    public void testFileSystemStorageService_invalidFilename() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn(null);

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("Invalid filename");
    }

    @Test
    public void testFileSystemStorageService_ioException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("fail.txt");
        when(file.getInputStream()).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to store file");
    }

    @Test
    public void testFileSystemStorageService_overwritesExistingFile() throws IOException {
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

}
