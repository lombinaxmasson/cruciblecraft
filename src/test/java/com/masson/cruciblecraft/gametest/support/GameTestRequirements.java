package com.masson.cruciblecraft.gametest.support;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;

/**
 * GameTest lookups that fail through {@link GameTestHelper#fail} instead of
 * an uncaught cast or {@code Optional} throw. An uncaught exception crashes
 * the test server tick and drops every later result.
 */
public final class GameTestRequirements {
    private GameTestRequirements() {}

    public static <T extends BlockEntity> T requireBlockEntity(
            GameTestHelper helper,
            BlockPos pos,
            Class<T> type) {
        BlockEntity entity = helper.getBlockEntity(pos);
        if (type.isInstance(entity)) {
            return type.cast(entity);
        }
        String message = "expected " + type.getSimpleName()
                + " at " + pos + " but found " + entity;
        GameTestFailures.fail(helper, message);
        throw new IllegalStateException(GameTestFailures.truncate(message));
    }

    public static <T> T requirePresent(
            GameTestHelper helper,
            Optional<T> optional,
            String message) {
        T value = optional.orElse(null);
        if (value != null) {
            return value;
        }
        GameTestFailures.fail(helper, message);
        throw new IllegalStateException(GameTestFailures.truncate(message));
    }

    public static <T, C> T requireCapability(
            GameTestHelper helper,
            BlockCapability<T, C> capability,
            BlockPos pos,
            C context,
            String message) {
        T value = helper.getLevel().getCapability(
                capability,
                helper.absolutePos(pos),
                context);
        if (value != null) {
            return value;
        }
        GameTestFailures.fail(helper, message);
        throw new IllegalStateException(GameTestFailures.truncate(message));
    }
}
