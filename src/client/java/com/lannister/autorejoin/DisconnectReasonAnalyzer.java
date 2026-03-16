package com.lannister.autorejoin;

import net.minecraft.text.Text;

public final class DisconnectReasonAnalyzer {

    private DisconnectReasonAnalyzer() {}

    /**
     * Returns true if the given disconnect reason should trigger auto-rejoin.
     *
     * Logic (in order):
     *   1. null / empty reason  -> always rejoin (network drop / restart)
     *   2. matches a BLOCK phrase -> never rejoin (ban, auth failure, etc.)
     *   3. matches a TRIGGER phrase -> rejoin
     *   4. very short reason (<5 chars) -> rejoin (probably a bare network error)
     *   5. everything else -> do NOT rejoin
     */

    public static boolean shouldRejoin(Text reason) {

        // Check if mod is enabled in config
        if (!AutoRejoinConfig.get().autoRejoinEnabled) {
            AutoRejoinClient.LOGGER.info("[AutoRejoin] Mod is disabled in config.");
            return false;
        }

        if (reason == null) {
            AutoRejoinClient.LOGGER.info("[AutoRejoin] No disconnect reason - triggering rejoin.");
            return true;
        }

        String raw = reason.getString().toLowerCase().trim();
        AutoRejoinClient.LOGGER.info("[AutoRejoin] Disconnect reason: \"{}\"", raw);

        // Block phrases take priority
        for (String blocked : AutoRejoinConfig.REJOIN_BLOCK_PHRASES) {
            if (raw.contains(blocked)) {
                AutoRejoinClient.LOGGER.info("[AutoRejoin] Rejoin BLOCKED - matched: \"{}\"", blocked);
                return false;
            }
        }

        // Trigger phrases
        for (String trigger : AutoRejoinConfig.REJOIN_TRIGGER_PHRASES) {
            if (raw.contains(trigger)) {
                AutoRejoinClient.LOGGER.info("[AutoRejoin] Rejoin TRIGGERED - matched: \"{}\"", trigger);
                return true;
            }
        }

        // Very short reason = bare network error
        if (raw.isEmpty() || raw.length() < 5) {
            AutoRejoinClient.LOGGER.info("[AutoRejoin] Rejoin TRIGGERED - reason too short to be a permanent kick.");
            return true;
        }

        AutoRejoinClient.LOGGER.info("[AutoRejoin] No rejoin triggered for this reason.");
        return false;
    }
}
