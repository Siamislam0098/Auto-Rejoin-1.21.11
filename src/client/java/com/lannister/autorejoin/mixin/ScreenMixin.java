package com.lannister.autorejoin.mixin;

import com.lannister.autorejoin.AutoRejoinClient;
import com.lannister.autorejoin.RejoinScheduler;
import com.lannister.autorejoin.ScreenRejoinState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin implements ScreenRejoinState {

    @Unique
    private ButtonWidget autoRejoin$cancelBtn = null;

    @Override
    public ButtonWidget autoRejoin$getCancelBtn() {
        return autoRejoin$cancelBtn;
    }

    @Override
    public void autoRejoin$setCancelBtn(ButtonWidget btn) {
        this.autoRejoin$cancelBtn = btn;
    }

    // tick() exists on Screen — resolves fine here
    @Inject(method = "tick", at = @At("HEAD"))
    private void autoRejoin$onTick(CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof DisconnectedScreen)) return;

        RejoinScheduler sched = RejoinScheduler.getInstance();
        Screen screen = (Screen) self;
        String timer = sched.getFormattedTimeRemaining();

        for (Element child : screen.children()) {
            if (!(child instanceof ButtonWidget btn)) continue;
            String label = btn.getMessage().getString();

            if (sched.isActive()) {
                // Update back/disconnect buttons to show the timer
                if (label.startsWith("Back to") || label.startsWith("Disconnect")) {
                    btn.setMessage(Text.literal("Rejoining in " + timer + "..."));
                }
                // Keep already-updated buttons current
                if (label.startsWith("Rejoining in")) {
                    btn.setMessage(Text.literal("Rejoining in " + timer + "..."));
                }
            } else {
                // Rejoin was canceled — restore original label
                if (label.startsWith("Rejoining in")) {
                    btn.setMessage(Text.literal("Back to Server List"));
                }
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void autoRejoin$onRender(DrawContext ctx, int mouseX, int mouseY,
                                     float delta, CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof DisconnectedScreen)) return;

        RejoinScheduler sched = RejoinScheduler.getInstance();
        if (!sched.isActive()) return;

        Screen screen = (Screen) self;

        if (autoRejoin$cancelBtn != null) {
            autoRejoin$cancelBtn.setPosition(screen.width / 2 - 100, screen.height / 2 + 60);
        }

        var textRenderer = MinecraftClient.getInstance().textRenderer;
        int cx = screen.width / 2;

        ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Auto-rejoining in: ")
                        .append(Text.literal(sched.getFormattedTimeRemaining()).withColor(0x55FF55)),
                cx, screen.height / 2 + 85, 0xFFFF55);

        ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Server: ")
                        .append(Text.literal(sched.getServerName()).withColor(0xFFFFFF)),
                cx, screen.height / 2 + 97, 0xAAAAAA);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void autoRejoin$onRemoved(CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof DisconnectedScreen)) return;

        if (RejoinScheduler.getInstance().isActive()) {
            AutoRejoinClient.LOGGER.info("[AutoRejoin] Screen closed — canceling rejoin.");
            RejoinScheduler.getInstance().cancelPending();
        }
    }
}