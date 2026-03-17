package com.lannister.autorejoin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AutoRejoinConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("autorejoin.json");

    private static AutoRejoinConfig INSTANCE = new AutoRejoinConfig();

    public int rejoinDelaySecs = 180;
    public boolean autoRejoinEnabled = true;

    public static AutoRejoinConfig get() { return INSTANCE; }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            INSTANCE = GSON.fromJson(reader, AutoRejoinConfig.class);
            INSTANCE.rejoinDelaySecs = clamp(INSTANCE.rejoinDelaySecs, 10, 600);
        } catch (IOException e) {
            AutoRejoinClient.LOGGER.error("[AutoRejoin] Failed to load config: {}", e.getMessage());
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            AutoRejoinClient.LOGGER.error("[AutoRejoin] Failed to save config: {}", e.getMessage());
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int getRejoinDelaySecs() {
        return INSTANCE.rejoinDelaySecs;
    }

    public static final String[] REJOIN_TRIGGER_PHRASES = {
            "server is restarting", "server restarting", "restarting", "restart",
            "maintenance", "under maintenance", "in maintenance",
            "failed to connect", "connection refused", "cannot connect",
            "no further information", "disconnected",
            "kicked", "timed out", "connection lost", "connection reset",
            "server closed", "end of stream", "read timed out",
            "connect timed out", "io.netty",
            "server full", "try again", "please wait",
            "back soon", "temporarily", "unavailable"
    };

    public static final String[] REJOIN_BLOCK_PHRASES = {
            "banned", "you are banned", "permanent", "ip ban",
            "multiplayer is disabled", "not authenticated",
            "invalid session", "outdated client", "outdated server",
            "incompatible"
    };
}
