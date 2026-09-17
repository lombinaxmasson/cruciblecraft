package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.mold.MoldCastingRules;
import com.masson.cruciblecraft.content.mold.MoldShape;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.recipe.AnvilMode;
import com.masson.cruciblecraft.energy.converter.EnergyConverterCatalog;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.content.item.PrefixMaterialItem;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMachineVariants;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.machine.MachineDurabilityComponent;
import com.masson.cruciblecraft.machine.MachineMaterialRules;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

@EmiEntrypoint
public final class CrucibleCraftEmiPlugin implements EmiPlugin {
    private static final Map<ResourceLocation, EmiRecipeCategory> INTERNED_CATEGORIES =
            new HashMap<>();

    public static final EmiRecipeCategory CRUCIBLE = internCategory(
            id("crucible"),
            EmiStack.of(ModBlocks.CRUCIBLE.get()));
    public static final EmiRecipeCategory ANVIL = internCategory(
            id("anvil"),
            EmiStack.of(ModBlocks.ANVIL.get()));
    public static final EmiRecipeCategory COKE_OVEN = internCategory(
            id("coke_oven"),
            EmiStack.of(ModBlocks.COKE_OVEN.get()));
    public static final EmiRecipeCategory MOLD_CASTING = internCategory(
            id("mold_casting"),
            EmiStack.of(ModItems.INGOT_MOLD.get()));
    public static final EmiRecipeCategory CRUSHER = internCategory(
            id("crusher"),
            EmiStack.of(ModBlocks.BRONZE_CRUSHER.get()));
    public static final EmiRecipeCategory FUSION = internCategory(
            id("fusion"),
            EmiStack.of(ModBlocks.FUSION_REACTOR.get()));

    @Override
    public void register(EmiRegistry registry) {
        Set<ResourceLocation> addedCategories = new HashSet<>();
        addCategory(registry, CRUCIBLE, addedCategories);
        addCategory(registry, ANVIL, addedCategories);
        addCategory(registry, COKE_OVEN, addedCategories);
        addCategory(registry, MOLD_CASTING, addedCategories);
        addCategory(registry, CRUSHER, addedCategories);
        addCategory(registry, FUSION, addedCategories);
        registry.addWorkstation(CRUCIBLE, EmiStack.of(ModBlocks.CRUCIBLE.get()));
        for (String material : List.of("stone", "iron", "bronze", "steel")) {
            registry.addWorkstation(ANVIL, EmiStacks.ofItem(anvilVariant(material)));
        }
        registry.addWorkstation(COKE_OVEN, EmiStack.of(ModBlocks.COKE_OVEN.get()));
        registry.addWorkstation(CRUSHER, EmiStack.of(ModBlocks.BRONZE_CRUSHER.get()));
        registry.addWorkstation(FUSION, EmiStack.of(ModBlocks.FUSION_REACTOR.get()));
        for (MoldShape shape : MoldShape.values()) {
            registry.addWorkstation(MOLD_CASTING, EmiStack.of(ModItems.moldItem(shape).get()));
        }

        registerAlloys(registry);
        registerAnvilRecipes(registry);
        registerCokeOvenRecipes(registry);
        registerMoldCasting(registry);
        registerCrusherRecipes(registry);
        registerFusionRecipes(registry);
        registerProcessingMachines(registry, addedCategories);
        registerFuelMaps(registry, addedCategories);
        registerHeatExchangerFuels(registry, addedCategories);
        registerDisplayStacks(registry);
        for (var cover : List.of(
                ModItems.LOGISTICS_ITEM_STORAGE_COVER,
                ModItems.LOGISTICS_ITEM_IMPORT_COVER,
                ModItems.LOGISTICS_ITEM_EXPORT_COVER,
                ModItems.LOGISTICS_FLUID_STORAGE_COVER,
                ModItems.LOGISTICS_FLUID_IMPORT_COVER,
                ModItems.LOGISTICS_FLUID_EXPORT_COVER,
                ModItems.LOGISTICS_GENERIC_STORAGE_COVER,
                ModItems.LOGISTICS_GENERIC_IMPORT_COVER,
                ModItems.LOGISTICS_GENERIC_EXPORT_COVER,
                ModItems.LOGISTICS_GENERIC_DUMP_COVER,
                ModItems.LOGISTICS_DISPLAY_CPU_LOGIC_COVER,
                ModItems.LOGISTICS_DISPLAY_CPU_CONTROL_COVER,
                ModItems.LOGISTICS_DISPLAY_CPU_STORAGE_COVER,
                ModItems.LOGISTICS_DISPLAY_CPU_CONVERSION_COVER)) {
            registry.addEmiStack(EmiStack.of(cover.get()));
        }
        ModItems.machineCovers().forEach(
                cover -> registry.addEmiStack(EmiStack.of(cover.get())));
    }

