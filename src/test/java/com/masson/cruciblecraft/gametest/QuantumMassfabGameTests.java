package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.energy.quantum.QuantumEnergizerCatalog;
import com.masson.cruciblecraft.fusion.FusionRecipeCatalog;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** QU Matter Fabricator and Neutronium bootstrap. */
@GameTestHolder(QuantumMassfabGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class QuantumMassfabGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_energy_quantum_massfab";
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private QuantumMassfabGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void neutroniumIsNotCosmicNeutronium(GameTestHelper helper) {
        helper.assertTrue(
                MaterialCatalog.find("neutronium").isPresent(),
                "neutronium material is not registered");
        helper.assertTrue(
                MaterialLookup.item("neutronium", MaterialPrefixes.INGOT).isPresent(),
                "neutronium ingot is missing");
        helper.assertTrue(
                MaterialLookup.item("cosmic_neutronium", MaterialPrefixes.INGOT)
                                .isPresent()
                        && MaterialLookup.item(
                                        "neutronium", MaterialPrefixes.INGOT)
                                .orElseThrow()
                                != MaterialLookup.item(
                                                "cosmic_neutronium",
                                                MaterialPrefixes.INGOT)
                                        .orElseThrow(),
                "neutronium must stay distinct from cosmic_neutronium");
        helper.assertFalse(
                BuiltInRegistries.ITEM.containsKey(id("cosmic_neutronium"))
                        && BuiltInRegistries.ITEM.get(id("cosmic_neutronium"))
                                == MaterialLookup.item(
                                                "neutronium",
                                                MaterialPrefixes.INGOT)
                                        .orElseThrow(),
                "cosmic_neutronium must not alias neutronium");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void massfabCompactsNeutralMatter(GameTestHelper helper) {
        var block = ModBlocks.tieredProcessingBlocksById().get(
                id("osmiridium_massfab_t5"));
        helper.assertTrue(block != null, "Osmiridium Massfab T5 is missing");
        helper.setBlock(
                POS,
                block.get().defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING, Direction.NORTH));
        ConfiguredProcessingMachineBlockEntity machine =
                helper.getBlockEntity(POS);
        helper.assertTrue(machine != null, "Massfab block entity missing");
        var matter = ModFluids.chemical("matter_neutral")
                .orElseThrow()
                .source()
                .get();
        machine.tanks().get(machine.spec().fluids().inputs().getFirst().index())
                .setFluid(new FluidStack(matter, 144));
        var neutronium = MaterialLookup.item(
                "neutronium", MaterialPrefixes.INGOT).orElseThrow();
        Direction energy = Direction.SOUTH;
        for (int tick = 0; tick < 128; tick++) {
            helper.assertTrue(
                    machine.insert(
                                    EnergyType.QUANTUM,
                                    4_096L,
                                    1L,
                                    energy,
                                    false)
                            == 1L,
                    "Massfab T5 rejected QUANTUM on tick " + tick);
            ConfiguredProcessingMachineBlockEntity.serverTick(
                    helper.getLevel(),
                    helper.absolutePos(POS),
                    helper.getBlockState(POS),
                    machine);
        }
        boolean produced = false;
        for (int slot = 0; slot < machine.inventory().getSlots(); slot++) {
            ItemStack stack = machine.inventory().getStackInSlot(slot);
            if (stack.is(neutronium) && stack.getCount() >= 1) {
                produced = true;
                break;
            }
        }
        helper.assertTrue(
                produced,
                "Massfab did not compact 144 mB matter_neutral into a neutronium ingot");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void noCircularNeutroniumDependency(GameTestHelper helper) {
        helper.assertTrue(
                FusionRecipeCatalog.entries().stream().noneMatch(
                        entry -> mentionsNeutronium(entry.id())
                                || entry.fluidInputs().stream().anyMatch(
                                        fluid -> "neutronium".equals(
                                                fluid.material()))
                                || entry.itemOutputs().stream().anyMatch(
                                        item -> "neutronium".equals(
                                                item.material()))),
                "Source fusion rows must not require neutronium");
        helper.assertTrue(
                QuantumEnergizerCatalog.profiles().stream()
                        .filter(profile -> !profile.extension())
                        .noneMatch(profile ->
                                "neutronium".equals(profile.material())
                                        || profile.recipe().keys().values()
                                                .stream()
                                                .anyMatch(ingredient ->
                                                        "neutronium".equals(
                                                                ingredient.material()))),
                "Source-backed Quantum Energizers must not require neutronium");
        helper.assertTrue(
                ModRecipeMaps.MASSFAB.entries().stream().anyMatch(
                        entry -> entry.recipe().itemOutputs().stream().anyMatch(
                                stack -> stack.is(
                                        MaterialLookup.item(
                                                        "neutronium",
                                                        MaterialPrefixes.INGOT)
                                                .orElseThrow()))),
                "Massfab must output a neutronium ingot");
        helper.succeed();
    }

    private static boolean mentionsNeutronium(String value) {
        return value != null && value.contains("neutronium");
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
