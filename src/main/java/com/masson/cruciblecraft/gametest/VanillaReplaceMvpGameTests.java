package com.masson.cruciblecraft.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated vanilla-replace MVP gate. Run with
 * {@code -PwaveRecipes=vanilla-replace-mvp}. Paper is the implemented
 * substitute; furnace and bone meal stay vanilla (deferred, not deleted).
 */
@GameTestHolder(VanillaReplaceMvpGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class VanillaReplaceMvpGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_vanilla_replace_mvp";
    private static final String TEMPLATE = "empty";

    private VanillaReplaceMvpGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "vanilla_replace_mvp", timeoutTicks = 80)
    public static void paperSubstituteAndLockOutsideRecipesHold(
            GameTestHelper helper) {
        assertLockRecipes(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "vanilla_replace_mvp", timeoutTicks = 400)
    public static void vanillaReplaceLockSurvivesDatapackReload(
            GameTestHelper helper) {
        assertLockRecipes(helper);
        MinecraftServer server = helper.getLevel().getServer();
        AtomicBoolean reloaded = new AtomicBoolean(false);
        server.reloadResources(server.getPackRepository().getSelectedIds())
                .thenRun(() -> reloaded.set(true));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        reloaded.get(),
                        "Datapack reload did not finish"))
                .thenExecute(() -> assertLockRecipes(helper))
                .thenSucceed();
    }

    private static void assertLockRecipes(GameTestHelper helper) {
        Level level = helper.getLevel();
        ItemStack paper = craft(
                level,
                3,
                1,
                List.of(
                        new ItemStack(Items.SUGAR_CANE),
                        new ItemStack(Items.SUGAR_CANE),
                        new ItemStack(Items.SUGAR_CANE)));
        helper.assertTrue(
                paper.is(Items.PAPER) && paper.getCount() == 1,
                "minecraft:paper must assemble 1 paper from 3 sugar cane, got "
                        + paper);
        helper.assertTrue(
                recipeId(level, 3, 1, caneRow()).equals(
                        ResourceLocation.parse("minecraft:paper")),
                "Live paper recipe id drifted");

        ItemStack furnace = craft(
                level,
                3,
                3,
                cobbleFurnaceSlots());
        helper.assertTrue(
                furnace.is(Items.FURNACE) && furnace.getCount() == 1,
                "Lock-outside minecraft:furnace must remain, got " + furnace);

        ItemStack table = craft(
                level,
                2,
                2,
                List.of(
                        new ItemStack(Items.OAK_PLANKS),
                        new ItemStack(Items.OAK_PLANKS),
                        new ItemStack(Items.OAK_PLANKS),
                        new ItemStack(Items.OAK_PLANKS)));
        helper.assertTrue(
                table.is(Items.CRAFTING_TABLE) && table.getCount() == 1,
                "Lock-outside minecraft:crafting_table must remain, got "
                        + table);

        ItemStack bonemeal = craft(level, 3, 3, boneMealSlots());
        helper.assertTrue(
                bonemeal.is(Items.BONE_MEAL) && bonemeal.getCount() == 3,
                "Deferred minecraft:bone_meal must remain, got " + bonemeal);
    }

    private static List<ItemStack> caneRow() {
        return List.of(
                new ItemStack(Items.SUGAR_CANE),
                new ItemStack(Items.SUGAR_CANE),
                new ItemStack(Items.SUGAR_CANE));
    }

    private static List<ItemStack> boneMealSlots() {
        List<ItemStack> slots = new ArrayList<>();
        slots.add(new ItemStack(Items.BONE));
        for (int index = 1; index < 9; index++) {
            slots.add(ItemStack.EMPTY);
        }
        return slots;
    }

    private static List<ItemStack> cobbleFurnaceSlots() {
        List<ItemStack> slots = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            slots.add(index == 4
                    ? ItemStack.EMPTY
                    : new ItemStack(Items.COBBLESTONE));
        }
        return slots;
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
