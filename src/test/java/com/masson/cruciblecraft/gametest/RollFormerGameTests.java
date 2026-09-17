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
import com.masson.cruciblecraft.verification.PlayerCompleteSmoke;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated Roll Former gate. Run with {@code -PwaveRecipes=machines/roll-former}.
 */
@GameTestHolder(RollFormerGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class RollFormerGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_machines_roll_former";
    private static final String TEMPLATE = "empty";
    private static final String CAPABILITY = "machines/roll-former";
    private static final Direction FRONT = Direction.NORTH;
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static final ResourceLocation PUBLICATION_GROUP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "rollformer/pilot/roll_former");
    private static final List<HostCraft> HOSTS = List.of(
            new HostCraft("bronze", "rollformer"),
            new HostCraft("steel", "steel_rollformer"),
            new HostCraft("titanium", "titanium_rollformer"),
            new HostCraft("tungstensteel", "tungstensteel_rollformer"));

    private RollFormerGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModItems.tieredProcessingItemsById().get(id("rollformer")).get()
                        != null,
                "Bronze roll former item missing");
        PlayerCompleteSmoke.writeIfConfigured("gameTestServer", CAPABILITY);
        helper.assertTrue(
                PlayerCompleteSmoke.snapshot("gameTestServer", CAPABILITY)
                        .get("status")
                        .getAsString()
                        .equals("PASS"),
                "Player-complete registry snapshot failed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void liveMapPublishesTwentySixSelectedRows(GameTestHelper helper) {
        RecipeMap.RecipeFamily family = ModRecipeMaps.ROLLFORMER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ROLLFORMER.id(), PUBLICATION_GROUP))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == 26,
                "Roll former compact family is not the 26 selected rows: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.assertTrue(
                ModRecipeMaps.ROLLFORMER.entries().size() == 26,
                "Roll former live map drifted from 26 selected rows: "
                        + ModRecipeMaps.ROLLFORMER.entries().size());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void bronzeHostRunsFirstLiveRecipe(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine = place(
                helper, POS, ModBlocks.ROLLFORMER.get(), ModProcessingMachines.ROLLFORMER);
        helper.assertTrue(
                helper.getBlockState(POS).getValue(ProcessingMachineBlock.FACING)
                        == FRONT,
                "Bronze roll former default facing is not NORTH");
        RecipeMap.RecipeFamily family = ModRecipeMaps.ROLLFORMER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.ROLLFORMER.id(), PUBLICATION_GROUP))
                .orElseThrow(() -> new IllegalStateException(
                        "Missing roll former compact family"));
        RecipeMap.Entry first = family.enumerationEntry(0);
        fillKu(helper, machine, Direction.SOUTH);
        loadRecipeInputs(machine, first.recipe());
        fillKu(helper, machine, Direction.SOUTH);
        helper.startSequence()
                .thenExecuteFor(4, () -> fillKu(helper, machine, Direction.SOUTH))
                .thenExecute(() -> helper.assertTrue(
                        machine.workProgressLong() > 0L
                                || machine.duration() > 0
                                || hasAnyOutput(machine),
                        "Bronze roll former did not select the first live recipe: "
                                + machine.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fourHostsAreSurvivalCraftable(GameTestHelper helper) {
        for (HostCraft host : HOSTS) {
            Item result = ModItems.tieredProcessingItemsById()
                    .get(id(host.itemPath()))
                    .get();
            ItemStack gear = MaterialLookup.stack(host.material(), MaterialPrefixes.GEAR);
            ItemStack casing = MaterialLookup.stack(
                    host.material(), MaterialPrefixes.MACHINE_CASING_DOUBLE);
            List<ItemStack> slots = List.of(
                    new ItemStack(ModItems.MATERIAL_WRENCH.get()),
                    gear,
                    ItemStack.EMPTY,
                    gear.copy(),
                    casing,
                    gear.copy(),
                    ItemStack.EMPTY,
                    gear.copy(),
                    new ItemStack(ModItems.SMITHING_HAMMER.get()));
            ItemStack assembled = craft(helper, slots);
            helper.assertTrue(
                    assembled.is(result) && assembled.getCount() == 1,
                    "Roll former host recipe missing for cruciblecraft:" + host.itemPath());
        }
        helper.succeed();
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
        Direction back = ProcessingMachineEnergyPlacement
                .connection(machine.spec(), front)
                .providerOffset();
        helper.assertTrue(
                back == requiredSide,
                "Roll former energy side drifted from SOUTH on NORTH facing: " + back);
        long packet = Math.max(1L, machine.spec().energy().maxPacket());
        long target = machine.capacity(type);
        for (int i = 0; i < 16 && machine.stored(type) < target; i++) {
            if (machine.insert(type, packet, 1L, requiredSide, false) <= 0L) {
                break;
            }
        }
        helper.assertTrue(
                machine.stored(type) > 0L,
                "Placed roll former stored no " + type + " on " + requiredSide);
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
