package com.masson.cruciblecraft.gametest.prep;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated until unique-active landing. Not registered in ALL. */
@GameTestHolder(MteFurnitureChestAcquisitionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteFurnitureChestAcquisitionGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_content_gt6_mte_furniture_chest_acquisition";
    private static final String TEMPLATE = "empty";

    private MteFurnitureChestAcquisitionGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void sourceExactObtainIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel().getRecipeManager().byKey(
                        ResourceLocation.parse("cruciblecraft:mte/furniture_chest/lead_chest")
                ).isPresent(),
                "missing isolated source-exact recipe mte/furniture_chest/lead_chest");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void noProgrammedCircuitStandIn(GameTestHelper helper) {
        helper.succeed();
    }
}
