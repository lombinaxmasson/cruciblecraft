package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.content.mold.MoldCastingRules;
import com.masson.cruciblecraft.content.mold.MoldShape;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.recipe.AnvilRecipe;
import com.masson.cruciblecraft.recipe.CokeOvenRecipe;
import com.masson.cruciblecraft.recipe.CrusherRecipe;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipes;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.machine.MachineDurabilityComponent;
import com.masson.cruciblecraft.machine.MachineMaterialRules;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

@EmiEntrypoint
public final class CrucibleCraftEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory CRUCIBLE = new EmiRecipeCategory(
            id("crucible"),
            EmiStack.of(ModBlocks.CRUCIBLE.get()));
    public static final EmiRecipeCategory ANVIL = new EmiRecipeCategory(
            id("anvil"),
            EmiStack.of(ModBlocks.ANVIL.get()));
    public static final EmiRecipeCategory COKE_OVEN = new EmiRecipeCategory(
            id("coke_oven"),
            EmiStack.of(ModBlocks.COKE_OVEN.get()));
    public static final EmiRecipeCategory MOLD_CASTING = new EmiRecipeCategory(
            id("mold_casting"),
            EmiStack.of(ModItems.INGOT_MOLD.get()));
    public static final EmiRecipeCategory CRUSHER = new EmiRecipeCategory(
            id("crusher"),
            EmiStack.of(ModBlocks.BRONZE_CRUSHER.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CRUCIBLE);
        registry.addCategory(ANVIL);
        registry.addCategory(COKE_OVEN);
        registry.addCategory(MOLD_CASTING);
        registry.addCategory(CRUSHER);
        registry.addWorkstation(CRUCIBLE, EmiStack.of(ModBlocks.CRUCIBLE.get()));
        for (String material : List.of("stone", "iron", "bronze", "steel")) {
            registry.addWorkstation(ANVIL, EmiStack.of(anvilVariant(material)));
        }
        registry.addWorkstation(COKE_OVEN, EmiStack.of(ModBlocks.COKE_OVEN.get()));
        registry.addWorkstation(CRUSHER, EmiStack.of(ModBlocks.BRONZE_CRUSHER.get()));
        for (MoldShape shape : MoldShape.values()) {
            registry.addWorkstation(MOLD_CASTING, EmiStack.of(ModItems.moldItem(shape).get()));
        }

        registerAlloys(registry);
        registerAnvilRecipes(registry);
        registerCokeOvenRecipes(registry);
        registerMoldCasting(registry);
        registerCrusherRecipes(registry);
    }

    private static void registerAlloys(EmiRegistry registry) {
        for (MaterialDefinition material : MaterialCatalog.values()) {
            if (material.composition().isEmpty()
                    || material.noDecompose()
                    || !material.forms().contains(MaterialForm.INGOT)) {
                continue;
            }

            List<EmiIngredient> inputs = new ArrayList<>();
            boolean complete = true;
            int outputCount = 0;
            for (var component : material.composition().entrySet()) {
                Optional<Item> item = displayItem(component.getKey());
                if (item.isEmpty()) {
                    complete = false;
                    break;
                }
                inputs.add(EmiStack.of(item.get(), component.getValue()));
                outputCount += component.getValue();
            }

            Optional<Item> output = MaterialLookup.item(material.id(), MaterialForm.INGOT);
            if (complete && output.isPresent()) {
                registry.addRecipe(new AlloyEmiRecipe(
                        material.id(),
                        inputs,
                        EmiStack.of(output.get(), outputCount)));
            }
        }
    }

    private static void registerAnvilRecipes(EmiRegistry registry) {
        for (RecipeHolder<AnvilRecipe> holder :
                registry.getRecipeManager().getAllRecipesFor(ModRecipes.ANVIL_TYPE.get())) {
            AnvilRecipe recipe = holder.value();
            for (MaterialDefinition material : MaterialCatalog.values()) {
                if (recipe.material().isPresent()
                        && !recipe.material().get().equals(material.id())) {
                    continue;
                }
                Optional<Item> input = MaterialLookup.item(material.id(), recipe.input());
                Optional<Item> secondInput = recipe.secondInput()
                        .flatMap(form -> MaterialLookup.item(material.id(), form));
                Optional<Item> output = MaterialLookup.item(material.id(), recipe.output());
                Optional<Item> secondaryOutput = recipe.secondaryOutput()
                        .flatMap(form -> MaterialLookup.item(material.id(), form));
                if (input.isEmpty() || output.isEmpty()
                        || (recipe.secondInput().isPresent() && secondInput.isEmpty())
                        || (recipe.secondaryOutput().isPresent() && secondaryOutput.isEmpty())) {
                    continue;
                }
                String workstationMaterial = material.tier() <= 0
                        ? "stone"
                        : material.tier() == 1 ? "bronze" : material.tier() == 2 ? "iron" : "steel";
                registry.addRecipe(new AnvilEmiRecipe(
                        holder,
                        material.id(),
                        input.get(),
                        secondInput.orElse(null),
                        output.get(),
                        secondaryOutput.orElse(null),
                        ModItems.SMITHING_HAMMER.get().variant(
                                "stone".equals(workstationMaterial) ? "bronze" : workstationMaterial),
                        anvilVariant(workstationMaterial)));
            }
        }
    }

    private static Optional<Item> displayItem(String materialId) {
        Optional<Item> ingot = MaterialLookup.item(materialId, MaterialForm.INGOT);
        return ingot.isPresent()
                ? ingot
                : MaterialLookup.item(materialId, MaterialForm.DUST);
    }

    private static void registerCokeOvenRecipes(EmiRegistry registry) {
        for (RecipeHolder<CokeOvenRecipe> holder :
                registry.getRecipeManager().getAllRecipesFor(ModRecipes.COKE_OVEN_TYPE.get())) {
            registry.addRecipe(new CokeOvenEmiRecipe(holder));
        }
    }

    private static void registerMoldCasting(EmiRegistry registry) {
        for (MaterialDefinition material : MaterialCatalog.values()) {
            Optional<Item> input = displayItem(material.id());
            if (input.isEmpty()) {
                continue;
            }
            var costPerIngot = material.composition().isEmpty() || material.noDecompose()
                    ? java.util.Map.of(material.id(), MaterialForm.INGOT.units())
                    : com.masson.cruciblecraft.material.MaterialCatalog.decompose(
                            material,
                            MaterialForm.INGOT.units());
            for (MoldShape shape : MoldShape.values()) {
                if (!material.forms().contains(shape.form())) {
                    continue;
                }
                Optional<Item> output = MaterialLookup.item(material.id(), shape.form());
                Optional<MoldCastingRules.Batch> batch =
                        MoldCastingRules.smallestBatch(costPerIngot, shape.form());
                if (output.isPresent() && batch.isPresent()) {
                    registry.addRecipe(new MoldCastingEmiRecipe(
                            material.id(),
                            shape.form().serializedName(),
                            input.get(),
                            ModItems.moldItem(shape).get(),
                            output.get(),
                            batch.get().outputCount()));
                }
            }
        }
    }

    private static void registerCrusherRecipes(EmiRegistry registry) {
        for (RecipeHolder<CrusherRecipe> holder :
                registry.getRecipeManager().getAllRecipesFor(ModRecipes.CRUSHER_TYPE.get())) {
            CrusherRecipe recipe = holder.value();
            for (MaterialDefinition material : MaterialCatalog.values()) {
                Optional<Item> input = MaterialLookup.item(material.id(), recipe.input());
                Optional<Item> output = MaterialLookup.item(material.id(), recipe.output());
                if (input.isPresent() && output.isPresent()) {
                    registry.addRecipe(new CrusherEmiRecipe(
                            holder, material.id(), input.get(), output.get()));
                }
            }
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }

    private static ItemStack anvilVariant(String material) {
        ItemStack stack = new ItemStack(ModItems.ANVIL.get());
        stack.set(ModComponents.MACHINE_MATERIAL, material);
        long max = MachineMaterialRules.anvilMaxDurability(material);
        stack.set(ModComponents.MACHINE_DURABILITY, new MachineDurabilityComponent(max, max));
        return stack;
    }
}
