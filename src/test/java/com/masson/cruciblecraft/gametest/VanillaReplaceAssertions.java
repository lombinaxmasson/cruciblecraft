package com.masson.cruciblecraft.gametest;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Shared assertions for GT6 Vanilla.java opening → chainmail / food
 * substitutes. Isolated wave and default-grid holders both call this.
 */
final class VanillaReplaceAssertions {
    private VanillaReplaceAssertions() {
    }

    static void openingThroughRedstone(GameTestHelper helper) {
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

        ItemStack emptyFurnace = craft(level, 3, 3, cobbleFurnaceSlots(false));
        helper.assertTrue(
                emptyFurnace.isEmpty(),
                "8 cobble without firestarter must not craft a furnace, got "
                        + emptyFurnace);
        ItemStack furnace = craft(level, 3, 3, cobbleFurnaceSlots(true));
        helper.assertTrue(
                furnace.is(Items.FURNACE) && furnace.getCount() == 1,
                "minecraft:furnace must need a firestarter, got " + furnace);

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

        ItemStack vanillaBone = craft(level, 3, 3, loneBoneSlots());
        helper.assertTrue(
                vanillaBone.isEmpty(),
                "Lone bone must not still make 3 bone meal, got " + vanillaBone);
        ItemStack bonemeal = craft(
                level,
                1,
                2,
                List.of(hammer(), new ItemStack(Items.BONE)));
        helper.assertTrue(
                bonemeal.is(Items.BONE_MEAL) && bonemeal.getCount() == 1,
                "Hammer + bone must make 1 bone meal, got " + bonemeal);

        ItemStack magma = craft(
                level,
                1,
                2,
                List.of(
                        new ItemStack(Items.BLAZE_POWDER),
                        new ItemStack(Items.SLIME_BALL)));
        helper.assertTrue(
                magma.isEmpty(),
                "Magma cream workbench recipe must be deleted, got " + magma);

        ItemStack bucket = craft(
                level,
                3,
                2,
                List.of(
                        curvedIron(),
                        hammer(),
                        curvedIron(),
                        ItemStack.EMPTY,
                        ironPlate(),
                        ItemStack.EMPTY));
        helper.assertTrue(
                bucket.is(Items.BUCKET) && bucket.getCount() == 1,
                "Iron curved plates + plate + hammer must make a bucket, got "
                        + bucket);

        ItemStack bars = craft(level, 3, 3, ironBarSlots());
        helper.assertTrue(
                bars.is(Items.IRON_BARS) && bars.getCount() == 8,
                "6 iron rods + wrench must make 8 bars, got " + bars);

        ItemStack hopper = craft(level, 3, 3, hopperSlots());
        helper.assertTrue(
                hopper.is(Items.HOPPER) && hopper.getCount() == 1,
                "Iron plates + chest + wrench must make a hopper, got "
                        + hopper);

        ItemStack piston = craft(level, 3, 3, pistonSlots());
        helper.assertTrue(
                piston.is(Items.PISTON) && piston.getCount() == 1,
                "Piston must still accept an iron ingot, got " + piston);
        ItemStack sticky = craft(
                level,
                1,
                2,
                List.of(new ItemStack(Items.SLIME_BALL), piston.copy()));
        helper.assertTrue(
                sticky.is(Items.STICKY_PISTON) && sticky.getCount() == 1,
                "Slime over piston must make a sticky piston, got " + sticky);

        ItemStack lever = craft(
                level,
                1,
                2,
                List.of(
                        new ItemStack(Items.STICK),
                        new ItemStack(Items.COBBLESTONE)));
        helper.assertTrue(
                lever.is(Items.LEVER) && lever.getCount() == 1,
                "Stick + cobble must make a lever, got " + lever);

        ItemStack repeater = craft(level, 3, 3, repeaterSlots());
        helper.assertTrue(
                repeater.is(Items.REPEATER) && repeater.getCount() == 1,
                "Repeater dust+stick recipe must remain, got " + repeater);

        ItemStack nameTag = craft(
                level,
                2,
                2,
                List.of(
                        ItemStack.EMPTY,
                        new ItemStack(Items.STRING),
                        new ItemStack(Items.PAPER),
                        new ItemStack(ModItems.MATERIAL_SCISSORS.get())));
        helper.assertTrue(
                nameTag.is(Items.NAME_TAG) && nameTag.getCount() == 1,
                "Paper + string + scissors must make a name tag, got "
                        + nameTag);

        ItemStack magnetic = craft(
                level,
                3,
                2,
                List.of(
                        ironRod(),
                        new ItemStack(Items.REDSTONE),
                        new ItemStack(Items.REDSTONE),
                        new ItemStack(Items.REDSTONE),
                        new ItemStack(Items.REDSTONE),
                        ItemStack.EMPTY));
        helper.assertTrue(
                MaterialLookup.matches(
                        magnetic, "iron_magnetic", MaterialPrefixes.ROD)
                        && magnetic.getCount() == 1,
                "Iron rod + 4 redstone must make a magnetic rod, got "
                        + magnetic);
    }

