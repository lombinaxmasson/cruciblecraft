package com.masson.cruciblecraft.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GT6 Vanilla.java opening → chainmail / food substitutes on the default
 * GameTest grid.
 */
@GameTestHolder(CrucibleCraftGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class VanillaReplaceGameTests {
    private static final String TEMPLATE = "empty";

    private VanillaReplaceGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void openingThroughRedstoneHolds(GameTestHelper helper) {
        VanillaReplaceAssertions.openingThroughRedstone(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void tntThroughChainmailHolds(GameTestHelper helper) {
        VanillaReplaceAssertions.tntThroughChainmail(helper);
        helper.succeed();
    }
}
