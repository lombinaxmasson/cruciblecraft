package com.masson.cruciblecraft.datagen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.item.ExtruderShapeCatalog;
import com.masson.cruciblecraft.content.item.SemanticObjectCatalog;
import com.masson.cruciblecraft.recipe.crafting.CraftingTools;
import com.masson.cruciblecraft.recipe.crafting.ShapedCatalystRecipe;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * GT6 {@code MultiItemTechnological} Shape_Extruder / Shape_SimpleEx crafts.
 * Empty is tungsten-carbide or ANY.Steel double plate plus hammer, file, and
 * wire cutter. Later shapes are wire-cutter conversions; {@code CR.DEF_REV}
 * is OreDict dismantle metadata, not reverse crafts.
 */
final class ExtruderShapeRecipes {
    private static final List<Conversion> CONVERSIONS = List.of(
            conversion("ingot", "x  ", " P ", "   ", "empty"),
            conversion("tiny_plate", " x ", " P ", "   ", "empty"),
            conversion("curved_plate", "  x", " P ", "   ", "empty"),
            conversion("rod", "   ", " Px", "   ", "empty"),
            conversion("foil", "   ", " P ", "  x", "empty"),
            conversion("ring", "   ", " P ", " x ", "empty"),
            conversion("bolt", "x  ", " P ", "   ", "rod"),
            conversion("wire", " x ", " P ", "   ", "rod"),
            conversion("long_rod", "  x", " P ", "   ", "rod"),
            conversion("fine_wire", "   ", " Px", "   ", "rod"),
            conversion("block", "x  ", " P ", "   ", "ingot"),
            conversion("pickaxe_head", " x ", " P ", "   ", "ingot"),
            conversion("hammer_head", "  x", " P ", "   ", "ingot"),
            conversion("hoe_head", "   ", " Px", "   ", "ingot"),
            conversion("gear", "x  ", " P ", "   ", "ring"),
            conversion("small_gear", " x ", " P ", "   ", "ring"),
            conversion("bottle", "  x", " P ", "   ", "ring"),
            conversion("cell", "   ", " Px", "   ", "ring"),
            conversion("ccc", "   ", " P ", "  x", "ring"),
            conversion("axe_head", "x  ", " P ", "   ", "tiny_plate"),
            conversion("shovel_head", " x ", " P ", "   ", "tiny_plate"),
            conversion("file_head", "  x", " P ", "   ", "tiny_plate"),
            conversion("sword_blade", "   ", " Px", "   ", "tiny_plate"),
            conversion("saw_blade", "   ", " P ", "  x", "tiny_plate"),
            conversion("plate", "x  ", " P ", "   ", "foil"),
            conversion("small_item_casing", " x ", " P ", "   ", "foil"),
            conversion("tiny_pipe", "x  ", " P ", "   ", "curved_plate"),
            conversion("small_pipe", " x ", " P ", "   ", "curved_plate"),
            conversion("normal_pipe", "  x", " P ", "   ", "curved_plate"),
            conversion("large_pipe", "   ", " Px", "   ", "curved_plate"),
            conversion("huge_pipe", "   ", " P ", "  x", "curved_plate"));

    private ExtruderShapeRecipes() {}

    static void addAll(RecipeOutput output) {
        emitEmpty(
                output,
                "gt_multiitem/extruder_shape_empty",
                ModItems.extruderShape("empty").get(),
                MaterialLookup.ingredient("tungsten_carbide", MaterialPrefixes.DOUBLE_PLATE)
                        .orElseThrow());
        emitEmpty(
                output,
                "gt_multiitem/low_heat_extruder_shape_empty",
                simpleEx(ExtruderShapeCatalog.SIMPLE_EX_EMPTY_META),
                MaterialLookup.ingredient("steel", MaterialPrefixes.DOUBLE_PLATE)
                        .orElseThrow());
        for (Conversion conversion : CONVERSIONS) {
            emitConversion(
                    output,
                    "gt_multiitem/extruder_shape_" + conversion.resultId(),
                    ModItems.extruderShape(conversion.sourceId()).get(),
                    ModItems.extruderShape(conversion.resultId()).get(),
                    conversion);
            DefinitionPair leftover = leftoverPair(conversion);
            emitConversion(
                    output,
                    "gt_multiitem/low_heat_extruder_shape_" + conversion.resultId(),
                    leftover.source(),
                    leftover.result(),
                    conversion);
        }
    }

    private static void emitEmpty(
            RecipeOutput output,
            String path,
            Item result,
            Ingredient plate) {
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put("P", plate);
        Map<String, Ingredient> catalysts = new LinkedHashMap<>();
        catalysts.put("h", Ingredient.of(CraftingTools.HAMMER));
        catalysts.put("f", Ingredient.of(CraftingTools.FILE));
        catalysts.put("x", Ingredient.of(CraftingTools.WIRE_CUTTER));
        output.accept(
                id(path),
                new ShapedCatalystRecipe(
                        List.of("hf ", "xP "),
                        ingredients,
                        catalysts,
                        new ItemStack(result)),
                null);
    }

    private static void emitConversion(
            RecipeOutput output,
            String path,
            Item source,
            Item result,
            Conversion conversion) {
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put("P", Ingredient.of(source));
        Map<String, Ingredient> catalysts = new LinkedHashMap<>();
        catalysts.put("x", Ingredient.of(CraftingTools.WIRE_CUTTER));
        output.accept(
                id(path),
                new ShapedCatalystRecipe(
                        List.of(conversion.row0(), conversion.row1(), conversion.row2()),
                        ingredients,
                        catalysts,
                        new ItemStack(result)),
                null);
    }

    private static DefinitionPair leftoverPair(Conversion conversion) {
        int sourceMeta = ExtruderShapeCatalog.require(conversion.sourceId()).gt6Meta()
                + 200;
        int resultMeta = ExtruderShapeCatalog.require(conversion.resultId()).gt6Meta()
                + 200;
        return new DefinitionPair(simpleEx(sourceMeta), simpleEx(resultMeta));
    }

    private static Item simpleEx(int meta) {
        SemanticObjectCatalog.Identity identity = SemanticObjectCatalog.identities()
                .stream()
                .filter(ExtruderShapeCatalog::isSimpleExIdentity)
                .filter(candidate -> candidate.meta() == meta)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing SimpleEx leftover meta " + meta));
        var holder = ModItems.semanticIdentityItemsById().get(identity.id());
        if (holder == null) {
            throw new IllegalStateException(
                    "Missing SimpleEx leftover item " + identity.id());
        }
        return holder.get();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }

    private static Conversion conversion(
            String resultId,
            String row0,
            String row1,
            String row2,
            String sourceId) {
        return new Conversion(resultId, row0, row1, row2, sourceId);
    }

    private record Conversion(
            String resultId,
            String row0,
            String row1,
            String row2,
            String sourceId) {}

    private record DefinitionPair(Item source, Item result) {}
}