    static void tntThroughChainmail(GameTestHelper helper) {
        Level level = helper.getLevel();
        ItemStack vanillaTnt = craft(level, 3, 3, vanillaSandTntSlots());
        helper.assertTrue(
                vanillaTnt.is(Items.TNT) && vanillaTnt.getCount() == 1,
                "Vanilla sand TNT extra must remain, got " + vanillaTnt);
        ItemStack dustTnt = craft(level, 3, 3, dustTntSlots());
        helper.assertTrue(
                dustTnt.is(Items.TNT) && dustTnt.getCount() == 1,
                "Gunpowder dust + silicon dioxide dust must make TNT, got "
                        + dustTnt);

        ItemStack fiveIngotCart = craft(level, 3, 2, fiveIngotMinecartSlots());
        helper.assertTrue(
                fiveIngotCart.isEmpty(),
                "Five iron ingots must not still make a minecart, got "
                        + fiveIngotCart);
        ItemStack minecart = craft(level, 3, 3, minecartSlots());
        helper.assertTrue(
                minecart.is(Items.MINECART) && minecart.getCount() == 1,
                "Iron plates + wheels + hammer/wrench must make a minecart, got "
                        + minecart);

        ItemStack chestCart = craft(
                level,
                1,
                2,
                List.of(new ItemStack(Items.CHEST), minecart.copy()));
        helper.assertTrue(
                chestCart.is(Items.CHEST_MINECART) && chestCart.getCount() == 1,
                "Chest over minecart must make a chest minecart, got "
                        + chestCart);
        ItemStack furnaceCart = craft(
                level,
                1,
                2,
                List.of(new ItemStack(Items.FURNACE), minecart.copy()));
        helper.assertTrue(
                furnaceCart.is(Items.FURNACE_MINECART)
                        && furnaceCart.getCount() == 1,
                "Furnace over minecart must make a furnace minecart, got "
                        + furnaceCart);

        ItemStack helmet = craft(level, 3, 2, chainmailHelmetSlots());
        helper.assertTrue(
                helmet.is(Items.CHAINMAIL_HELMET) && helmet.getCount() == 1,
                "Steel rings + hammer must make a chainmail helmet, got "
                        + helmet);

        ItemStack recycled = craft(
                level,
                2,
                1,
                List.of(
                        new ItemStack(Items.LEATHER_HELMET),
                        new ItemStack(ModItems.MATERIAL_KNIFE.get())));
        helper.assertTrue(
                recycled.is(Items.LEATHER) && recycled.getCount() == 1,
                "Knife + leather helmet must recycle 1 leather, got "
                        + recycled);

        ItemStack cookie = craft(
                level,
                3,
                1,
                List.of(
                        new ItemStack(Items.WHEAT),
                        new ItemStack(Items.COCOA_BEANS),
                        new ItemStack(Items.WHEAT)));
        helper.assertTrue(
                cookie.isEmpty(),
                "Cookie workbench recipe must be deleted, got " + cookie);
        ItemStack goldenApple = craft(level, 3, 3, goldenAppleSlots());
        helper.assertTrue(
                goldenApple.isEmpty(),
                "Golden apple workbench recipe must be deleted, got "
                        + goldenApple);
    }

    private static ItemStack hammer() {
        return new ItemStack(ModItems.SMITHING_HAMMER.get());
    }

    private static ItemStack ironPlate() {
        return MaterialLookup.stack("iron", MaterialPrefixes.PLATE);
    }

    private static ItemStack curvedIron() {
        return MaterialLookup.stack("iron", MaterialPrefixes.CURVED_PLATE);
    }

    private static ItemStack ironRod() {
        return MaterialLookup.stack("iron", MaterialPrefixes.ROD);
    }

    private static ItemStack ironWheels() {
        return MaterialLookup.stack(
                "iron", new MaterialPrefix("cruciblecraft:minecart_wheels"));
    }

    private static ItemStack steelRing() {
        return MaterialLookup.stack("steel", MaterialPrefixes.RING);
    }

    private static ItemStack gunpowderDust() {
        return MaterialLookup.stack("gunpowder", MaterialPrefixes.DUST);
    }

