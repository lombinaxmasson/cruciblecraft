package com.masson.cruciblecraft.gametest.support;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * GameTest failure text must stay short. A longer message is written into a
 * lectern book while the server shuts down and the save then fails.
 */
public final class GameTestFailures {
    public static final int LIMIT = 900;

    private GameTestFailures() {}

    public static String truncate(String message) {
        if (message == null || message.length() <= LIMIT) {
            return message == null ? "" : message;
        }
        return message.substring(0, LIMIT - 3) + "...";
    }

    public static void fail(GameTestHelper helper, String message) {
        helper.fail(truncate(message));
    }
}
