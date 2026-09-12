package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineEnergyPlacement;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/**
 * Isolated Sanding Machine gate. Run with {@code -PwaveRecipes=machines/sanding}.
 */
@GameTestHolder(SandingGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SandingGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_machines_sanding";
    private static final String TEMPLATE = "empty";
    private static final int LIVE_ROWS = 7_637;
    private static final Direction FRONT = Direction.NORTH;
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static final ResourceLocation PUBLICATION_GROUP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "sanding/pilot/sanding");
    private static final List<HostCraft> HOSTS = List.of(
            new HostCraft("bronze", "sanding"),
            new HostCraft("steel", "steel_sanding"),
            new HostCraft("titanium", "titanium_sanding"),
            new HostCraft("tungstensteel", "tungstensteel_sanding"));

    private SandingGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.tieredProcessingBlocksById().containsKey(id("sanding")),
                "Sanding block is missing from the catalog");
        helper.assertTrue(
                ModItems.tieredProcessingItemsById().containsKey(id("sanding")),
                "Sanding item is missing from the catalog");
        helper.assertTrue(
                ModProcessingMachines.SANDING != null
                        && ModRecipeMaps.SANDING != null,
                "Sanding RecipeMap or spec missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void liveMapPublishesSelectedRows(GameTestHelper helper) {
        RecipeMap.RecipeFamily family = ModRecipeMaps.SANDING
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.SANDING.id(), PUBLICATION_GROUP))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == LIVE_ROWS,
                "Sanding compact family is not the 7637 selected rows: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.assertTrue(
                ModRecipeMaps.SANDING.entries().size() == LIVE_ROWS,
                "Sanding live map drifted from 7637 selected rows: "
                        + ModRecipeMaps.SANDING.entries().size());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void bronzeHostRunsFirstLiveRecipe(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine = place(
                helper, POS, ModBlocks.SANDING.get(), ModProcessingMachines.SANDING);
        helper.assertTrue(
                helper.getBlockState(POS).getValue(ProcessingMachineBlock.FACING)
                        == FRONT,
                "Bronze sanding default facing is not NORTH");
        RecipeMap.RecipeFamily family = ModRecipeMaps.SANDING
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.SANDING.id(), PUBLICATION_GROUP))
                .orElseThrow(() -> new IllegalStateException(
                        "Missing sanding compact family"));
        RecipeMap.Entry chosen = firstRegisteredRecipe(family);
        helper.assertTrue(
                chosen != null,
                "Sanding family has no fully registered live recipe");
        fillKu(helper, machine, Direction.UP);
        loadRecipeInputs(machine, chosen.recipe());
        fillKu(helper, machine, Direction.UP);
        helper.startSequence()
                .thenExecuteFor(4, () -> fillKu(helper, machine, Direction.UP))
                .thenExecute(() -> helper.assertTrue(
                        machine.workProgressLong() > 0L
                                || machine.duration() > 0
                                || hasAnyOutput(machine),
                        "Bronze sanding did not select a live recipe: "
                                + machine.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fourHostsAreSurvivalCraftable(GameTestHelper helper) {
        for (HostCraft host : HOSTS) {
            Item result = ModItems.tieredProcessingItemsById()
                    .get(id(host.itemPath()))
                    .get();
            Item smallGear = MaterialLookup.item(
                            host.material(), MaterialPrefixes.SMALL_GEAR)
                    .orElseThrow();
            Item gear = MaterialLookup.item(host.material(), MaterialPrefixes.GEAR)
                    .orElseThrow();
            Item casing = MaterialLookup.item(
                            host.material(), MaterialPrefixes.MACHINE_CASING_DOUBLE)
                    .orElseThrow();
            List<ItemStack> slots = List.of(
                    new ItemStack(smallGear),
                    new ItemStack(gear),
                    new ItemStack(smallGear),
                    new ItemStack(Items.SANDSTONE),
                    new ItemStack(Items.SANDSTONE),
                    new ItemStack(Items.SANDSTONE),
                    new ItemStack(ModItems.MATERIAL_WRENCH.get()),
                    new ItemStack(casing),
                    new ItemStack(ModItems.SMITHING_HAMMER.get()));
            ItemStack assembled = craft(helper, slots);
            helper.assertTrue(
                    assembled.is(result) && assembled.getCount() == 1,
                    "Sanding host recipe missing for cruciblecraft:" + host.itemPath());
        }
        helper.succeed();
    }

    private static RecipeMap.Entry firstRegisteredRecipe(RecipeMap.RecipeFamily family) {
        for (int index = 0; index < family.logicalRecipeCount(); index++) {
            RecipeMap.Entry entry = family.enumerationEntry(index);
            if (recipeItemsExist(entry.recipe())) {
                return entry;
            }
        }
        return null;
    }

    private static boolean recipeItemsExist(GTRecipe recipe) {
        return recipe.itemInputs().stream().noneMatch(
                ingredient -> ingredient.getItems().length == 0)
                && recipe.itemOutputs().stream().noneMatch(ItemStack::isEmpty);
    }

    private static ConfiguredProcessingMachineBlockEntity place(
            GameTestHelper helper,
            BlockPos pos,
            Block block,
            ProcessingMachineSpec spec) {
        helper.setBlock(
                pos,
                block.defaultBlockState().setValue(
                        ProcessingMachineBlock.FACING, FRONT));
        ConfiguredProcessingMachineBlockEntity machine = helper.getBlockEntity(pos);
        helper.assertTrue(
                machine.spec() == spec
                        || machine.variant().kind().behavior() == spec,
                "Placed block resolved wrong machine kind");
        return machine;
    }

    private static void loadRecipeInputs(
            ConfiguredProcessingMachineBlockEntity machine, GTRecipe recipe) {
        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            ItemStack sample = recipe.itemInputs().get(i).getItems()[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(i)));
            machine.inventory().setStackInSlot(
                    machine.spec().items().inputs().get(i), sample);
        }
    }

    private static boolean hasAnyOutput(
            ConfiguredProcessingMachineBlockEntity machine) {
        return machine.spec().items().outputs().stream().anyMatch(slot ->
                !machine.inventory().getStackInSlot(slot).isEmpty());
    }

    private static void fillKu(
            GameTestHelper helper,
            ConfiguredProcessingMachineBlockEntity machine,
            Direction requiredSide) {
        EnergyType type = machine.spec().energy().type();
        Direction front = machine.getBlockState().getValue(ProcessingMachineBlock.FACING);
        Direction energySide = ProcessingMachineEnergyPlacement
                .connection(machine.spec(), front)
                .providerOffset();
        helper.assertTrue(
                energySide == requiredSide,
                "Sanding energy side drifted from UP: " + energySide);
        long packet = Math.max(1L, machine.spec().energy().maxPacket());
        long target = machine.capacity(type);
        for (int i = 0; i < 16 && machine.stored(type) < target; i++) {
            if (machine.insert(type, packet, 1L, requiredSide, false) <= 0L) {
                break;
            }
        }
        helper.assertTrue(
                machine.stored(type) > 0L,
                "Placed sanding stored no " + type + " on " + requiredSide);
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

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }

    private record HostCraft(String material, String itemPath) {}
}