    /** Item-list polish layers: gated prefix+material stacks join the index,
     *  routed tool variants do the same, and each material's dust /
     *  small_dust / tiny_dust triple aliases so the 9 tiny = 4 small =
     *  1 dust conversion search reaches all three. Per-material only —
     *  never an "any ingot" merge. */
    private static void registerDisplayStacks(EmiRegistry registry) {
        registry.removeEmiStacks(stack -> {
            ItemStack itemStack = stack.getItemStack();
            return itemStack.getItem() instanceof PrefixMaterialItem
                    && itemStack.get(ModComponents.PREFIX_MATERIAL) == null;
        });
        com.masson.cruciblecraft.content.item.ToolDisplayPlan
                .routedVariantStacks()
                .forEach(stack -> registry.addEmiStack(EmiStacks.ofItem(stack)));

        var materials = MaterialCatalog.startupValues();
        Map<String, List<com.masson.cruciblecraft.api.material.MaterialPrefix>> forms =
                new java.util.LinkedHashMap<>();
        materials.forEach(material -> forms.put(
                material.id(), MaterialCatalog.registeredForms(material)));
        for (String logicalId : EmiDisplayPlan.gatedPrefixStacks(materials, forms)) {
            EmiStack stack = stackOf(logicalId);
            if (!stack.isEmpty()) {
                registry.addEmiStack(stack);
            }
        }
        ModItems.prefixMaterialItems().forEach(item ->
                registry.setDefaultComparison(
                        item.get(), Comparison.compareComponents()));
        for (EmiDisplayPlan.DustFamily family :
                EmiDisplayPlan.dustFamilies(materials, forms)) {
            EmiStack dust = stackOf(family.dust());
            EmiStack small = stackOf(family.smallDust());
            EmiStack tiny = stackOf(family.tinyDust());
            if (dust.isEmpty() || small.isEmpty() || tiny.isEmpty()) {
                continue;
            }
            registry.addAlias(
                    new dev.emi.emi.api.stack.ListEmiIngredient(
                            List.of(dust, small, tiny), 1),
                    dust.getItemStack().getHoverName());
        }
    }

    private static EmiStack stackOf(String itemId) {
        return MaterialLookup.stackFromLogicalId(itemId)
                .map(EmiStacks::ofItem)
                .orElseGet(() -> EmiStack.of(
                        net.minecraft.world.item.Items.AIR));
    }

    private static void registerAlloys(EmiRegistry registry) {
        for (MaterialDefinition material : MaterialCatalog.values()) {
            if (material.composition().isEmpty()
                    || material.noDecompose()
                    || !MaterialCatalog.isFormRegistered(
                            material, MaterialPrefixes.INGOT)) {
                continue;
            }

            List<EmiIngredient> inputs = new ArrayList<>();
            boolean complete = true;
            int outputCount = 0;
            for (var component : material.composition().entrySet()) {
                Optional<ItemStack> item = displayStack(component.getKey());
                if (item.isEmpty()) {
                    complete = false;
                    break;
                }
                ItemStack stack = item.orElseThrow().copy();
                stack.setCount(component.getValue());
                inputs.add(EmiStack.of(stack));
                outputCount += component.getValue();
            }

            Optional<ItemStack> output = MaterialLookup.tryStack(
                    material.id(), MaterialPrefixes.INGOT, outputCount);
            if (complete && output.isPresent()) {
                registry.addRecipe(new AlloyEmiRecipe(
                        material.id(),
                        inputs,
                        EmiStack.of(output.orElseThrow())));
            }
        }
    }

