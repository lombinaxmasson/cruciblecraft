package com.masson.cruciblecraft.gametest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.block.MaterialDustBlock;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.FallingBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GT6 dust-family packing, nugget/ingot packing, placeable {@code OP.blockDust},
 * and plate-family packing into placeable {@code OP.blockPlate}. Lives on the
 * default GameTest namespace so a bare {@code runGameTestServer} exercises it.
 */
@GameTestHolder("cruciblecraft_content")
@PrefixGameTestTemplate(false)
public final class PrefixPackGameTests {
    private static final String TEMPLATE = "empty";

    private PrefixPackGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void sugarCraftsNineTinyDust(GameTestHelper helper) {
        ItemStack assembled = craft(
                helper,
                3,
                3,
                sugarInFirstSlot());
        helper.assertTrue(
                MaterialLookup.matches(
                        assembled, "sugar", MaterialPrefixes.TINY_DUST)
                        && assembled.getCount() == 9,
                "1 sugar did not unpack to 9 tiny dust: " + assembled);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void nineTinySugarCraftsVanillaSugar(GameTestHelper helper) {
        ItemStack tiny = MaterialLookup.stack(
                "sugar", MaterialPrefixes.TINY_DUST);
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(9, tiny.copy()));
        ItemStack assembled = craft(helper, 3, 3, slots);
        helper.assertTrue(
                assembled.is(Items.SUGAR) && assembled.getCount() == 1,
                "9 tiny sugar dust did not pack to vanilla sugar: " + assembled);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void nineCoalCokeDustCraftsPlaceableDustBlock(
            GameTestHelper helper) {
        ItemStack dust = MaterialLookup.stack(
                "coal_coke", MaterialPrefixes.DUST);
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(9, dust.copy()));
        ItemStack assembled = craft(helper, 3, 3, slots);
        helper.assertTrue(
                MaterialLookup.matches(
                        assembled, "coal_coke", MaterialPrefixes.STORAGE_DUST)
                        && assembled.getCount() == 1,
                "9 coal coke dust did not pack to dust block: " + assembled);
        helper.assertTrue(
                ModBlocks.hasDustBlock("coal_coke"),
                "coal coke dust block was not registered");
        helper.assertTrue(
                ModBlocks.dustBlock("coal_coke").get() instanceof FallingBlock,
                "coal coke dust block is not a falling block");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void coalCokeDustBlockPlacesAndDropsSelf(GameTestHelper helper) {
        MaterialDustBlock block = ModBlocks.dustBlock("coal_coke").get();
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, block.defaultBlockState());
        helper.assertTrue(
                helper.getBlockState(pos).is(block),
                "coal coke dust block did not place");
        helper.assertTrue(
                BuiltInRegistries.BLOCK.containsKey(
                        ResourceLocation.fromNamespaceAndPath(
                                CrucibleCraft.MODID, "coal_coke/storage_dust")),
                "coal_coke/storage_dust is not a live block id");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gt6StorageDustBatchHasCensusDenominator(
            GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.dustBlocks().size() == 963,
                "storage_dust block denominator drifted: "
                        + ModBlocks.dustBlocks().size());
        for (String material : List.of("iron", "diamond", "rubber", "sugar")) {
            helper.assertTrue(
                    ModBlocks.hasDustBlock(material)
                            && BuiltInRegistries.ITEM.containsKey(
                                    ResourceLocation.fromNamespaceAndPath(
                                            CrucibleCraft.MODID,
                                            material + "/storage_dust")),
                    material + "/storage_dust is not fully registered");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void nineIronPlatesCraftPlaceableStoragePlate(
            GameTestHelper helper) {
        ItemStack plate = MaterialLookup.stack("iron", MaterialPrefixes.PLATE);
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(9, plate.copy()));
        ItemStack assembled = craft(helper, 3, 3, slots);
        helper.assertTrue(
                MaterialLookup.matches(
                        assembled, "iron", MaterialPrefixes.STORAGE_PLATE)
                        && assembled.getCount() == 1,
                "9 iron plates did not pack to storage plate: " + assembled);
        helper.assertTrue(
                ModBlocks.hasPlateStorageBlock("iron"),
                "iron storage plate block was not registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void ironStoragePlatePlacesAndUnpacksInCraft(GameTestHelper helper) {
        var block = ModBlocks.plateStorageBlock("iron").get();
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, block.defaultBlockState());
        helper.assertTrue(
                helper.getBlockState(pos).is(block),
                "iron storage plate did not place");
        helper.assertTrue(
                BuiltInRegistries.BLOCK.containsKey(
                        ResourceLocation.fromNamespaceAndPath(
                                CrucibleCraft.MODID, "iron/storage_plate")),
                "iron/storage_plate is not a live block id");
        ItemStack packed = MaterialLookup.stack(
                "iron", MaterialPrefixes.STORAGE_PLATE);
        List<ItemStack> slots = new ArrayList<>(
                Collections.nCopies(9, ItemStack.EMPTY));
        slots.set(0, packed);
        ItemStack assembled = craft(helper, 3, 3, slots);
        helper.assertTrue(
                MaterialLookup.matches(assembled, "iron", MaterialPrefixes.PLATE)
                        && assembled.getCount() == 9,
                "1 iron storage plate did not unpack to 9 plates: " + assembled);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void nineRubberNuggetsCraftIngot(GameTestHelper helper) {
        ItemStack nugget = MaterialLookup.stack("rubber", MaterialPrefixes.NUGGET);
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(9, nugget.copy()));
        ItemStack assembled = craft(helper, 3, 3, slots);
        helper.assertTrue(
                MaterialLookup.matches(
                        assembled, "rubber", MaterialPrefixes.INGOT)
                        && assembled.getCount() == 1,
                "9 rubber nuggets did not pack to a rubber ingot: " + assembled);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void oneRubberIngotUnpacksNineNuggets(GameTestHelper helper) {
        ItemStack ingot = MaterialLookup.stack("rubber", MaterialPrefixes.INGOT);
        List<ItemStack> slots = new ArrayList<>(
                Collections.nCopies(9, ItemStack.EMPTY));
        slots.set(0, ingot);
        ItemStack assembled = craft(helper, 3, 3, slots);
        helper.assertTrue(
                MaterialLookup.matches(
                        assembled, "rubber", MaterialPrefixes.NUGGET)
                        && assembled.getCount() == 9,
                "1 rubber ingot did not unpack to 9 nuggets: " + assembled);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void vanillaSugarResolvesAsSugarDust(GameTestHelper helper) {
        helper.assertTrue(
                MaterialUnits.resolve(new ItemStack(Items.SUGAR))
                        .filter(entry -> "sugar".equals(entry.materialId())
                                && entry.form().equals(MaterialPrefixes.DUST))
                        .isPresent(),
                "minecraft:sugar did not unify as sugar dust");
        helper.succeed();
    }

    private static List<ItemStack> sugarInFirstSlot() {
        List<ItemStack> slots = new ArrayList<>(
                Collections.nCopies(9, ItemStack.EMPTY));
        slots.set(0, new ItemStack(Items.SUGAR));
        return slots;
    }

    private static ItemStack craft(
            GameTestHelper helper,
            int width,
            int height,
            List<ItemStack> slots) {
        CraftingInput input = CraftingInput.of(
                width, height, new ArrayList<>(slots));
        var match = helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElse(null);
        helper.assertTrue(match != null, "no crafting match for prefix pack");
        return match.value().assemble(
                input, helper.getLevel().registryAccess());
    }
}
