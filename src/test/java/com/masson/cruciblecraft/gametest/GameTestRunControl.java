package com.masson.cruciblecraft.gametest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Dev-only GameTest server controls. {@code -PgameTestFilter} keeps matching
 * tests, and the dedicated game test server skips the shutdown chunk save.
 */
@EventBusSubscriber(modid = CrucibleCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class GameTestRunControl {
    private static final String FILTER_PROPERTY = "cruciblecraft.gameTestFilter";
    private static boolean filtered;

    static {
        NeoForge.EVENT_BUS.addListener(GameTestRunControl::skipGameTestSave);
        NeoForge.EVENT_BUS.addListener(GameTestRunControl::skipGameTestSaveAfterStart);
        NeoForge.EVENT_BUS.addListener(GameTestRunControl::skipGameTestSaveOnStop);
        NeoForge.EVENT_BUS.addListener(GameTestRunControl::skipGameTestSaveOnLevelLoad);
    }

    private GameTestRunControl() {}

    @SubscribeEvent
    public static void filterBeforeServerCopiesTests(AddReloadListenerEvent event) {
        if (filtered) {
            return;
        }
        String raw = System.getProperty(FILTER_PROPERTY, "").trim();
        if (raw.isEmpty()) {
            return;
        }
        List<String> tokens = new ArrayList<>();
        for (String part : raw.split(",")) {
            String token = part.trim().toLowerCase(Locale.ROOT);
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        if (tokens.isEmpty()) {
            return;
        }
        Collection<TestFunction> tests = testFunctions();
        int before = tests.size();
        tests.removeIf(test -> tokens.stream().noneMatch(
                token -> test.testName().toLowerCase(Locale.ROOT).contains(token)));
        filtered = true;
        CrucibleCraft.LOGGER.info(
                "GameTest filter {} kept {} of {} tests",
                tokens,
                tests.size(),
                before);
        if (tests.isEmpty()) {
            throw new IllegalStateException(
                    "GameTest filter matched nothing: " + raw);
        }
    }

    private static void skipGameTestSave(ServerAboutToStartEvent event) {
        markGameTestLevelsNoSave(event.getServer());
    }

    private static void skipGameTestSaveAfterStart(ServerStartedEvent event) {
        markGameTestLevelsNoSave(event.getServer());
    }

    private static void skipGameTestSaveOnStop(ServerStoppingEvent event) {
        markGameTestLevelsNoSave(event.getServer());
    }

    private static void skipGameTestSaveOnLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            markGameTestLevelNoSave(level);
        }
    }

    private static void markGameTestLevelsNoSave(MinecraftServer server) {
        if (!Boolean.getBoolean("neoforge.gameTestServer")) {
            return;
        }
        int marked = 0;
        for (ServerLevel level : server.getAllLevels()) {
            markGameTestLevelNoSave(level);
            marked++;
        }
        CrucibleCraft.LOGGER.info("GameTest noSave marked {} levels", marked);
    }

    private static void markGameTestLevelNoSave(ServerLevel level) {
        if (Boolean.getBoolean("neoforge.gameTestServer")) {
            level.noSave = true;
        }
    }

    @SuppressWarnings("unchecked")
    private static Collection<TestFunction> testFunctions() {
        try {
            Field field = GameTestRegistry.class.getDeclaredField("TEST_FUNCTIONS");
            field.setAccessible(true);
            return (Collection<TestFunction>) field.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not read GameTestRegistry", failure);
        }
    }
}
