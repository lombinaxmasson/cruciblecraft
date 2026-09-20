package com.masson.cruciblecraft.gametest;

import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated vanilla-replace gate. Run with
 * {@code -PwaveRecipes=vanilla-replace-mvp}. Opening through chainmail /
 * food delates is implemented; arrows / dyes / RecipeMap stay later.
 */
@GameTestHolder(VanillaReplaceMvpGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class VanillaReplaceMvpGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_vanilla_replace_mvp";
    private static final String TEMPLATE = "empty";

    private VanillaReplaceMvpGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "vanilla_replace_mvp", timeoutTicks = 80)
    public static void paperSubstituteAndLockOutsideRecipesHold(
            GameTestHelper helper) {
        VanillaReplaceAssertions.openingThroughRedstone(helper);
        VanillaReplaceAssertions.tntThroughChainmail(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "vanilla_replace_mvp", timeoutTicks = 400)
    public static void vanillaReplaceLockSurvivesDatapackReload(
            GameTestHelper helper) {
        VanillaReplaceAssertions.openingThroughRedstone(helper);
        VanillaReplaceAssertions.tntThroughChainmail(helper);
        MinecraftServer server = helper.getLevel().getServer();
        AtomicBoolean reloaded = new AtomicBoolean(false);
        server.reloadResources(server.getPackRepository().getSelectedIds())
                .thenRun(() -> reloaded.set(true));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        reloaded.get(),
                        "Datapack reload did not finish"))
                .thenExecute(() -> {
                    VanillaReplaceAssertions.openingThroughRedstone(helper);
                    VanillaReplaceAssertions.tntThroughChainmail(helper);
                })
                .thenSucceed();
    }
}
