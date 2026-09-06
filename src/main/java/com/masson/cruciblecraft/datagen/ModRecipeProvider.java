package com.masson.cruciblecraft.datagen;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.content.item.ToolPatternCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeAcquisitionRecipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverComponentTiers;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;
import com.masson.cruciblecraft.logistics.hopper.HopperKind;
import com.masson.cruciblecraft.logistics.hopper.HopperVariantCatalog;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.recipe.rule.MaterialRule;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleRecipe;
import com.masson.cruciblecraft.recipe.rule.MaterialChainRules;
import com.masson.cruciblecraft.recipe.rule.ToolRules;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeEntry;
import com.masson.cruciblecraft.energy.battery.EnergyBatteryTierCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterTierCatalog;
import com.masson.cruciblecraft.energy.transformer.EnergyTransformerTierCatalog;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.fluids.FluidStack;

/** Recipes derived mechanically from the bootstrapped material catalog. */
public final class ModRecipeProvider extends RecipeProvider {
    private static final String COMPAT_SHORTCUT_GROUP = "cruciblecraft:compat_shortcut";
    private static final float ORE_EXPERIENCE = 0.7F;
    private static final int SMELTING_TIME = 200;
    private static final int BLASTING_TIME = 100;

    public ModRecipeProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        RecipeOutput recipesOnly = new AdvancementFreeRecipeOutput(output);
        addMachineRecipes(recipesOnly);
        addHopperRecipes(recipesOnly);
        addStorageRecipes(recipesOnly);
        MaterialCatalog.startupValues().stream()
                .sorted(Comparator.comparing(MaterialDefinition::id))
                .forEach(material -> addDerivedOreRecipes(recipesOnly, material));
    }

    private static void compactElectricCoverRecipes(RecipeOutput output) {
        for (CoverComponentTiers.Family family
                : CoverComponentTiers.Family.values()) {
            CoverComponentTiers.Entry ulv = CoverComponentTiers.entries()
                    .stream()
                    .filter(entry -> entry.family() == family
                            && entry.tier() == 0)
                    .findFirst()
                    .orElseThrow();
            var ulvItem = ModItems.compactElectricCover(ulv.itemPath()).get();
            if (family == CoverComponentTiers.Family.CONVEYOR) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ulvItem)
                        .pattern("IRI")
                        .pattern("IHI")
                        .define('I', Items.IRON_INGOT)
                        .define('R', Items.POWERED_RAIL)
                        .define('H', Items.HOPPER)
                        .unlockedBy("has_hopper", has(Items.HOPPER))
                        .save(output, id(ulv.itemPath()));
            } else {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ulvItem)
                        .pattern("IRI")
                        .pattern("IPI")
                        .define('I', Items.IRON_INGOT)
                        .define('R', Items.REDSTONE)
                        .define('P', Items.PISTON)
                        .unlockedBy("has_piston", has(Items.PISTON))
                        .save(output, id(ulv.itemPath()));
            }
            for (int tier = 1;
                    tier < CoverComponentTiers.TIER_COUNT;
                    tier++) {
                CoverComponentTiers.Entry previous =
                        CoverComponentTiers.entries().get(
                                family.ordinal()
                                        * CoverComponentTiers.TIER_COUNT
                                        + tier
                                        - 1);
                CoverComponentTiers.Entry next =
                        CoverComponentTiers.entries().get(
                                family.ordinal()
                                        * CoverComponentTiers.TIER_COUNT
                                        + tier);
                ShapelessRecipeBuilder.shapeless(
                                RecipeCategory.MISC,
                                ModItems.compactElectricCover(
                                        next.itemPath()).get())
                        .requires(ModItems.compactElectricCover(
                                previous.itemPath()).get())
                        .requires(ModItems.PROGRAMMED_CIRCUIT.get())
                        .unlockedBy(
                                "has_programmed_circuit",
                                has(ModItems.PROGRAMMED_CIRCUIT.get()))
                        .save(output, id(next.itemPath()));
            }
        }
    }

    private static void addHopperRecipes(RecipeOutput output) {
        HopperVariantCatalog.variants().forEach(variant -> {
            Item result = ModItems.hopperItemsById().get(variant.id()).get();
            Item plate = materialItem(
                    variant.materialPath(), MaterialPrefixes.PLATE);
            ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(
                            RecipeCategory.MISC, result)
                    .define('P', plate)
                    .define('C', Items.CHEST)
                    .unlockedBy("has_plate", has(plate))
                    .unlockedBy("has_chest", has(Items.CHEST));
            if (variant.kind() == HopperKind.HOPPER) {
                builder.pattern("P P").pattern("PCP").pattern(" P ");
            } else {
                builder.pattern("PPP").pattern("C C").pattern("P P");
            }
            builder.save(output, id("hoppers/" + variant.id().getPath()));
        });
        Item ironPlate = materialItem("iron", MaterialPrefixes.PLATE);
        Item ironRod = materialItem("iron", MaterialPrefixes.ROD);
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC, ModItems.STEEL_DUST_FUNNEL.get())
                .pattern(" P ")
                .pattern(" H ")
                .pattern(" R ")
                .define('P', ironPlate)
                .define('H', Items.HOPPER)
                .define('R', ironRod)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .unlockedBy("has_plate", has(ironPlate))
                .save(output, id("hoppers/steel_dust_funnel"));
    }

    private static void addDisplayCpuRecipes(RecipeOutput output) {
        Item circuit = ModItems.PROGRAMMED_CIRCUIT.get();
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_DISPLAY_CPU_LOGIC_COVER.get())
                .pattern("TL ")
                .pattern(" Q ")
                .pattern(" C ")
                .define('T', Items.REDSTONE_TORCH)
                .define('L', Items.REDSTONE)
                .define('Q', Items.IRON_TRAPDOOR)
                .define('C', circuit)
                .unlockedBy("has_programmed_circuit", has(circuit))
                .save(output, id("logistics_display_cpu_logic_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_DISPLAY_CPU_CONTROL_COVER.get())
                .pattern(" LT")
                .pattern(" Q ")
                .pattern(" C ")
                .define('T', Items.REDSTONE_TORCH)
                .define('L', Items.REDSTONE)
                .define('Q', Items.IRON_TRAPDOOR)
                .define('C', circuit)
                .unlockedBy("has_programmed_circuit", has(circuit))
                .save(output, id("logistics_display_cpu_control_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_DISPLAY_CPU_STORAGE_COVER.get())
                .pattern(" L ")
                .pattern(" Q ")
                .pattern("TC ")
                .define('T', Items.REDSTONE_TORCH)
                .define('L', Items.REDSTONE)
                .define('Q', Items.IRON_TRAPDOOR)
                .define('C', circuit)
                .unlockedBy("has_programmed_circuit", has(circuit))
                .save(output, id("logistics_display_cpu_storage_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_DISPLAY_CPU_CONVERSION_COVER.get())
                .pattern(" L ")
                .pattern(" Q ")
                .pattern(" CT")
                .define('T', Items.REDSTONE_TORCH)
                .define('L', Items.REDSTONE)
                .define('Q', Items.IRON_TRAPDOOR)
                .define('C', circuit)
                .unlockedBy("has_programmed_circuit", has(circuit))
                .save(output, id("logistics_display_cpu_conversion_cover"));
        ShapelessRecipeBuilder.shapeless(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_DISPLAY_CPU_LOGIC_COVER.get())
                .requires(ModItems.LOGISTICS_DISPLAY_CPU_CONVERSION_COVER.get())
                .unlockedBy(
                        "has_display_cpu_conversion",
                        has(ModItems.LOGISTICS_DISPLAY_CPU_CONVERSION_COVER.get()))
                .save(output, id("logistics_display_cpu_logic_cycle_cover"));
        ShapelessRecipeBuilder.shapeless(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_DISPLAY_CPU_CONTROL_COVER.get())
                .requires(ModItems.LOGISTICS_DISPLAY_CPU_LOGIC_COVER.get())
                .unlockedBy(
                        "has_display_cpu_logic",
                        has(ModItems.LOGISTICS_DISPLAY_CPU_LOGIC_COVER.get()))
                .save(output, id("logistics_display_cpu_control_cycle_cover"));
        ShapelessRecipeBuilder.shapeless(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_DISPLAY_CPU_STORAGE_COVER.get())
                .requires(ModItems.LOGISTICS_DISPLAY_CPU_CONTROL_COVER.get())
                .unlockedBy(
                        "has_display_cpu_control",
                        has(ModItems.LOGISTICS_DISPLAY_CPU_CONTROL_COVER.get()))
                .save(output, id("logistics_display_cpu_storage_cycle_cover"));
        ShapelessRecipeBuilder.shapeless(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_DISPLAY_CPU_CONVERSION_COVER.get())
                .requires(ModItems.LOGISTICS_DISPLAY_CPU_STORAGE_COVER.get())
                .unlockedBy(
                        "has_display_cpu_storage",
                        has(ModItems.LOGISTICS_DISPLAY_CPU_STORAGE_COVER.get()))
                .save(output, id("logistics_display_cpu_conversion_cycle_cover"));
    }

    private static void addStorageRecipes(RecipeOutput output) {
        Item steelPlate = materialItem("steel", MaterialPrefixes.PLATE);
        Item plasticPlate = materialItem("plastic", MaterialPrefixes.PLATE);
        Item treatedRod = materialItem("wood_treated", MaterialPrefixes.ROD);
        StorageVariantCatalog.sourceVisible().forEach(variant -> {
            Item result = ModItems.storageItemsById().get(variant.id()).get();
            String recipeId = "storage/" + variant.path();
            switch (variant.acquisitionProfile()) {
                case "steel_shaped" -> ShapedRecipeBuilder.shaped(
                                RecipeCategory.MISC, result)
                        .pattern("PPP")
                        .pattern("PCP")
                        .pattern("PPP")
                        .define('P', steelPlate)
                        .define('C', Items.CHEST)
                        .unlockedBy("has_steel_plate", has(steelPlate))
                        .save(output, id(recipeId));
                case "oak_planks", "cheap_barrel" -> ShapedRecipeBuilder.shaped(
                                RecipeCategory.MISC, result)
                        .pattern("PPP")
                        .pattern("PCP")
                        .pattern("PPP")
                        .define('P', Items.OAK_PLANKS)
                        .define('C', Items.CHEST)
                        .unlockedBy("has_oak_planks", has(Items.OAK_PLANKS))
                        .save(output, id(recipeId));
                case "treated_barrel" -> ShapedRecipeBuilder.shaped(
                                RecipeCategory.MISC, result)
                        .pattern("PPP")
                        .pattern("PCP")
                        .pattern("PPP")
                        .define('P', treatedRod)
                        .define('C', Items.CHEST)
                        .unlockedBy("has_treated_rod", has(treatedRod))
                        .save(output, id(recipeId));
                case "plastic_box" -> ShapedRecipeBuilder.shaped(
                                RecipeCategory.MISC, result)
                        .pattern("PPP")
                        .pattern("PCP")
                        .pattern("PPP")
                        .define('P', plasticPlate)
                        .define('C', Items.CHEST)
                        .unlockedBy("has_plastic_plate", has(plasticPlate))
                        .save(output, id(recipeId));
                case "charging_locker" -> ShapedRecipeBuilder.shaped(
                                RecipeCategory.MISC, result)
                        .pattern("PPP")
                        .pattern("PCP")
                        .pattern("PRP")
                        .define('P', steelPlate)
                        .define('C', Items.CHEST)
                        .define('R', ModItems.PROGRAMMED_CIRCUIT.get())
                        .unlockedBy("has_circuit", has(ModItems.PROGRAMMED_CIRCUIT.get()))
                        .save(output, id(recipeId));
                case "inserter" -> ShapedRecipeBuilder.shaped(
                                RecipeCategory.MISC, result)
                        .pattern(" P ")
                        .pattern(" H ")
                        .pattern(" P ")
                        .define('P', steelPlate)
                        .define('H', Items.HOPPER)
                        .unlockedBy("has_hopper", has(Items.HOPPER))
                        .save(output, id(recipeId));
                default -> throw new IllegalStateException(
                        "Visible storage row missing acquisition recipe: "
                                + variant.id());
            }
        });
    }

    private static void addMachineRecipes(RecipeOutput output) {
        machineCrafting(
                output, ModItems.ROTATIONAL_AXLE.get(), "rotational_axle");
        machineCrafting(
                output,
                ModItems.ROTATIONAL_GEARBOX.get(),
                "rotational_gearbox");
        addConverterRecipes(output);
        addBatteryRecipes(output);
        addTransformerRecipes(output);
        for (MachineVariant variant
                : com.masson.cruciblecraft.registry.ModMachineVariants.ALL) {
            emitAcquisition(output, variant);
        }
        machineCrafting(
                output,
                ModItems.FLUID_DEPOSIT_EXTRACTOR.get(),
                "fluid_deposit_extractor");
        addCasingFormRecipes(output);
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.MULTIBLOCK_CASING.get(),
                        4)
                .pattern("III")
                .pattern("ICI")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(output, id("machines/multiblock_casing"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.MULTIBLOCK_ITEM_FLUID_PORT.get())
                .pattern(" C ")
                .pattern("HBH")
                .pattern(" C ")
                .define('C', ModItems.MULTIBLOCK_CASING.get())
                .define('H', Items.HOPPER)
                .define('B', Items.BUCKET)
                .unlockedBy(
                        "has_multiblock_casing",
                        has(ModItems.MULTIBLOCK_CASING.get()))
                .save(output, id("machines/multiblock_item_fluid_port"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.MULTIBLOCK_ENERGY_INPUT_PORT.get())
                .pattern(" R ")
                .pattern("RCR")
                .pattern(" R ")
                .define('C', ModItems.MULTIBLOCK_CASING.get())
                .define('R', Items.REDSTONE)
                .unlockedBy(
                        "has_multiblock_casing",
                        has(ModItems.MULTIBLOCK_CASING.get()))
                .save(output, id("machines/multiblock_energy_input_port"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.LARGE_CENTRIFUGE.get())
                .pattern("CCC")
                .pattern("CMC")
                .pattern("CCC")
                .define('C', ModItems.MULTIBLOCK_CASING.get())
                .define('M', ModItems.CENTRIFUGE.get())
                .unlockedBy(
                        "has_centrifuge",
                        has(ModItems.CENTRIFUGE.get()))
                .save(output, id("machines/large_centrifuge"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC, ModItems.PORTABLE_FLUID_TANK.get())
                .pattern("CGC")
                .pattern("G G")
                .pattern("CGC")
                .define('C', Items.COPPER_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output, id("portable_fluid_tank"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.FLUID_CELL.get(),
                        4)
                .pattern(" C ")
                .pattern("CGC")
                .pattern(" C ")
                .define('C', Items.COPPER_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output, id("fluid_cell"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.GAS_CELL.get(),
                        4)
                .pattern(" I ")
                .pattern("IGI")
                .pattern(" I ")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(output, id("gas_cell"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.PIPE_FILTER_COVER.get())
                .pattern(" G ")
                .pattern("GHG")
                .define('G', Items.GLASS_PANE)
                .define('H', Items.HOPPER)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output, id("pipe_filter_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.PIPE_VALVE_COVER.get())
                .pattern(" I ")
                .pattern("IRI")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REPEATER)
                .unlockedBy("has_repeater", has(Items.REPEATER))
                .save(output, id("pipe_valve_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.PIPE_PUMP_COVER.get())
                .pattern("IRI")
                .pattern("IPI")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('P', Items.PISTON)
                .unlockedBy("has_piston", has(Items.PISTON))
                .save(output, id("pipe_pump_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.CONVEYOR_COVER.get())
                .pattern("IRI")
                .pattern("IHI")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.POWERED_RAIL)
                .define('H', Items.HOPPER)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output, id("conveyor_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.RETRIEVER_ITEM_COVER.get())
                .pattern(" R ")
                .pattern("CHC")
                .define('R', Items.REDSTONE)
                .define('C', Items.COMPASS)
                .define('H', Items.HOPPER)
                .unlockedBy("has_compass", has(Items.COMPASS))
                .save(output, id("retriever_item_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.ROBOT_ARM_COVER.get())
                .pattern("IRI")
                .pattern("IPI")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('P', Items.PISTON)
                .unlockedBy("has_piston", has(Items.PISTON))
                .save(output, id("robot_arm_cover"));
        compactElectricCoverRecipes(output);
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.PRESSURE_VALVE_COVER.get())
                .pattern(" C ")
                .pattern("CTC")
                .define('C', Items.COPPER_INGOT)
                .define('T', Items.TRIPWIRE_HOOK)
                .unlockedBy(
                        "has_tripwire_hook",
                        has(Items.TRIPWIRE_HOOK))
                .save(output, id("pressure_valve_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.SELECTOR_MANUAL_COVER.get())
                .pattern(" R ")
                .pattern("LCL")
                .define('R', Items.REDSTONE)
                .define('L', Items.LEVER)
                .define('C', Items.COMPARATOR)
                .unlockedBy("has_comparator", has(Items.COMPARATOR))
                .save(output, id("selector_manual_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_ITEM_STORAGE_COVER.get())
                .pattern(" I ")
                .pattern("CHC")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CHEST)
                .define('H', Items.HOPPER)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output, id("logistics_item_storage_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_ITEM_IMPORT_COVER.get())
                .pattern(" I ")
                .pattern("CHC")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COMPARATOR)
                .define('H', Items.HOPPER)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output, id("logistics_item_import_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_ITEM_EXPORT_COVER.get())
                .pattern(" I ")
                .pattern("DHD")
                .define('I', Items.IRON_INGOT)
                .define('D', Items.DROPPER)
                .define('H', Items.HOPPER)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output, id("logistics_item_export_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_FLUID_STORAGE_COVER.get())
                .pattern(" I ")
                .pattern("BHB")
                .define('I', Items.IRON_INGOT)
                .define('B', Items.BUCKET)
                .define('H', Items.HOPPER)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output, id("logistics_fluid_storage_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_FLUID_IMPORT_COVER.get())
                .pattern(" I ")
                .pattern("CHC")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CAULDRON)
                .define('H', Items.HOPPER)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output, id("logistics_fluid_import_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_FLUID_EXPORT_COVER.get())
                .pattern(" I ")
                .pattern("DHD")
                .define('I', Items.IRON_INGOT)
                .define('D', Items.DISPENSER)
                .define('H', Items.HOPPER)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output, id("logistics_fluid_export_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_GENERIC_STORAGE_COVER.get())
                .pattern(" I ")
                .pattern("CHB")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CHEST)
                .define('H', Items.HOPPER)
                .define('B', Items.BUCKET)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output, id("logistics_generic_storage_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_GENERIC_IMPORT_COVER.get())
                .pattern(" I ")
                .pattern("CHB")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COMPARATOR)
                .define('H', Items.HOPPER)
                .define('B', Items.BUCKET)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output, id("logistics_generic_import_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_GENERIC_EXPORT_COVER.get())
                .pattern(" I ")
                .pattern("DHB")
                .define('I', Items.IRON_INGOT)
                .define('D', Items.DROPPER)
                .define('H', Items.HOPPER)
                .define('B', Items.BUCKET)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output, id("logistics_generic_export_cover"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.LOGISTICS_GENERIC_DUMP_COVER.get())
                .pattern(" H ")
                .pattern("HCH")
                .pattern(" D ")
                .define('H', Items.HOPPER)
                .define('C', Items.CHEST)
                .define('D', Items.DROPPER)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output, id("logistics_generic_dump_cover"));
        addDisplayCpuRecipes(output);
        Item galvanizedPlate = materialItem(
                "steel_galvanized", MaterialPrefixes.PLATE);
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC,
                        ModItems.GALVANIZED_STEEL_WALL.get())
                .pattern("PP")
                .pattern("PP")
                .define('P', galvanizedPlate)
                .unlockedBy("has_plate", has(galvanizedPlate))
                .save(output, id("galvanized_steel_wall"));
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC, ModItems.LOGISTICS_CORE.get())
                .pattern("CCC")
                .pattern("PMP")
                .pattern("CCC")
                .define('C', ModItems.STEEL_GALVANIZED_MACHINE_CASING.get())
                .define('P', ModItems.PROGRAMMED_CIRCUIT.get())
                .define('M', Items.EMERALD)
                .unlockedBy(
                        "has_casing",
                        has(ModItems.STEEL_GALVANIZED_MACHINE_CASING.get()))
                .save(output, id("logistics_core"));
        addProcessorUnitRecipe(
                output,
                ModItems.VENTILATION_UNIT.get(),
                "ventilation_unit",
                Items.IRON_BARS);
        addProcessorUnitRecipe(
                output,
                ModItems.VERSATILE_PROCESSOR_UNIT.get(),
                "versatile_processor_unit",
                Items.EMERALD);
        addProcessorUnitRecipe(
                output,
                ModItems.LOGIC_PROCESSOR_UNIT.get(),
                "logic_processor_unit",
                Items.LAPIS_LAZULI);
        addProcessorUnitRecipe(
                output,
                ModItems.CONTROL_PROCESSOR_UNIT.get(),
                "control_processor_unit",
                Items.REDSTONE);
        addProcessorUnitRecipe(
                output,
                ModItems.STORAGE_PROCESSOR_UNIT.get(),
                "storage_processor_unit",
                Items.CHEST);
        addProcessorUnitRecipe(
                output,
                ModItems.CONVERSION_PROCESSOR_UNIT.get(),
                "conversion_processor_unit",
                Items.GOLD_INGOT);
        addNonmetalPipeAcquisitionRecipes(output);
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.TOOLS, ModItems.MATERIAL_FILE.get())
                .pattern(" II")
                .pattern(" SI")
                .pattern("S  ")
                .define('I', Items.IRON_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(output, id("tools/iron_file"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.FLINT_KNIFE.get())
                .pattern("SF")
                .define('S', Items.STICK)
                .define('F', Items.FLINT)
                .unlockedBy("has_flint", has(Items.FLINT))
                .save(output, id("tools/flint_knife"));
        ToolPatternCatalog.DEFINITIONS.forEach(pattern -> {
            ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(
                            RecipeCategory.TOOLS,
                            ModItems.toolPattern(pattern.id()).get())
                    .define('P', Items.PAPER)
                    .define('C', Items.CHARCOAL)
                    .unlockedBy("has_paper", has(Items.PAPER));
            if (pattern.recipePattern().stream()
                    .anyMatch(row -> row.contains("S"))) {
                builder.define('S', Items.STICK);
            }
            pattern.recipePattern().forEach(builder::pattern);
            builder.save(output, id("tools/pattern/" + pattern.id()));
        });
        ToolRules.ALL.forEach(definition -> output.accept(
                id(definition.path()),
                new MaterialRuleRecipe(definition.rule()),
                null));
        output.accept(
                id("coke_oven/coal"),
                new GTRecipeEntry(
                        ModRecipeMaps.COKE_OVEN.id(),
                        new GTRecipe(
                                List.of(Ingredient.of(Items.COAL)),
                                List.of(1),
                                List.of(new ItemStack(materialItem(
                                        "coal_coke",
                                        MaterialPrefixes.GEM))),
                                List.of(),
                                List.of(new FluidStack(ModFluids.CREOSOTE_SOURCE.get(), 500)),
                                List.of(GTRecipe.GUARANTEED_CHANCE),
                                3_600,
                                0L,
                                0L)),
                null);
        output.accept(
                id("anvil/ingot_to_plate"),
                materialRule(ModRecipeMaps.ANVIL.id(), MaterialPrefixes.INGOT,
                        MaterialPrefixes.PLATE, 1, 1, 1, 10_000, 4, Map.of()),
                null);
        output.accept(
                id("anvil/plate_to_rods"),
                materialRule(ModRecipeMaps.ANVIL.id(), MaterialPrefixes.PLATE,
                        MaterialPrefixes.ROD, 1, 2, 1, 10_000, 5, Map.of()),
                null);
        output.accept(
                id("anvil/rod_to_bolts"),
                materialRule(ModRecipeMaps.ANVIL.id(), MaterialPrefixes.ROD,
                        MaterialPrefixes.BOLT, 1, 4, 1, 10_000, 3, Map.of()),
                null);
        output.accept(
                id("anvil/raw_ore_to_crushed_ore"),
                new MaterialRuleRecipe(MaterialChainRules.ANVIL_RAW_TO_CRUSHED),
                null);
        MaterialChainRules.ALL.stream()
                .filter(definition ->
                        !MaterialChainRules.CONCRETE_ORE_CHAIN_PATHS.contains(definition.path()))
                .forEach(definition -> output.accept(
                        id(definition.path()), new MaterialRuleRecipe(definition.rule()), null));
    }

    private static void emitAcquisition(
            RecipeOutput output,
            MachineVariant variant) {
        var resolved = com.masson.cruciblecraft.machine.processing.MachineAcquisition.resolve(variant);
        Item result = ModItems.tieredProcessingItemsById()
                .get(variant.id())
                .get();
        String path = variant.id().getPath();
        String material = resolved.materialPath();
        Item casing = resolved.casingItem() == null
                ? null
                : resolveRegisteredItem(resolved.casingItem());
        switch (resolved.template()) {
            case "machine_generic" -> machineCrafting(output, result, path);
            case "centrifuge" -> centrifugeCrafting(
                    output, result, casing, material, path);
            case "sifter" -> sifterCrafting(
                    output, result, casing, material, path);
            case "kinetic" -> kineticMachineCrafting(
                    output, result, casing, material, resolved.kineticKind(), path);
            case "heat" -> heatMachineCrafting(
                    output,
                    result,
                    casing,
                    material,
                    resolved.distilleryWire() == null
                            ? null
                            : resolved.distilleryWire().wireMaterial(),
                    resolved.distilleryWire() == null
                            ? null
                            : wirePrefix(resolved.distilleryWire().wirePrefix()),
                    variant.kind().id().getPath(),
                    path);
            case "electrolyzer" -> electrolyzerCrafting(
                    output,
                    result,
                    casing,
                    resolved.electrolyzerCableMaterial(),
                    path);
            case "compressor" -> compressorCrafting(
                    output, result, casing, material, path);
            default -> throw new IllegalStateException(
                    "Machine variant " + path + " has no source-backed acquisition");
        }
    }

    private static MaterialPrefix wirePrefix(String serialized) {
        return switch (serialized) {
            case "double_wire" -> MaterialPrefixes.DOUBLE_WIRE;
            case "quadruple_wire" -> MaterialPrefixes.QUADRUPLE_WIRE;
            case "octuple_wire" -> MaterialPrefixes.OCTUPLE_WIRE;
            default -> throw new IllegalStateException(
                    "Unknown distillery wire prefix " + serialized);
        };
    }

    private static void compressorCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String material,
            String id) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("PSP")
                .pattern("PCP")
                .pattern(" R ")
                .define(
                        'P',
                        materialItem(
                                material, MaterialPrefixes.QUINTUPLE_PLATE))
                .define(
                        'S',
                        materialItem(material, MaterialPrefixes.SPRING))
                .define(
                        'R',
                        materialItem(material, MaterialPrefixes.ROD))
                .define('C', casing)
                .unlockedBy("has_casing", has(casing))
                .save(output, id("machines/" + id));
    }

    private static void centrifugeCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String material,
            String id) {
        Item gear = materialItem(material, MaterialPrefixes.GEAR);
        Item longRod = materialItem(
                material, MaterialPrefixes.LONG_ROD);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("G ")
                .pattern("SC")
                .pattern("G ")
                .define('G', gear)
                .define('S', longRod)
                .define('C', casing)
                .unlockedBy("has_casing", has(casing))
                .save(output, id("machines/" + id));
    }

    private static void addNonmetalPipeAcquisitionRecipes(
            RecipeOutput output) {
        for (PipeAcquisitionRecipeCatalog.RecipeSpec spec
                : PipeAcquisitionRecipeCatalog.ALL) {
            Item result = materialItem(spec.materialId(), spec.output());
            ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(
                    RecipeCategory.MISC, result, spec.outputCount());
            spec.pattern().forEach(builder::pattern);
            Item unlock = null;
            for (var entry : spec.operands().entrySet()) {
                Item candidate = definePipeOperand(
                        builder, entry.getKey(), entry.getValue());
                if (unlock == null) {
                    unlock = candidate;
                }
            }
            builder.unlockedBy(
                            "has_pipe_operand",
                            has(java.util.Objects.requireNonNull(unlock)))
                    .save(output, spec.id());
        }
    }

    private static Item definePipeOperand(
            ShapedRecipeBuilder builder,
            char symbol,
            PipeAcquisitionRecipeCatalog.Operand operand) {
        return switch (operand) {
            case WOODEN_SLABS -> {
                builder.define(symbol, ItemTags.WOODEN_SLABS);
                yield Items.OAK_SLAB;
            }
            case PLANKS -> {
                builder.define(symbol, ItemTags.PLANKS);
                yield Items.OAK_PLANKS;
            }
            case LOGS -> {
                builder.define(symbol, ItemTags.LOGS);
                yield Items.OAK_LOG;
            }
            case CARBON_DUST -> {
                Item item = materialItem("carbon", MaterialPrefixes.DUST);
                builder.define(symbol, item);
                yield item;
            }
            case PLASTIC_PLATE -> {
                Item item = materialItem("plastic", MaterialPrefixes.PLATE);
                builder.define(symbol, item);
                yield item;
            }
            case RUBBER_PLATE -> {
                Item item = materialItem("rubber", MaterialPrefixes.PLATE);
                builder.define(symbol, item);
                yield item;
            }
            case COAL_COKE -> {
                Item item = materialItem("coal_coke", MaterialPrefixes.GEM);
                builder.define(symbol, item);
                yield item;
            }
        };
    }

    private static void sifterCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String material,
            String id) {
        Item wire = materialItem(
                material, MaterialPrefixes.FINE_WIRE);
        Item rod = materialItem(material, MaterialPrefixes.ROD);
        Item spring = materialItem(
                material, MaterialPrefixes.SPRING);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("W W")
                .pattern("RCR")
                .pattern("S S")
                .define('W', wire)
                .define('R', rod)
                .define('S', spring)
                .define('C', casing)
                .unlockedBy("has_casing", has(casing))
                .save(output, id("machines/" + id));
    }

    private static void electrolyzerCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String cableMaterial,
            String id) {
        Item platinumWire = materialItem(
                "platinum", MaterialPrefixes.WIRE);
        Item cable = materialItem(
                cableMaterial, MaterialPrefixes.CABLE);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("SMS")
                .pattern("W W")
                .define('S', platinumWire)
                .define('M', casing)
                .define('W', cable)
                .unlockedBy("has_casing", has(casing))
                .save(output, id("machines/" + id));
    }

    private static void kineticMachineCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String material,
            String kind,
            String id) {
        Item gear = materialItem(material, MaterialPrefixes.GEAR);
        ShapedRecipeBuilder builder =
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                        .define('C', casing)
                        .unlockedBy("has_casing", has(casing));
        if (!kind.equals("press")) {
            builder.define('G', gear);
        }
        switch (kind) {
            case "lathe" -> builder
                    .pattern("TDS")
                    .pattern(" CG")
                    .define(
                            'T',
                            materialItem(
                                    material,
                                    MaterialPrefixes.SCREW))
                    .define(
                            'D',
                            materialItem(
                                    "diamond",
                                    MaterialPrefixes.GEM))
                    .define(
                            'S',
                            materialItem(
                                    material,
                                    MaterialPrefixes.SMALL_GEAR));
            case "rollingmill", "rollbender" -> builder
                    .pattern("G ")
                    .pattern("C ")
                    .pattern("G ");
            case "wiremill" -> builder
                    .pattern("SGS")
                    .pattern(" C ")
                    .define(
                            'S',
                            materialItem(
                                    material,
                                    MaterialPrefixes.SMALL_GEAR));
            case "shredder", "cutter" -> builder
                    .pattern("GDG")
                    .pattern(" C ")
                    .define(
                            'D',
                            materialItem(
                                    "diamond",
                                    MaterialPrefixes.GEM));
            case "press" -> builder
                    .pattern("RS")
                    .pattern("PC")
                    .pattern("P ")
                    .define(
                            'P',
                            materialItem(
                                    material,
                                    MaterialPrefixes.DOUBLE_PLATE))
                    .define(
                            'R',
                            materialItem(
                                    material,
                                    MaterialPrefixes.ROD))
                    .define(
                            'S',
                            materialItem(
                                    material,
                                    MaterialPrefixes.SPRING));
            default -> throw new IllegalArgumentException(
                    "Unsupported kinetic machine kind " + kind);
        }
        builder.save(output, id("machines/" + id));
    }

    private static void heatMachineCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String machineMaterial,
            String wireMaterial,
            MaterialPrefix wirePrefix,
            String kind,
            String id) {
        Item tierPlate = materialItem(
                machineMaterial, MaterialPrefixes.PLATE);
        Item copperDoublePlate = materialItem(
                "copper", MaterialPrefixes.DOUBLE_PLATE);
        ShapedRecipeBuilder builder =
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                        .define('M', casing)
                        .define('P', tierPlate)
                        .define('C', copperDoublePlate)
                        .unlockedBy("has_casing", has(casing));
        switch (kind) {
            case "distillery" -> builder
                    .pattern("GPG")
                    .pattern("WMW")
                    .pattern(" C ")
                    .define('G', Items.GLASS)
                    .define(
                            'W',
                            materialItem(
                                    java.util.Objects.requireNonNull(
                                            wireMaterial,
                                            "distillery wire material"),
                                    java.util.Objects.requireNonNull(
                                            wirePrefix,
                                            "distillery wire prefix")));
            case "drying" -> builder
                    .pattern(" P ")
                    .pattern("BMB")
                    .pattern("BCB")
                    .define('B', Items.BRICKS);
            case "smelter" -> builder
                    .pattern(" U ")
                    .pattern("PMP")
                    .pattern("BCB")
                    .define('U', ModItems.CRUCIBLE.get())
                    .define('B', Items.BRICKS);
            case "roaster" -> builder
                    .pattern(" P ")
                    .pattern("PMP")
                    .pattern("BCB")
                    .define('B', Items.BRICKS);
            case "extruder" -> builder
                    .pattern("PPP")
                    .pattern("WMW")
                    .pattern(" C ")
                    .define(
                            'W',
                            materialItem(
                                    machineMaterial,
                                    MaterialPrefixes.ROD));
            default -> throw new IllegalArgumentException(
                    "Unsupported heat machine kind " + kind);
        }
        builder.save(output, id("machines/" + id));
    }

    private static void addCasingFormRecipes(RecipeOutput output) {
        MaterialCatalog.startupValues().stream()
                .map(MaterialDefinition::id)
                .sorted()
                .forEach(material -> {
                    emitCasingFormRecipe(
                            output,
                            material,
                            MaterialPrefixes.MACHINE_CASING,
                            MaterialPrefixes.PLATE);
                    emitCasingFormRecipe(
                            output,
                            material,
                            MaterialPrefixes.MACHINE_CASING_DOUBLE,
                            MaterialPrefixes.DOUBLE_PLATE);
                });
    }

    private static void emitCasingFormRecipe(
            RecipeOutput output,
            String material,
            MaterialPrefix casingForm,
            MaterialPrefix plateForm) {
        if (!ModItems.hasMaterialItem(material, casingForm)) {
            return;
        }
        Item plate = MaterialLookup.item(material, plateForm).orElse(null);
        Item rod = MaterialLookup.item(material, MaterialPrefixes.LONG_ROD)
                .orElse(null);
        if (plate == null || rod == null) {
            return;
        }
        Item result = ModItems.materialItem(material, casingForm).get();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("YXX")
                .pattern("X X")
                .pattern("XXY")
                .define('X', plate)
                .define('Y', rod)
                .unlockedBy("has_plate", has(plate))
                .save(output, id("components/" + material + "/"
                        + casingForm.serializedName()));
    }

    private static Item resolveRegisteredItem(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            throw new IllegalStateException("Missing registered item " + id);
        }
        return item;
    }

    private static void addProcessorUnitRecipe(
            RecipeOutput output, Item result, String path, Item gem) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern(" C ")
                .pattern("GPG")
                .pattern(" C ")
                .define('C', ModItems.STEEL_GALVANIZED_MACHINE_CASING.get())
                .define('G', gem)
                .define('P', ModItems.PROGRAMMED_CIRCUIT.get())
                .unlockedBy(
                        "has_casing",
                        has(ModItems.STEEL_GALVANIZED_MACHINE_CASING.get()))
                .save(output, id(path));
    }

    private static Item materialItem(
            String material, MaterialPrefix prefix) {
        return MaterialLookup.item(material, prefix)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing source machine component "
                                + material
                                + "/"
                                + prefix.serializedName()));
    }

    private static final java.util.Set<String> REQUIRED_CONVERTER_RECIPES =
            java.util.Set.of(
                    "bronze_burning_box_solid",
                    "bronze_burning_box_gas",
                    "bronze_boiler",
                    "bronze_steam_engine",
                    "bronze_fuel_engine",
                    "bronze_dynamo",
                    "steel_galvanized_electric_motor");

    private static void addConverterRecipes(RecipeOutput output) {
        for (var entry : EnergyConverterTierCatalog.entries()) {
            emitConverterRecipe(output, entry);
        }
    }

    private static void emitConverterRecipe(
            RecipeOutput output,
            EnergyConverterTierCatalog.Entry entry) {
        String path = entry.id().getPath();
        boolean required = REQUIRED_CONVERTER_RECIPES.contains(path);
        Item result = ModItems.converterItemsById().get(entry.id()).get();
        java.util.LinkedHashMap<Character, Item> keys = new java.util.LinkedHashMap<>();
        for (var key : entry.recipe().keys().entrySet()) {
            Item item = resolveConverterIngredient(entry, key.getValue());
            if (item == null) {
                if (required) {
                    throw new IllegalStateException(
                            "Missing converter recipe ingredient for "
                                    + path
                                    + " key "
                                    + key.getKey());
                }
                return;
            }
            keys.put(key.getKey().charAt(0), item);
        }
        java.util.HashSet<Character> used = new java.util.HashSet<>();
        for (String row : entry.recipe().pattern()) {
            for (int index = 0; index < row.length(); index++) {
                char letter = row.charAt(index);
                if (letter != ' ') {
                    used.add(letter);
                }
            }
        }
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(
                RecipeCategory.MISC, result);
        for (String row : entry.recipe().pattern()) {
            builder.pattern(row);
        }
        Item unlock = null;
        for (var key : keys.entrySet()) {
            if (!used.contains(key.getKey())) {
                continue;
            }
            builder.define(key.getKey(), key.getValue());
            if (unlock == null) {
                unlock = key.getValue();
            }
        }
        builder.unlockedBy("has_part", has(unlock)).save(output, id(path));
    }

    private static void addBatteryRecipes(RecipeOutput output) {
        for (var entry : EnergyBatteryTierCatalog.entries()) {
            emitBatteryRecipe(output, entry);
        }
    }

    private static void emitBatteryRecipe(
            RecipeOutput output,
            EnergyBatteryTierCatalog.Entry entry) {
        String path = entry.id().getPath();
        Item result = ModItems.batteryItemsById().get(entry.id()).get();
        java.util.LinkedHashMap<Character, Item> keys = new java.util.LinkedHashMap<>();
        for (var key : entry.recipe().keys().entrySet()) {
            Item item = resolveBatteryIngredient(key.getValue());
            if (item == null) {
                throw new IllegalStateException(
                        "Missing battery recipe ingredient for "
                                + path
                                + " key "
                                + key.getKey());
            }
            keys.put(key.getKey().charAt(0), item);
        }
        java.util.HashSet<Character> used = new java.util.HashSet<>();
        for (String row : entry.recipe().pattern()) {
            for (int index = 0; index < row.length(); index++) {
                char letter = row.charAt(index);
                if (letter != ' ') {
                    used.add(letter);
                }
            }
        }
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(
                RecipeCategory.MISC, result);
        for (String row : entry.recipe().pattern()) {
            builder.pattern(row);
        }
        Item unlock = null;
        for (var key : keys.entrySet()) {
            if (!used.contains(key.getKey())) {
                continue;
            }
            builder.define(key.getKey(), key.getValue());
            if (unlock == null) {
                unlock = key.getValue();
            }
        }
        builder.unlockedBy("has_part", has(unlock)).save(output, id(path));
    }

    private static void addTransformerRecipes(RecipeOutput output) {
        for (var entry : EnergyTransformerTierCatalog.entries()) {
            emitTransformerRecipe(output, entry);
        }
    }

    private static void emitTransformerRecipe(
            RecipeOutput output,
            EnergyTransformerTierCatalog.Entry entry) {
        String path = entry.id().getPath();
        Item result = ModItems.transformerItemsById().get(entry.id()).get();
        java.util.LinkedHashMap<Character, Item> keys =
                new java.util.LinkedHashMap<>();
        for (var key : entry.recipe().keys().entrySet()) {
            Item item = resolveTransformerIngredient(key.getValue());
            if (item == null) {
                return;
            }
            keys.put(key.getKey().charAt(0), item);
        }
        java.util.HashSet<Character> used = new java.util.HashSet<>();
        for (String row : entry.recipe().pattern()) {
            for (int index = 0; index < row.length(); index++) {
                char letter = row.charAt(index);
                if (letter != ' ') {
                    used.add(letter);
                }
            }
        }
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(
                RecipeCategory.MISC, result);
        for (String row : entry.recipe().pattern()) {
            builder.pattern(row);
        }
        Item unlock = null;
        for (var key : keys.entrySet()) {
            if (!used.contains(key.getKey())) {
                continue;
            }
            builder.define(key.getKey(), key.getValue());
            if (unlock == null) {
                unlock = key.getValue();
            }
        }
        if (unlock == null) {
            return;
        }
        builder.unlockedBy("has_part", has(unlock)).save(output, id(path));
    }

    private static Item resolveTransformerIngredient(
            EnergyTransformerTierCatalog.Ingredient ingredient) {
        if (ingredient.item() != null) {
            ResourceLocation loc = ResourceLocation.parse(ingredient.item());
            if (!BuiltInRegistries.ITEM.containsKey(loc)) {
                return null;
            }
            Item item = BuiltInRegistries.ITEM.get(loc);
            return item == Items.AIR ? null : item;
        }
        MaterialPrefix prefix;
        try {
            prefix = new MaterialPrefix("cruciblecraft:" + ingredient.prefix());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return MaterialLookup.item(ingredient.material(), prefix).orElse(null);
    }

    private static Item resolveBatteryIngredient(
            EnergyBatteryTierCatalog.Ingredient ingredient) {
        if (ingredient.item() != null) {
            ResourceLocation loc = ResourceLocation.parse(ingredient.item());
            if (!BuiltInRegistries.ITEM.containsKey(loc)) {
                return null;
            }
            Item item = BuiltInRegistries.ITEM.get(loc);
            return item == Items.AIR ? null : item;
        }
        MaterialPrefix prefix;
        try {
            prefix = new MaterialPrefix("cruciblecraft:" + ingredient.prefix());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return MaterialLookup.item(ingredient.material(), prefix).orElse(null);
    }

    private static Item resolveConverterIngredient(
            EnergyConverterTierCatalog.Entry entry,
            EnergyConverterTierCatalog.Ingredient ingredient) {
        if (ingredient.item() != null) {
            ResourceLocation loc = ResourceLocation.parse(ingredient.item());
            if (!BuiltInRegistries.ITEM.containsKey(loc)) {
                return null;
            }
            Item item = BuiltInRegistries.ITEM.get(loc);
            return item == Items.AIR ? null : item;
        }
        String material = "variant".equals(ingredient.material())
                ? entry.material()
                : ingredient.material();
        MaterialPrefix prefix;
        try {
            prefix = new MaterialPrefix("cruciblecraft:" + ingredient.prefix());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return MaterialLookup.item(material, prefix).orElse(null);
    }

    private static void machineCrafting(RecipeOutput output, Item result, String id) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("CCC")
                .pattern("CFC")
                .pattern("CCC")
                .define('C', Items.COPPER_INGOT)
                .define('F', Items.FURNACE)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output, ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "machines/" + id));
    }

    private static void addDerivedOreRecipes(
            RecipeOutput output,
            MaterialDefinition material) {
        if (!MaterialCatalog.isFormRegistered(material, MaterialPrefixes.INGOT)) {
            return;
        }
        if (MaterialCatalog.isFormRegistered(material, MaterialPrefixes.CRUSHED_ORE)) {
            addOreRecipes(output, material, MaterialPrefixes.CRUSHED_ORE);
        }
        if (MaterialCatalog.isFormRegistered(material, MaterialPrefixes.RAW_ORE)
                && !material.formItems().containsKey(MaterialPrefixes.RAW_ORE)) {
            addOreRecipes(output, material, MaterialPrefixes.RAW_ORE);
        }
    }

    private static void addOreRecipes(
            RecipeOutput output,
            MaterialDefinition material,
            MaterialPrefix inputForm) {
        Item ore = MaterialLookup.item(material.id(), inputForm)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing " + inputForm.serializedName() + " item for " + material.id()));
        Item ingot = MaterialLookup.item(material.id(), MaterialPrefixes.INGOT)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing ingot item for " + material.id()));
        Ingredient ingredient = Ingredient.of(ore);
        String unlockName = "has_" + material.registryName(inputForm);

        SimpleCookingRecipeBuilder.smelting(
                        ingredient,
                        RecipeCategory.MISC,
                        ingot,
                        ORE_EXPERIENCE,
                        SMELTING_TIME)
                .group(COMPAT_SHORTCUT_GROUP)
                .unlockedBy(unlockName, has(ore))
                .save(output, recipeId(material, inputForm, "smelting"));
        SimpleCookingRecipeBuilder.blasting(
                        ingredient,
                        RecipeCategory.MISC,
                        ingot,
                        ORE_EXPERIENCE,
                        BLASTING_TIME)
                .group(COMPAT_SHORTCUT_GROUP)
                .unlockedBy(unlockName, has(ore))
                .save(output, recipeId(material, inputForm, "blasting"));
    }

    private static ResourceLocation recipeId(
            MaterialDefinition material,
            MaterialPrefix inputForm,
            String process) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID,
                material.registryName(inputForm) + "_" + process);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }

    private static MaterialRuleRecipe materialRule(
            ResourceLocation target,
            MaterialPrefix input,
            MaterialPrefix output,
            int inputCount,
            int outputCount,
            int duration,
            long eut,
            long specialValue,
            Map<String, Integer> durationOverrides) {
        Map<String, MaterialRule.MaterialOverride> overrides = durationOverrides.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> new MaterialRule.MaterialOverride(
                                java.util.Optional.of(Integer.toString(entry.getValue())),
                                java.util.Optional.empty(),
                                java.util.Optional.empty(),
                                Map.of(), Map.of(), Map.of(), Map.of())));
        return new MaterialRuleRecipe(new MaterialRule(
                java.util.Optional.of(target),
                List.of(ruleItem(input, inputCount)),
                List.of(ruleItem(output, outputCount)),
                List.of(),
                List.of(),
                Integer.toString(duration),
                Long.toString(eut),
                Long.toString(specialValue),
                true,
                java.util.Optional.empty(),
                overrides,
                List.of(),
                java.util.Optional.empty(),
                List.of()));
    }

    private record AdvancementFreeRecipeOutput(RecipeOutput delegate) implements RecipeOutput {
        @Override
        public void accept(
                ResourceLocation id,
                Recipe<?> recipe,
                AdvancementHolder advancement) {
            delegate.accept(id, recipe, null);
        }

        @Override
        public void accept(
                ResourceLocation id,
                Recipe<?> recipe,
                AdvancementHolder advancement,
                ICondition... conditions) {
            delegate.accept(id, recipe, null, conditions);
        }

        @Override
        public Advancement.Builder advancement() {
            return delegate.advancement();
        }
    }

    private static MaterialRule.ItemResource ruleItem(
            MaterialPrefix prefix,
            int count) {
        return new MaterialRule.ItemResource(
                java.util.Optional.of(prefix.serializedId()),
                java.util.Optional.empty(),
                Integer.toString(count),
                "10000");
    }

}
