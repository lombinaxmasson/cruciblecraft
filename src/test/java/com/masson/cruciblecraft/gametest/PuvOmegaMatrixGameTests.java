package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.machine.processing.MachineTierCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Explicit ULV–OMEGA machine matrix. No kind × tier completion. */
@GameTestHolder(PuvOmegaMatrixGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class PuvOmegaMatrixGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";

    private PuvOmegaMatrixGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void nativeEuGapsRegistered(GameTestHelper helper) {
        for (MachineTierCatalog.Entry entry : MachineTierCatalog.entries()) {
            if (entry.resourceProfile().skipGenericRegistration()) {
                continue;
            }
            helper.assertTrue(
                    ModBlocks.tieredProcessingBlocksById()
                            .containsKey(entry.variantId()),
                    "Catalog variant has no block: " + entry.variantId());
        }
        for (int tier = 1; tier <= 5; tier++) {
            ResourceLocation id = id("osmiridium_replicator_t" + tier);
            helper.assertTrue(
                    ModBlocks.tieredProcessingBlocksById().containsKey(id),
                    "Replicator T" + tier + " is missing");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void puv2OmegaRowsAreExtensions(GameTestHelper helper) {
        boolean sawExtension = false;
        for (MachineTierCatalog.Entry entry : MachineTierCatalog.entries()) {
            String path = entry.variantId().getPath();
            boolean puvOmega = path.contains("_puv2")
                    || path.contains("_puv3")
                    || path.contains("_puv4")
                    || path.contains("_puv5")
                    || path.endsWith("_omega");
            if (!puvOmega || !path.contains("neutronium")) {
                continue;
            }
            sawExtension = true;
            helper.assertTrue(
                    entry.sourceId() >= 81_000,
                    "PUV2+/OMEGA row reused a GT6 source id: "
                            + entry.variantId()
                            + " sourceId="
                            + entry.sourceId());
        }
        helper.assertTrue(
                sawExtension,
                "No neutronium PUV2+/OMEGA machine rows were catalogued");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void noAutomaticKindTierCompletion(GameTestHelper helper) {
        helper.assertFalse(
                MachineTierCatalog.namingPolicy().automaticKindTierCompletion(),
                "automaticKindTierCompletion must stay false");
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
