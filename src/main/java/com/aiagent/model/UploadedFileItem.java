package com.aiagent.model;

import java.io.File;
import java.util.Objects;

public final class UploadedFileItem {
    private final File file;

    public UploadedFileItem(File file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return file.getName();
    }

    public long getSizeBytes() {
        return file.length();
    }
}

