package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineEnergyPlacement;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated Oven gate. Run with {@code -PwaveRecipes=machines/oven}.
 */
@GameTestHolder(OvenGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class OvenGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_machines_oven";
    private static final String TEMPLATE = "empty";
    private static final Direction FRONT = Direction.NORTH;
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static final List<HostCraft> HOSTS = List.of(
            new HostCraft("steel", "oven"),
            new HostCraft("invar", "invar_oven"),
            new HostCraft("titanium", "titanium_oven"),
            new HostCraft("tungsten_carbide", "tungsten_carbide_oven"));

    private OvenGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.tieredProcessingBlocksById().containsKey(id("oven")),
                "Oven block is missing from the catalog");
        helper.assertTrue(
                ModItems.tieredProcessingItemsById().containsKey(id("oven")),
                "Oven item is missing from the catalog");
        helper.assertTrue(
                ModProcessingMachines.OVEN != null
                        && ModRecipeMaps.OVEN != null,
                "Oven RecipeMap or spec missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void liveMapMirrorsVanillaSmelting(GameTestHelper helper) {
        int vanilla = helper.getLevel()
                .getRecipeManager()
                .getAllRecipesFor(RecipeType.SMELTING)
                .size();
        helper.assertTrue(
                !ModRecipeMaps.OVEN.entries().isEmpty(),
                "Oven live map is empty; vanilla SMELTING snapshot did not land");
        helper.assertTrue(
                ModRecipeMaps.OVEN.entries().size() <= vanilla,
                "Oven live map exceeded vanilla SMELTING count: "
                        + ModRecipeMaps.OVEN.entries().size()
                        + " vs "
                        + vanilla);
        helper.assertTrue(
                ModRecipeMaps.OVEN.entries().stream().anyMatch(entry ->
                        matches(entry.recipe(), Items.COBBLESTONE, Items.STONE)),
                "Oven map is missing cobblestone → stone");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void steelHostSmeltsCobble(GameTestHelper helper) {
        ConfiguredProcessingMachineBlockEntity machine = OrdinaryClosureHostGameTests.place(
                helper, POS, ModBlocks.OVEN.get(), ModProcessingMachines.OVEN);
        helper.assertTrue(
                helper.getBlockState(POS).getValue(ProcessingMachineBlock.FACING)
                        == Direction.EAST,
                "Steel oven default facing drifted");
        Direction energySide = ProcessingMachineEnergyPlacement
                .connection(machine.spec(), FRONT)
                .providerOffset();
        helper.assertTrue(
                energySide == Direction.DOWN,
                "Oven energy side drifted from DOWN: " + energySide);
        machine.inventory().setStackInSlot(
                machine.spec().items().inputs().getFirst(),
                new ItemStack(Items.COBBLESTONE));
        helper.startSequence()
                .thenExecuteFor(4, () -> OrdinaryClosureHostGameTests.fillEnergy(
                        helper, machine))
                .thenExecute(() -> helper.assertTrue(
                        machine.workProgressLong() > 0L
                                || machine.duration() > 0
                                || hasAnyOutput(machine),
                        "Steel oven did not select cobblestone smelting: "
                                + machine.pausedReason()))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fourHostsAreSurvivalCraftable(GameTestHelper helper) {
        ItemStack copperPlate = MaterialLookup.stack(
                "copper", MaterialPrefixes.DOUBLE_PLATE);
        for (HostCraft host : HOSTS) {
            Item result = ModItems.tieredProcessingItemsById()
                    .get(id(host.itemPath()))
                    .get();
            ItemStack casing = MaterialLookup.stack(
                    host.material(), MaterialPrefixes.MACHINE_CASING);
            List<ItemStack> slots = List.of(
                    new ItemStack(ModItems.MATERIAL_WRENCH.get()),
                    casing,
                    new ItemStack(ModItems.SMITHING_HAMMER.get()),
                    new ItemStack(Items.BRICKS),
                    copperPlate.copy(),
                    new ItemStack(Items.BRICKS));
            ItemStack assembled = craft(helper, slots);
            helper.assertTrue(
                    assembled.is(result) && assembled.getCount() == 1,
                    "Oven host recipe missing for cruciblecraft:" + host.itemPath());
        }
        helper.succeed();
    }

    private static boolean matches(GTRecipe recipe, Item input, Item output) {
        return recipe.itemInputs().stream().anyMatch(ingredient -> {
            for (ItemStack stack : ingredient.getItems()) {
                if (stack.is(input)) {
                    return true;
                }
            }
            return false;
        }) && recipe.itemOutputs().stream().anyMatch(stack -> stack.is(output));
    }

    private static boolean hasAnyOutput(
            ConfiguredProcessingMachineBlockEntity machine) {
        return machine.spec().items().outputs().stream().anyMatch(slot ->
                !machine.inventory().getStackInSlot(slot).isEmpty());
    }

    private static ItemStack craft(GameTestHelper helper, List<ItemStack> slots) {
        CraftingInput input = CraftingInput.of(3, 2, slots);
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
