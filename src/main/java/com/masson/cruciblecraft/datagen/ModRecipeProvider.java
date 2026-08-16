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
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.recipe.rule.MaterialRule;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleRecipe;
import com.masson.cruciblecraft.recipe.rule.T2ChainRules;
import com.masson.cruciblecraft.recipe.rule.T4ToolRules;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeEntry;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
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
        MaterialCatalog.startupValues().stream()
                .sorted(Comparator.comparing(MaterialDefinition::id))
                .forEach(material -> addDerivedOreRecipes(recipesOnly, material));
    }

    private static void addMachineRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.MISC, ModItems.FIREBOX.get())
                .pattern("BBB")
                .pattern("BFB")
                .pattern("BBB")
                .define('B', ModItems.FIREBRICK.get())
                .define('F', Items.FURNACE)
                .unlockedBy(
                        "has_firebrick", has(ModItems.FIREBRICK.get()))
                .save(output, id("machines/firebox"));
        machineCrafting(
                output, ModItems.ELECTRIC_MOTOR.get(), "electric_motor");
        machineCrafting(
                output, ModItems.ROTATIONAL_AXLE.get(), "rotational_axle");
        machineCrafting(
                output,
                ModItems.ROTATIONAL_GEARBOX.get(),
                "rotational_gearbox");
        machineCrafting(output, ModItems.SLUICE.get(), "sluice");
        machineCrafting(output, ModItems.BATH.get(), "bath");
        centrifugeCrafting(
                output,
                ModItems.CENTRIFUGE.get(),
                ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                "bronze",
                "centrifuge");
        centrifugeCrafting(
                output,
                ModItems.STEEL_CENTRIFUGE.get(),
                ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                "steel",
                "steel_centrifuge");
        centrifugeCrafting(
                output,
                ModItems.TITANIUM_CENTRIFUGE.get(),
                ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                "titanium",
                "titanium_centrifuge");
        t16MachineCrafting(
                output,
                ModItems.SHREDDER.get(),
                ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                "bronze",
                "shredder",
                "shredder");
        t16MachineCrafting(
                output,
                ModItems.STEEL_SHREDDER.get(),
                ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                "steel",
                "shredder",
                "steel_shredder");
        t16MachineCrafting(
                output,
                ModItems.TITANIUM_SHREDDER.get(),
                ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                "titanium",
                "shredder",
                "titanium_shredder");
        sifterCrafting(
                output,
                ModItems.SIFTER.get(),
                ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                "bronze",
                "sifter");
        sifterCrafting(
                output,
                ModItems.STEEL_SIFTER.get(),
                ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                "steel",
                "steel_sifter");
        sifterCrafting(
                output,
                ModItems.TITANIUM_SIFTER.get(),
                ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                "titanium",
                "titanium_sifter");
        t17HeatMachineCrafting(
                output,
                ModItems.SMELTER.get(),
                ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                "steel",
                null,
                null,
                "smelter",
                "smelter");
        t17HeatMachineCrafting(
                output,
                ModItems.INVAR_SMELTER.get(),
                ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                "invar",
                null,
                null,
                "smelter",
                "invar_smelter");
        t17HeatMachineCrafting(
                output,
                ModItems.TITANIUM_SMELTER.get(),
                ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                "titanium",
                null,
                null,
                "smelter",
                "titanium_smelter");
        machineCrafting(output, ModItems.MORTAR.get(), "mortar");
        machineCrafting(output, ModItems.EXTRUDER.get(), "extruder");
        machineCrafting(output, ModItems.CUTTER.get(), "cutter");
        t16MachineCrafting(
                output,
                ModItems.LATHE.get(),
                ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                "bronze",
                "lathe",
                "lathe");
        t16MachineCrafting(
                output,
                ModItems.STEEL_LATHE.get(),
                ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                "steel",
                "lathe",
                "steel_lathe");
        t16MachineCrafting(
                output,
                ModItems.TITANIUM_LATHE.get(),
                ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                "titanium",
                "lathe",
                "titanium_lathe");
        t16MachineCrafting(
                output,
                ModItems.ROLLINGMILL.get(),
                ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                "bronze",
                "rollingmill",
                "rollingmill");
        t16MachineCrafting(
                output,
                ModItems.STEEL_ROLLINGMILL.get(),
                ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                "steel",
                "rollingmill",
                "steel_rollingmill");
        t16MachineCrafting(
                output,
                ModItems.TITANIUM_ROLLINGMILL.get(),
                ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                "titanium",
                "rollingmill",
                "titanium_rollingmill");
        machineCrafting(output, ModItems.ROLLBENDER.get(), "rollbender");
        t16MachineCrafting(
                output,
                ModItems.WIREMILL.get(),
                ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                "bronze",
                "wiremill",
                "wiremill");
        t16MachineCrafting(
                output,
                ModItems.STEEL_WIREMILL.get(),
                ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                "steel",
                "wiremill",
                "steel_wiremill");
        t16MachineCrafting(
                output,
                ModItems.TITANIUM_WIREMILL.get(),
                ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                "titanium",
                "wiremill",
                "titanium_wiremill");
        machineCrafting(output, ModItems.BENDER.get(), "bender");
        machineCrafting(output, ModItems.ASSEMBLER.get(), "assembler");
        machineCrafting(output, ModItems.WELDER.get(), "welder");
        t16MachineCrafting(
                output,
                ModItems.PRESS.get(),
                ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                "bronze",
                "press",
                "press");
        t16MachineCrafting(
                output,
                ModItems.STEEL_PRESS.get(),
                ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                "steel",
                "press",
                "steel_press");
        t16MachineCrafting(
                output,
                ModItems.TITANIUM_PRESS.get(),
                ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                "titanium",
                "press",
                "titanium_press");
        electrolyzerCrafting(
                output,
                ModItems.ELECTROLYZER.get(),
                ModItems.STEEL_GALVANIZED_MACHINE_CASING.get(),
                "tin",
                "electrolyzer");
        electrolyzerCrafting(
                output,
                ModItems.ALUMINIUM_ELECTROLYZER.get(),
                ModItems.ALUMINIUM_MACHINE_CASING.get(),
                "copper",
                "aluminium_electrolyzer");
        electrolyzerCrafting(
                output,
                ModItems.STAINLESS_STEEL_ELECTROLYZER.get(),
                ModItems.STAINLESS_STEEL_MACHINE_CASING.get(),
                "gold",
                "stainless_steel_electrolyzer");
        machineCrafting(output, ModItems.MIXER.get(), "mixer");
        t17HeatMachineCrafting(
                output,
                ModItems.DISTILLERY.get(),
                ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                "steel",
                "constantan",
                MaterialPrefixes.DOUBLE_WIRE,
                "distillery",
                "distillery");
        t17HeatMachineCrafting(
                output,
                ModItems.INVAR_DISTILLERY.get(),
                ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                "invar",
                "kanthal",
                MaterialPrefixes.QUADRUPLE_WIRE,
                "distillery",
                "invar_distillery");
        t17HeatMachineCrafting(
                output,
                ModItems.TITANIUM_DISTILLERY.get(),
                ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                "titanium",
                "nichrome",
                MaterialPrefixes.OCTUPLE_WIRE,
                "distillery",
                "titanium_distillery");
        machineCrafting(output, ModItems.AUTOCLAVE.get(), "autoclave");
        t17HeatMachineCrafting(
                output,
                ModItems.DRYING.get(),
                ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                "steel",
                null,
                null,
                "drying",
                "drying");
        t17HeatMachineCrafting(
                output,
                ModItems.INVAR_DRYING.get(),
                ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                "invar",
                null,
                null,
                "drying",
                "invar_drying");
        t17HeatMachineCrafting(
                output,
                ModItems.TITANIUM_DRYING.get(),
                ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                "titanium",
                null,
                null,
                "drying",
                "titanium_drying");
        machineCrafting(output, ModItems.COMPRESSOR.get(), "compressor");
        machineCrafting(output, ModItems.GENERIFIER.get(), "generifier");
        machineCrafting(
                output,
                ModItems.FLUID_DEPOSIT_EXTRACTOR.get(),
                "fluid_deposit_extractor");
        machineCrafting(
                output, ModItems.FUEL_ENGINE.get(), "fuel_engine");
        machineCrafting(
                output,
                ModItems.BURNING_GAS_GENERATOR.get(),
                "burning_gas_generator");
        casingCrafting(
                output,
                ModItems.BRONZE_DOUBLE_MACHINE_CASING.get(),
                "bronze",
                true);
        casingCrafting(
                output,
                ModItems.STEEL_DOUBLE_MACHINE_CASING.get(),
                "steel",
                true);
        casingCrafting(
                output,
                ModItems.TITANIUM_DOUBLE_MACHINE_CASING.get(),
                "titanium",
                true);
        casingCrafting(
                output,
                ModItems.STEEL_GALVANIZED_MACHINE_CASING.get(),
                "steel_galvanized",
                false);
        casingCrafting(
                output,
                ModItems.ALUMINIUM_MACHINE_CASING.get(),
                "aluminium",
                false);
        casingCrafting(
                output,
                ModItems.STAINLESS_STEEL_MACHINE_CASING.get(),
                "stainless_steel",
                false);
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
            pattern.recipePattern().forEach(builder::pattern);
            builder.save(output, id("tools/pattern/" + pattern.id()));
        });
        T4ToolRules.ALL.forEach(definition -> output.accept(
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
                                List.of(new ItemStack(ModItems.COAL_COKE.get())),
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
                new MaterialRuleRecipe(T2ChainRules.ANVIL_RAW_TO_CRUSHED),
                null);
        T2ChainRules.ALL.stream()
                .filter(definition ->
                        !T2ChainRules.CONCRETE_ORE_CHAIN_PATHS.contains(definition.path()))
                .forEach(definition -> output.accept(
                        id(definition.path()), new MaterialRuleRecipe(definition.rule()), null));
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
                Item item = ModItems.COAL_COKE.get();
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

    private static void t16MachineCrafting(
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
            case "rollingmill" -> builder
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
            case "shredder" -> builder
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
                    "Unsupported T16 machine kind " + kind);
        }
        builder.save(output, id("machines/" + id));
    }

    private static void t17HeatMachineCrafting(
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
            default -> throw new IllegalArgumentException(
                    "Unsupported T17 heat machine kind " + kind);
        }
        builder.save(output, id("machines/" + id));
    }

    private static void casingCrafting(
            RecipeOutput output,
            Item result,
            String material,
            boolean doubled) {
        Item plate = materialItem(
                material,
                doubled
                        ? MaterialPrefixes.DOUBLE_PLATE
                        : MaterialPrefixes.PLATE);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("PPP")
                .pattern("P P")
                .pattern("PPP")
                .define('P', plate)
                .unlockedBy("has_plate", has(plate))
                .save(output, id("components/"
                        + net.minecraft.core.registries.BuiltInRegistries
                                .ITEM.getKey(result).getPath()));
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
