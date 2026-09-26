package com.masson.cruciblecraft.datagen;

import java.util.HashMap;
import java.util.Map;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.energy.remainder.RemainderDevice;
import com.masson.cruciblecraft.energy.remainder.RemainderDevice.ObtainKind;
import com.masson.cruciblecraft.energy.remainder.RemainderDevices;
import com.masson.cruciblecraft.energy.transformer.EnergyTransformerCatalog;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Source-exact shaped grids for remainder energy hosts. Blocked grids are skipped. */
public final class RemainderDeviceRecipes {
    private RemainderDeviceRecipes() {}

    public static void addAll(RecipeOutput output) {
        for (RemainderDevice device : RemainderDevices.devices()) {
            RemainderDevice.ObtainPlan plan = RemainderDevices.obtain(device);
            if (plan.kind() != ObtainKind.SHAPED) {
                continue;
            }
            Item result = result(device);
            ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(
                    RecipeCategory.MISC, result);
            for (String row : plan.pattern()) {
                builder.pattern(row);
            }
            Map<Character, Item> keys = new HashMap<>();
            for (RemainderDevice.GridPart part : plan.parts()) {
                Item item = resolve(part.token());
                if (item == null) {
                    throw new IllegalStateException(
                            "Remainder recipe part missing for "
                                    + device.id()
                                    + ": "
                                    + part.token());
                }
                keys.put(part.symbol(), item);
                builder.define(part.symbol(), Ingredient.of(item));
            }
            Item unlock = keys.get('M');
            if (unlock == null) {
                unlock = result;
            }
            builder.unlockedBy(
                    "has_center",
                    InventoryChangeTrigger.TriggerInstance.hasItems(unlock));
            builder.save(output, device.id());
        }
    }

    private static Item result(RemainderDevice device) {
        DeferredHolder<Item, ? extends Item> item =
                ModItems.remainderItemsById().get(device.id());
        if (item == null) {
            throw new IllegalStateException("Missing remainder item " + device.id());
        }
        return item.get();
    }

    private static Item resolve(String token) {
        if (token.startsWith("material:")) {
            String rest = token.substring("material:".length());
            int slash = rest.indexOf('/');
            return MaterialLookup.item(
                    rest.substring(0, slash),
                    prefix(rest.substring(slash + 1))).orElse(null);
        }
        if (token.startsWith("item:")) {
            String path = token.substring("item:".length());
            try {
                return ModItems.technologicalPart(path).get();
            } catch (IllegalArgumentException missing) {
                return null;
            }
        }
        if (token.startsWith("transformer:")) {
            int sourceId = Integer.parseInt(token.substring("transformer:".length()));
            return EnergyTransformerCatalog.profiles().stream()
                    .filter(profile -> profile.sourceId() == sourceId)
                    .map(profile -> ModItems.transformerItemsById().get(profile.id()))
                    .filter(item -> item != null)
                    .map(DeferredHolder::get)
                    .findFirst()
                    .orElse(null);
        }
        if (token.startsWith("remainder:")) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, token.substring("remainder:".length()));
            var item = ModItems.remainderItemsById().get(id);
            return item == null ? null : item.get();
        }
        throw new IllegalArgumentException("Unknown remainder part " + token);
    }

    private static MaterialPrefix prefix(String name) {
        return switch (name) {
            case "cable" -> MaterialPrefixes.CABLE;
            case "wire" -> MaterialPrefixes.WIRE;
            case "quadruple_cable" -> MaterialPrefixes.QUADRUPLE_CABLE;
            case "quadruple_wire" -> MaterialPrefixes.QUADRUPLE_WIRE;
            case "machine_casing" -> MaterialPrefixes.MACHINE_CASING;
            case "plate_gem" -> MaterialPrefixes.PLATE_GEM;
            default -> throw new IllegalArgumentException("Unknown prefix " + name);
        };
    }
}