    private static void registerAnvilRecipes(EmiRegistry registry) {
        for (AnvilMode mode : AnvilMode.values()) {
            var map = ModRecipeMaps.anvil(mode);
            for (var entry : map.entries()) {
                var recipe = entry.recipe();
                ItemStack[] primaryItems = recipe.itemInputs().getFirst().getItems();
                if (primaryItems.length == 0) {
                    continue;
                }
                int materialTier = MaterialUnits.resolve(primaryItems[0])
                        .map(materialEntry -> materialEntry.material().tier())
                        .orElse(1);
                String workstationMaterial = materialTier <= 0
                        ? "stone"
                        : materialTier == 1 ? "bronze" : materialTier == 2 ? "iron" : "steel";
                registry.addRecipe(new AnvilEmiRecipe(
                        entry.id(),
                        mode,
                        recipe,
                        ModItems.SMITHING_HAMMER.get().variant(
                                "stone".equals(workstationMaterial) ? "bronze" : workstationMaterial),
                        anvilVariant(workstationMaterial)));
            }
        }
    }

    private static Optional<ItemStack> displayStack(String materialId) {
        Optional<ItemStack> ingot = MaterialLookup.tryStack(
                materialId, MaterialPrefixes.INGOT, 1);
        return ingot.isPresent()
                ? ingot
                : MaterialLookup.tryStack(materialId, MaterialPrefixes.DUST, 1);
    }

    private static void registerCokeOvenRecipes(EmiRegistry registry) {
        for (var entry : ModRecipeMaps.COKE_OVEN.entries()) {
            registry.addRecipe(new CokeOvenEmiRecipe(
                    entry.id(),
                    entry.recipe()));
        }
    }

    private static void registerMoldCasting(EmiRegistry registry) {
        for (MaterialDefinition material : MaterialCatalog.values()) {
            Optional<ItemStack> input = displayStack(material.id());
            if (input.isEmpty()) {
                continue;
            }
            var costPerIngot = material.composition().isEmpty() || material.noDecompose()
                    ? java.util.Map.of(material.id(), MaterialPrefixes.INGOT.units())
                    : com.masson.cruciblecraft.material.MaterialCatalog.decompose(
                            material,
                            MaterialPrefixes.INGOT.units());
            for (MoldShape shape : MoldShape.values()) {
                if (!MaterialCatalog.isFormRegistered(material, shape.form())) {
                    continue;
                }
                Optional<ItemStack> output = MaterialLookup.tryStack(
                        material.id(), shape.form(), 1);
                Optional<MoldCastingRules.Batch> batch =
                        MoldCastingRules.smallestBatch(costPerIngot, shape.form());
                if (output.isPresent() && batch.isPresent()) {
                    registry.addRecipe(new MoldCastingEmiRecipe(
                            material.id(),
                            shape.form().serializedName(),
                            input.orElseThrow(),
                            ModItems.moldItem(shape).get(),
                            output.orElseThrow(),
                            batch.get().outputCount()));
                }
            }
        }
    }

    private static void registerCrusherRecipes(EmiRegistry registry) {
        for (var entry : ModRecipeMaps.CRUSHER.entries()) {
            registry.addRecipe(new CrusherEmiRecipe(
                    entry.id(),
                    entry.recipe()));
        }
    }

    private static void registerFusionRecipes(EmiRegistry registry) {
        for (var entry : ModRecipeMaps.FUSION.entries()) {
            registry.addRecipe(new FusionEmiRecipe(entry.id(), entry.recipe()));
        }
        for (var entry : ModRecipeMaps.FUSION_EXTENSION.entries()) {
            registry.addRecipe(new FusionEmiRecipe(entry.id(), entry.recipe()));
        }
    }

    private static void registerFuelMaps(
            EmiRegistry registry, Set<ResourceLocation> addedCategories) {
        registerFuelMap(
                registry,
                addedCategories,
                ModRecipeMaps.FUELS_ENGINE,
                "fuels_engine",
                "fuel_engine");
        registerFuelMap(
                registry,
                addedCategories,
                ModRecipeMaps.FUELS_GAS,
                "fuels_gas",
                "fluid_burning_box");
        registerFuelMap(
                registry,
                addedCategories,
                ModRecipeMaps.FUELS_FLUIDBED,
                "fuels_fluidbed",
                "fluid_bed_burning_box");
    }

