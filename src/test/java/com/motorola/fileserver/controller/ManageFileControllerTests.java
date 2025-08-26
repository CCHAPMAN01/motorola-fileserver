package com.motorola.fileserver.controller;

import com.motorola.fileserver.exception.DownloadException;
import com.motorola.fileserver.exception.FileValidationException;
import com.motorola.fileserver.service.IStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
public class ManageFileControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IStorageService storageService;

    @Test
    public void testUploadFile_shouldSaveUploadedFile() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "test.txt",
                "text/plain", "Spring Framework".getBytes());
        this.mvc.perform(multipart("/upload").file(multipartFile))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("Successfully uploaded: test.txt")));

        then(this.storageService).should().store(multipartFile);
    }

    @Test
    public void testDeleteFile_shouldRemoveDeletedFile() throws Exception {
        String filename = "test_file.txt";

        MockHttpServletRequest request = new MockHttpServletRequest();
        Resource mockResult = new UrlResource(Paths.get(filename).normalize().toAbsolutePath().toUri());
        when(storageService.download(filename)).thenReturn(ResponseEntity.ok(mockResult));

        this.mvc.perform(get("/download/" + filename))
                .andExpect(status().isOk());
    }

    @Test
    public void testDeleteFile_fileNotFound() throws Exception {
        String filename = "invalid_file.csv";

        when(storageService.download(Mockito.any()))
                .thenThrow(new DownloadException("File " + filename + " does not exist."));

        this.mvc.perform(get("/download/" + filename))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteFile_internalServerError() throws Exception {
        String filename = " ";

        when(storageService.download(Mockito.any()))
                .thenThrow(new FileValidationException("Invalid filename"));

        this.mvc.perform(get("/download/" + filename))
                .andExpect(status().isBadRequest());
    }

}
