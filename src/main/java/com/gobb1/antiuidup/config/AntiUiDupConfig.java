package com.gobb1.antiuidup.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AntiUiDupConfig {
    public AntiUiDupConfig() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;

    public boolean logToConsole = true;
    public boolean logToFile = true;

    public boolean webhookEnabled = false;
    public String discordWebhookUrl = "";
    public int webhookCooldownSeconds = 10;
    public boolean webhookIncludeServerName = true;
    public String serverName = "server";

    public int maxViolationsInWindow = 3;
    public long windowMs = 2500;
    public long openGraceMs = 350;

    public boolean webhookOnInvalidContainerId = true;
    public boolean webhookOnIllegalWorldAction = true;
    public boolean webhookOnKick = true;

    public static AntiUiDupConfig loadOrCreate(Path path) {
        try {
            Files.createDirectories(path.getParent());

            if (!Files.exists(path)) {
                AntiUiDupConfig cfg = new AntiUiDupConfig();
                writeAtomic(path, cfg);
                return cfg;
            }

            String raw = Files.readString(path);
            AntiUiDupConfig cfg = GSON.fromJson(raw, AntiUiDupConfig.class);
            if (cfg == null) cfg = new AntiUiDupConfig();

            writeAtomic(path, cfg);
            return cfg;
        } catch (Exception e) {
            AntiUiDupConfig cfg = new AntiUiDupConfig();
            try { Files.createDirectories(path.getParent()); } catch (Exception ignored) {}
            return cfg;
        }
    }

    private static void writeAtomic(Path path, AntiUiDupConfig cfg) throws IOException {
        Path tmp = Files.createTempFile(path.getParent(), "antiuidup", ".tmp");
        Files.writeString(tmp, GSON.toJson(cfg));
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