    private static void registerFuelMap(
            EmiRegistry registry,
            Set<ResourceLocation> addedCategories,
            RecipeMap map,
            String categoryPath,
            String runtime) {
        Block[] workstations = ModBlocks.converterBlocks(runtime);
        if (workstations.length == 0) {
            return;
        }
        EmiRecipeCategory category = internCategory(
                id(categoryPath),
                EmiStack.of(workstations[0]));
        addCategory(registry, category, addedCategories);
        for (Block block : workstations) {
            registry.addWorkstation(category, EmiStack.of(block));
        }
        for (var entry : map.entries()) {
            registry.addRecipe(new FuelMapEmiRecipe(
                    entry.id(), category, entry.recipe()));
        }
        EnergyConverterCatalog.profiles().stream()
                .filter(profile -> runtime.equals(profile.runtimeBinding()))
                .forEach(profile -> registry.addEmiStack(
                        EmiStack.of(ModItems.converterItemsById()
                                .get(profile.id())
                                .get())));
    }

    private static void registerHeatExchangerFuels(
            EmiRegistry registry, Set<ResourceLocation> addedCategories) {
        Block[] workstations = ModBlocks.heatExchangerBlockArray();
        if (workstations.length == 0) {
            return;
        }
        EmiRecipeCategory category = internCategory(
                id("fuels_hot"),
                EmiStack.of(workstations[0]));
        addCategory(registry, category, addedCategories);
        for (Block block : workstations) {
            registry.addWorkstation(category, EmiStack.of(block));
        }
        for (var entry : ModRecipeMaps.FUELS_HOT.entries()) {
            registry.addRecipe(new FuelMapEmiRecipe(
                    entry.id(), category, entry.recipe()));
        }
    }

    private static void registerProcessingMachines(
            EmiRegistry registry, Set<ResourceLocation> addedCategories) {
        ProcessingEmiRegistrationPlan plan = ProcessingEmiProjectionCache.planFor(
                ModProcessingMachines.CONFIGURED_MACHINES);
        Map<ResourceLocation, EmiRecipeCategory> categories = new HashMap<>();
        for (ProcessingEmiRegistrationPlan.MachineRegistration machine
                : plan.machines()) {
            EmiStack workstation = EmiStack.of(
                    ModBlocks.configuredProcessingBlock(machine.spec()));
            EmiRecipeCategory category = internCategory(
                    machine.categoryId(), workstation);
            categories.put(machine.spec().id(), category);
            addCategory(registry, category, addedCategories);
            registry.addWorkstation(category, workstation);
            if (machine.spec() == ModProcessingMachines.CENTRIFUGE) {
                registry.addWorkstation(
                        category,
                        EmiStack.of(ModBlocks.LARGE_CENTRIFUGE.get()));
            }
            for (var variant : ModMachineVariants.forKind(
                    machine.spec().id())) {
                if (!variant.id().equals(machine.spec().id())) {
                    registry.addWorkstation(
                            category,
                            EmiStack.of(ModBlocks
                                    .configuredProcessingBlock(variant)));
                }
            }
        }
        for (ProcessingEmiRegistrationPlan.RecipeRegistration recipe
                : plan.recipes()) {
            EmiRecipeCategory category = Objects.requireNonNull(
                    categories.get(recipe.machine().spec().id()),
                    "Missing processing EMI category");
            registry.addRecipe(new ProcessingEmiRecipe(
                    recipe.id(),
                    category,
                    recipe.machine().spec(),
                    recipe.recipe()));
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }

    static EmiRecipeCategory internCategory(
            ResourceLocation id, EmiRenderable icon) {
        return INTERNED_CATEGORIES.computeIfAbsent(
                id, key -> new CanonicalEmiRecipeCategory(key, icon));
    }

    private static void addCategory(
            EmiRegistry registry,
            EmiRecipeCategory category,
            Set<ResourceLocation> added) {
        if (added.add(category.getId())) {
            registry.addCategory(category);
        }
    }

    private static ItemStack anvilVariant(String material) {
        ItemStack stack = new ItemStack(ModItems.ANVIL.get());
        stack.set(ModComponents.MACHINE_MATERIAL, material);
        long max = MachineMaterialRules.anvilMaxDurability(material);
        stack.set(ModComponents.MACHINE_DURABILITY, new MachineDurabilityComponent(max, max));
        return stack;
    }
}
