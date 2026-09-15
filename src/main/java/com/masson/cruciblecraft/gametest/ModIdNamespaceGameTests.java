package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Bare {@code runGameTestServer} enables {@link CrucibleCraft#MODID}. NeoForge
 * crashes when that namespace has zero tests, so this stub succeeds immediately.
 * {@link CrucibleCraftGameTests} and {@link CircuitTierGameTests} live on
 * {@link CrucibleCraftGameTests#NAMESPACE}.
 */
@GameTestHolder(CrucibleCraft.MODID)
@PrefixGameTestTemplate(false)
public final class ModIdNamespaceGameTests {
    private static final String TEMPLATE = "empty";

    private ModIdNamespaceGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void defaultKitchenSinkGridIsOptIn(GameTestHelper helper) {
        helper.succeed();
    }
}
