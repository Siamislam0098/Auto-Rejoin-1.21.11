package com.lannister.autorejoin.mixin;

import com.lannister.autorejoin.AutoRejoinClient;
import com.lannister.autorejoin.DisconnectReasonAnalyzer;
import com.lannister.autorejoin.RejoinScheduler;
import com.lannister.autorejoin.ScreenRejoinState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {

    @Shadow @Final private DisconnectionInfo info;

    @Unique private boolean autoRejoin$thisScreenScheduled = false;

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void autoRejoin$onInit(CallbackInfo ci) {

        // Screen was resized — just reposition the button
        if (autoRejoin$thisScreenScheduled) {
            ScreenRejoinState state = (ScreenRejoinState) this;
            ButtonWidget btn = state.autoRejoin$getCancelBtn();
            if (btn != null) {
                btn.setPosition(this.width / 2 - 100, this.height / 2 + 60);
            }
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        String host = null;
        int    port = 25565;
        String name = "Server";

        // Try getCurrentServerEntry first
        if (mc.getCurrentServerEntry() != null) {
            String raw = mc.getCurrentServerEntry().address;
            name = mc.getCurrentServerEntry().name;
            AutoRejoinClient.LOGGER.info("[AutoRejoin] Server entry found: {} ({})", name, raw);
            if (raw != null && !raw.isEmpty()) {
                int colon = raw.lastIndexOf(':');
                if (colon > 0 && colon < raw.length() - 1) {
                    host = raw.substring(0, colon);
                    try {
                        port = Integer.parseInt(raw.substring(colon + 1));
                    } catch (NumberFormatException ignored) {
                        port = 25565;
                    }
                } else {
                    host = raw;
                }
            }
        }

        // Fallback — try getServerAddress which is set even when connection fails
        if (host == null && mc.getNetworkHandler() != null
                && mc.getNetworkHandler().getServerInfo() != null) {
            String raw = mc.getNetworkHandler().getServerInfo().address;
            name = mc.getNetworkHandler().getServerInfo().name;
            AutoRejoinClient.LOGGER.info("[AutoRejoin] Fallback networkHandler address: {}", raw);
            if (raw != null && !raw.isEmpty()) {
                int colon = raw.lastIndexOf(':');
                if (colon > 0 && colon < raw.length() - 1) {
                    host = raw.substring(0, colon);
                    try {
                        port = Integer.parseInt(raw.substring(colon + 1));
                    } catch (NumberFormatException ignored) {
                        port = 25565;
                    }
                } else {
                    host = raw;
                }
            }
        }

        // Last resort — check RejoinScheduler's stored address from a previous attempt
        if (host == null) {
            String lastHost = RejoinScheduler.getInstance().getServerHost();
            int    lastPort = RejoinScheduler.getInstance().getServerPort();
            String lastName = RejoinScheduler.getInstance().getServerName();
            if (lastHost != null && !lastHost.isEmpty()) {
                AutoRejoinClient.LOGGER.info("[AutoRejoin] Using last known address: {}:{}", lastHost, lastPort);
                host = lastHost;
                port = lastPort;
                name = lastName;
            }
        }

        if (host == null) {
            AutoRejoinClient.LOGGER.warn("[AutoRejoin] No server address found anywhere — skipping.");
            return;
        }

        // Combine title + reason for better phrase matching
        // e.g. title = "Failed to connect to the server", reason = "Disconnected"
        String titleStr  = this.title != null ? this.title.getString() : "";
        String reasonStr = (info != null && info.reason() != null)
                ? info.reason().getString() : "";
        String combined  = (titleStr + " " + reasonStr).trim();

        AutoRejoinClient.LOGGER.info("[AutoRejoin] Title: \"{}\" | Reason: \"{}\"", titleStr, reasonStr);

        Text combinedText = Text.literal(combined);

        if (!DisconnectReasonAnalyzer.shouldRejoin(combinedText)) {
            AutoRejoinClient.LOGGER.info("[AutoRejoin] Rejoin suppressed.");
            return;
        }

        final String fHost = host;
        final int    fPort = port;
        final String fName = name;

        autoRejoin$thisScreenScheduled = true;
        RejoinScheduler.getInstance().scheduleRejoin(fHost, fPort, fName);

        ButtonWidget cancelBtn = ButtonWidget.builder(
                Text.literal("Cancel Auto-Rejoin"),
                b -> {
                    RejoinScheduler.getInstance().cancelPending();
                    b.active = false;
                    b.setMessage(Text.literal("Auto-Rejoin Canceled"));
                })
                .dimensions(this.width / 2 - 100, this.height / 2 + 60, 200, 20)
                .build();

        ScreenRejoinState state = (ScreenRejoinState) this;
        state.autoRejoin$setCancelBtn(cancelBtn);
        this.addDrawableChild(cancelBtn);
    }
}
