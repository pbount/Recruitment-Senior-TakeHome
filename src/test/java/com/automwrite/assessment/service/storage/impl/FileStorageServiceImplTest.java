package com.automwrite.assessment.service.storage.impl;

import com.automwrite.assessment.service.storage.*;
import com.automwrite.assessment.service.storage.impl.FileStorageServiceImpl;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileStorageServiceImplTest {

    private FileStorageServiceImpl fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageServiceImpl(tempDir.toString());
    }

    @Test
    @DisplayName("Write a document to storage and verify it exists")
    void testWriteDocument() throws IOException {
        /* Given */
        String fileName = "testDoc";
        FileCategory fileType = FileCategory.TONE_SOURCE;
        XWPFDocument document = new XWPFDocument();
        document.createParagraph().createRun().setText("This is a test document.");

        /* When */
        fileStorageService.write(fileName, fileType, document);

        /* Then */
        assertTrue(fileStorageService.exists(fileName, fileType), "Document should exist after being written");
    }

    @Test
    @DisplayName("Read a document from storage")
    void testReadDocument() throws IOException {
        /* Given */
        String fileName = "testDoc";
        FileCategory fileType = FileCategory.ORIGINAL_TONE;
        XWPFDocument document = new XWPFDocument();
        document.createParagraph().createRun().setText("This is a test document.");
        fileStorageService.write(fileName, fileType, document);

        /* When */
        XWPFDocument readDocument = fileStorageService.read(fileName, fileType);

        /* Then */
        assertNotNull(readDocument);
        assertEquals("This is a test document.", readDocument.getParagraphArray(0).getText(), "Read document content should match written content");
    }

    @Test
    @DisplayName("Delete a document from storage")
    void testDeleteDocument() throws IOException {
        /* Given */
        String fileName = "testDoc";
        FileCategory fileType = FileCategory.ADJUSTED_TONE;
        XWPFDocument document = new XWPFDocument();
        document.createParagraph().createRun().setText("This is a test document.");
        fileStorageService.write(fileName, fileType, document);

        /* When */
        fileStorageService.delete(fileName, fileType);

        /* Then */
        assertFalse(fileStorageService.exists(fileName, fileType), "Document should not exist after deletion");
    }

    @Test
    @DisplayName("List files in the storage directory")
    void testListFiles() throws IOException {
        /* Given */
        XWPFDocument document1 = new XWPFDocument();
        document1.createParagraph().createRun().setText("Document 1 content.");
        XWPFDocument document2 = new XWPFDocument();
        document2.createParagraph().createRun().setText("Document 2 content.");

        fileStorageService.write("testDoc1", FileCategory.TONE_SOURCE, document1);
        fileStorageService.write("testDoc2", FileCategory.ORIGINAL_TONE, document2);

        /* When */
        List<String> files = fileStorageService.list();

        /* Then */
        assertEquals(2, files.size(), "List should contain two files");
        assertTrue(files.contains("testDoc1-TONE_SOURCE"), "List should contain testDoc1-TONE_SOURCE");
        assertTrue(files.contains("testDoc2-ORIGINAL_TONE"), "List should contain testDoc2-ORIGINAL_TONE");
    }

    @Test
    @DisplayName("Verify normalized file name with different file types")
    void testGetNormalizedFileName() {
        /* Given */
        String fileName = "Sample Document";

        /* Then */
        assertEquals("Sample_Document-TONE_SOURCE", fileStorageService.getNormalizedFileName(fileName, FileCategory.TONE_SOURCE));
        assertEquals("Sample_Document-ORIGINAL_TONE", fileStorageService.getNormalizedFileName(fileName, FileCategory.ORIGINAL_TONE));
        assertEquals("Sample_Document-ADJUSTED_TONE", fileStorageService.getNormalizedFileName(fileName, FileCategory.ADJUSTED_TONE));
    }

    @Test
    @DisplayName("Handle file read when file does not exist")
    void testReadNonExistentFile() {
        /* Given */
        String fileName = "nonexistentDoc";
        FileCategory fileType = FileCategory.ORIGINAL_TONE;

        /* Then */
        Exception exception = assertThrows(RuntimeException.class, () -> fileStorageService.read(fileName, fileType));
        assertTrue(exception.getMessage().contains("Could not read file"), "Exception message should indicate file could not be read");
    }

    @Test
    @DisplayName("Handle file deletion when file does not exist")
    void testDeleteNonExistentFile() {
        /* Given */
        String fileName = "nonexistentDoc";
        FileCategory fileType = FileCategory.ADJUSTED_TONE;

        /* Then */
        assertDoesNotThrow(() -> fileStorageService.delete(fileName, fileType), "Deleting a non-existent file should not throw an exception");
    }
}
