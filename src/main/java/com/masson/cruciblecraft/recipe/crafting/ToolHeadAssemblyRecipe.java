package com.masson.cruciblecraft.recipe.crafting;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModRecipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * GT6 {@code AdvancedCraftingTool}: shapeless tool head plus a wooden stick.
 *
 * <p>Heads are live material stacks ({@code PrefixMaterialItem} or unique
 * {@code MaterialItem}), not a bare prefix Item. Handle materials besides
 * {@code minecraft:stick} are not invented.
 *
 * <p>This matcher stays one recipe per tool kind. EMI lists the per-material
 * rows the way GT6 {@code RM.ToolHeads} did, not a single "any head → iron"
 * crafting preview.
 */
public final class ToolHeadAssemblyRecipe implements CraftingRecipe {
    public static final MapCodec<ToolHeadAssemblyRecipe> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING
                            .fieldOf("head_prefix")
                            .xmap(
                                    MaterialPrefixCatalog::require,
                                    MaterialPrefix::serializedName)
                            .forGetter(ToolHeadAssemblyRecipe::headPrefix),
                    BuiltInRegistries.ITEM.byNameCodec()
                            .fieldOf("result")
                            .forGetter(ToolHeadAssemblyRecipe::resultItem))
                    .apply(instance, ToolHeadAssemblyRecipe::new));

    private final MaterialPrefix headPrefix;
    private final ToolKind kind;
    private final Item resultItem;

    public ToolHeadAssemblyRecipe(MaterialPrefix headPrefix, Item resultItem) {
        this.headPrefix = headPrefix;
        this.kind = kindFor(headPrefix, resultItem);
        this.resultItem = resultItem;
    }

    public MaterialPrefix headPrefix() {
        return headPrefix;
    }

    public Item resultItem() {
        return resultItem;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return materialFrom(input) != null;
    }

    @Override
    public ItemStack assemble(
            CraftingInput input,
            HolderLookup.Provider registries) {
        String material = materialFrom(input);
        if (material == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(resultItem);
        stack.set(ModComponents.TOOL_MATERIAL.get(), material);
        return stack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(resultItem);
        stack.set(ModComponents.TOOL_MATERIAL.get(), "iron");
        return stack;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TOOL_HEAD_ASSEMBLY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    private String materialFrom(CraftingInput input) {
        String material = null;
        boolean stick = false;
        int used = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            used++;
            if (stack.is(Items.STICK)) {
                if (stick) {
                    return null;
                }
                stick = true;
                continue;
            }
            var entry = MaterialUnits.resolve(stack);
            if (entry.isPresent()
                    && entry.orElseThrow().form().equals(headPrefix)
                    && ToolMaterialRules.isAllowed(
                            kind, entry.orElseThrow().materialId())) {
                if (material != null) {
                    return null;
                }
                material = entry.orElseThrow().materialId();
                continue;
            }
            return null;
        }
        return used == 2 && stick ? material : null;
    }

    static ToolKind kindFor(MaterialPrefix prefix, Item resultItem) {
        String resultPath = BuiltInRegistries.ITEM.getKey(resultItem).getPath();
        return switch (prefix.serializedName()) {
            case "tool_head_pickaxe" -> ToolKind.PICKAXE;
            case "tool_head_shovel" -> ToolKind.SHOVEL;
            case "tool_head_axe" -> ToolKind.AXE;
            case "tool_head_hoe" -> ToolKind.HOE;
            case "tool_head_sword" -> ToolKind.SWORD;
            case "tool_head_file" -> ToolKind.FILE;
            case "tool_head_chisel" -> ToolKind.CHISEL;
            case "tool_head_saw" -> ToolKind.SAW;
            case "tool_head_screwdriver" -> ToolKind.SCREWDRIVER;
            case "tool_head_hammer" -> "material_soft_hammer".equals(resultPath)
                    ? ToolKind.SOFT_HAMMER
                    : ToolKind.SMITHING_HAMMER;
            case "tool_head_spade" -> ToolKind.SPADE;
            case "tool_head_axe_double" -> ToolKind.DOUBLE_AXE;
            case "tool_head_sense" -> ToolKind.SENSE;
            case "tool_head_plow" -> ToolKind.PLOW;
            case "tool_head_construction_pickaxe" -> ToolKind.CONSTRUCTION_PICK;
            case "tool_head_builderwand" -> ToolKind.BUILDER_WAND;
            case "lens" -> ToolKind.MAGNIFYING_GLASS;
            default -> throw new IllegalArgumentException(
                    "No finished tool for prefix " + prefix.serializedId());
        };
    }
}
