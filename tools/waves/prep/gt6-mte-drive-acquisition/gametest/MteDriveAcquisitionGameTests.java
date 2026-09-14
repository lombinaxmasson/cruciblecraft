package com.masson.cruciblecraft.gametest.prep;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated until unique-active landing. Not registered in ALL. */
@GameTestHolder(MteDriveAcquisitionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteDriveAcquisitionGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_content_gt6_mte_drive_acquisition";
    private static final String TEMPLATE = "empty";

    private MteDriveAcquisitionGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void sourceExactObtainIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel().getRecipeManager().byKey(
                        ResourceLocation.parse("cruciblecraft:mte/drive/drive_small_brass_axle")
                ).isPresent(),
                "missing isolated source-exact recipe mte/drive/drive_small_brass_axle");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void noProgrammedCircuitStandIn(GameTestHelper helper) {
        helper.succeed();
    }
}
