package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GT6 NERFED_WOOD: hand craft 2 planks, saw 4. Runs on
 * {@link CrucibleCraftGameTests#NAMESPACE}.
 */
@GameTestHolder(CrucibleCraftGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class WoodPlankCraftingGameTests {
    private static final String TEMPLATE = "empty";

    private WoodPlankCraftingGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void logsHandCraftTwoPlanks(GameTestHelper helper) {
        Level level = helper.getLevel();
        ItemStack oak = craft(level, 1, 1, List.of(new ItemStack(Items.OAK_LOG)));
        helper.assertTrue(
                oak.is(Items.OAK_PLANKS) && oak.getCount() == 2,
                "oak log must hand-craft 2 oak planks, got " + oak);
        helper.assertTrue(
                recipeId(level, 1, 1, List.of(new ItemStack(Items.OAK_LOG)))
                        .equals(ResourceLocation.parse("minecraft:oak_planks")),
                "vanilla oak plank overlay id drifted");

        ItemStack mapleLog = new ItemStack(
                ModItems.treeLogItem(GtTreeSpecies.MAPLE).get());
        ItemStack maple = craft(level, 1, 1, List.of(mapleLog));
        helper.assertTrue(
                maple.is(ModItems.gtWood("maple_planks").get())
                        && maple.getCount() == 2,
                "maple log must hand-craft 2 maple planks, got " + maple);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void sawCraftsFourPlanks(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<ItemStack> slots = List.of(
                new ItemStack(ModItems.MATERIAL_SAW.get()),
                new ItemStack(Items.OAK_LOG));
        ItemStack oak = craft(level, 1, 2, slots);
        helper.assertTrue(
                oak.is(Items.OAK_PLANKS) && oak.getCount() == 4,
                "oak log + saw must craft 4 oak planks, got " + oak);
        helper.succeed();
    }

    private static ResourceLocation recipeId(
            Level level,
            int width,
            int height,
            List<ItemStack> slots) {
        var match = level.getRecipeManager()
                .getRecipeFor(
                        RecipeType.CRAFTING,
                        CraftingInput.of(width, height, slots),
                        level)
                .orElse(null);
        if (match == null) {
            throw new IllegalStateException("No crafting match");
        }
        return match.id();
    }

    private static ItemStack craft(
            Level level,
            int width,
            int height,
            List<ItemStack> slots) {
        CraftingInput input = CraftingInput.of(width, height, slots);
        var match = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level)
                .orElse(null);
        if (match == null) {
            return ItemStack.EMPTY;
        }
        return match.value().assemble(input, level.registryAccess());
    }
}
