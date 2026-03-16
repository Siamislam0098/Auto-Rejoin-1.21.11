package com.lannister.autorejoin;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoRejoinClient implements ClientModInitializer {

    public static final String MOD_ID = "autorejoin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        AutoRejoinConfig.load();
        LOGGER.info("[AutoRejoin] Initialized. Rejoin delay: {} seconds.",
                AutoRejoinConfig.getRejoinDelaySecs());
    }
}