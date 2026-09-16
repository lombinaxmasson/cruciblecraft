package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.item.ReactorRodItem;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated reactor-rod host fold. Run with
 * {@code -PwaveRecipes=content/gt6-mte-reactor-rod-host-fold}.
 */
@GameTestHolder(MteReactorRodHostFoldGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteReactorRodHostFoldGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_mte_reactor_rod_host_fold";
    private static final String TEMPLATE = "empty";

    private MteReactorRodHostFoldGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void meta9203FoldsOntoNeutronReflectorRod(
            GameTestHelper helper) {
        Item rod = item("neutron_reflector_rod");
        helper.assertTrue(
                rod instanceof ReactorRodItem
                        && ModItems.reactorRod("neutron_reflector_rod").get()
                                == rod,
                "neutron reflector rod lost its live ReactorRodItem");
        helper.assertTrue(
                withdrawn("neutron/reflector_rod"),
                "reactor dummy neutron/reflector_rod is still registered");
        helper.assertTrue(
                !(item("desh") instanceof ReactorRodItem),
                "desh material 9203 was treated as the reactor rod host");
        helper.succeed();
    }

    private static boolean withdrawn(String path) {
        return !BuiltInRegistries.ITEM.containsKey(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", path));
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", path));
    }
}
