package com.qa.proton.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Unique, self-describing test data so tests never collide and are easy to find/clean in the mailbox. */
public final class TestData {
    private TestData() {}

    public static String subject(String tag) {
        return "QA-" + tag + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String body(String tag) {
        return "Automated body for " + tag + " " + UUID.randomUUID();
    }

    /** Creates a small text file with a unique name and returns its path. */
    public static Path attachmentFile(String content) {
        try {
            Path dir = Files.createTempDirectory("qa-attach");
            Path file = dir.resolve("report-" + UUID.randomUUID().toString().substring(0, 6) + ".txt");
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Could not create attachment fixture", e);
        }
    }
}
