package com.automwrite.assessment.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ToneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Test
    void transformTone_ShouldReturnOk() throws Exception {
        assertThat(apiKey).isNotEmpty();
        Path toneFilePath = Paths.get("different tones/automwrite - A - Casual tone.docx");
        Path contentFilePath = Paths.get("different tones/automwrite - B - Formal tone.docx");
        MockMultipartFile toneFile = new MockMultipartFile(
                "toneFile",
                toneFilePath.getFileName().toString(),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                Files.readAllBytes(toneFilePath)
        );
        MockMultipartFile contentFile = new MockMultipartFile(
                "contentFile",
                contentFilePath.getFileName().toString(),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                Files.readAllBytes(contentFilePath)
        );
        mockMvc.perform(multipart("/api/transform/tone")
                        .file(toneFile)
                        .file(contentFile))
                .andExpect(status().isOk());
    }
}
