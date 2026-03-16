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

    @Unique private boolean autoRejoin$scheduled = false;

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void autoRejoin$onInit(CallbackInfo ci) {
        if (autoRejoin$scheduled) {
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

        if (mc.getCurrentServerEntry() != null) {
            String raw = mc.getCurrentServerEntry().address;
            name = mc.getCurrentServerEntry().name;
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

        if (host == null) {
            AutoRejoinClient.LOGGER.warn("[AutoRejoin] No server address — skipping.");
            return;
        }

        Text reasonText = (info != null) ? info.reason() : this.title;

        if (!DisconnectReasonAnalyzer.shouldRejoin(reasonText)) {
            AutoRejoinClient.LOGGER.info("[AutoRejoin] Rejoin suppressed.");
            return;
        }

        final String fHost = host;
        final int    fPort = port;
        final String fName = name;

        autoRejoin$scheduled = true;
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