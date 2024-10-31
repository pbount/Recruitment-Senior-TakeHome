package com.automwrite.assessment.service.storage;

import java.util.List;

public interface FileStorageService<T> {

    List<String> list();

    boolean exists(String fileName, FileCategory fileType);

    T read(String fileName, FileCategory fileType);

    void write(String fileName, FileCategory fileType, T value);

    void delete(String fileName, FileCategory fileType);

}
