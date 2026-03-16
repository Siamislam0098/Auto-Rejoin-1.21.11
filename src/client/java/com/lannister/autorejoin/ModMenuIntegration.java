package com.lannister.autorejoin;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            AutoRejoinConfig cfg = AutoRejoinConfig.get();

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Auto Rejoin Settings"))
                    .setSavingRunnable(AutoRejoinConfig::save);

            ConfigCategory general = builder.getOrCreateCategory(
                    Text.literal("General"));

            ConfigEntryBuilder entries = builder.entryBuilder();

            // Toggle — enable/disable the mod entirely
            general.addEntry(entries
                    .startBooleanToggle(
                            Text.literal("Enable Auto-Rejoin"),
                            cfg.autoRejoinEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Toggle automatic rejoining on or off."))
                    .setSaveConsumer(val -> cfg.autoRejoinEnabled = val)
                    .build());

            // Slider — rejoin delay in seconds (10s to 600s)
            general.addEntry(entries
                    .startIntSlider(
                            Text.literal("Rejoin Delay (seconds)"),
                            cfg.rejoinDelaySecs,
                            10,   // min: 10 seconds
                            600)  // max: 10 minutes
                    .setDefaultValue(180)
                    .setTooltip(
                            Text.literal("How long to wait before rejoining."),
                            Text.literal("Default: 180 seconds (3 minutes)"))
                    .setTextGetter(val -> {
                        int mins = val / 60;
                        int secs = val % 60;
                        if (mins > 0) {
                            return Text.literal(val + "s  (" + mins + "m " + secs + "s)");
                        }
                        return Text.literal(val + " seconds");
                    })
                    .setSaveConsumer(val -> cfg.rejoinDelaySecs = val)
                    .build());

            return builder.build();
        };
    }
}