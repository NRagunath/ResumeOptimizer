package com.resumeopt.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResumeParserServiceTest {

    private final ResumeParserService service = new ResumeParserService();

    @Test
    void extractText_shouldReturnContent_whenTxtFileIsProvided() throws IOException {
        String content = "Hello World";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                content.getBytes()
        );

        String result = service.extractText(file);

        assertEquals(content, result);
    }
}
