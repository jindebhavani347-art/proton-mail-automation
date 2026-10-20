package com.qa.proton.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/** Tiny metrics sink: prints and appends measurements (e.g. delivery latency) to reports/metrics.csv. */
public final class Metrics {
    private static final Path FILE = Path.of("reports", "metrics.csv");

    private Metrics() {}

    public static synchronized void record(String name, long value) {
        System.out.println("[metric] " + name + "=" + value);
        try {
            Files.createDirectories(FILE.getParent());
            if (!Files.exists(FILE)) Files.writeString(FILE, "timestamp,metric,value\n");
            Files.writeString(FILE, Instant.now() + "," + name + "," + value + "\n", StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // metrics must never fail a test
        }
    }
}
