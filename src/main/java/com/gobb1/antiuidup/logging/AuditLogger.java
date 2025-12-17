package com.gobb1.antiuidup.logging;

import com.gobb1.antiuidup.config.AntiUiDupConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public final class AuditLogger {
    private AuditLogger() {}

    private static Path filePath;
    private static volatile AntiUiDupConfig cfg;
    private static Logger console;

    public static void init(Path logFile, AntiUiDupConfig config, Logger consoleLogger) {
        filePath = logFile;
        cfg = config;
        console = consoleLogger;
        try {
            Files.createDirectories(filePath.getParent());
        } catch (Exception ignored) {}
    }

    public static void applyConfig(AntiUiDupConfig config) {
        if (config != null) cfg = config;
    }

    public static void info(String msg) {
        AntiUiDupConfig c = cfg;
        if (c != null && c.logToConsole && console != null) console.info(msg);
        writeToFile("INFO", msg);
    }

    public static void warn(String msg) {
        AntiUiDupConfig c = cfg;
        if (c != null && c.logToConsole && console != null) console.warn(msg);
        writeToFile("WARN", msg);
    }

    public static void error(String msg) {
        AntiUiDupConfig c = cfg;
        if (c != null && c.logToConsole && console != null) console.error(msg);
        writeToFile("ERROR", msg);
    }

    private static void writeToFile(String level, String msg) {
        AntiUiDupConfig c = cfg;
        if (c == null || !c.logToFile || filePath == null) return;

        String line = "[" + Instant.now() + "] [" + level + "] " + msg + "\n";
        try {
            Files.writeString(filePath, line, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }
}
