package com.automwrite.assessment.service.storage.impl;

import com.automwrite.assessment.service.storage.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileStorageServiceImpl implements FileStorageService<XWPFDocument> {

    private final Path rootPath;

    public FileStorageServiceImpl(
            @Value("${upload.file.storage.path}") String rootPath
    ) {
        this.rootPath = Paths.get(rootPath);
        try {
            Files.createDirectories(this.rootPath); // Ensure the storage directory exists
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    @Override
    public List<String> list() {
        try {
            return Files.walk(rootPath, 1)
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Could not list files", e);
        }
    }

    @Override
    public boolean exists(String fileName, FileCategory fileType) {
        String normalizedFileName = getNormalizedFileName(fileName, fileType);
        Path filePath = rootPath.resolve(normalizedFileName);
        return Files.exists(filePath);
    }

    @Override
    public XWPFDocument read(String fileName, FileCategory fileType) {
        String normalizedFileName = getNormalizedFileName(fileName, fileType);
        Path filePath = rootPath.resolve(normalizedFileName);
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            return new XWPFDocument(fis);
        } catch (IOException e) {
            throw new RuntimeException("Could not read file: " + normalizedFileName, e);
        }
    }

    @Override
    public void write(String fileName, FileCategory fileType, XWPFDocument value) {
        String normalizedFileName = getNormalizedFileName(fileName, fileType);
        Path filePath = rootPath.resolve(normalizedFileName);
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            value.write(fos);
        } catch (IOException e) {
            throw new RuntimeException("Could not write file: " + normalizedFileName, e);
        }
    }

    @Override
    public void delete(String fileName, FileCategory fileType) {
        String normalizedFileName = getNormalizedFileName(fileName, fileType);
        Path filePath = rootPath.resolve(normalizedFileName);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete file: " + normalizedFileName, e);
        }
    }

    protected String getNormalizedFileName(String fileName, FileCategory fileType) {
        if (fileName == null || fileType == null) {
            throw new IllegalArgumentException("fileName and fileType cannot be null");
        }

        String normalizedFileName = fileName.replace(" ", "_");

        int dotIndex = normalizedFileName.lastIndexOf(".");
        if (dotIndex != -1) {
            String namePart = normalizedFileName.substring(0, dotIndex);
            String extensionPart = normalizedFileName.substring(dotIndex);
            return namePart + "-" + fileType.name() + extensionPart;
        } else {
            return normalizedFileName + "-" + fileType.name();
        }
    }
}