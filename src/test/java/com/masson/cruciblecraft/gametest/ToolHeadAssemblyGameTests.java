package com.masson.cruciblecraft.gametest;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.content.item.MaterialToolItem;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GT6 {@code AdvancedCraftingTool}: tool head plus a wooden stick.
 */
@GameTestHolder("cruciblecraft_content")
@PrefixGameTestTemplate(false)
public final class ToolHeadAssemblyGameTests {
    private static final String TEMPLATE = "empty";
    private static final MaterialPrefix TOOL_HEAD_PICKAXE =
            new MaterialPrefix("cruciblecraft:tool_head_pickaxe");

    private ToolHeadAssemblyGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void ironPickaxeHeadAndStickCraftFinishedTool(
            GameTestHelper helper) {
        ItemStack head = MaterialLookup.stack("iron", TOOL_HEAD_PICKAXE);
        List<ItemStack> slots = new ArrayList<>(
                List.of(head, new ItemStack(Items.STICK),
                        ItemStack.EMPTY, ItemStack.EMPTY));
        CraftingInput input = CraftingInput.of(2, 2, slots);
        var match = helper.getLevel()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElse(null);
        helper.assertTrue(
                match != null,
                "iron pickaxe head plus stick did not match assembly");
        ItemStack assembled = match.value().assemble(
                input, helper.getLevel().registryAccess());
        helper.assertTrue(
                assembled.getItem() instanceof MaterialToolItem
                        && "iron".equals(
                                assembled.get(ModComponents.TOOL_MATERIAL))
                        && BuiltInRegistries.ITEM.getKey(assembled.getItem())
                                .equals(ResourceLocation.fromNamespaceAndPath(
                                        CrucibleCraft.MODID,
                                        "material_pickaxe")),
                "assembly did not yield an iron pickaxe: " + assembled);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void pickaxeAssemblyRecipeIsLoaded(GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel()
                        .getRecipeManager()
                        .byKey(ResourceLocation.fromNamespaceAndPath(
                                CrucibleCraft.MODID,
                                "tools/assemble/pickaxe"))
                        .isPresent(),
                "tools/assemble/pickaxe is not in the recipe manager");
        helper.succeed();
    }
}