    private static ItemStack siliconDioxideDust() {
        return MaterialLookup.stack("silicon_dioxide", MaterialPrefixes.DUST);
    }

    private static List<ItemStack> vanillaSandTntSlots() {
        return List.of(
                new ItemStack(Items.GUNPOWDER),
                new ItemStack(Items.SAND),
                new ItemStack(Items.GUNPOWDER),
                new ItemStack(Items.SAND),
                new ItemStack(Items.GUNPOWDER),
                new ItemStack(Items.SAND),
                new ItemStack(Items.GUNPOWDER),
                new ItemStack(Items.SAND),
                new ItemStack(Items.GUNPOWDER));
    }

    private static List<ItemStack> dustTntSlots() {
        return List.of(
                gunpowderDust(),
                siliconDioxideDust(),
                gunpowderDust(),
                siliconDioxideDust(),
                gunpowderDust(),
                siliconDioxideDust(),
                gunpowderDust(),
                siliconDioxideDust(),
                gunpowderDust());
    }

    private static List<ItemStack> fiveIngotMinecartSlots() {
        return List.of(
                new ItemStack(Items.IRON_INGOT),
                ItemStack.EMPTY,
                new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.IRON_INGOT));
    }

    private static List<ItemStack> minecartSlots() {
        return List.of(
                ItemStack.EMPTY,
                hammer(),
                ItemStack.EMPTY,
                ironPlate(),
                new ItemStack(ModItems.MATERIAL_WRENCH.get()),
                ironPlate(),
                ironWheels(),
                ironPlate(),
                ironWheels());
    }

    private static List<ItemStack> chainmailHelmetSlots() {
        return List.of(
                steelRing(),
                steelRing(),
                steelRing(),
                steelRing(),
                hammer(),
                steelRing());
    }

    private static List<ItemStack> goldenAppleSlots() {
        List<ItemStack> slots = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            slots.add(index == 4
                    ? new ItemStack(Items.APPLE)
                    : new ItemStack(Items.GOLD_INGOT));
        }
        return slots;
    }

    private static List<ItemStack> caneRow() {
        return List.of(
                new ItemStack(Items.SUGAR_CANE),
                new ItemStack(Items.SUGAR_CANE),
                new ItemStack(Items.SUGAR_CANE));
    }

    private static List<ItemStack> cobbleFurnaceSlots(boolean firestarter) {
        List<ItemStack> slots = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            if (index == 4) {
                slots.add(firestarter
                        ? new ItemStack(Items.FLINT_AND_STEEL)
                        : ItemStack.EMPTY);
            } else {
                slots.add(new ItemStack(Items.COBBLESTONE));
            }
        }
        return slots;
    }

    private static List<ItemStack> loneBoneSlots() {
        List<ItemStack> slots = new ArrayList<>();
        slots.add(new ItemStack(Items.BONE));
        for (int index = 1; index < 9; index++) {
            slots.add(ItemStack.EMPTY);
        }
        return slots;
    }

    private static List<ItemStack> ironBarSlots() {
        return List.of(
                ItemStack.EMPTY,
                new ItemStack(ModItems.MATERIAL_WRENCH.get()),
                ItemStack.EMPTY,
                ironRod(),
                ironRod(),
                ironRod(),
                ironRod(),
                ironRod(),
                ironRod());
    }

    private static List<ItemStack> hopperSlots() {
        return List.of(
                ironPlate(),
                new ItemStack(ModItems.MATERIAL_WRENCH.get()),
                ironPlate(),
                ironPlate(),
                new ItemStack(Items.CHEST),
                ironPlate(),
                ItemStack.EMPTY,
                ironPlate(),
                ItemStack.EMPTY);
    }

    private static List<ItemStack> pistonSlots() {
        return List.of(
                new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.COBBLESTONE),
                new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.COBBLESTONE),
                new ItemStack(Items.COBBLESTONE),
                new ItemStack(Items.REDSTONE),
                new ItemStack(Items.COBBLESTONE));
    }

    private static List<ItemStack> repeaterSlots() {
        return List.of(
                new ItemStack(Items.REDSTONE),
                ItemStack.EMPTY,
                new ItemStack(Items.REDSTONE),
                new ItemStack(Items.STICK),
                new ItemStack(Items.REDSTONE),
                new ItemStack(Items.STICK),
                new ItemStack(Items.STONE),
                new ItemStack(Items.STONE),
                new ItemStack(Items.STONE));
    }

    static ResourceLocation recipeId(
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

    static ItemStack craft(
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
