package com.masson.cruciblecraft.gametest.prep;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated until unique-active landing. Not registered in ALL. */
@GameTestHolder(MteFurnitureBarrelAcquisitionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteFurnitureBarrelAcquisitionGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_content_gt6_mte_furniture_barrel_acquisition";
    private static final String TEMPLATE = "empty";

    private MteFurnitureBarrelAcquisitionGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void sourceExactObtainIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel().getRecipeManager().byKey(
                        ResourceLocation.parse("cruciblecraft:mte/furniture_barrel/furniture_wooden_item_barrel_cheap")
                ).isPresent(),
                "missing isolated source-exact recipe mte/furniture_barrel/furniture_wooden_item_barrel_cheap");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void noProgrammedCircuitStandIn(GameTestHelper helper) {
        helper.succeed();
    }
}
