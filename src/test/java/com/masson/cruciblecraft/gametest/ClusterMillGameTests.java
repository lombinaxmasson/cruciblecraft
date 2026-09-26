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
 * Isolated Cluster Mill gate. Run with {@code -PgameTestGrid=machines}.
 */
@GameTestHolder(ClusterMillGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ClusterMillGameTests {
    public static final String NAMESPACE = "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";
    private static final String CAPABILITY = "machines/cluster-mill";
    private static final Direction FRONT = Direction.NORTH;
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static final ResourceLocation PUBLICATION_GROUP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "clustermill/pilot/cluster_mill");
    private static final List<HostCraft> HOSTS = List.of(
            new HostCraft("bronze", "clustermill"),
            new HostCraft("steel", "steel_clustermill"),
            new HostCraft("titanium", "titanium_clustermill"),
            new HostCraft("tungstensteel", "tungstensteel_clustermill"));

    private ClusterMillGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModItems.tieredProcessingItemsById().get(id("clustermill")).get()
                        != null,
                "Bronze cluster mill item missing");
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
    public static void liveMapPublishesThreeHundredSevenSelectedRows(
            GameTestHelper helper) {
        RecipeMap.RecipeFamily family = ModRecipeMaps.CLUSTERMILL
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.CLUSTERMILL.id(), PUBLICATION_GROUP))
                .orElse(null);
        helper.assertTrue(
                family != null && family.logicalRecipeCount() == 307,
                "Cluster mill compact family is not the 307 selected rows: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        helper.assertTrue(
                ModRecipeMaps.CLUSTERMILL.entries().size() == 307,
                "Cluster mill live map drifted from 307 selected rows: "
                        + ModRecipeMaps.CLUSTERMILL.entries().size());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void bronzeHostRunsFirstLiveRecipe(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine = place(
                helper, POS, ModBlocks.CLUSTERMILL.get(),
                ModProcessingMachines.CLUSTERMILL);
        helper.assertTrue(
                helper.getBlockState(POS).getValue(ProcessingMachineBlock.FACING)
                        == FRONT,
                "Bronze cluster mill default facing is not NORTH");
        RecipeMap.RecipeFamily family = ModRecipeMaps.CLUSTERMILL
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.CLUSTERMILL.id(), PUBLICATION_GROUP))
                .orElseThrow(() -> new IllegalStateException(
                        "Missing cluster mill compact family"));
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
                        "Bronze cluster mill did not select the first live recipe: "
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
            ItemStack smallGear = MaterialLookup.stack(
                    host.material(), MaterialPrefixes.SMALL_GEAR);
            ItemStack casing = MaterialLookup.stack(
                    host.material(),
                    MaterialPrefixes.MACHINE_CASING_QUADRUPLE);
            List<ItemStack> slots = List.of(
                    smallGear,
                    smallGear.copy(),
                    smallGear.copy(),
                    new ItemStack(ModItems.MATERIAL_WRENCH.get()),
                    gear,
                    new ItemStack(ModItems.SMITHING_HAMMER.get()),
                    smallGear.copy(),
                    casing,
                    smallGear.copy());
            ItemStack assembled = craft(helper, slots);
            helper.assertTrue(
                    assembled.is(result) && assembled.getCount() == 1,
                    "Cluster mill host recipe missing for cruciblecraft:"
                            + host.itemPath());
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
                "Cluster mill energy side drifted from SOUTH on NORTH facing: "
                        + back);
        long packet = Math.max(1L, machine.spec().energy().maxPacket());
        long target = machine.capacity(type);
        for (int i = 0; i < 16 && machine.stored(type) < target; i++) {
            if (machine.insert(type, packet, 1L, requiredSide, false) <= 0L) {
                break;
            }
        }
        helper.assertTrue(
                machine.stored(type) > 0L,
                "Placed cluster mill stored no " + type + " on " + requiredSide);
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
