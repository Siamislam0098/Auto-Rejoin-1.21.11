package com.lannister.autorejoin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RejoinScheduler {

    private static final RejoinScheduler INSTANCE = new RejoinScheduler();
    public static RejoinScheduler getInstance() { return INSTANCE; }
    private RejoinScheduler() {}

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "AutoRejoin-Thread");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> countdownFuture;
    private ScheduledFuture<?> rejoinFuture;

    private final AtomicBoolean active           = new AtomicBoolean(false);
    private final AtomicInteger secondsRemaining = new AtomicInteger(0);

    private volatile String serverHost;
    private volatile int    serverPort;
    private volatile String serverName;

    public void scheduleRejoin(String host, int port, String name) {
        cancelPending();

        serverHost = host;
        serverPort = port;
        serverName = name;

        int delay = AutoRejoinConfig.getRejoinDelaySecs();
        secondsRemaining.set(delay);
        active.set(true);

        AutoRejoinClient.LOGGER.info(
                "[AutoRejoin] Scheduled rejoin to \"{}\" ({}:{}) in {} seconds.",
                name, host, port, delay);

        countdownFuture = executor.scheduleAtFixedRate(() -> {
            if (secondsRemaining.decrementAndGet() <= 0) stopCountdown();
        }, 1, 1, TimeUnit.SECONDS);

        rejoinFuture = executor.schedule(this::doRejoin, delay, TimeUnit.SECONDS);
    }

    public void cancelPending() {
        active.set(false);
        secondsRemaining.set(0);
        stopCountdown();
        if (rejoinFuture != null && !rejoinFuture.isDone()) {
            rejoinFuture.cancel(false);
            rejoinFuture = null;
            AutoRejoinClient.LOGGER.info("[AutoRejoin] Pending rejoin canceled.");
        }
    }

    public boolean isActive()            { return active.get(); }
    public int     getSecondsRemaining() { return Math.max(0, secondsRemaining.get()); }
    public String  getServerName()       { return serverName != null ? serverName : "Unknown"; }

    public String getFormattedTimeRemaining() {
        int s = getSecondsRemaining();
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private void stopCountdown() {
        if (countdownFuture != null && !countdownFuture.isDone()) {
            countdownFuture.cancel(false);
            countdownFuture = null;
        }
    }

    private void doRejoin() {
        active.set(false);

        if (serverHost == null || serverHost.isEmpty()) {
            AutoRejoinClient.LOGGER.warn("[AutoRejoin] No stored server address. Aborting.");
            return;
        }

        AutoRejoinClient.LOGGER.info("[AutoRejoin] Connecting to \"{}\" ({}:{})...",
                serverName, serverHost, serverPort);

        MinecraftClient client = MinecraftClient.getInstance();

        client.execute(() -> {
            try {
                String addressStr = serverHost + ":" + serverPort;

                ServerInfo info = new ServerInfo(
                        serverName != null ? serverName : serverHost,
                        addressStr,
                        ServerInfo.ServerType.OTHER
                );

                ServerAddress address = ServerAddress.parse(addressStr);

                // Confirmed from Yarn 1.21.4+ docs — ConnectScreen is in multiplayer package:
                // static void connect(Screen, MinecraftClient, ServerAddress, ServerInfo,
                //                     boolean quickPlay, @Nullable CookieStorage)
                ConnectScreen.connect(
                        new MultiplayerScreen(new TitleScreen()),
                        client,
                        address,
                        info,
                        false,
                        null
                );

                AutoRejoinClient.LOGGER.info("[AutoRejoin] ConnectScreen opened for {}:{}.",
                        serverHost, serverPort);

            } catch (Exception e) {
                AutoRejoinClient.LOGGER.error("[AutoRejoin] Rejoin failed: {}", e.getMessage(), e);
            }
        });
    }
}