package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.item.PrefixMaterialItem;
import com.masson.cruciblecraft.machine.processing.MachineTierCatalog;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineIgnition;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineIoFaces;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMachineVariants;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Gt6BasicMachineBatchGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public class Gt6BasicMachineBatchGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_machines_gt6_basic_machine_batch";
    private static final String TEMPLATE = "empty";
    private static final BlockPos ORIGIN = new BlockPos(1, 2, 1);

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void tierOneHostsAcceptTheirEnergy(GameTestHelper helper) {
        for (String path : List.of(
                "burn_mixer",
                "catalytic_cracker",
                "crystallisation_crucible",
                "steam_cracker")) {
            ConfiguredProcessingMachineBlockEntity machine = place(helper, path);
            OrdinaryClosureHostGameTests.fillEnergy(helper, machine);
            helper.assertTrue(
                    machine.stored(machine.spec().energy().type()) > 0L,
                    path + " stored no energy");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void burnMixerStartsOnlyAfterIgnition(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine = place(helper, "burn_mixer");
        helper.assertTrue(
                ProcessingMachineIgnition.requires(machine.spec()),
                "burn mixer lost its ignition hook");
        helper.assertTrue(machine.ignitionTicks() == 0, "burn mixer started ignited");
        machine.ignite();
        helper.assertTrue(
                machine.ignitionTicks() == ProcessingMachineIgnition.IGNITION_TICKS,
                "igniter did not set the GT6 40-tick window");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void crystallisationTierOneCraftsFromQuartzCrucible(GameTestHelper helper) {
        helper.assertFalse(
                MachineTierCatalog.acquisitionBlocked(id("crystallisation_crucible")),
                "tier-1 crystallisation crucible is still acquisition-blocked");
        helper.assertTrue(
                craft(helper, crystallisationSlots(false, ItemStack.EMPTY)).isEmpty(),
                "tier-1 crystallisation crucible crafted without the quartz crucible");
        ItemStack tierOne = craft(
                helper,
                crystallisationSlots(
                        false,
                        stack(itemOf("foundry/smelting_crucible_nether_quartz"))));
        helper.assertTrue(
                tierOne.getItem() == itemOf("crystallisation_crucible"),
                "tier-1 crystallisation crucible did not craft: " + tierOne);
        ItemStack tierTwo = craft(
                helper, crystallisationSlots(true, stack(itemOf("foundry/smelting_crucible_iridium"))));
        helper.assertTrue(
                tierTwo.getItem() == itemOf("invar_crystallisation_crucible"),
                "invar crystallisation crucible did not craft: " + tierTwo);
        ItemStack burner = craft(helper, burnMixerSlots());
        helper.assertTrue(
                burner.getItem() == itemOf("burn_mixer"),
                "bronze burn mixer did not craft: " + burner);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 25000)
    public static void sampleRecipesProduceOutput(GameTestHelper helper) {
        List<Sample> samples = List.of(
                sample(helper, "burn_mixer", true),
                sample(helper, "catalytic_cracker", false),
                // Tier 1 accepts at most 64 HU/t. Every 18000-tick row is 256 HU/t,
                // and every 16 HU/t row is at least 72000 ticks. Tier 2 accepts 256.
                sample(helper, "invar_crystallisation_crucible", false));
        int[] index = {0};
        helper.onEachTick(() -> {
            if (index[0] >= samples.size()) {
                helper.succeed();
                return;
            }
            Sample current = samples.get(index[0]);
            current.tick(helper);
            if (current.finished) {
                index[0]++;
            }
        });
    }

    private static Sample sample(GameTestHelper helper, String path, boolean ignite) {
        MachineVariant variant = ModMachineVariants.require(id(path));
        RecipeMap map = variant.kind().behavior().requireRecipeMap();
        long packet = variant.tierBand().inputMaximum();
        RecipeMap.Entry chosen = null;
        for (RecipeMap.Entry entry : map.entries()) {
            if (variant.runtimeSpec().validator().validate(entry.recipe()).isPresent()) {
                continue;
            }
            if (entry.recipe().eut() > packet) {
                continue;
            }
            if (entry.recipe().duration() <= 0 || entry.recipe().duration() > 18_000) {
                continue;
            }
            if (chosen == null || entry.recipe().duration() < chosen.recipe().duration()) {
                chosen = entry;
            }
        }
        helper.assertTrue(chosen != null, path + " has no runnable sample recipe");
        return new Sample(path, chosen.recipe(), ignite);
    }

    private static ConfiguredProcessingMachineBlockEntity place(
            GameTestHelper helper, String path) {
        MachineVariant variant = ModMachineVariants.require(id(path));
        Block block = ModBlocks.configuredProcessingBlock(variant);
        return OrdinaryClosureHostGameTests.place(
                helper, ORIGIN, block, variant.kind().behavior());
    }

    private static ItemStack craft(GameTestHelper helper, List<ItemStack> slots) {
        CraftingInput input = CraftingInput.of(3, 3, slots);
        return helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(
                        input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private static List<ItemStack> burnMixerSlots() {
        return List.of(
                material("invar", MaterialPrefixes.PLATE),
                material("bronze", MaterialPrefixes.MACHINE_CASING),
                material("invar", MaterialPrefixes.PLATE),
                material("invar", MaterialPrefixes.PLATE),
                material("invar", MaterialPrefixes.ROTOR),
                material("invar", MaterialPrefixes.PLATE),
                stack(ModItems.SMITHING_HAMMER.get()),
                material("bronze", MaterialPrefixes.ROD),
                stack(ModItems.MATERIAL_WRENCH.get()));
    }

    private static List<ItemStack> crystallisationSlots(
            boolean invarTier,
            ItemStack crucible) {
        String material = invarTier ? "invar" : "steel";
        return List.of(
                stack(ModItems.MATERIAL_WRENCH.get()),
                crucible,
                stack(ModItems.SMITHING_HAMMER.get()),
                material(material, MaterialPrefixes.FLUID_PIPE),
                material(material, MaterialPrefixes.MACHINE_CASING_DOUBLE),
                material(material, MaterialPrefixes.FLUID_PIPE),
                stack(Items.BRICKS),
                material("copper", MaterialPrefixes.DOUBLE_PLATE),
                stack(Items.BRICKS));
    }

    private static ItemStack material(String materialId, MaterialPrefix form) {
        ItemStack stack = new ItemStack(
                ModItems.materialItem(materialId, form).get());
        if (stack.getItem() instanceof PrefixMaterialItem) {
            stack.set(ModComponents.PREFIX_MATERIAL, materialId);
        }
        return stack;
    }

    private static Item itemOf(String path) {
        return BuiltInRegistries.ITEM.get(id(path));
    }

    private static ItemStack stack(Item item) {
        return new ItemStack(item);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }

    private static final class Sample {
        private final String path;
        private final GTRecipe recipe;
        private final boolean ignite;
        private boolean placed;
        private int waited;
        private boolean finished;

        private Sample(String path, GTRecipe recipe, boolean ignite) {
            this.path = path;
            this.recipe = recipe;
            this.ignite = ignite;
        }

        private void tick(GameTestHelper helper) {
            if (!placed) {
                ConfiguredProcessingMachineBlockEntity machine = place(helper, path);
                OrdinaryClosureHostGameTests.loadRecipeInputs(machine, recipe);
                if (ignite) {
                    machine.ignite();
                }
                OrdinaryClosureHostGameTests.fillEnergy(helper, machine);
                placed = true;
                return;
            }
            ConfiguredProcessingMachineBlockEntity machine = helper.getBlockEntity(ORIGIN);
            EnergyType type = machine.spec().energy().type();
            machine.insert(
                    type,
                    machine.spec().energy().maxPacket(),
                    1L,
                    ProcessingMachineIoFaces.energy(machine.spec(), machine.facing()),
                    false);
            waited++;
            if (hasOutput(machine)) {
                finished = true;
                return;
            }
            if (waited > recipe.duration() + 80) {
                helper.fail(path + " sample produced no output after " + waited + " ticks");
            }
        }

        private static boolean hasOutput(ConfiguredProcessingMachineBlockEntity machine) {
            for (int slot : machine.spec().items().outputs()) {
                if (!machine.inventory().getStackInSlot(slot).isEmpty()) {
                    return true;
                }
            }
            for (var tank : machine.spec().fluids().outputs()) {
                if (!machine.tanks().get(tank.index()).getFluid().isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }
}
