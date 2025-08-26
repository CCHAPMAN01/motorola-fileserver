package com.motorola.fileserver.controller;

import com.motorola.fileserver.exception.DownloadException;
import com.motorola.fileserver.exception.FileValidationException;
import com.motorola.fileserver.exception.StorageException;
import com.motorola.fileserver.service.IStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
public class ManageFileControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IStorageService storageService;

    @Test
    public void testUpload_shouldSaveUploadedFile() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "test.txt",
                "text/plain", "Spring Framework".getBytes());
        this.mvc.perform(multipart("/upload").file(multipartFile))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("Successfully uploaded: test.txt")));

        then(this.storageService).should().store(multipartFile);
    }

    @Test
    public void testDownload_shouldDownloadFile() throws Exception {
        String filename = "test_file.txt";

        Resource mockResult = Mockito.mock();
        when(storageService.download(filename)).thenReturn(ResponseEntity.ok(mockResult));

        this.mvc.perform(get("/download/" + filename))
                .andExpect(status().isOk());
    }

    @Test
    public void testDownload_fileNotFound() throws Exception {
        String filename = "invalid_file.csv";

        when(storageService.download(filename))
                .thenThrow(new DownloadException("File " + filename + " does not exist."));

        this.mvc.perform(get("/download/" + filename))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDownload_internalServerError() throws Exception {
        String filename = " ";

        when(storageService.download(filename))
                .thenThrow(new FileValidationException("Invalid filename"));

        this.mvc.perform(get("/download/" + filename))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDelete_shouldDeleteFile() throws Exception {
        String filename = "test_file.png";

        this.mvc.perform(delete("/delete/" + filename))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Successfully deleted: test_file.png")));
    }

    @Test
    public void testDelete_fileNotFound() throws Exception {
        String filename = "invalid.file";

        doThrow(new StorageException("File not found")).when(storageService).delete(filename);

        this.mvc.perform(delete("/delete/" + filename))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDelete_invalidFilename() throws Exception {
        String filename = "..";

        doThrow(new FileValidationException("Invalid filename")).when(storageService).delete(filename);

        this.mvc.perform(delete("/delete/" + filename))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testList_shouldListFiles() throws Exception {
        List<String> filenames = List.of("test_file.txt", "some_file.csv", "temp_pic.png");

        when(storageService.retrieveFilesList()).thenReturn(filenames);

        this.mvc.perform(get("/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("test_file.txt"))
                .andExpect(jsonPath("$[1]").value("some_file.csv"))
                .andExpect(jsonPath("$[2]").value("temp_pic.png"));
    }

    @Test
    public void testList_storageException() throws Exception {
        when(storageService.retrieveFilesList()).thenThrow(new StorageException("Unable to retrieve files list"));

        this.mvc.perform(get("/list"))
                .andExpect(status().isInternalServerError());
    }

}
