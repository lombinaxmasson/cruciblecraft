package com.masson.cruciblecraft.gametest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineIoFaces;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated extruder bulk gate. Run with {@code -PwaveRecipes=recipe/gt6-extruder-bulk}.
 */
@GameTestHolder(Gt6ExtruderBulkGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class Gt6ExtruderBulkGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_recipe_gt6_extruder_bulk";
    private static final String TEMPLATE = "empty";
    private static final int LIVE_ROWS = 299_143;
    private static final long MAX_SAMPLE_EUT = 32L;
    private static final ResourceLocation PUBLICATION_GROUP =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "extruder/bulk");

    private Gt6ExtruderBulkGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void bulkMapPublishesTranslatedRows(GameTestHelper helper) {
        RecipeMap.RecipeFamily family = family();
        helper.assertTrue(
                family != null
                        && family.logicalRecipeCount() == LIVE_ROWS
                        && family.eagerRecipeCount() == 0
                        && family.cacheCeiling() == 16,
                "Extruder bulk family is not the on-demand translated rows: "
                        + (family == null ? "missing" : family.logicalRecipeCount()));
        String fingerprint = family.stableFingerprint();
        helper.assertTrue(
                fingerprint != null
                        && fingerprint.equals(family().stableFingerprint())
                        && fingerprint.matches("[0-9a-f]{64}"),
                "Extruder bulk fingerprint is not stable");
        Set<String> shapes = new LinkedHashSet<>();
        int scanned = Math.min(family.logicalRecipeCount(), 8_192);
        for (int index = 0; index < scanned && shapes.size() < 8; index++) {
            RecipeMap.Entry entry = family.enumerationEntry(index);
            GTRecipe recipe = entry.recipe();
            if (recipe.itemInputs().size() < 2) {
                continue;
            }
            ItemStack[] mold = recipe.itemInputs().get(1).getItems();
            if (mold.length != 1 || !ExtruderShapeCatalog.isShape(mold[0])) {
                continue;
            }
            String shape = mold[0].getItem().toString();
            if (!shapes.add(shape)) {
                continue;
            }
            GTRecipeQuery query = queryFor(recipe);
            helper.assertTrue(
                    ModRecipeMaps.EXTRUDER.findMatch(query).isPresent(),
                    "Extruder bulk shape did not match: " + shape);
        }
        helper.assertTrue(shapes.size() >= 1, "No live extruder shape was sampled");
        GTRecipeQuery unrelated = GTRecipeQuery.items(
                new ItemStack(Items.DIRT), new ItemStack(Items.DIRT));
        helper.assertTrue(
                ModRecipeMaps.EXTRUDER.findMatch(unrelated).isEmpty(),
                "Dirt inputs matched an extruder recipe");
        helper.succeed();
    }

    /** A non-template tool-head row and a GT6 stack-split row both finish on a real extruder. */
    @GameTest(template = TEMPLATE, timeoutTicks = 2_400)
    public static void remainderAndTwoOutputRowsRunOnMachine(GameTestHelper helper) {
        RecipeMap.RecipeFamily family = family();
        helper.assertTrue(family != null, "Extruder bulk family missing");
        GTRecipe remainder = null;
        GTRecipe twoOutputs = null;
        for (int index = 0;
                index < family.logicalRecipeCount() && (remainder == null || twoOutputs == null);
                index++) {
            GTRecipe recipe = family.enumerationEntry(index).recipe();
            if (recipe.eut() > MAX_SAMPLE_EUT) {
                continue;
            }
            if (twoOutputs == null && recipe.itemOutputs().size() == 2) {
                twoOutputs = recipe;
            } else if (remainder == null
                    && recipe.itemOutputs().size() == 1
                    && BuiltInRegistries.ITEM.getKey(recipe.itemOutputs().getFirst().getItem())
                            .getPath().contains("tool_head_raw")) {
                remainder = recipe;
            }
        }
        helper.assertTrue(remainder != null, "No live non-template tool-head extruder row");
        helper.assertTrue(twoOutputs != null, "No live two-output extruder row");
        ConfiguredProcessingMachineBlockEntity first = load(
                helper, new BlockPos(1, 2, 1), remainder);
        ConfiguredProcessingMachineBlockEntity second = load(
                helper, new BlockPos(4, 2, 1), twoOutputs);
        GTRecipe remainderRecipe = remainder;
        GTRecipe twoOutputRecipe = twoOutputs;
        helper.onEachTick(() -> {
            power(first);
            power(second);
        });
        helper.succeedWhen(() -> {
            assertProduced(helper, first, remainderRecipe);
            assertProduced(helper, second, twoOutputRecipe);
            helper.assertTrue(
                    ExtruderShapeCatalog.isShape(first.inventory().getStackInSlot(1))
                            && ExtruderShapeCatalog.isShape(second.inventory().getStackInSlot(1)),
                    "Extruder consumed its mold");
        });
    }

    private static ConfiguredProcessingMachineBlockEntity load(
            GameTestHelper helper, BlockPos pos, GTRecipe recipe) {
        helper.setBlock(pos, ModBlocks.EXTRUDER.get());
        ConfiguredProcessingMachineBlockEntity machine = helper.getBlockEntity(pos);
        helper.assertTrue(
                machine.spec() == ModProcessingMachines.EXTRUDER
                        || machine.variant().kind().behavior() == ModProcessingMachines.EXTRUDER,
                "Placed block resolved wrong machine kind");
        List<ItemStack> inputs = queryFor(recipe).itemInputs();
        for (int slot = 0; slot < inputs.size(); slot++) {
            ItemStack left = machine.inventory().insertItem(slot, inputs.get(slot), false);
            helper.assertTrue(left.isEmpty(), "Extruder slot " + slot + " rejected " + inputs.get(slot));
        }
        return machine;
    }

    private static void power(ConfiguredProcessingMachineBlockEntity machine) {
        machine.insert(
                machine.spec().energy().type(),
                32L,
                16L,
                ProcessingMachineIoFaces.energy(machine.spec(), machine.facing()),
                false);
    }

    private static void assertProduced(
            GameTestHelper helper, ConfiguredProcessingMachineBlockEntity machine, GTRecipe recipe) {
        for (ItemStack target : recipe.itemOutputs()) {
            int expected = recipe.itemOutputs().stream()
                    .filter(stack -> ItemStack.isSameItemSameComponents(stack, target))
                    .mapToInt(ItemStack::getCount)
                    .sum();
            int produced = 0;
            for (int slot : machine.spec().items().outputs()) {
                ItemStack stack = machine.inventory().getStackInSlot(slot);
                if (ItemStack.isSameItemSameComponents(stack, target)) {
                    produced += stack.getCount();
                }
            }
            helper.assertTrue(
                    produced == expected,
                    "Extruder produced " + produced + " of " + expected + " " + target);
        }
    }

    private static RecipeMap.RecipeFamily family() {
        return ModRecipeMaps.EXTRUDER
                .family(CompactRecipeFamilyProvider.familyId(
                        ModRecipeMaps.EXTRUDER.id(), PUBLICATION_GROUP))
                .orElse(null);
    }

    private static GTRecipeQuery queryFor(GTRecipe recipe) {
        List<ItemStack> items = new ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            ItemStack[] candidates = recipe.itemInputs().get(index).getItems();
            ItemStack sample = candidates[0].copy();
            sample.setCount(Math.max(1, recipe.itemInputCounts().get(index)));
            items.add(sample);
        }
        return new GTRecipeQuery(items, recipe.fluidInputs());
    }
}
