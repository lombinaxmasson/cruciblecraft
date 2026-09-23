package com.masson.cruciblecraft.datagen;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.content.item.ToolPatternCatalog;
import com.masson.cruciblecraft.content.block.WoodDebark;
import com.masson.cruciblecraft.content.block.StainlessSteelMixerWalls;
import com.masson.cruciblecraft.content.block.AutoclaveWalls;
import com.masson.cruciblecraft.content.block.ImplosionCompressorWalls;
import com.masson.cruciblecraft.content.block.InvarOvenWalls;
import com.masson.cruciblecraft.content.block.ElectrolyzerParts;
import com.masson.cruciblecraft.content.block.SluiceWalls;
import com.masson.cruciblecraft.content.block.TungstensteelCrusherWalls;
import com.masson.cruciblecraft.logistics.pipe.PipeAcquisitionRecipeCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverComponentTiers;
import com.masson.cruciblecraft.content.storage.StorageVariantCatalog;
import com.masson.cruciblecraft.logistics.hopper.HopperKind;
import com.masson.cruciblecraft.logistics.hopper.HopperVariantCatalog;
import com.masson.cruciblecraft.content.item.GeigerCounterItem;
import com.masson.cruciblecraft.content.item.ElectroMeterItem;
import com.masson.cruciblecraft.content.item.TachoMeterItem;
import com.masson.cruciblecraft.content.item.ThermometerItem;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind;
import com.masson.cruciblecraft.content.sensor.SensorKind;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.recipe.rule.MaterialRule;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleRecipe;
import com.masson.cruciblecraft.recipe.rule.MaterialChainRules;
import com.masson.cruciblecraft.recipe.rule.ToolRules;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeEntry;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.recipe.crafting.CraftingTools;
import com.masson.cruciblecraft.recipe.crafting.PrefixPackRecipe;
import com.masson.cruciblecraft.recipe.crafting.RockCobbleCrafting;
import com.masson.cruciblecraft.recipe.crafting.RockGtProcessing;
import com.masson.cruciblecraft.recipe.crafting.ShapedCatalystRecipe;
import com.masson.cruciblecraft.recipe.crafting.ToolHeadAssemblyRecipe;
import com.masson.cruciblecraft.recipe.crafting.WorkbenchToolRecipePlan;
import com.masson.cruciblecraft.fusion.FusionRecipeCatalog;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.energy.battery.EnergyBatteryTierCatalog;
import com.masson.cruciblecraft.energy.cable.ElectricalConductorCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterTierCatalog;
import com.masson.cruciblecraft.energy.transformer.EnergyTransformerTierCatalog;
import com.masson.cruciblecraft.energy.heatexchanger.HeatExchangerCatalog;
import com.masson.cruciblecraft.energy.heatexchanger.HeatExchangerProfile;
import com.masson.cruciblecraft.energy.cooler.CoolerCatalog;
import com.masson.cruciblecraft.energy.cooler.CoolerProfile;
import com.masson.cruciblecraft.energy.flux.FluxCatalog;
import com.masson.cruciblecraft.energy.flux.FluxProfile;
import com.masson.cruciblecraft.energy.largeheatexchanger.LargeHeatExchangerCatalog;
import com.masson.cruciblecraft.energy.largeheatexchanger.LargeHeatExchangerProfile;
import com.masson.cruciblecraft.energy.steam.SteamTurbineCatalog;
import com.masson.cruciblecraft.energy.largegasturbine.LargeGasTurbineCatalog;
import com.masson.cruciblecraft.content.mte.MteInPlaceAcquisitionCatalog;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.registry.ModItemTags;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
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
        addSensorRecipes(recipesOnly);
        addStorageRecipes(recipesOnly);
        addRockCobbleRecipes(recipesOnly);
        addRockFurnaceRecipes(recipesOnly);
        addWoodBeamRecipes(recipesOnly);
        MaterialCatalog.startupValues().stream()
                .sorted(Comparator.comparing(MaterialDefinition::id))
                .forEach(material -> addDerivedOreRecipes(recipesOnly, material));
    }

    private static void compactElectricCoverRecipes(RecipeOutput output) {
        // Cover TIER_COUNT stays 10. Compact part recipes are GT6 grids;
        // programmed_circuit upgrades are not emitted.
    }

    private static void machineCoverRecipes(RecipeOutput output) {
        Item aluminiumPlate = sourceItem(
                "aluminium", MaterialPrefixes.PLATE);
        Item aluminiumScrew = sourceItem(
                "aluminium", MaterialPrefixes.SCREW);
        boolean blankAvailable = available(aluminiumPlate, aluminiumScrew);
        if (blankAvailable) {
            acceptShapedCatalyst(
                    output,
                    MachineCoverKinds.itemPath("cover_blank"),
                    List.of("Sh ", "Pd "),
                    Map.of(
                            "P", keyedIngredient(
                                    aluminiumPlate,
                                    "aluminium",
                                    MaterialPrefixes.PLATE),
                            "S", keyedIngredient(
                                    aluminiumScrew,
                                    "aluminium",
                                    MaterialPrefixes.SCREW)),
                    Map.of(
                            "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get()),
                            "d", Ingredient.of(
                                    ModItems.MATERIAL_SCREWDRIVER.get())),
                    new ItemStack(machineCoverItem("cover_blank")));
        }

        Item tinCable = sourceItem("tin", MaterialPrefixes.CABLE);
        Item copperCable = sourceItem("copper", MaterialPrefixes.CABLE);
        Item tinWire = sourceItem("tin", MaterialPrefixes.WIRE);
        Item lumiumWire = sourceItem("lumium", MaterialPrefixes.WIRE);
        Item redAlloyWire = sourceItem(
                "red_alloy", MaterialPrefixes.WIRE);
        Item ironRod = sourceItem("iron", MaterialPrefixes.ROD);
        Item ironRotor = sourceItem("iron", MaterialPrefixes.ROTOR);
        Item brassSmallGear = sourceItem(
                "brass", MaterialPrefixes.SMALL_GEAR);
        Item circuitBasic = techPart("circuit_basic");
        Item circuitGood = techPart("circuit_good");
        Item circuitAdvanced = techPart("circuit_advanced");
        Item circuitElite = techPart("circuit_elite");
        Item circuitMaster = techPart("circuit_master");
        Item selector = ModItems.PROGRAMMED_CIRCUIT.get();
        Ingredient button = Ingredient.of(ItemTags.BUTTONS);

        if (blankAvailable
                && available(tinCable, lumiumWire, circuitBasic)) {
            machineRecipe(
                    output,
                    "controller_display",
                    List.of("LLB", "CQW"),
                    Map.of(
                            "L", keyedIngredient(lumiumWire, "lumium", MaterialPrefixes.WIRE),
                            "B", Ingredient.of(Items.LEVER),
                            "C", Ingredient.of(circuitBasic),
                            "Q", blank(),
                            "W", keyedIngredient(tinCable, "tin", MaterialPrefixes.CABLE)));
        }
        if (blankAvailable && available(tinCable, circuitBasic)) {
            machineRecipe(
                    output,
                    "controller_auto",
                    List.of("BW ", "CQ "),
                    Map.of(
                            "B", Ingredient.of(Items.LEVER),
                            "W", keyedIngredient(tinCable, "tin", MaterialPrefixes.CABLE),
                            "C", Ingredient.of(circuitBasic),
                            "Q", blank()));
        }
        if (blankAvailable
                && available(tinCable, tinWire, lumiumWire, circuitBasic)) {
            machineRecipe(
                    output,
                    "display_energy",
                    List.of("CLB", "WQW"),
                    Map.of(
                            "C", Ingredient.of(circuitBasic),
                            "L", keyedIngredient(lumiumWire, "lumium", MaterialPrefixes.WIRE),
                            "B", keyedIngredient(tinWire, "tin", MaterialPrefixes.WIRE),
                            "W", keyedIngredient(tinCable, "tin", MaterialPrefixes.CABLE),
                            "Q", blank()));
        }
        if (blankAvailable && available(tinCable, circuitBasic)) {
            machineRecipe(
                    output,
                    "controller_redstone",
                    List.of("BW ", "CQ "),
                    Map.of(
                            "B", Ingredient.of(Items.REDSTONE_TORCH),
                            "W", keyedIngredient(tinCable, "tin", MaterialPrefixes.CABLE),
                            "C", Ingredient.of(circuitBasic),
                            "Q", blank()));
        }
        if (blankAvailable && available(copperCable, circuitGood)) {
            machineRecipe(
                    output,
                    "controller_auto_redstone",
                    List.of("BW ", "CQ "),
                    Map.of(
                            "B", Ingredient.of(Items.LEVER),
                            "W", keyedIngredient(copperCable, "copper", MaterialPrefixes.CABLE),
                            "C", Ingredient.of(circuitGood),
                            "Q", blank()));
        }
        if (blankAvailable && available(copperCable, circuitBasic)) {
            machineRecipe(
                    output,
                    "selector_redstone",
                    List.of(" C ", "WQX", " B "),
                    Map.of(
                            "C", Ingredient.of(circuitBasic),
                            "W", keyedIngredient(copperCable, "copper", MaterialPrefixes.CABLE),
                            "Q", blank(),
                            "X", Ingredient.of(selector),
                            "B", Ingredient.of(Items.COMPARATOR)));
        }
        if (blankAvailable) {
            timerRecipe(
                    output,
                    "controller_auto_timer_1m",
                    circuitMaster,
                    List.of("BWd", "CQ "));
            timerRecipe(
                    output,
                    "controller_auto_timer_5m",
                    circuitElite,
                    List.of("BW ", "CQd"));
            timerRecipe(
                    output,
                    "controller_auto_timer_10m",
                    circuitAdvanced,
                    List.of("BW ", "CQ ", "  d"));
            timerRecipe(
                    output,
                    "controller_auto_timer_20m",
                    circuitGood,
                    List.of("BW ", "CQ ", " d "));
            timerRecipe(
                    output,
                    "controller_auto_timer_30m",
                    circuitBasic,
                    List.of("BW ", "CQ ", "d  "));
        }
        if (blankAvailable && available(tinCable, tinWire, circuitGood)) {
            machineRecipe(
                    output,
                    "scale_energy",
                    List.of("WQW", "BCB"),
                    Map.of(
                            "W", keyedIngredient(tinCable, "tin", MaterialPrefixes.CABLE),
                            "Q", blank(),
                            "B", keyedIngredient(tinWire, "tin", MaterialPrefixes.WIRE),
                            "C", Ingredient.of(circuitGood)));
        }
        if (blankAvailable && available(tinCable, circuitGood)) {
            machineRecipe(
                    output,
                    "detector_running_possible",
                    List.of("WQW", "BCB"),
                    Map.of(
                            "W", keyedIngredient(tinCable, "tin", MaterialPrefixes.CABLE),
                            "Q", blank(),
                            "B", Ingredient.of(Items.COMPARATOR),
                            "C", Ingredient.of(circuitGood)));
            machineRecipe(
                    output,
                    "detector_running_passively",
                    List.of("WQW", "BCB"),
                    Map.of(
                            "W", keyedIngredient(tinCable, "tin", MaterialPrefixes.CABLE),
                            "Q", blank(),
                            "B", Ingredient.of(Items.REPEATER),
                            "C", Ingredient.of(circuitGood)));
            machineRecipe(
                    output,
                    "detector_running_actively",
                    List.of("WQW", "BCX"),
                    Map.of(
                            "W", keyedIngredient(tinCable, "tin", MaterialPrefixes.CABLE),
                            "Q", blank(),
                            "B", Ingredient.of(Items.COMPARATOR),
                            "C", Ingredient.of(circuitGood),
                            "X", Ingredient.of(Items.REPEATER)));
            machineRecipe(
                    output,
                    "detector_running_successfully",
                    List.of("WQW", "BCX"),
                    Map.of(
                            "W", keyedIngredient(tinCable, "tin", MaterialPrefixes.CABLE),
                            "Q", blank(),
                            "B", button,
                            "C", Ingredient.of(circuitGood),
                            "X", Ingredient.of(Items.REDSTONE_TORCH)));
        }
        if (blankAvailable
                && available(tinCable, circuitGood, brassSmallGear)) {
            machineRecipe(
                    output,
                    "scale_progress",
                    List.of("WQW", "BCB"),
                    Map.of(
                            "W", keyedIngredient(tinCable, "tin", MaterialPrefixes.CABLE),
                            "Q", blank(),
                            "B", Ingredient.of(brassSmallGear),
                            "C", Ingredient.of(circuitGood)));
        }
        if (blankAvailable && available(tinCable)) {
            machineRecipe(
                    output,
                    "redstone_emitter",
                    List.of("BQB", "WXW"),
                    Map.of(
                            "B", button,
                            "Q", blank(),
                            "W", keyedIngredient(tinCable, "tin", MaterialPrefixes.CABLE),
                            "X", Ingredient.of(Items.COMPARATOR)));
        }
        if (available(ironRod, ironRotor)) {
            machineRecipe(
                    output,
                    "vent",
                    List.of("RRR", "RXR", "RRR"),
                    Map.of(
                            "R", keyedIngredient(ironRod, "iron", MaterialPrefixes.ROD),
                            "X", keyedIngredient(ironRotor, "iron", MaterialPrefixes.ROTOR)));
        }
        if (blankAvailable && available(tinCable, circuitGood)) {
            machineRecipe(
                    output,
                    "controller_covers",
                    List.of("BW ", "CQ "),
                    Map.of(
                            "B", Ingredient.of(Items.COMPARATOR),
                            "W", keyedIngredient(tinCable, "tin", MaterialPrefixes.CABLE),
                            "C", Ingredient.of(circuitGood),
                            "Q", blank()));
        }
        if (blankAvailable && available(circuitBasic)) {
            machineRecipe(
                    output,
                    "selector_button_panel",
                    List.of("BXB", "BQB", "BCB"),
                    Map.of(
                            "B", button,
                            "X", Ingredient.of(selector),
                            "Q", blank(),
                            "C", Ingredient.of(circuitBasic)));
        }
        if (blankAvailable) {
            machineRecipe(
                    output,
                    "cover_crafting",
                    List.of("C", "Q"),
                    Map.of(
                            "C", Ingredient.of(Items.CRAFTING_TABLE),
                            "Q", blank()));
            machineRecipe(
                    output,
                    "cover_warning",
                    List.of("G ", "YQ"),
                    Map.of(
                            "G", Ingredient.of(ModItemTags.CRAFTING_PISTON_GLUE),
                            "Y", Ingredient.of(Items.YELLOW_DYE),
                            "Q", blank()));
        }
        Item zincFoil = sourceItem("zinc", MaterialPrefixes.FOIL);
        if (blankAvailable && available(zincFoil)) {
            machineRecipe(
                    output,
                    "filter_fluid",
                    List.of("Z Z", " Q ", "Z Z"),
                    Map.of(
                            "Z", keyedIngredient(
                                    zincFoil, "zinc", MaterialPrefixes.FOIL),
                            "Q", blank()));
        }
        if (blankAvailable && available(ironRod)) {
            machineCatalystRecipe(
                    output,
                    "cover_drain",
                    List.of("RRR", "RwR", "RRR"),
                    Map.of(
                            "R", keyedIngredient(
                                    ironRod, "iron", MaterialPrefixes.ROD)),
                    Map.of(
                            "w", Ingredient.of(ModItems.MATERIAL_WRENCH.get())));
        }
        if (blankAvailable && available(redAlloyWire)) {
            machineRecipe(
                    output,
                    "redstone_conductor_in",
                    List.of("R  ", "Q  "),
                    Map.of(
                            "R", Ingredient.of(redAlloyWire),
                            "Q", blank()));
            machineRecipe(
                    output,
                    "redstone_conductor_out",
                    List.of("Q  ", "R  "),
                    Map.of(
                            "R", Ingredient.of(redAlloyWire),
                            "Q", blank()));
            ShapelessRecipeBuilder.shapeless(
                            RecipeCategory.MISC,
                            machineCoverItem("redstone_conductor_in"))
                    .requires(machineCoverItem("redstone_conductor_out"))
                    .unlockedBy(
                            "has_redstone_conductor_out",
                            has(machineCoverItem("redstone_conductor_out")))
                    .save(output, id("redstone_conductor_in_convert"));
            ShapelessRecipeBuilder.shapeless(
                            RecipeCategory.MISC,
                            machineCoverItem("redstone_conductor_out"))
                    .requires(machineCoverItem("redstone_conductor_in"))
                    .unlockedBy(
                            "has_redstone_conductor_in",
                            has(machineCoverItem("redstone_conductor_in")))
                    .save(output, id("redstone_conductor_out_convert"));
        }
        // selector_tag is carried by an existing programmed circuit and has
        // no new item recipe; material screwdriver is a catalyst for timers.
    }

    private static Ingredient blank() {
        return Ingredient.of(machineCoverItem("cover_blank"));
    }

    private static Item machineCoverItem(String definitionPath) {
        return ModItems.machineCover(
                MachineCoverKinds.itemPath(definitionPath)).get();
    }

    private static void machineRecipe(
            RecipeOutput output,
            String path,
            List<String> pattern,
            Map<String, Ingredient> ingredients) {
        boolean catalystPattern = pattern.size() >= 2
                && pattern.size() <= 3
                && pattern.stream().allMatch(row -> row.length() == 3);
        if (!catalystPattern) {
            ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(
                    RecipeCategory.MISC,
                    machineCoverItem(path));
            pattern.forEach(builder::pattern);
            ingredients.forEach((symbol, ingredient) ->
                    builder.define(symbol.charAt(0), ingredient));
            Item unlock = Items.CRAFTING_TABLE;
            for (Ingredient ingredient : ingredients.values()) {
                if (ingredient.getItems().length > 0) {
                    unlock = ingredient.getItems()[0].getItem();
                    break;
                }
            }
            builder.unlockedBy("has_ingredient", has(unlock))
                    .save(output, id(MachineCoverKinds.itemPath(path)));
            return;
        }
        output.accept(
                id(MachineCoverKinds.itemPath(path)),
                new ShapedCatalystRecipe(
                        pattern,
                        ingredients,
                        Map.of(),
                        new ItemStack(machineCoverItem(path))),
                null);
    }

    private static void timerRecipe(
            RecipeOutput output,
            String path,
            Item circuit,
            List<String> pattern) {
        Item tinCable = sourceItem("tin", MaterialPrefixes.CABLE);
        if (circuit == null || tinCable == null) {
            return;
        }
        machineCatalystRecipe(
                output,
                path,
                pattern,
                Map.of(
                        "B", Ingredient.of(Items.REPEATER),
                        "W", keyedIngredient(tinCable, "tin", MaterialPrefixes.CABLE),
                        "C", Ingredient.of(circuit),
                        "Q", blank()),
                Map.of(
                        "d", Ingredient.of(
                                ModItems.MATERIAL_SCREWDRIVER.get())));
    }

    private static void machineCatalystRecipe(
            RecipeOutput output,
            String path,
            List<String> pattern,
            Map<String, Ingredient> ingredients,
            Map<String, Ingredient> catalysts) {
        output.accept(
                id(MachineCoverKinds.itemPath(path)),
                new ShapedCatalystRecipe(
                        pattern,
                        ingredients,
                        catalysts,
                        new ItemStack(machineCoverItem(path))),
                null);
    }

    private static Item sourceItem(
            String material,
            MaterialPrefix prefix) {
        return MaterialLookup.item(material, prefix).orElse(null);
    }

    private static Ingredient materialIngredient(
            String material, MaterialPrefix prefix) {
        return MaterialLookup.ingredient(material, prefix)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing source machine component "
                                + material
                                + "/"
                                + prefix.serializedName()));
    }

    private static ItemStack materialStack(String material, MaterialPrefix prefix) {
        return MaterialLookup.tryStack(material, prefix, 1)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing source machine component "
                                + material
                                + "/"
                                + prefix.serializedName()));
    }

    private static Ingredient keyedIngredient(
            Item item, String material, MaterialPrefix prefix) {
        if (item instanceof com.masson.cruciblecraft.content.item.PrefixMaterialItem) {
            return materialIngredient(material, prefix);
        }
        return Ingredient.of(item);
    }

    private static ItemStack keyedStack(
            Item item, String material, MaterialPrefix prefix, int count) {
        if (item instanceof com.masson.cruciblecraft.content.item.PrefixMaterialItem) {
            return MaterialLookup.stack(material, prefix, count);
        }
        return new ItemStack(item, count);
    }

    private static Item unlockSample(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? Items.CRAFTING_TABLE : items[0].getItem();
    }

    private static Item techPart(String path) {
        try {
            return ModItems.technologicalPart(path).get();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static boolean available(Item... items) {
        for (Item item : items) {
            if (item == null) {
                return false;
            }
        }
        return true;
    }

    private static void addHopperRecipes(RecipeOutput output) {
        HopperVariantCatalog.variants().forEach(variant -> {
            Item result = ModItems.hopperItemsById().get(variant.id()).get();
            Item plate = materialItem(
                    variant.materialPath(), MaterialPrefixes.PLATE);
            ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(
                            RecipeCategory.MISC, result)
                    .define(
                            'P',
                            keyedIngredient(
                                    plate,
                                    variant.materialPath(),
                                    MaterialPrefixes.PLATE))
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
                .define(
                        'P',
                        keyedIngredient(
                                ironPlate, "iron", MaterialPrefixes.PLATE))
                .define('H', Items.HOPPER)
                .define(
                        'R',
                        keyedIngredient(ironRod, "iron", MaterialPrefixes.ROD))
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .unlockedBy("has_plate", has(ironPlate))
                .save(output, id("hoppers/steel_dust_funnel"));
    }

    private static void addSensorRecipes(RecipeOutput output) {
        Item plate = materialItem("tin_alloy", MaterialPrefixes.DOUBLE_PLATE);
        Item fineWire = materialItem("red_alloy", MaterialPrefixes.FINE_WIRE);
        Item bolt = materialItem("tin_alloy", MaterialPrefixes.BOLT);
        for (SensorKind kind : SensorKind.all()) {
            if (kind.d0Blocked()) {
                continue;
            }
            Item result = ModItems.sensorItemsById().get(kind.id()).get();
            ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(
                            RecipeCategory.MISC, result)
                    .pattern(kind.grid()[0])
                    .pattern(kind.grid()[1])
                    .pattern(kind.grid()[2])
                    .unlockedBy("has_redstone", has(Items.REDSTONE));
            String letters = String.join("", kind.grid());
            if (letters.indexOf('P') >= 0) {
                builder.define(
                        'P',
                        keyedIngredient(
                                plate,
                                "tin_alloy",
                                MaterialPrefixes.DOUBLE_PLATE));
            }
            if (letters.indexOf('W') >= 0) {
                builder.define(
                        'W',
                        keyedIngredient(
                                fineWire,
                                "red_alloy",
                                MaterialPrefixes.FINE_WIRE));
            }
            if (letters.indexOf('R') >= 0) {
                builder.define('R', Items.REDSTONE);
            }
            if (letters.indexOf('G') >= 0) {
                builder.define('G', Items.GLASS);
            }
            if (letters.indexOf('B') >= 0) {
                builder.define(
                        'B',
                        keyedIngredient(
                                bolt, "tin_alloy", MaterialPrefixes.BOLT));
            }
            if (letters.indexOf('C') >= 0) {
                builder.define('C', Items.COMPARATOR);
            }
            if (letters.indexOf('X') >= 0) {
                builder.define('X', sensorSpecial(kind.specialX()));
            }
            if (letters.indexOf('Y') >= 0) {
                builder.define('Y', sensorSpecial(kind.specialY()));
            }
            builder.save(output, id("sensors/" + kind.path()));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result)
                    .requires(result)
                    .unlockedBy("has_sensor", has(result))
                    .save(output, id("sensors/" + kind.path() + "_cycle"));
        }
    }

    private static Ingredient sensorSpecial(String key) {
        return switch (key) {
            case "thermometer" -> Ingredient.of(semanticItem(
                    ThermometerItem.REGISTRY_PATH));
            case "electro_meter" -> Ingredient.of(semanticItem(
                    ElectroMeterItem.REGISTRY_PATH));
            case "tacho_meter" -> Ingredient.of(semanticItem(
                    TachoMeterItem.REGISTRY_PATH));
            case "sio2_gem" -> materialIngredient("glass", MaterialPrefixes.GEM);
            case "silicon_plate" -> materialIngredient("silicon", MaterialPrefixes.PLATE);
            case "copper_fine_wire" -> materialIngredient("copper", MaterialPrefixes.FINE_WIRE);
            case "copper_wire" -> materialIngredient("copper", MaterialPrefixes.WIRE);
            case "clock" -> Ingredient.of(Items.CLOCK);
            case "gold_pressure_plate" -> Ingredient.of(
                    Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
            case "iron_pressure_plate" -> Ingredient.of(
                    Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
            case "stone_pressure_plate" -> Ingredient.of(
                    Items.STONE_PRESSURE_PLATE);
            case "wood_pressure_plate" -> Ingredient.of(
                    Items.OAK_PRESSURE_PLATE);
            case "chest" -> Ingredient.of(Items.CHEST);
            case "bucket" -> Ingredient.of(Items.BUCKET);
            case "brass_small_gear" -> materialIngredient("brass", MaterialPrefixes.SMALL_GEAR);
            case "brass_gear" -> materialIngredient("brass", MaterialPrefixes.GEAR);
            case "geiger_counter" -> Ingredient.of(semanticItem(
                    GeigerCounterItem.FILLED_PATH));
            case "lead_double_plate" -> materialIngredient("lead", MaterialPrefixes.DOUBLE_PLATE);
            case "compact_sensor_lv" -> Ingredient.of(
                    ModItems.technologicalPart("compact_sensor_lv").get());
            case "diamond_gem" -> materialIngredient("diamantine", MaterialPrefixes.GEM);
            default -> throw new IllegalStateException(
                    "Unknown sensor D0 special " + key);
        };
    }

    private static Item semanticItem(String path) {
        var item = ModItems.semanticIdentityItemsById().get(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", path));
        if (item == null) {
            throw new IllegalStateException("Missing semantic item " + path);
        }
        return item.get();
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
        Item plasticScrew = materialItem("plastic", MaterialPrefixes.SCREW);
        Item treatedPlate = materialItem("wood_treated", MaterialPrefixes.PLATE);
        Item ironLongRod = materialItem("iron", MaterialPrefixes.LONG_ROD);
        StorageVariantCatalog.sourceVisible().forEach(variant -> {
            Item result = ModItems.storageItemsById().get(variant.id()).get();
            String recipeId = "storage/" + variant.path();
            switch (variant.acquisitionProfile()) {
                case "steel_shaped" -> ShapedRecipeBuilder.shaped(
                                RecipeCategory.MISC, result)
                        .pattern("PPP")
                        .pattern("PCP")
                        .pattern("PPP")
                        .define(
                                'P',
                                keyedIngredient(
                                        steelPlate,
                                        "steel",
                                        MaterialPrefixes.PLATE))
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
                case "treated_barrel" -> acceptShapedCatalyst(
                        output,
                        recipeId,
                        List.of("rCs", "PSP", "PSP"),
                        Map.of(
                                "C",
                                Ingredient.of(Items.CHEST),
                                "P",
                                keyedIngredient(
                                        treatedPlate,
                                        "wood_treated",
                                        MaterialPrefixes.PLATE),
                                "S",
                                keyedIngredient(
                                        ironLongRod,
                                        "iron",
                                        MaterialPrefixes.LONG_ROD)),
                        Map.of(
                                "r",
                                CraftingTools.of(ModItems.MATERIAL_SOFT_HAMMER.get()),
                                "s",
                                CraftingTools.of(ModItems.MATERIAL_SAW.get())),
                        new ItemStack(result));
                case "plastic_box" -> acceptShapedCatalyst(
                        output,
                        recipeId,
                        plasticBoxPattern(variant.expansionKey()),
                        Map.of(
                                "C",
                                Ingredient.of(Items.CHEST),
                                "P",
                                keyedIngredient(
                                        plasticPlate,
                                        "plastic",
                                        MaterialPrefixes.PLATE),
                                "T",
                                keyedIngredient(
                                        plasticScrew,
                                        "plastic",
                                        MaterialPrefixes.SCREW)),
                        Map.of(
                                "d",
                                CraftingTools.of(ModItems.MATERIAL_SCREWDRIVER.get())),
                        new ItemStack(result));
                case "charging_locker" -> ShapedRecipeBuilder.shaped(
                                RecipeCategory.MISC, result)
                        .pattern("PPP")
                        .pattern("PCP")
                        .pattern("PRP")
                        .define(
                                'P',
                                keyedIngredient(
                                        steelPlate,
                                        "steel",
                                        MaterialPrefixes.PLATE))
                        .define('C', Items.CHEST)
                        .define('R', ModItems.PROGRAMMED_CIRCUIT.get())
                        .unlockedBy("has_circuit", has(ModItems.PROGRAMMED_CIRCUIT.get()))
                        .save(output, id(recipeId));
                case "inserter" -> ShapedRecipeBuilder.shaped(
                                RecipeCategory.MISC, result)
                        .pattern(" P ")
                        .pattern(" H ")
                        .pattern(" P ")
                        .define(
                                'P',
                                keyedIngredient(
                                        steelPlate,
                                        "steel",
                                        MaterialPrefixes.PLATE))
                        .define('H', Items.HOPPER)
                        .unlockedBy("has_hopper", has(Items.HOPPER))
                        .save(output, id(recipeId));
                case "folded_gt6_catalyst" -> {
                    // Source-exact GT6 grid is emitted from MteInPlaceAcquisitionCatalog
                    // onto the T44 host id after the storage-art fold.
                }
                default -> throw new IllegalStateException(
                        "Visible storage row missing acquisition recipe: "
                                + variant.id());
            }
        });
        addPlasticBoxCycleRecipes(output);
    }

    private static List<String> plasticBoxPattern(String expansionKey) {
        return switch (expansionKey) {
            case "6996" -> List.of("dPT", "PCP", "TPT");
            case "6995" -> List.of("TPd", "PCP", "TPT");
            case "6994" -> List.of("TPT", "PCP", "dPT");
            case "6993" -> List.of("TPT", "PCP", "TPd");
            default -> throw new IllegalStateException(
                    "Unknown plastic storage box meta " + expansionKey);
        };
    }

    private static void addPlasticBoxCycleRecipes(RecipeOutput output) {
        Item box128 = storageItem("plastic_storage_box_6993");
        Item box256 = storageItem("plastic_storage_box_6994");
        Item box512 = storageItem("plastic_storage_box_6995");
        Item box1024 = storageItem("plastic_storage_box_6996");
        plasticBoxCycle(output, box1024, box512, "storage/plastic_storage_box_6996_from_6995");
        plasticBoxCycle(output, box512, box256, "storage/plastic_storage_box_6995_from_6994");
        plasticBoxCycle(output, box256, box128, "storage/plastic_storage_box_6994_from_6993");
        plasticBoxCycle(output, box128, box1024, "storage/plastic_storage_box_6993_from_6996");
    }

    private static void plasticBoxCycle(
            RecipeOutput output, Item result, Item input, String path) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result)
                .requires(input)
                .unlockedBy("has_plastic_storage_box", has(input))
                .save(output, id(path));
    }

    private static Item storageItem(String path) {
        var item = ModItems.storageItemsById().get(
                ResourceLocation.parse("cruciblecraft:" + path));
        if (item == null) {
            throw new IllegalStateException("Missing storage item " + path);
        }
        return item.get();
    }

    private static void addMachineRecipes(RecipeOutput output) {
        machineCrafting(
                output, ModItems.ROTATIONAL_AXLE.get(), "rotational_axle");
        machineCrafting(
                output,
                ModItems.ROTATIONAL_GEARBOX.get(),
                "rotational_gearbox");
        addConverterRecipes(output);
        addLuFiberRecipe(output);
        addFusionPartRecipes(output);
        addFusionRecipes(output);
        addFusionExtensionRecipe(output);
        PuvOmegaRecipes.addAll(output);
        addLargeHeatExchangerRecipes(output);
        addBatteryCellRecipes(output);
        addBatteryRecipes(output);
        addTransformerRecipes(output);
        addHeatExchangerRecipes(output);
        addCoolerRecipes(output);
        addFluxConverterRecipes(output);
        addSteamTurbineRecipes(output);
        addLargeGasTurbineRecipes(output);
        addMteInPlaceAcquisitionRecipes(output);
        ExtruderShapeRecipes.addAll(output);
        addAutomaticHammerRecipes(output);
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
                        ModItems.MULTIBLOCK_FLUID_OUT_PORT.get())
                .pattern(" B ")
                .pattern("BCB")
                .pattern(" B ")
                .define('C', ModItems.MULTIBLOCK_CASING.get())
                .define('B', Items.BUCKET)
                .unlockedBy(
                        "has_multiblock_casing",
                        has(ModItems.MULTIBLOCK_CASING.get()))
                .save(output, id("machines/multiblock_fluid_out_port"));
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
        Map<String, Ingredient> mixerIngredients = new LinkedHashMap<>();
        mixerIngredients.put(
                "P",
                materialIngredient("stainless_steel", MaterialPrefixes.DENSE_PLATE));
        mixerIngredients.put(
                "S",
                materialIngredient("stainless_steel", MaterialPrefixes.LONG_ROD));
        mixerIngredients.put(
                "R",
                Ingredient.of(ModItems.technologicalPart("processor_crystal_ruby").get()));
        mixerIngredients.put(
                "M",
                Ingredient.of(ModItems.mteInPlaceItemsById()
                        .get(StainlessSteelMixerWalls.WALL_ID)
                        .get()));
        mixerIngredients.put("C", circuitIngredient("circuit_ultimate"));
        acceptShapedCatalyst(
                output,
                "machines/large_mixer",
                List.of("PSP", "PSP", "RMC"),
                mixerIngredients,
                Map.of(),
                new ItemStack(ModItems.LARGE_MIXER.get()));
        Map<String, Ingredient> electrolyzerIngredients = new LinkedHashMap<>();
        electrolyzerIngredients.put(
                "M",
                Ingredient.of(ModItems.mteInPlaceItemsById()
                        .get(ElectrolyzerParts.PART_ID)
                        .get()));
        electrolyzerIngredients.put(
                "R",
                Ingredient.of(ModItems.technologicalPart("processor_crystal_ruby").get()));
        electrolyzerIngredients.put("C", circuitIngredient("circuit_ultimate"));
        acceptShapedCatalyst(
                output,
                "machines/large_electrolyzer",
                List.of("CMC", "RCR"),
                electrolyzerIngredients,
                Map.of(),
                new ItemStack(ModItems.LARGE_ELECTROLYZER.get()));
        Map<String, Ingredient> ovenIngredients = new LinkedHashMap<>();
        ovenIngredients.put(
                "P",
                materialIngredient("invar", MaterialPrefixes.DENSE_PLATE));
        ovenIngredients.put(
                "R",
                Ingredient.of(ModItems.technologicalPart("processor_crystal_ruby").get()));
        ovenIngredients.put("C", circuitIngredient("circuit_ultimate"));
        ovenIngredients.put(
                "M",
                Ingredient.of(ModItems.mteInPlaceItemsById()
                        .get(InvarOvenWalls.WALL_ID)
                        .get()));
        acceptShapedCatalyst(
                output,
                "machines/large_oven",
                List.of("PPP", "PwP", "RMC"),
                ovenIngredients,
                Map.of("w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(ModItems.LARGE_OVEN.get()));
        Map<String, Ingredient> crusherIngredients = new LinkedHashMap<>();
        crusherIngredients.put(
                "G",
                materialIngredient("tungstensteel", MaterialPrefixes.GEAR));
        crusherIngredients.put(
                "S",
                materialIngredient("tungstensteel", MaterialPrefixes.SMALL_GEAR));
        crusherIngredients.put(
                "R",
                Ingredient.of(ModItems.technologicalPart("processor_crystal_ruby").get()));
        crusherIngredients.put(
                "M",
                Ingredient.of(ModItems.mteInPlaceItemsById()
                        .get(TungstensteelCrusherWalls.WALL_ID)
                        .get()));
        crusherIngredients.put("C", circuitIngredient("circuit_ultimate"));
        acceptShapedCatalyst(
                output,
                "machines/large_crusher",
                List.of("GSG", "SGS", "RMC"),
                crusherIngredients,
                Map.of(),
                new ItemStack(ModItems.LARGE_CRUSHER.get()));
        Map<String, Ingredient> shredderIngredients = new LinkedHashMap<>();
        shredderIngredients.put(
                "G",
                materialIngredient("tungstensteel", MaterialPrefixes.GEAR));
        shredderIngredients.put(
                "S",
                materialIngredient("tungstensteel", MaterialPrefixes.SMALL_GEAR));
        shredderIngredients.put(
                "R",
                Ingredient.of(ModItems.technologicalPart("processor_crystal_ruby").get()));
        shredderIngredients.put(
                "M",
                Ingredient.of(ModItems.mteInPlaceItemsById()
                        .get(TungstensteelCrusherWalls.WALL_ID)
                        .get()));
        shredderIngredients.put("C", circuitIngredient("circuit_ultimate"));
        acceptShapedCatalyst(
                output,
                "machines/large_shredder",
                List.of("SGS", "GSG", "RMC"),
                shredderIngredients,
                Map.of(),
                new ItemStack(ModItems.LARGE_SHREDDER.get()));
        Map<String, Ingredient> sluiceIngredients = new LinkedHashMap<>();
        sluiceIngredients.put(
                "G",
                materialIngredient("titanium", MaterialPrefixes.GEAR));
        sluiceIngredients.put(
                "S",
                materialIngredient("titanium", MaterialPrefixes.ROD));
        sluiceIngredients.put(
                "R",
                Ingredient.of(ModItems.technologicalPart(
                        "processor_crystal_ruby").get()));
        sluiceIngredients.put(
                "M",
                Ingredient.of(ModItems.mteInPlaceItemsById()
                        .get(SluiceWalls.WALL_ID)
                        .get()));
        sluiceIngredients.put("C", circuitIngredient("circuit_ultimate"));
        acceptShapedCatalyst(
                output,
                "machines/large_sluice",
                List.of("GGG", "SwS", "RMC"),
                sluiceIngredients,
                Map.of("w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(ModItems.LARGE_SLUICE.get()));
        Map<String, Ingredient> squeezerIngredients = new LinkedHashMap<>();
        squeezerIngredients.put("G", materialIngredient("steel", MaterialPrefixes.GEAR));
        squeezerIngredients.put("S", materialIngredient("steel", MaterialPrefixes.SMALL_GEAR));
        squeezerIngredients.put("R", Ingredient.of(ModItems.technologicalPart(
                "processor_crystal_ruby").get()));
        squeezerIngredients.put("M", Ingredient.of(ModItems.mteInPlaceItemsById()
                .get(ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "steel/wall")).get()));
        squeezerIngredients.put("C", circuitIngredient("circuit_ultimate"));
        acceptShapedCatalyst(
                output,
                "machines/large_squeezer",
                List.of("GSG", "GSG", "RMC"),
                squeezerIngredients,
                Map.of(),
                new ItemStack(ModItems.LARGE_SQUEEZER.get()));
        Map<String, Ingredient> bathIngredients = new LinkedHashMap<>();
        bathIngredients.put(
                "P",
                materialIngredient("stainless_steel", MaterialPrefixes.DENSE_PLATE));
        bathIngredients.put(
                "A",
                Ingredient.of(ModItems.technologicalPart(
                        "compact_electric_robot_arm_mv").get()));
        bathIngredients.put(
                "R",
                Ingredient.of(ModItems.technologicalPart("processor_crystal_ruby").get()));
        bathIngredients.put(
                "M",
                Ingredient.of(ModItems.mteInPlaceItemsById()
                        .get(StainlessSteelMixerWalls.WALL_ID)
                        .get()));
        bathIngredients.put("C", circuitIngredient("circuit_ultimate"));
        acceptShapedCatalyst(
                output,
                "machines/large_bath",
                List.of("CRC", "PMP", "APA"),
                bathIngredients,
                Map.of(),
                new ItemStack(ModItems.LARGE_BATH.get()));
        Map<String, Ingredient> coagulatorIngredients = new LinkedHashMap<>();
        coagulatorIngredients.put(
                "P",
                materialIngredient("stainless_steel", MaterialPrefixes.DENSE_PLATE));
        coagulatorIngredients.put(
                "R",
                Ingredient.of(ModItems.technologicalPart("processor_crystal_ruby").get()));
        coagulatorIngredients.put("C", circuitIngredient("circuit_ultimate"));
        coagulatorIngredients.put(
                "M",
                Ingredient.of(ModItems.mteInPlaceItemsById()
                        .get(StainlessSteelMixerWalls.WALL_ID)
                        .get()));
        acceptShapedCatalyst(
                output,
                "machines/large_coagulator",
                List.of("CRC", "PMP", "PPP"),
                coagulatorIngredients,
                Map.of(),
                new ItemStack(ModItems.LARGE_COAGULATOR.get()));
        Map<String, Ingredient> fermenterIngredients = new LinkedHashMap<>();
        fermenterIngredients.put(
                "P",
                materialIngredient("stainless_steel", MaterialPrefixes.DENSE_PLATE));
        fermenterIngredients.put(
                "R",
                Ingredient.of(ModItems.technologicalPart("processor_crystal_ruby").get()));
        fermenterIngredients.put("C", circuitIngredient("circuit_ultimate"));
        fermenterIngredients.put(
                "M",
                Ingredient.of(ModItems.mteInPlaceItemsById()
                        .get(StainlessSteelMixerWalls.WALL_ID)
                        .get()));
        acceptShapedCatalyst(
                output,
                "machines/large_fermenter",
                List.of("PPP", "CRC", "PMP"),
                fermenterIngredients,
                Map.of(),
                new ItemStack(ModItems.LARGE_FERMENTER.get()));
        Map<String, Ingredient> autoclaveIngredients = new LinkedHashMap<>();
        autoclaveIngredients.put(
                "P",
                materialIngredient("stainless_steel", MaterialPrefixes.DENSE_PLATE));
        autoclaveIngredients.put(
                "R",
                Ingredient.of(ModItems.technologicalPart("processor_crystal_ruby").get()));
        autoclaveIngredients.put("C", circuitIngredient("circuit_ultimate"));
        autoclaveIngredients.put(
                "M",
                Ingredient.of(ModItems.mteInPlaceItemsById()
                        .get(AutoclaveWalls.WALL_ID)
                        .get()));
        acceptShapedCatalyst(
                output,
                "machines/large_autoclave",
                List.of("CRC", "PMP", "PPP"),
                autoclaveIngredients,
                Map.of(),
                new ItemStack(ModItems.LARGE_AUTOCLAVE.get()));
        Map<String, Ingredient> implosionIngredients = new LinkedHashMap<>();
        implosionIngredients.put(
                "P",
                materialIngredient("tungstensteel", MaterialPrefixes.DENSE_PLATE));
        implosionIngredients.put(
                "A",
                Ingredient.of(ModItems.technologicalPart(
                        "compact_electric_robot_arm_mv").get()));
        implosionIngredients.put(
                "R",
                Ingredient.of(ModItems.technologicalPart("processor_crystal_ruby").get()));
        implosionIngredients.put("C", circuitIngredient("circuit_ultimate"));
        implosionIngredients.put(
                "M",
                Ingredient.of(ModItems.mteInPlaceItemsById()
                        .get(ImplosionCompressorWalls.WALL_ID)
                        .get()));
        acceptShapedCatalyst(
                output,
                "machines/implosion_compressor",
                List.of("CPC", "PAP", "RMR"),
                implosionIngredients,
                Map.of(),
                new ItemStack(ModItems.IMPLOSION_COMPRESSOR.get()));
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
        machineCoverRecipes(output);
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
        addFusionWallRecipe(
                output,
                "steel_galvanized",
                ModItems.GALVANIZED_STEEL_WALL.get(),
                "galvanized_steel_wall");
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
        addNonmetalPipeAcquisitionRecipes(output);
        addComboFluidPipeRecipes(output);
        addMetalFluidPipeTableRecipes(output);
        addMetalItemPipeTableRecipes(output);
        addRestrictiveItemPipeRecipes(output);
        addEuWireTableRecipes(output);
        addEuCableShapelessRecipes(output);
        addEuWirePackRecipes(output);
        addInsulatedRedstoneLaminatorRecipes(output);
        addWorkbenchToolRecipes(output);
        addDustPrefixPackRecipes(output);
        addNuggetPrefixPackRecipes(output);
        addPlatePrefixPackRecipes(output);
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
                                List.of(materialStack("coal_coke", MaterialPrefixes.GEM)),
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
        output.accept(
                id("anvil/rock_to_pulver_dust"),
                new MaterialRuleRecipe(MaterialChainRules.ANVIL_ROCK_TO_PULVER),
                null);
        List<String> smithable = List.of("material.tag(\"PROCESSING.SMITHABLE\")");
        output.accept(
                id("anvil_bend_big/plate_to_curved_plate"),
                materialRule(
                        ModRecipeMaps.ANVIL_BEND_BIG.id(),
                        MaterialPrefixes.PLATE,
                        MaterialPrefixes.CURVED_PLATE,
                        1, 1, 1, 10_000, 4, Map.of(), smithable),
                null);
        output.accept(
                id("anvil_bend_big/rod_to_small_spring"),
                materialRule(
                        ModRecipeMaps.ANVIL_BEND_BIG.id(),
                        MaterialPrefixes.ROD,
                        MaterialPrefixes.SMALL_SPRING,
                        1, 1, 1, 10_000, 3, Map.of(), smithable),
                null);
        output.accept(
                id("anvil_bend_big/long_rod_to_spring"),
                materialRule(
                        ModRecipeMaps.ANVIL_BEND_BIG.id(),
                        MaterialPrefixes.LONG_ROD,
                        MaterialPrefixes.SPRING,
                        1, 1, 1, 10_000, 4, Map.of(), smithable),
                null);
        output.accept(
                id("anvil_bend_small/plate_to_foils"),
                materialRule(
                        ModRecipeMaps.ANVIL_BEND_SMALL.id(),
                        MaterialPrefixes.PLATE,
                        MaterialPrefixes.FOIL,
                        1, 2, 1, 10_000, 4, Map.of(),
                        List.of(
                                "material.tag(\"PROCESSING.SMITHABLE\")",
                                "!material.tag(\"COMPOUNDS.COATED\")")),
                null);
        output.accept(
                id("anvil_bend_small/rod_to_ring"),
                materialRule(
                        ModRecipeMaps.ANVIL_BEND_SMALL.id(),
                        MaterialPrefixes.ROD,
                        MaterialPrefixes.RING,
                        1, 1, 1, 10_000, 3, Map.of(), smithable),
                null);
        List<String> smithableUncoated = List.of(
                "material.tag(\"PROCESSING.SMITHABLE\")",
                "!material.tag(\"COMPOUNDS.COATED\")");
        output.accept(
                id("welder/ingots_to_double_ingot"),
                materialRule(
                        ModRecipeMaps.WELDER.id(),
                        List.of(ruleItem(MaterialPrefixes.INGOT, 2)),
                        List.of(ruleItem(MaterialPrefixes.DOUBLE_INGOT, 1)),
                        160, 48, 0, smithableUncoated),
                null);
        output.accept(
                id("welder/rods_to_long_rod"),
                materialRule(
                        ModRecipeMaps.WELDER.id(),
                        List.of(ruleItem(MaterialPrefixes.ROD, 2)),
                        List.of(ruleItem(MaterialPrefixes.LONG_ROD, 1)),
                        80, 24, 0, smithable),
                null);
        output.accept(
                id("welder/bolts_to_rod"),
                materialRule(
                        ModRecipeMaps.WELDER.id(),
                        List.of(ruleItem(MaterialPrefixes.BOLT, 4)),
                        List.of(ruleItem(MaterialPrefixes.ROD, 1)),
                        64, 16, 0, smithable),
                null);
        output.accept(
                id("welder/bolts_to_long_rod"),
                materialRule(
                        ModRecipeMaps.WELDER.id(),
                        List.of(ruleItem(MaterialPrefixes.BOLT, 8)),
                        List.of(ruleItem(MaterialPrefixes.LONG_ROD, 1)),
                        80, 24, 0, smithable),
                null);
        output.accept(
                id("welder/small_casings_to_plate"),
                materialRule(
                        ModRecipeMaps.WELDER.id(),
                        List.of(ruleItem(
                                MaterialPrefixCatalog.require("small_casing"), 2)),
                        List.of(ruleItem(MaterialPrefixes.PLATE, 1)),
                        80, 24, 0, smithableUncoated),
                null);
        output.accept(
                id("welder/plates_and_long_rods_to_machine_casing"),
                materialRule(
                        ModRecipeMaps.WELDER.id(),
                        List.of(
                                ruleItem(MaterialPrefixes.PLATE, 6),
                                ruleItem(MaterialPrefixes.LONG_ROD, 2)),
                        List.of(ruleItem(MaterialPrefixes.MACHINE_CASING, 1)),
                        320, 64, 0, smithable),
                null);
        output.accept(
                id("welder/curved_plates_and_ring_to_rotor"),
                materialRule(
                        ModRecipeMaps.WELDER.id(),
                        List.of(
                                ruleItem(MaterialPrefixes.CURVED_PLATE, 4),
                                ruleItem(MaterialPrefixes.RING, 1)),
                        List.of(ruleItem(MaterialPrefixes.ROTOR, 1)),
                        300, 64, 0, smithable),
                null);
        output.accept(
                id("welder/ingots_to_triple_ingot"),
                materialRule(
                        ModRecipeMaps.WELDER.id(),
                        List.of(ruleItem(MaterialPrefixes.INGOT, 3)),
                        List.of(ruleItem(MaterialPrefixes.TRIPLE_INGOT, 1)),
                        240, 48, 0, smithableUncoated),
                null);
        output.accept(
                id("welder/ingots_to_quadruple_ingot"),
                materialRule(
                        ModRecipeMaps.WELDER.id(),
                        List.of(ruleItem(MaterialPrefixes.INGOT, 4)),
                        List.of(ruleItem(
                                MaterialPrefixCatalog.require("quadruple_ingot"), 1)),
                        320, 48, 0, smithableUncoated),
                null);
        output.accept(
                id("welder/ingots_to_quintuple_ingot"),
                materialRule(
                        ModRecipeMaps.WELDER.id(),
                        List.of(ruleItem(MaterialPrefixes.INGOT, 5)),
                        List.of(ruleItem(
                                MaterialPrefixCatalog.require("quintuple_ingot"), 1)),
                        400, 48, 0, smithableUncoated),
                null);
        output.accept(
                id("welder/ingots_to_block"),
                materialRule(
                        ModRecipeMaps.WELDER.id(),
                        List.of(ruleItem(MaterialPrefixes.INGOT, 9)),
                        List.of(ruleItem(MaterialPrefixes.BLOCK, 1)),
                        720, 48, 0, smithableUncoated),
                null);
        output.accept(
                id("welder/double_plates_and_long_rods_to_machine_casing_double"),
                materialRule(
                        ModRecipeMaps.WELDER.id(),
                        List.of(
                                ruleItem(MaterialPrefixes.DOUBLE_PLATE, 6),
                                ruleItem(MaterialPrefixes.LONG_ROD, 2)),
                        List.of(ruleItem(MaterialPrefixes.MACHINE_CASING_DOUBLE, 1)),
                        560, 64, 0, smithable),
                null);
        output.accept(
                id("welder/quadruple_plates_and_long_rods_to_machine_casing_quadruple"),
                materialRule(
                        ModRecipeMaps.WELDER.id(),
                        List.of(
                                ruleItem(MaterialPrefixes.QUADRUPLE_PLATE, 6),
                                ruleItem(MaterialPrefixes.LONG_ROD, 2)),
                        List.of(ruleItem(
                                MaterialPrefixes.MACHINE_CASING_QUADRUPLE, 1)),
                        1040, 64, 0, smithable),
                null);
        output.accept(
                id("welder/dense_plates_and_long_rods_to_machine_casing_dense"),
                materialRule(
                        ModRecipeMaps.WELDER.id(),
                        List.of(
                                ruleItem(MaterialPrefixes.DENSE_PLATE, 6),
                                ruleItem(MaterialPrefixes.LONG_ROD, 2)),
                        List.of(ruleItem(MaterialPrefixes.MACHINE_CASING_DENSE, 1)),
                        2240, 64, 0, smithable),
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
        if (com.masson.cruciblecraft.machine.processing.MachineTierCatalog
                .acquisitionBlocked(variant.id())) {
            return;
        }
        String path = variant.id().getPath();
        try {
            emitAcquisitionResolved(output, variant, path);
        } catch (RuntimeException exception) {
            if (isMissingSourceMachineComponent(exception)) {
                return;
            }
            if (compactIndexFromVariant(path) >= 0
                    && isSkippablePuvAcquisition(exception)) {
                return;
            }
            throw exception;
        }
    }

    private static boolean isMissingSourceMachineComponent(
            RuntimeException exception) {
        String message = exception.getMessage();
        return message != null
                && message.startsWith("Missing source machine component");
    }

    private static boolean isSkippablePuvAcquisition(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        return message.startsWith("Missing source machine component")
                || message.startsWith("No electrolyzer cable")
                || message.startsWith("No distillery wire")
                || message.startsWith("Missing electrolyzer conductor")
                || message.startsWith("No kinetic/heat casing")
                || message.startsWith("Missing registered item")
                || message.startsWith("Unknown technological part")
                || message.startsWith("Unknown compact-tier variant");
    }

    private static void emitAcquisitionResolved(
            RecipeOutput output,
            MachineVariant variant,
            String path) {
        var resolved = com.masson.cruciblecraft.machine.processing.MachineAcquisition.resolve(variant);
        Item result = ModItems.tieredProcessingItemsById()
                .get(variant.id())
                .get();
        String material = resolved.materialPath();
        Item casing = resolved.casingItem() == null
                ? null
                : resolveRegisteredItem(resolved.casingItem());
        switch (resolved.template()) {
            case "machine_generic" -> {
                if (isExplicitProcessingKind(variant.kind().id().getPath())) {
                    return;
                }
                machineCrafting(output, result, path);
            }
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
            // GT6 ANY.Iron includes steel; steel is the registered iron-family
            // member for the medium fluid pipe in CrucibleCraft.
            case "melter" -> melterCrafting(
                    output, result, casing, "steel", path);
            case "oven" -> ovenCrafting(
                    output, result, casing, path);
            case "laminator" -> laminatorCrafting(
                    output, result, casing, material, path);
            case "bath" -> bathCrafting(
                    output, result, casing, material, path);
            case "pressurewasher" -> pressureWasherCrafting(
                    output, result, casing, material, path);
            case "loom" -> loomCrafting(
                    output, result, casing, material, path);
            case "electricloom" -> electricLoomCrafting(
                    output, result, casing, material, path);
            case "injector" -> injectorCrafting(
                    output, result, casing, material, path);
            case "slicer" -> slicerCrafting(
                    output, result, casing, material, path);
            case "squeezer" -> squeezerCrafting(
                    output, result, casing, material, path);
            case "laser_engraver" -> laserEngraverCrafting(
                    output, result, casing, material, path);
            case "laser_welder" -> laserWelderCrafting(
                    output, result, casing, material, path);
            case "nanofab" -> nanofabCrafting(
                    output, result, casing, path);
            case "electrolyzer" -> electrolyzerCrafting(
                    output,
                    result,
                    casing,
                    resolved.electrolyzerCableMaterial(),
                    path);
            case "compressor" -> compressorCrafting(
                    output, result, casing, material, path);
            case "canner" -> cannerCrafting(output, result, casing, path);
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
                        materialIngredient(
                                material, MaterialPrefixes.QUINTUPLE_PLATE))
                .define(
                        'S',
                        materialIngredient(material, MaterialPrefixes.SPRING))
                .define(
                        'R',
                        materialIngredient(material, MaterialPrefixes.ROD))
                .define('C', casing)
                .unlockedBy("has_casing", has(casing))
                .save(output, id("machines/" + id));
    }

    private static void cannerCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String id) {
        Item pump = ModItems.compactElectricCover("compact_electric_pump_lv").get();
        Item circuit = ModItems.technologicalPart("circuit_basic").get();
        Item cable = materialItem("tin", MaterialPrefixes.CABLE);
        Item pipe = materialItem(
                "stainless_steel", MaterialPrefixes.TINY_FLUID_PIPE);
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put("P", Ingredient.of(pipe));
        ingredients.put("X", Ingredient.of(pump));
        ingredients.put("M", Ingredient.of(casing));
        ingredients.put("C", Ingredient.of(circuit));
        ingredients.put("W", keyedIngredient(cable, "tin", MaterialPrefixes.CABLE));
        // GT6 Loader_MultiTileEntities.java:1379 {"wPh","XMX","CPW"}.
        acceptShapedCatalyst(
                output,
                "machines/" + id,
                List.of("wPh", "XMX", "CPW"),
                ingredients,
                Map.of(
                        "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get()),
                        "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get())),
                new ItemStack(result));
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
                .define('G', keyedIngredient(gear, material, MaterialPrefixes.GEAR))
                .define(
                        'S',
                        keyedIngredient(
                                longRod, material, MaterialPrefixes.LONG_ROD))
                .define('C', casing)
                .unlockedBy("has_casing", has(casing))
                .save(output, id("machines/" + id));
    }

    private static void addWoodBeamRecipes(RecipeOutput output) {
        for (var beamId : WoodDebark.VANILLA_BEAM_COKE_INPUTS) {
            Item beam = WoodDebark.requireItem(beamId);
            acceptCokeOven(
                    output,
                    "coke_oven/" + beamId.getPath(),
                    Ingredient.of(beam),
                    WoodDebark.vanillaBeamCharcoal(),
                    WoodDebark.vanillaBeamCreosoteMb());
        }
        for (GtTreeSpecies species : GtTreeSpecies.ALL) {
            acceptCokeOven(
                    output,
                    "coke_oven/" + species.id() + "_beam",
                    Ingredient.of(ModItems.treeBeamItem(species).get()),
                    WoodDebark.beamCharcoal(species),
                    WoodDebark.beamCreosoteMb(species));
        }
        for (WoodDebark.VanillaPair pair : WoodDebark.VANILLA_PAIRS) {
            if (WoodDebark.GT6_PRESSURE_WASHER_VANILLA_LOGS.contains(pair.log())) {
                continue;
            }
            Item log = WoodDebark.requireItem(pair.log());
            Item beam = WoodDebark.requireItem(pair.beam());
            acceptPressureWasherDebark(
                    output,
                    "pressurewasher/" + pair.log().getPath(),
                    Ingredient.of(log),
                    new ItemStack(beam),
                    WoodDebark.barkDust(1));
        }
        for (GtTreeSpecies species : GtTreeSpecies.ALL) {
            acceptPressureWasherDebark(
                    output,
                    "pressurewasher/" + species.id() + "_log",
                    Ingredient.of(ModItems.treeLogItem(species).get()),
                    new ItemStack(ModItems.treeBeamItem(species).get()),
                    WoodDebark.pressureWasherBark(species));
        }
    }

    private static void acceptCokeOven(
            RecipeOutput output,
            String path,
            Ingredient input,
            int charcoal,
            int creosoteMb) {
        output.accept(
                id(path),
                new GTRecipeEntry(
                        ModRecipeMaps.COKE_OVEN.id(),
                        new GTRecipe(
                                List.of(input),
                                List.of(1),
                                List.of(new ItemStack(Items.CHARCOAL, charcoal)),
                                List.of(),
                                List.of(new FluidStack(
                                        ModFluids.CREOSOTE_SOURCE.get(),
                                        creosoteMb)),
                                List.of(GTRecipe.GUARANTEED_CHANCE),
                                WoodDebark.COKE_DURATION,
                                0L,
                                0L)),
                null);
    }

    private static void acceptPressureWasherDebark(
            RecipeOutput output,
            String path,
            Ingredient input,
            ItemStack beam,
            ItemStack bark) {
        output.accept(
                id(path),
                new GTRecipeEntry(
                        ModRecipeMaps.PRESSUREWASHER.id(),
                        new GTRecipe(
                                List.of(input),
                                List.of(1),
                                List.of(beam, bark),
                                List.of(new FluidStack(
                                        Fluids.WATER, WoodDebark.WASHER_WATER_MB)),
                                List.of(),
                                List.of(
                                        GTRecipe.GUARANTEED_CHANCE,
                                        GTRecipe.GUARANTEED_CHANCE),
                                WoodDebark.WASHER_DURATION,
                                WoodDebark.WASHER_EUT,
                                0L)),
                null);
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

    private static void addComboFluidPipeRecipes(RecipeOutput output) {
        for (PipeCatalog.Entry pipe : PipeCatalog.fluid()) {
            if (pipe.form().equals(MaterialPrefixes.QUADRUPLE_FLUID_PIPE)) {
                Item result = materialItem(
                        pipe.materialId(), pipe.form());
                Item medium = materialItem(
                        pipe.materialId(), MaterialPrefixes.FLUID_PIPE);
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                        .pattern("PP")
                        .pattern("PP")
                        .define('P', medium)
                        .unlockedBy("has_medium_pipe", has(medium))
                        .save(
                                output,
                                id("pipe/combo/"
                                        + pipe.materialId()
                                        + "/quadruple_fluid_pipe"));
                ShapelessRecipeBuilder.shapeless(
                                RecipeCategory.MISC, medium, 4)
                        .requires(result)
                        .unlockedBy("has_quadruple_pipe", has(result))
                        .save(
                                output,
                                id("pipe/combo/"
                                        + pipe.materialId()
                                        + "/unpack_quadruple_fluid_pipe"));
            } else if (pipe.form().equals(
                    MaterialPrefixes.NONUPLE_FLUID_PIPE)) {
                Item result = materialItem(
                        pipe.materialId(), pipe.form());
                Item small = materialItem(
                        pipe.materialId(), MaterialPrefixes.SMALL_FLUID_PIPE);
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                        .pattern("PPP")
                        .pattern("PPP")
                        .pattern("PPP")
                        .define('P', small)
                        .unlockedBy("has_small_pipe", has(small))
                        .save(
                                output,
                                id("pipe/combo/"
                                        + pipe.materialId()
                                        + "/nonuple_fluid_pipe"));
                ShapelessRecipeBuilder.shapeless(
                                RecipeCategory.MISC, small, 9)
                        .requires(result)
                        .unlockedBy("has_nonuple_pipe", has(result))
                        .save(
                                output,
                                id("pipe/combo/"
                                        + pipe.materialId()
                                        + "/unpack_nonuple_fluid_pipe"));
            }
        }
    }

    /**
     * GT6 five-gauge fluid-pipe table crafts. Operands are
     * {@code OP.plateCurved} / {@code OP.plateDouble} plus tool catalysts.
     * Missing curved or double plates stay blocked; flat {@code plate} is
     * never substituted.
     */
    private static void addMetalFluidPipeTableRecipes(RecipeOutput output) {
        for (PipeCatalog.Entry pipe : PipeCatalog.fluid()) {
            List<String> pattern = fluidTablePattern(pipe.form());
            if (pattern == null) {
                continue;
            }
            if (!PipeCatalog.recipeEnabled(
                    MaterialCatalog.require(pipe.materialId()),
                    PipeCatalog.Kind.FLUID,
                    pipe.sourceSpecification())) {
                continue;
            }
            MaterialPrefix plateForm = pipe.form().equals(
                    MaterialPrefixes.HUGE_FLUID_PIPE)
                    ? MaterialPrefixes.DOUBLE_PLATE
                    : MaterialPrefixes.CURVED_PLATE;
            if (!ModItems.hasMaterialItem(pipe.materialId(), plateForm)
                    || !ModItems.hasMaterialItem(
                            pipe.materialId(), pipe.form())) {
                continue;
            }
            Item plate = materialItem(pipe.materialId(), plateForm);
            Item result = materialItem(pipe.materialId(), pipe.form());
            Map<String, Ingredient> ingredients = new LinkedHashMap<>();
            ingredients.put("P", keyedIngredient(plate, pipe.materialId(), plateForm));
            Map<String, Ingredient> catalysts = new LinkedHashMap<>();
            String joined = String.join("", pattern);
            if (joined.indexOf('s') >= 0) {
                catalysts.put(
                        "s", CraftingTools.of(ModItems.MATERIAL_SAW.get()));
            }
            catalysts.put(
                    "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get()));
            catalysts.put(
                    "z", CraftingTools.of(ModItems.MATERIAL_FILE.get()));
            catalysts.put(
                    "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get()));
            acceptShapedCatalyst(
                    output,
                    "pipe/table/"
                            + pipe.materialId()
                            + "/"
                            + pipe.form().serializedName(),
                    pattern,
                    ingredients,
                    catalysts,
                    keyedStack(result, pipe.materialId(), pipe.form(), 1));
        }
    }

    private static List<String> fluidTablePattern(MaterialPrefix form) {
        if (form.equals(MaterialPrefixes.TINY_FLUID_PIPE)) {
            return List.of("sP ", "wzh");
        }
        if (form.equals(MaterialPrefixes.SMALL_FLUID_PIPE)) {
            return List.of(" P ", "wzh");
        }
        if (form.equals(MaterialPrefixes.FLUID_PIPE)) {
            return List.of("PPP", "wzh");
        }
        if (form.equals(MaterialPrefixes.LARGE_FLUID_PIPE)
                || form.equals(MaterialPrefixes.HUGE_FLUID_PIPE)) {
            return List.of("PPP", "wzh", "PPP");
        }
        return null;
    }

    /**
     * GT6 ordinary item-pipe table crafts. Medium/large use
     * {@code OP.plateCurved}; huge uses {@code OP.plateDouble}. Flat
     * {@code plate} is never substituted.
     */
    private static void addMetalItemPipeTableRecipes(RecipeOutput output) {
        for (PipeCatalog.Entry pipe : PipeCatalog.item()) {
            List<String> pattern = itemTablePattern(pipe.form());
            if (pattern == null) {
                continue;
            }
            if (pipe.item() == null || !pipe.item().recipe()) {
                continue;
            }
            MaterialPrefix plateForm = pipe.form().equals(
                    MaterialPrefixes.HUGE_ITEM_PIPE)
                    ? MaterialPrefixes.DOUBLE_PLATE
                    : MaterialPrefixes.CURVED_PLATE;
            if (!ModItems.hasMaterialItem(pipe.materialId(), plateForm)
                    || !ModItems.hasMaterialItem(
                            pipe.materialId(), pipe.form())) {
                continue;
            }
            Item plate = materialItem(pipe.materialId(), plateForm);
            Item result = materialItem(pipe.materialId(), pipe.form());
            Map<String, Ingredient> ingredients = new LinkedHashMap<>();
            ingredients.put("P", keyedIngredient(plate, pipe.materialId(), plateForm));
            Map<String, Ingredient> catalysts = new LinkedHashMap<>();
            catalysts.put(
                    "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get()));
            catalysts.put(
                    "z", CraftingTools.of(ModItems.MATERIAL_FILE.get()));
            catalysts.put(
                    "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get()));
            acceptShapedCatalyst(
                    output,
                    "pipe/item_table/"
                            + pipe.materialId()
                            + "/"
                            + pipe.form().serializedName(),
                    pattern,
                    ingredients,
                    catalysts,
                    keyedStack(result, pipe.materialId(), pipe.form(), 1));
        }
    }

    private static List<String> itemTablePattern(MaterialPrefix form) {
        if (form.equals(MaterialPrefixes.ITEM_PIPE)) {
            return List.of("PPP", "wzh");
        }
        if (form.equals(MaterialPrefixes.LARGE_ITEM_PIPE)
                || form.equals(MaterialPrefixes.HUGE_ITEM_PIPE)) {
            return List.of("PPP", "wzh", "PPP");
        }
        return null;
    }

    /**
     * GT6 restrictive item-pipe crafts. {@code P} is the matching ordinary
     * gauge; {@code R} is {@code OP.ring.dat(ANY.Steel)} which resolves to
     * live {@code steel/ring}. Do not invent a different ring.
     */
    private static void addRestrictiveItemPipeRecipes(RecipeOutput output) {
        if (!ModItems.hasMaterialItem("steel", MaterialPrefixes.RING)) {
            return;
        }
        Item steelRing = materialItem("steel", MaterialPrefixes.RING);
        for (PipeCatalog.Entry pipe : PipeCatalog.item()) {
            List<String> pattern = restrictivePattern(pipe.form());
            if (pattern == null) {
                continue;
            }
            if (pipe.item() == null || !pipe.item().recipe()) {
                continue;
            }
            MaterialPrefix sourceForm = ordinaryItemForm(pipe.form());
            if (!ModItems.hasMaterialItem(pipe.materialId(), sourceForm)
                    || !ModItems.hasMaterialItem(
                            pipe.materialId(), pipe.form())) {
                continue;
            }
            Item source = materialItem(pipe.materialId(), sourceForm);
            Item result = materialItem(pipe.materialId(), pipe.form());
            Map<String, Ingredient> ingredients = new LinkedHashMap<>();
            ingredients.put("P", keyedIngredient(source, pipe.materialId(), sourceForm));
            ingredients.put("R", keyedIngredient(steelRing, "steel", MaterialPrefixes.RING));
            Map<String, Ingredient> catalysts = new LinkedHashMap<>();
            catalysts.put(
                    "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get()));
            acceptShapedCatalyst(
                    output,
                    "pipe/restrictive/"
                            + pipe.materialId()
                            + "/"
                            + pipe.form().serializedName(),
                    pattern,
                    ingredients,
                    catalysts,
                    keyedStack(result, pipe.materialId(), pipe.form(), 1));
        }
    }

    private static List<String> restrictivePattern(MaterialPrefix form) {
        if (form.equals(MaterialPrefixes.RESTRICTIVE_ITEM_PIPE)) {
            return List.of(" h ", "RPR", " R ");
        }
        if (form.equals(MaterialPrefixes.LARGE_RESTRICTIVE_ITEM_PIPE)) {
            return List.of("hR ", "RPR", " R ");
        }
        if (form.equals(MaterialPrefixes.HUGE_RESTRICTIVE_ITEM_PIPE)) {
            return List.of(" h ", "RPR", "RRR");
        }
        return null;
    }

    private static MaterialPrefix ordinaryItemForm(MaterialPrefix form) {
        if (form.equals(MaterialPrefixes.RESTRICTIVE_ITEM_PIPE)) {
            return MaterialPrefixes.ITEM_PIPE;
        }
        if (form.equals(MaterialPrefixes.LARGE_RESTRICTIVE_ITEM_PIPE)) {
            return MaterialPrefixes.LARGE_ITEM_PIPE;
        }
        if (form.equals(MaterialPrefixes.HUGE_RESTRICTIVE_ITEM_PIPE)) {
            return MaterialPrefixes.HUGE_ITEM_PIPE;
        }
        return form;
    }

    /**
     * GT6 {@code OreProcessing_CraftFrom} plate2wire: {@code "Px"} with
     * {@code P=plate} and {@code x=wirecutter}. Only live EU
     * {@code wireGt01} hosts. Red alloy / Signalum / Lumium stay out of
     * {@link ElectricalConductorCatalog}.
     */
    private static void addEuWireTableRecipes(RecipeOutput output) {
        if (!ElectricalConductorCatalog.isInitialized()) {
            return;
        }
        for (ElectricalConductorCatalog.Entry wire :
                ElectricalConductorCatalog.wires()) {
            if (!wire.form().equals(MaterialPrefixes.WIRE)) {
                continue;
            }
            if (!ModItems.hasMaterialItem(
                    wire.materialId(), MaterialPrefixes.PLATE)
                    || !ModItems.hasMaterialItem(
                            wire.materialId(), MaterialPrefixes.WIRE)) {
                continue;
            }
            Item plate = materialItem(
                    wire.materialId(), MaterialPrefixes.PLATE);
            Item result = materialItem(
                    wire.materialId(), MaterialPrefixes.WIRE);
            Map<String, Ingredient> ingredients = new LinkedHashMap<>();
            ingredients.put("P", keyedIngredient(
                    plate, wire.materialId(), MaterialPrefixes.PLATE));
            Map<String, Ingredient> catalysts = new LinkedHashMap<>();
            catalysts.put(
                    "x",
                    CraftingTools.of(ModItems.MATERIAL_WIRE_CUTTER.get()));
            acceptShapedCatalyst(
                    output,
                    "cable/table/"
                            + wire.materialId()
                            + "/wire",
                    List.of("Px ", "   "),
                    ingredients,
                    catalysts,
                    keyedStack(
                            result,
                            wire.materialId(),
                            MaterialPrefixes.WIRE,
                            1));
        }
    }

    /**
     * GT6 shapeless {@code cableGt01/02 = wireGt01/02 + plate.dat(ANY.Rubber)}.
     * Live rubber is a shared inventory plate, so this is a component
     * ingredient rather than {@code #c:plates/rubber}.
     */
    private static void addEuCableShapelessRecipes(RecipeOutput output) {
        if (!ElectricalConductorCatalog.isInitialized()
                || !ModItems.hasMaterialItem(
                        "rubber", MaterialPrefixes.PLATE)) {
            return;
        }
        for (ElectricalConductorCatalog.Entry cable :
                ElectricalConductorCatalog.cables()) {
            MaterialPrefix wireForm = cableWireForm(cable.form());
            if (wireForm == null) {
                continue;
            }
            if (!ElectricalConductorCatalog.contains(
                    cable.materialId(), wireForm)
                    || !ModItems.hasMaterialItem(
                            cable.materialId(), wireForm)
                    || !ModItems.hasMaterialItem(
                            cable.materialId(), cable.form())) {
                continue;
            }
            Item wire = materialItem(cable.materialId(), wireForm);
            Item result = materialItem(cable.materialId(), cable.form());
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result)
                    .requires(wire)
                    .requires(materialIngredient("rubber", MaterialPrefixes.PLATE))
                    .unlockedBy("has_wire", has(wire))
                    .save(
                            output,
                            id("cable/shapeless/"
                                    + cable.materialId()
                                    + "/"
                                    + cable.form().serializedName()));
        }
    }

    /**
     * GT6 {@code AdvancedCraftingXToY} / {@code AdvancedCrafting1ToY} wire
     * packing. Pack only when {@code tAmount < 10}; unpack always.
     */
    private static void addEuWirePackRecipes(RecipeOutput output) {
        if (!ElectricalConductorCatalog.isInitialized()) {
            return;
        }
        Set<String> materials = new LinkedHashSet<>();
        for (ElectricalConductorCatalog.Entry wire :
                ElectricalConductorCatalog.wires()) {
            materials.add(wire.materialId());
        }
        for (String material : materials) {
            for (int big = 1; big <= 16; big++) {
                for (int small = 1; small < big; small++) {
                    if (big % small != 0) {
                        continue;
                    }
                    MaterialPrefix bigForm = wireGaugeForm(big);
                    MaterialPrefix smallForm = wireGaugeForm(small);
                    if (!ElectricalConductorCatalog.contains(material, bigForm)
                            || !ElectricalConductorCatalog.contains(
                                    material, smallForm)
                            || !ModItems.hasMaterialItem(material, bigForm)
                            || !ModItems.hasMaterialItem(
                                    material, smallForm)) {
                        continue;
                    }
                    int amount = big / small;
                    Item bigItem = materialItem(material, bigForm);
                    Item smallItem = materialItem(material, smallForm);
                    ShapelessRecipeBuilder.shapeless(
                                    RecipeCategory.MISC, smallItem, amount)
                            .requires(bigItem)
                            .unlockedBy("has_wire", has(bigItem))
                            .save(
                                    output,
                                    id("cable/unpack/"
                                            + material
                                            + "/"
                                            + smallForm.serializedName()
                                            + "_from_"
                                            + bigForm.serializedName()));
                    if (amount >= 10) {
                        continue;
                    }
                    ShapelessRecipeBuilder pack =
                            ShapelessRecipeBuilder.shapeless(
                                    RecipeCategory.MISC, bigItem);
                    for (int index = 0; index < amount; index++) {
                        pack.requires(smallItem);
                    }
                    pack.unlockedBy("has_wire", has(smallItem))
                            .save(
                                    output,
                                    id("cable/pack/"
                                            + material
                                            + "/"
                                            + bigForm.serializedName()
                                            + "_from_"
                                            + smallForm.serializedName()));
                }
            }
        }
    }

    private static MaterialPrefix cableWireForm(MaterialPrefix cableForm) {
        if (cableForm.equals(MaterialPrefixes.CABLE)) {
            return MaterialPrefixes.WIRE;
        }
        if (cableForm.equals(MaterialPrefixes.DOUBLE_CABLE)) {
            return MaterialPrefixes.DOUBLE_WIRE;
        }
        return null;
    }

    private static MaterialPrefix wireGaugeForm(int gauge) {
        return switch (gauge) {
            case 1 -> MaterialPrefixes.WIRE;
            case 2 -> MaterialPrefixes.DOUBLE_WIRE;
            case 3 -> MaterialPrefixes.TRIPLE_WIRE;
            case 4 -> MaterialPrefixes.QUADRUPLE_WIRE;
            case 5 -> MaterialPrefixes.QUINTUPLE_WIRE;
            case 6 -> MaterialPrefixes.SEXTUPLE_WIRE;
            case 7 -> MaterialPrefixes.SEPTUPLE_WIRE;
            case 8 -> MaterialPrefixes.OCTUPLE_WIRE;
            case 9 -> MaterialPrefixes.NONUPLE_WIRE;
            case 10 -> MaterialPrefixes.DECUPLE_WIRE;
            case 11 -> MaterialPrefixes.UNDECUPLE_WIRE;
            case 12 -> MaterialPrefixes.DODECUPLE_WIRE;
            case 13 -> MaterialPrefixes.TREDECUPLE_WIRE;
            case 14 -> MaterialPrefixes.TETRADECUPLE_WIRE;
            case 15 -> MaterialPrefixes.PENTADECUPLE_WIRE;
            case 16 -> MaterialPrefixes.HEXADECUPLE_WIRE;
            default -> throw new IllegalArgumentException(
                    "unsupported wire gauge " + gauge);
        };
    }

    /**
     * GT6 {@code RM.Laminator.addRecipe2} for insulated redstone
     * 27006/27056/27506. Plate or four foils of {@code ANY.Rubber}
     * (live member {@code rubber}) plus the matching bare wire. Not EU
     * and not {@code tin/cable}.
     */
    private static void addInsulatedRedstoneLaminatorRecipes(
            RecipeOutput output) {
        if (!ModItems.hasMaterialItem("rubber", MaterialPrefixes.PLATE)) {
            return;
        }
        Item rubberPlate = materialItem("rubber", MaterialPrefixes.PLATE);
        Item rubberFoil = ModItems.hasMaterialItem(
                "rubber", MaterialPrefixes.FOIL)
                ? materialItem("rubber", MaterialPrefixes.FOIL)
                : null;
        for (RedstoneWireKind cable : RedstoneWireKind.insulatedKinds()) {
            if (!ModItems.hasMaterialItem(
                    cable.materialId(), MaterialPrefixes.WIRE)
                    || !ModItems.hasMaterialItem(
                            cable.materialId(), MaterialPrefixes.CABLE)) {
                continue;
            }
            Item wire = materialItem(
                    cable.materialId(), MaterialPrefixes.WIRE);
            Item result = materialItem(
                    cable.materialId(), MaterialPrefixes.CABLE);
            output.accept(
                    id("redstone/laminator/"
                            + cable.materialId()
                            + "/cable_from_plate"),
                    new GTRecipeEntry(
                            ModRecipeMaps.LAMINATOR.id(),
                            new GTRecipe(
                                    List.of(
                                            keyedIngredient(rubberPlate, "rubber", MaterialPrefixes.PLATE),
                                            keyedIngredient(
                                                    wire,
                                                    cable.materialId(),
                                                    MaterialPrefixes.WIRE)),
                                    List.of(1, 1),
                                    List.of(keyedStack(result, cable.materialId(), cable.form(), 1)),
                                    List.of(),
                                    List.of(),
                                    List.of(GTRecipe.GUARANTEED_CHANCE),
                                    16,
                                    16L,
                                    0L)),
                    null);
            if (rubberFoil == null) {
                continue;
            }
            output.accept(
                    id("redstone/laminator/"
                            + cable.materialId()
                            + "/cable_from_foil"),
                    new GTRecipeEntry(
                            ModRecipeMaps.LAMINATOR.id(),
                            new GTRecipe(
                                    List.of(
                                            Ingredient.of(rubberFoil),
                                            keyedIngredient(
                                                    wire,
                                                    cable.materialId(),
                                                    MaterialPrefixes.WIRE)),
                                    List.of(4, 1),
                                    List.of(keyedStack(result, cable.materialId(), cable.form(), 1)),
                                    List.of(),
                                    List.of(),
                                    List.of(GTRecipe.GUARANTEED_CHANCE),
                                    16,
                                    16L,
                                    0L)),
                    null);
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
            case WOODEN_BEAMS -> {
                builder.define(symbol, ModItemTags.WOODEN_BEAMS);
                yield Items.STRIPPED_OAK_LOG;
            }
            case CARBON_DUST -> {
                Item item = materialItem("carbon", MaterialPrefixes.DUST);
                builder.define(
                        symbol,
                        keyedIngredient(item, "carbon", MaterialPrefixes.DUST));
                yield item;
            }
            case PLASTIC_PLATE -> {
                Item item = materialItem("plastic", MaterialPrefixes.PLATE);
                builder.define(
                        symbol,
                        keyedIngredient(
                                item, "plastic", MaterialPrefixes.PLATE));
                yield item;
            }
            case RUBBER_PLATE -> {
                Item item = materialItem("rubber", MaterialPrefixes.PLATE);
                builder.define(
                        symbol,
                        keyedIngredient(item, "rubber", MaterialPrefixes.PLATE));
                yield item;
            }
            case COAL_COKE -> {
                Item item = materialItem("coal_coke", MaterialPrefixes.GEM);
                builder.define(
                        symbol,
                        keyedIngredient(
                                item, "coal_coke", MaterialPrefixes.GEM));
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
                .define(
                        'W',
                        keyedIngredient(
                                wire, material, MaterialPrefixes.FINE_WIRE))
                .define(
                        'R',
                        keyedIngredient(rod, material, MaterialPrefixes.ROD))
                .define(
                        'S',
                        keyedIngredient(
                                spring, material, MaterialPrefixes.SPRING))
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
        MaterialPrefix conductorForm = MaterialLookup.item(
                        cableMaterial, MaterialPrefixes.CABLE)
                .isPresent()
                ? MaterialPrefixes.CABLE
                : MaterialPrefixes.WIRE;
        Item cable = MaterialLookup.item(cableMaterial, conductorForm)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing electrolyzer conductor "
                                + cableMaterial));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("SMS")
                .pattern("W W")
                .define(
                        'S',
                        keyedIngredient(
                                platinumWire,
                                "platinum",
                                MaterialPrefixes.WIRE))
                .define('M', casing)
                .define(
                        'W',
                        keyedIngredient(cable, cableMaterial, conductorForm))
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
        if ("clustermill".equals(kind)) {
            Item smallGear = materialItem(material, MaterialPrefixes.SMALL_GEAR);
            Item quadruple = materialItem(
                    material, MaterialPrefixes.MACHINE_CASING_QUADRUPLE);
            Map<String, Ingredient> ingredients = new LinkedHashMap<>();
            ingredients.put("S", keyedIngredient(smallGear, material, MaterialPrefixes.SMALL_GEAR));
            ingredients.put("G", keyedIngredient(gear, material, MaterialPrefixes.GEAR));
            ingredients.put("M", Ingredient.of(quadruple));
            // GT6 Loader_MultiTileEntities.java:1367 {"SSS","wGh","SMS"}.
            acceptShapedCatalyst(
                    output,
                    "machines/" + id,
                    List.of("SSS", "wGh", "SMS"),
                    ingredients,
                    Map.of(
                            "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get()),
                            "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get())),
                    new ItemStack(result));
            return;
        }
        if ("rollformer".equals(kind)) {
            Map<String, Ingredient> ingredients = new LinkedHashMap<>();
            ingredients.put("G", keyedIngredient(gear, material, MaterialPrefixes.GEAR));
            ingredients.put("M", Ingredient.of(casing));
            // GT6 Loader_MultiTileEntities.java:1361 {"wG ","GMG"," Gh"}.
            acceptShapedCatalyst(
                    output,
                    "machines/" + id,
                    List.of("wG ", "GMG", " Gh"),
                    ingredients,
                    Map.of(
                            "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get()),
                            "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get())),
                    new ItemStack(result));
            return;
        }
        if ("sanding".equals(kind)) {
            Item smallGear = materialItem(material, MaterialPrefixes.SMALL_GEAR);
            Map<String, Ingredient> ingredients = new LinkedHashMap<>();
            ingredients.put("S", keyedIngredient(smallGear, material, MaterialPrefixes.SMALL_GEAR));
            ingredients.put("G", keyedIngredient(gear, material, MaterialPrefixes.GEAR));
            ingredients.put("X", Ingredient.of(Items.SANDSTONE));
            ingredients.put("M", Ingredient.of(casing));
            // GT6 Loader_MultiTileEntities.java:1589 {"SGS","XXX","wMh"}.
            acceptShapedCatalyst(
                    output,
                    "machines/" + id,
                    List.of("SGS", "XXX", "wMh"),
                    ingredients,
                    Map.of(
                            "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get()),
                            "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get())),
                    new ItemStack(result));
            return;
        }
        ShapedRecipeBuilder builder =
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                        .define('C', casing)
                        .unlockedBy("has_casing", has(casing));
        if (!kind.equals("press")) {
            builder.define(
                    'G', keyedIngredient(gear, material, MaterialPrefixes.GEAR));
        }
        switch (kind) {
            case "lathe" -> builder
                    .pattern("TDS")
                    .pattern(" CG")
                    .define(
                            'T',
                            materialIngredient(
                                    material,
                                    MaterialPrefixes.SCREW))
                    .define(
                            'D',
                            materialIngredient(
                                    "diamond",
                                    MaterialPrefixes.GEM))
                    .define(
                            'S',
                            materialIngredient(
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
                            materialIngredient(
                                    material,
                                    MaterialPrefixes.SMALL_GEAR));
            case "shredder", "cutter" -> builder
                    .pattern("GDG")
                    .pattern(" C ")
                    .define(
                            'D',
                            materialIngredient(
                                    "diamond",
                                    MaterialPrefixes.GEM));
            case "press" -> builder
                    .pattern("RS")
                    .pattern("PC")
                    .pattern("P ")
                    .define(
                            'P',
                            materialIngredient(
                                    material,
                                    MaterialPrefixes.DOUBLE_PLATE))
                    .define(
                            'R',
                            materialIngredient(
                                    material,
                                    MaterialPrefixes.ROD))
                    .define(
                            'S',
                            materialIngredient(
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
                        .define(
                                'P',
                                keyedIngredient(
                                        tierPlate,
                                        machineMaterial,
                                        MaterialPrefixes.PLATE))
                        .define(
                                'C',
                                keyedIngredient(
                                        copperDoublePlate,
                                        "copper",
                                        MaterialPrefixes.DOUBLE_PLATE))
                        .unlockedBy("has_casing", has(casing));
        switch (kind) {
            case "distillery" -> builder
                    .pattern("GPG")
                    .pattern("WMW")
                    .pattern(" C ")
                    .define('G', Items.GLASS)
                    .define(
                            'W',
                            materialIngredient(
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
                    .define('U', ModItemTags.SMELTING_CRUCIBLES)
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
                            materialIngredient(
                                    machineMaterial,
                                    MaterialPrefixes.ROD));
            default -> throw new IllegalArgumentException(
                    "Unsupported heat machine kind " + kind);
        }
        builder.save(output, id("machines/" + id));
    }

    private static void ovenCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String id) {
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put("M", Ingredient.of(casing));
        ingredients.put("C", materialIngredient("copper", MaterialPrefixes.DOUBLE_PLATE));
        ingredients.put("B", Ingredient.of(Items.BRICKS));
        acceptShapedCatalyst(
                output,
                "machines/" + id,
                List.of("wMh", "BCB"),
                ingredients,
                Map.of(
                        "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get()),
                        "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get())),
                new ItemStack(result));
    }

    private static void melterCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String material,
            String id) {
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put("M", Ingredient.of(casing));
        ingredients.put("P", materialIngredient(material, MaterialPrefixes.FLUID_PIPE));
        ingredients.put("C", materialIngredient("copper", MaterialPrefixes.DOUBLE_PLATE));
        ingredients.put("B", Ingredient.of(Items.BRICKS));
        ingredients.put("U", Ingredient.of(ModItemTags.SMELTING_CRUCIBLES));
        acceptShapedCatalyst(
                output,
                "machines/" + id,
                List.of("wUh", "PMP", "BCB"),
                ingredients,
                Map.of(
                        "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get()),
                        "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get())),
                new ItemStack(result));
    }

    private static void laminatorCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String material,
            String id) {
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put(
                "G",
                materialIngredient(material, MaterialPrefixes.SMALL_GEAR));
        ingredients.put(
                "S",
                materialIngredient(material, MaterialPrefixes.ROD));
        ingredients.put(
                "M",
                Ingredient.of(casing));
        ingredients.put(
                "C",
                materialIngredient("copper", MaterialPrefixes.DOUBLE_PLATE));
        // GT6 Loader_MultiTileEntities.java:1532-1535 {"SwS","GMG","SCS"}.
        acceptShapedCatalyst(
                output,
                "machines/" + id,
                List.of("SwS", "GMG", "SCS"),
                ingredients,
                Map.of("w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static void bathCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String material,
            String id) {
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put("M", Ingredient.of(casing));
        ingredients.put(
                "C",
                materialIngredient(
                        material, MaterialPrefixCatalog.require("small_casing")));
        ingredients.put("P", materialIngredient(material, MaterialPrefixes.PLATE));
        // GT6 Loader_MultiTileEntities.java:1653 {"CwC","PMP","PPP"}.
        acceptShapedCatalyst(
                output,
                "machines/" + id,
                List.of("CwC", "PMP", "PPP"),
                ingredients,
                Map.of("w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static void pressureWasherCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String material,
            String id) {
        MaterialPrefix pipePrefix = switch (id) {
            case "pressurewasher" -> MaterialPrefixes.SMALL_FLUID_PIPE;
            case "steel_pressurewasher" -> MaterialPrefixes.FLUID_PIPE;
            case "titanium_pressurewasher" -> MaterialPrefixes.LARGE_FLUID_PIPE;
            case "tungstensteel_pressurewasher" ->
                    MaterialPrefixes.HUGE_FLUID_PIPE;
            default -> compactIndexOrThrow(id) >= 0
                    ? MaterialPrefixes.HUGE_FLUID_PIPE
                    : MaterialPrefixes.SMALL_FLUID_PIPE;
        };
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put(
                "R",
                materialIngredient("stainless_steel", MaterialPrefixes.ROTOR));
        ingredients.put(
                "P",
                materialIngredient("stainless_steel", pipePrefix));
        ingredients.put(
                "G",
                materialIngredient(material, MaterialPrefixes.SMALL_GEAR));
        ingredients.put("M", Ingredient.of(casing));
        // GT6 Loader_MultiTileEntities.java:1615-1618 {"RPG","wMG"}.
        acceptShapedCatalyst(
                output,
                "machines/" + id,
                List.of("RPG", "wMG", "   "),
                ingredients,
                Map.of("w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static void loomCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String material,
            String id) {
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put("S", materialIngredient(material, MaterialPrefixes.LONG_ROD));
        ingredients.put("G", materialIngredient(material, MaterialPrefixes.GEAR));
        ingredients.put("M", Ingredient.of(casing));
        // GT6 Loader_MultiTileEntities.java:1412-1415 {"ShS","GMG","SwS"}.
        acceptShapedCatalyst(
                output,
                "machines/" + id,
                List.of("ShS", "GMG", "SwS"),
                ingredients,
                Map.of(
                        "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get()),
                        "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static void electricLoomCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String material,
            String id) {
        String motorPath = switch (id) {
            case "electricloom" -> "compact_electric_motor_lv";
            case "aluminium_electricloom" -> "compact_electric_motor_mv";
            case "stainless_steel_electricloom" -> "compact_electric_motor_hv";
            case "chromium_electricloom" -> "compact_electric_motor_ev";
            case "titanium_electricloom" -> "compact_electric_motor_iv";
            default -> compactPartOrThrow(id, "compact_electric_motor");
        };
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put("S", materialIngredient(material, MaterialPrefixes.LONG_ROD));
        ingredients.put("G", Ingredient.of(
                ModItems.technologicalPart(motorPath).get()));
        ingredients.put("M", Ingredient.of(casing));
        // GT6 electric Loom uses the kinetic grid with IL.MOTORS in G.
        acceptShapedCatalyst(
                output,
                "machines/" + id,
                List.of("ShS", "GMG", "SwS"),
                ingredients,
                Map.of(
                        "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get()),
                        "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static void injectorCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String material,
            String id) {
        int tier = switch (id) {
            case "injector" -> 1;
            case "aluminium_injector" -> 2;
            case "stainless_steel_injector" -> 3;
            case "chromium_injector" -> 4;
            case "titanium_injector" -> 5;
            default -> compactIndexOrThrow(id);
        };
        String pipe = switch (Math.min(tier, 5)) {
            case 1 -> "tiny_fluid_pipe";
            case 2 -> "small_fluid_pipe";
            case 3 -> "fluid_pipe";
            case 4 -> "large_fluid_pipe";
            default -> "huge_fluid_pipe";
        };
        MaterialPrefix cablePrefix = tier <= 5
                ? MaterialPrefixes.CABLE
                : MaterialPrefixes.WIRE;
        String cable = switch (tier) {
            case 1 -> "tin";
            case 2 -> "copper";
            case 3 -> "gold";
            case 4 -> "aluminium";
            case 5 -> "platinum";
            default -> tier <= 10 ? "graphene" : "superconductor";
        };
        Item piston = technologicalCompact("compact_electric_piston", tier);
        if (piston == null) {
            return;
        }
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put("X", Ingredient.of(piston));
        ingredients.put("P", materialIngredient("stainless_steel", MaterialPrefixCatalog.require(pipe)));
        ingredients.put("C", Ingredient.of(ModItems.technologicalPart(
                circuitPath(tier)).get()));
        ingredients.put("M", Ingredient.of(casing));
        ingredients.put("W", materialIngredient(cable, cablePrefix));
        // GT6 Loader_MultiTileEntities.java:1443-1447 {"XPw","CMW"}.
        acceptShapedCatalyst(
                output,
                "machines/" + id,
                List.of("XPw", "CMW", "   "),
                ingredients,
                Map.of("w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static void slicerCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String material,
            String id) {
        int tier = switch (id) {
            case "slicer" -> 1;
            case "aluminium_slicer" -> 2;
            case "stainless_steel_slicer" -> 3;
            case "chromium_slicer" -> 4;
            case "titanium_slicer" -> 5;
            default -> compactIndexOrThrow(id);
        };
        Item piston = technologicalCompact("compact_electric_piston", tier);
        Item conveyor = technologicalCompact("compact_electric_conveyor", tier);
        if (piston == null || conveyor == null) {
            return;
        }
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put("P", Ingredient.of(piston));
        ingredients.put("R", materialIngredient(material, MaterialPrefixes.ROD));
        ingredients.put("Y", Ingredient.of(conveyor));
        ingredients.put("M", Ingredient.of(casing));
        ingredients.put("C", Ingredient.of(ModItems.technologicalPart(
                circuitPath(tier)).get()));
        // GT6 Loader_MultiTileEntities.java:1525-1529 {"PRw","YMC"}.
        acceptShapedCatalyst(
                output,
                "machines/" + id,
                List.of("PRw", "YMC", "   "),
                ingredients,
                Map.of("w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static void squeezerCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String material,
            String id) {
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put("R", materialIngredient(material, MaterialPrefixes.ROD));
        ingredients.put("S", materialIngredient(material, MaterialPrefixes.SPRING));
        ingredients.put("P", materialIngredient(material, MaterialPrefixes.TRIPLE_PLATE));
        ingredients.put("M", Ingredient.of(casing));
        acceptShapedCatalyst(
                output,
                "machines/" + id,
                List.of("RS ", "PM ", "Pw "),
                ingredients,
                Map.of("w", Ingredient.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static void nanofabCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String id) {
        int tier = switch (id) {
            case "nanofab" -> 1;
            case "aluminium_nanofab" -> 2;
            case "stainless_steel_nanofab" -> 3;
            case "chromium_nanofab" -> 4;
            case "titanium_nanofab" -> 5;
            default -> compactIndexOrThrow(id);
        };
        Item argon = ModItems.technologicalPart("laser_gas_ar").get();
        Item krypton = ModItems.technologicalPart("laser_gas_kr").get();
        Item xenon = ModItems.technologicalPart("laser_gas_xe").get();
        Item sapphire = ModItems.technologicalPart("processor_crystal_sapphire").get();
        Item circuit = ModItems.technologicalPart("circuit_ultimate").get();
        Item emitter = technologicalCompact("compact_signal_emitter", tier);
        Item sensor = technologicalCompact("compact_sensor", tier);
        if (emitter == null || sensor == null) {
            throw new IllegalStateException(
                    "Missing source machine component nanofab compact " + id);
        }
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put("K", Ingredient.of(krypton));
        ingredients.put("A", Ingredient.of(argon));
        ingredients.put("X", Ingredient.of(xenon));
        ingredients.put("Z", Ingredient.of(sensor));
        ingredients.put("M", Ingredient.of(casing));
        ingredients.put("Y", Ingredient.of(emitter));
        ingredients.put("C", Ingredient.of(circuit));
        ingredients.put("S", Ingredient.of(sapphire));
        // GT6 Loader_MultiTileEntities.java:1563-1567 {"KAX","ZMY","CSC"}.
        acceptShapedCatalyst(
                output,
                "machines/" + id,
                List.of("KAX", "ZMY", "CSC"),
                ingredients,
                Map.of(),
                new ItemStack(result));
    }

    private static void laserEngraverCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String material,
            String id) {
        int tier = laserTier(id);
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put("T", materialIngredient(material, MaterialPrefixes.SCREW));
        ingredients.put("G", materialIngredient(material, MaterialPrefixes.SMALL_GEAR));
        ingredients.put("P", Ingredient.of(Items.TERRACOTTA));
        ingredients.put("C", Ingredient.of(ModItems.technologicalPart(
                circuitPath(tier)).get()));
        ingredients.put("M", Ingredient.of(casing));
        acceptShapedCatalyst(
                output,
                "machines/" + id,
                List.of("TdT", "GPG", "CMC"),
                ingredients,
                Map.of("d", Ingredient.of(ModItems.MATERIAL_SCREWDRIVER.get())),
                new ItemStack(result));
    }

    private static void laserWelderCrafting(
            RecipeOutput output,
            Item result,
            Item casing,
            String material,
            String id) {
        int tier = laserTier(id);
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put("T", materialIngredient(material, MaterialPrefixes.SCREW));
        ingredients.put("L", yellowLensIngredient());
        ingredients.put("G", materialIngredient(material, MaterialPrefixes.SMALL_GEAR));
        ingredients.put("P", Ingredient.of(Items.TERRACOTTA));
        ingredients.put("C", Ingredient.of(ModItems.technologicalPart(
                circuitPath(tier)).get()));
        ingredients.put("M", Ingredient.of(casing));
        acceptShapedCatalyst(
                output,
                "machines/" + id,
                List.of("TLT", "GPG", "CMC"),
                ingredients,
                Map.of(),
                new ItemStack(result));
    }

    private static Ingredient yellowLensIngredient() {
        MaterialPrefix lens = MaterialPrefixCatalog.require("lens");
        Ingredient[] parts = List.of(
                        "yellow_sapphire",
                        "heliodor",
                        "amber",
                        "topaz")
                .stream()
                .map(material -> materialIngredient(material, lens))
                .toArray(Ingredient[]::new);
        return CompoundIngredient.of(parts);
    }

    private static int laserTier(String id) {
        return switch (id) {
            case "laser_welder" -> 1;
            case "aluminium_laser_engraver", "aluminium_laser_welder" -> 2;
            case "stainless_steel_laser_engraver",
                    "stainless_steel_laser_welder" -> 3;
            case "chromium_laser_engraver", "chromium_laser_welder" -> 4;
            case "titanium_laser_engraver", "titanium_laser_welder" -> 5;
            default -> throw new IllegalStateException(
                    "T1 laser engraver must use its handwritten recipe: " + id);
        };
    }

    private static void addAutomaticHammerRecipes(RecipeOutput output) {
        Map<String, Item> results = Map.of(
                "bronze", ModItems.AUTOMATIC_HAMMER.get(),
                "steel", ModItems.STEEL_AUTOMATIC_HAMMER.get(),
                "titanium", ModItems.TITANIUM_AUTOMATIC_HAMMER.get(),
                "tungstensteel", ModItems.TUNGSTENSTEEL_AUTOMATIC_HAMMER.get());
        MaterialPrefix hammerHead =
                MaterialPrefixCatalog.require("tool_head_hammer");
        results.forEach((material, result) -> {
            Map<String, Ingredient> ingredients = new LinkedHashMap<>();
            ingredients.put(
                    "R",
                    materialIngredient(material, MaterialPrefixes.LONG_ROD));
            ingredients.put(
                    "M",
                    materialIngredient(
                            material, MaterialPrefixes.MACHINE_CASING_DOUBLE));
            ingredients.put(
                    "S",
                    materialIngredient(material, MaterialPrefixes.SPRING));
            ingredients.put("H", materialIngredient(material, hammerHead));
            String recipeName = "bronze".equals(material)
                    ? "automatic_hammer"
                    : material + "_automatic_hammer";
            acceptShapedCatalyst(
                    output,
                    "machines/" + recipeName,
                    List.of("wR ", "MS ", "hH "),
                    ingredients,
                    Map.of(
                            "w", Ingredient.of(ModItems.MATERIAL_WRENCH.get()),
                            "h", Ingredient.of(ModItems.SMITHING_HAMMER.get())),
                    new ItemStack(result));
        });
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
                    emitCasingFormRecipe(
                            output,
                            material,
                            MaterialPrefixes.MACHINE_CASING_QUADRUPLE,
                            MaterialPrefixes.QUADRUPLE_PLATE);
                    emitCasingFormRecipe(
                            output,
                            material,
                            MaterialPrefixes.MACHINE_CASING_DENSE,
                            MaterialPrefixes.DENSE_PLATE);
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
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        ingredients.put("X", keyedIngredient(plate, material, plateForm));
        ingredients.put(
                "Y", keyedIngredient(rod, material, MaterialPrefixes.LONG_ROD));
        // GT6 Loader_OreProcessing.java:152-153 {"YXX", "XwX", "XXY"}.
        acceptShapedCatalyst(
                output,
                "components/" + material + "/" + casingForm.serializedName(),
                List.of("YXX", "XwX", "XXY"),
                ingredients,
                Map.of("w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static void addWorkbenchToolRecipes(RecipeOutput output) {
        LinkedHashMap<String, List<MaterialPrefix>> registeredForms =
                new LinkedHashMap<>();
        MaterialCatalog.startupValues().forEach(material ->
                registeredForms.put(
                        material.id(),
                        MaterialCatalog.registeredForms(material)));
        WorkbenchToolRecipePlan.plan(
                MaterialCatalog.startupValues(), registeredForms)
                .forEach(recipe -> emitPlannedWorkbenchTool(output, recipe));
        WorkbenchToolRecipePlan.assemblies()
                .forEach(assembly -> emitToolHeadAssembly(output, assembly));
    }

    private static void emitPlannedWorkbenchTool(
            RecipeOutput output, WorkbenchToolRecipePlan.Recipe planned) {
        LinkedHashMap<String, Ingredient> ingredients = new LinkedHashMap<>();
        planned.ingredients().forEach((symbol, itemId) ->
                ingredients.put(
                        symbol,
                        MaterialLookup.ingredientFromLogicalId(itemId)
                                .orElseThrow(() -> new IllegalStateException(
                                        "Missing planned ingredient " + itemId))));
        LinkedHashMap<String, Ingredient> catalysts = new LinkedHashMap<>();
        planned.catalysts().forEach((symbol, itemId) ->
                catalysts.put(
                        symbol,
                        MaterialLookup.ingredientFromLogicalId(itemId)
                                .orElseThrow(() -> new IllegalStateException(
                                        "Missing planned catalyst " + itemId))));
        ItemStack result = planned.persistToolMaterial()
                ? toolStack(
                        resolveRegisteredItem(
                                ResourceLocation.parse(planned.resultId())),
                        planned.material(),
                        planned.electricCapacity(),
                        planned.electricVoltage())
                : MaterialLookup.stackFromLogicalId(planned.resultId())
                        .map(stack -> {
                            stack.setCount(planned.count());
                            return stack;
                        })
                        .orElseGet(() -> new ItemStack(
                                resolveRegisteredItem(
                                        ResourceLocation.parse(planned.resultId())),
                                planned.count()));
        acceptShapedCatalyst(
                output,
                planned.path(),
                planned.pattern(),
                ingredients,
                catalysts,
                result,
                planned.mirrored());
    }

    private static void addRockCobbleRecipes(RecipeOutput output) {
        MaterialPrefix rock = MaterialPrefixCatalog.require("rock");
        RockCobbleCrafting.cobbleResults().forEach((materialId, cobbleId) -> {
            Item rockItem = MaterialLookup.item(materialId, rock).orElse(null);
            if (rockItem == null) {
                return;
            }
            Item cobble = BuiltInRegistries.ITEM.getOptional(cobbleId).orElse(null);
            if (cobble == null || cobble == Items.AIR) {
                return;
            }
            Ingredient rockIngredient = rockItem
                    instanceof com.masson.cruciblecraft.content.item.PrefixMaterialItem
                    ? MaterialLookup.prefixMaterialIngredient(rockItem, materialId)
                    : Ingredient.of(rockItem);
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, cobble)
                    .pattern("XX")
                    .pattern("XX")
                    .define('X', rockIngredient)
                    .unlockedBy("has_rock", has(rockItem))
                    .save(output, id("rocks/" + materialId + "_to_cobble"));
        });
    }

    private static void addRockFurnaceRecipes(RecipeOutput output) {
        Map<String, MaterialDefinition> byId = new LinkedHashMap<>();
        MaterialCatalog.startupValues().forEach(material ->
                byId.putIfAbsent(material.id(), material));
        MaterialPrefix rock = MaterialPrefixCatalog.require("rock");
        for (MaterialDefinition material : MaterialCatalog.startupValues()) {
            if (!MaterialCatalog.isFormRegistered(material, rock)) {
                continue;
            }
            Item rockItem = MaterialLookup.item(material.id(), rock).orElse(null);
            if (rockItem == null) {
                continue;
            }
            ItemStack result = RockGtProcessing.furnaceStack(material, byId)
                    .orElse(ItemStack.EMPTY);
            if (result.isEmpty()) {
                continue;
            }
            float experience = RockGtProcessing.furnacePlan(material, byId)
                    .map(plan -> RockGtProcessing.furnaceExperience(material, plan))
                    .orElse(0.0F);
            output.accept(
                    id("rocks/" + material.id() + "_smelting"),
                    new SmeltingRecipe(
                            "cruciblecraft:rock_smelting",
                            CookingBookCategory.MISC,
                            Ingredient.of(rockItem),
                            result,
                            experience,
                            SMELTING_TIME),
                    null);
        }
    }

    private static void addDustPrefixPackRecipes(RecipeOutput output) {
        emitPrefixPack(output, "prefix_pack/tiny_dust_to_dust",
                MaterialPrefixes.TINY_DUST, 9, MaterialPrefixes.DUST, 1, 0, 1);
        emitPrefixPack(output, "prefix_pack/small_dust_to_dust",
                MaterialPrefixes.SMALL_DUST, 4, MaterialPrefixes.DUST, 1, 0, 1);
        emitPrefixPack(output, "prefix_pack/small_dust_to_dust_x2",
                MaterialPrefixes.SMALL_DUST, 8, MaterialPrefixes.DUST, 2, 0, 1);
        emitPrefixPack(output, "prefix_pack/dust_div72_to_tiny_dust",
                MaterialPrefixes.DUST_DIV72, 8, MaterialPrefixes.TINY_DUST, 1, 0, 1);
        emitPrefixPack(output, "prefix_pack/dust_to_storage_dust",
                MaterialPrefixes.DUST, 9, MaterialPrefixes.STORAGE_DUST, 1, 0, 1);
        emitPrefixPack(output, "prefix_pack/dust_to_tiny_dust",
                MaterialPrefixes.DUST, 1, MaterialPrefixes.TINY_DUST, 9, 0, 2);
        emitPrefixPack(output, "prefix_pack/dust_to_small_dust",
                MaterialPrefixes.DUST, 1, MaterialPrefixes.SMALL_DUST, 4, 1, 2);
        emitPrefixPack(output, "prefix_pack/tiny_dust_to_dust_div72",
                MaterialPrefixes.TINY_DUST, 1, MaterialPrefixes.DUST_DIV72, 8, 0, 1);
        emitPrefixPack(output, "prefix_pack/small_dust_to_dust_div72",
                MaterialPrefixes.SMALL_DUST, 1, MaterialPrefixes.DUST_DIV72, 18, 0, 1);
        emitPrefixPack(output, "prefix_pack/storage_dust_to_dust",
                MaterialPrefixes.STORAGE_DUST, 1, MaterialPrefixes.DUST, 9, 0, 2);
        emitPrefixPack(output, "prefix_pack/storage_dust_to_small_dust",
                MaterialPrefixes.STORAGE_DUST, 1, MaterialPrefixes.SMALL_DUST, 36, 1, 2);
    }

    private static void addNuggetPrefixPackRecipes(RecipeOutput output) {
        emitPrefixPack(output, "prefix_pack/nugget_to_ingot",
                MaterialPrefixes.NUGGET, 9, MaterialPrefixes.INGOT, 1, 0, 1);
        emitPrefixPack(output, "prefix_pack/ingot_to_nugget",
                MaterialPrefixes.INGOT, 1, MaterialPrefixes.NUGGET, 9, 0, 1);
    }

    private static void addPlatePrefixPackRecipes(RecipeOutput output) {
        emitPrefixPack(output, "prefix_pack/plate_to_storage_plate",
                MaterialPrefixes.PLATE, 9, MaterialPrefixes.STORAGE_PLATE, 1, 0, 1);
        emitPrefixPack(output, "prefix_pack/storage_plate_to_plate",
                MaterialPrefixes.STORAGE_PLATE, 1, MaterialPrefixes.PLATE, 9, 0, 1);
    }

    private static void emitPrefixPack(
            RecipeOutput output,
            String path,
            MaterialPrefix input,
            int inputCount,
            MaterialPrefix result,
            int outputCount,
            int unpackIndex,
            int unpackModulus) {
        output.accept(
                id(path),
                new PrefixPackRecipe(
                        input,
                        inputCount,
                        result,
                        outputCount,
                        unpackIndex,
                        unpackModulus),
                null);
    }

    private static void emitToolHeadAssembly(
            RecipeOutput output, WorkbenchToolRecipePlan.Assembly assembly) {
        output.accept(
                id(assembly.path()),
                new ToolHeadAssemblyRecipe(
                        MaterialPrefixCatalog.require(assembly.headPrefix()),
                        resolveRegisteredItem(
                                ResourceLocation.parse(assembly.resultId()))),
                null);
    }

    private static ItemStack toolStack(Item tool, String material) {
        return toolStack(tool, material, 0L, 0L);
    }

    private static ItemStack toolStack(
            Item tool, String material, long capacity, long voltage) {
        ItemStack stack = new ItemStack(tool);
        stack.set(ModComponents.TOOL_MATERIAL.get(), material);
        if (capacity > 0L) {
            com.masson.cruciblecraft.content.item.tool.ElectricToolCharge
                    .applyEmpty(stack, capacity, voltage);
        }
        return stack;
    }

    private static void acceptShapedCatalyst(
            RecipeOutput output,
            String path,
            List<String> pattern,
            Map<String, Ingredient> ingredients,
            Map<String, Ingredient> catalysts,
            ItemStack result) {
        acceptShapedCatalyst(
                output, path, pattern, ingredients, catalysts, result, false);
    }

    private static void acceptShapedCatalyst(
            RecipeOutput output,
            String path,
            List<String> pattern,
            Map<String, Ingredient> ingredients,
            Map<String, Ingredient> catalysts,
            ItemStack result,
            boolean mirrored) {
        output.accept(
                id(path),
                new ShapedCatalystRecipe(
                        pattern, ingredients, catalysts, result, mirrored),
                null);
    }

    private static Item resolveRegisteredItem(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            throw new IllegalStateException("Missing registered item " + id);
        }
        return item;
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
                    "bronze_small_gas_turbine",
                    "bronze_dynamo",
                    "clay_brick_burning_box_brick",
                    "steel_galvanized_electric_motor",
                    "steel_galvanized_electric_heater",
                    "aluminium_electric_heater",
                    "stainless_steel_electric_heater",
                    "chromium_electric_heater",
                    "titanium_electric_heater",
                    "steel_galvanized_electric_engine",
                    "aluminium_electric_engine",
                    "stainless_steel_electric_engine",
                    "chromium_electric_engine",
                    "titanium_electric_engine");

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
        java.util.LinkedHashMap<Character, Ingredient> keys =
                new java.util.LinkedHashMap<>();
        for (var key : entry.recipe().keys().entrySet()) {
            Ingredient item = resolveConverterIngredient(entry, key.getValue());
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
                unlock = unlockSample(key.getValue());
            }
        }
        builder.unlockedBy("has_part", has(unlock)).save(output, id(path));
    }

    private static void addLuFiberRecipe(RecipeOutput output) {
        if (MaterialLookup.ingredient("silver", MaterialPrefixes.PLATE).isEmpty()) {
            return;
        }
        output.accept(
                id("machines/lu_fiber_cable"),
                new ShapedCatalystRecipe(
                        List.of("PGR", "DxD", "RGP"),
                        Map.of(
                                "P", materialIngredient(
                                        "silver", MaterialPrefixes.PLATE),
                                "G", Ingredient.of(Items.GLASS),
                                "D", Ingredient.of(Items.DIAMOND),
                                "R", Ingredient.of(Items.REDSTONE)),
                        Map.of(
                                "x",
                                Ingredient.of(
                                        ModItems.MATERIAL_WIRE_CUTTER.get())),
                        new ItemStack(ModItems.LU_FIBER_CABLE.get())),
                null);
    }

    private static void addFusionPartRecipes(RecipeOutput output) {
        addFusionWallRecipe(
                output,
                "stainless_steel",
                ModItems.STAINLESS_STEEL_WALL.get(),
                "stainless_steel_wall");
        addFusionWallRecipe(
                output,
                "tungstensteel",
                ModItems.TUNGSTENSTEEL_WALL.get(),
                "tungstensteel_wall");
        Item emitter = ModItems.technologicalPart("compact_force_field_emitter_iv").get();
        output.accept(
                id("machines/fusion_reactor"),
                new ShapedCatalystRecipe(
                        List.of("FFF", "FMF", "FFF"),
                        Map.of(
                                "F", Ingredient.of(emitter),
                                "M", Ingredient.of(ModItems.TUNGSTENSTEEL_WALL.get())),
                        Map.of(),
                        new ItemStack(ModItems.FUSION_REACTOR.get())),
                null);
    }

    private static void addFusionWallRecipe(
            RecipeOutput output,
            String material,
            Item result,
            String path) {
        var plate = MaterialLookup.ingredient(material, MaterialPrefixes.PLATE);
        if (plate.isEmpty()) {
            return;
        }
        output.accept(
                id(path),
                new ShapedCatalystRecipe(
                        List.of("wPP", "hPP", "   "),
                        Map.of("P", plate.orElseThrow()),
                        Map.of(
                                "w",
                                CraftingTools.of(ModItems.MATERIAL_WRENCH.get()),
                                "h",
                                CraftingTools.of(ModItems.SMITHING_HAMMER.get())),
                        new ItemStack(result)),
                null);
    }

    private static void addFusionRecipes(RecipeOutput output) {
        for (FusionRecipeCatalog.Entry entry : FusionRecipeCatalog.entries()) {
            output.accept(
                    id("fusion/" + entry.id()),
                    new GTRecipeEntry(
                            ModRecipeMaps.FUSION.id(),
                            new GTRecipe(
                                    List.of(fusionCircuit(entry.circuit())),
                                    List.of(0),
                                    fusionItems(entry),
                                    fusionFluids(entry.fluidInputs(), entry.id()),
                                    fusionFluids(entry.fluidOutputs(), entry.id()),
                                    List.of(),
                                    entry.duration(),
                                    entry.eut(),
                                    entry.luStart())),
                    null);
        }
    }

    private static void addFusionExtensionRecipe(RecipeOutput output) {
        var deuterium = ModFluids.chemical("deuterium")
                .map(entry -> entry.source().get());
        var tritium = ModFluids.chemical("tritium")
                .map(entry -> entry.source().get());
        var matter = ModFluids.chemical("matter_neutral")
                .map(entry -> entry.source().get());
        if (deuterium.isEmpty() || tritium.isEmpty() || matter.isEmpty()) {
            return;
        }
        output.accept(
                id("fusion_extension/neutral_matter_bootstrap"),
                new GTRecipeEntry(
                        ModRecipeMaps.FUSION_EXTENSION.id(),
                        new GTRecipe(
                                List.of(fusionCircuit(3)),
                                List.of(0),
                                List.of(),
                                List.of(
                                        new FluidStack(deuterium.orElseThrow(), 1_000),
                                        new FluidStack(tritium.orElseThrow(), 1_000)),
                                List.of(new FluidStack(matter.orElseThrow(), 144)),
                                List.of(),
                                1_760,
                                -8_192L,
                                1_760L * 8_192L * 16L)),
                null);
    }

    private static Ingredient fusionCircuit(int config) {
        return DataComponentIngredient.of(
                false,
                DataComponentPredicate.builder()
                        .expect(ModComponents.CIRCUIT_CONFIG.get(), config)
                        .build(),
                ModItems.PROGRAMMED_CIRCUIT.get());
    }

    private static List<ItemStack> fusionItems(FusionRecipeCatalog.Entry entry) {
        return entry.itemOutputs().stream()
                .map(item -> MaterialLookup.tryStack(
                                item.material(),
                                switch (item.prefix()) {
                                    case "dust" -> MaterialPrefixes.DUST;
                                    default -> throw new IllegalStateException(
                                            "Unsupported fusion item prefix "
                                                    + item.prefix());
                                },
                                item.count())
                        .orElseThrow(() -> new IllegalStateException(
                                "Missing fusion item "
                                        + item.material()
                                        + "/"
                                        + item.prefix()
                                        + " for "
                                        + entry.id())))
                .toList();
    }

    private static List<FluidStack> fusionFluids(
            List<FusionRecipeCatalog.FluidIo> fluids, String recipeId) {
        return fluids.stream()
                .map(fluid -> new FluidStack(
                        resolveFusionFluid(fluid, recipeId),
                        fluid.milliBuckets()))
                .toList();
    }

    private static net.minecraft.world.level.material.Fluid resolveFusionFluid(
            FusionRecipeCatalog.FluidIo fluid, String recipeId) {
        var resolved = fluid.molten()
                ? ModFluids.molten(fluid.material()).map(entry -> entry.source().get())
                : ModFluids.chemical(fluid.material()).map(entry -> entry.source().get());
        return resolved.orElseThrow(() -> new IllegalStateException(
                "Missing fusion fluid " + fluid.material()
                        + (fluid.molten() ? " (molten)" : " (gas)")
                        + " for " + recipeId));
    }

    private static void addBatteryRecipes(RecipeOutput output) {
        for (var entry : EnergyBatteryTierCatalog.entries()) {
            if (!isBatteryRecipeReady(entry.id().getPath())) {
                continue;
            }
            emitBatteryRecipe(output, entry);
        }
    }

    private static boolean isBatteryRecipeReady(String path) {
        return path.startsWith("lead_acid_battery_")
                || path.startsWith("alkaline_battery_")
                || path.startsWith("nickel_cadmium_battery_")
                || path.startsWith("lithium_cobalt_battery_")
                || path.startsWith("lithium_manganese_battery_");
    }

    private static void addBatteryCellRecipes(RecipeOutput output) {
        batteryCellRecipe(
                output,
                "lead_acid",
                List.of(" Fh", "FPF", "xF "),
                Map.of(
                        "P", materialIngredient(
                                "battery_alloy", MaterialPrefixes.CURVED_PLATE),
                        "F", materialIngredient("lead", MaterialPrefixes.FOIL)),
                Map.of(
                        "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get()),
                        "x", CraftingTools.of(ModItems.MATERIAL_WIRE_CUTTER.get())));
        batteryCellRecipe(
                output,
                "alkaline",
                List.of("KSM", "OPF", "CWZ"),
                Map.of(
                        "K", materialIngredient(
                                "potassium_hydroxide", MaterialPrefixes.DUST),
                        "S", materialIngredient(
                                "stainless_steel", MaterialPrefixes.CURVED_PLATE),
                        "M", materialIngredient(
                                "pyrolusite", MaterialPrefixes.DUST),
                        "O", materialIngredient(
                                "plastic", MaterialPrefixes.RING),
                        "P", materialIngredient(
                                "battery_alloy", MaterialPrefixes.CURVED_PLATE),
                        "F", materialIngredient("aluminium", MaterialPrefixes.FOIL),
                        "C", materialIngredient("carbon", MaterialPrefixes.DUST),
                        "W", materialIngredient("iron", MaterialPrefixes.WIRE),
                        "Z", materialIngredient("zinc", MaterialPrefixes.DUST)),
                Map.of());
        batteryCellRecipe(
                output,
                "nickel_cadmium",
                List.of("KSM", "OPF", "CWZ"),
                Map.of(
                        "K", materialIngredient(
                                "potassium_hydroxide", MaterialPrefixes.DUST),
                        "S", materialIngredient(
                                "stainless_steel", MaterialPrefixes.CURVED_PLATE),
                        "M", materialIngredient(
                                "cadmium", MaterialPrefixes.CURVED_PLATE),
                        "O", materialIngredient(
                                "plastic", MaterialPrefixes.RING),
                        "P", materialIngredient(
                                "battery_alloy", MaterialPrefixes.CURVED_PLATE),
                        "F", materialIngredient("aluminium", MaterialPrefixes.FOIL),
                        "C", materialIngredient("graphite", MaterialPrefixes.ROD),
                        "W", materialIngredient("iron", MaterialPrefixes.WIRE),
                        "Z", materialIngredient(
                                "nickel", MaterialPrefixes.CURVED_PLATE)),
                Map.of());
        // MultiItemTechnological.java:479 / :484. Fill stays FluidContainerData.
        batteryCellRecipe(
                output,
                "lithium_cobalt",
                List.of("CLF", "XSG", "FLP"),
                Map.of(
                        "C", circuitIngredient("circuit_elite"),
                        "L", materialIngredient(
                                "lithium_perchlorate", MaterialPrefixes.DUST),
                        "F", materialIngredient("plastic", MaterialPrefixes.FOIL),
                        "X", materialIngredient("cobalt", MaterialPrefixes.ROD),
                        "S", materialIngredient(
                                "chromium", MaterialPrefixes.CURVED_PLATE),
                        "G", materialIngredient("graphite", MaterialPrefixes.ROD),
                        "P", materialIngredient(
                                "battery_alloy", MaterialPrefixes.CURVED_PLATE)),
                Map.of());
        batteryCellRecipe(
                output,
                "lithium_manganese",
                List.of("CLF", "XSG", "FLP"),
                Map.of(
                        "C", circuitIngredient("circuit_ultimate"),
                        "L", materialIngredient(
                                "lithium_perchlorate", MaterialPrefixes.DUST),
                        "F", materialIngredient("plastic", MaterialPrefixes.FOIL),
                        "X", materialIngredient(
                                "manganese", MaterialPrefixes.ROD),
                        "S", materialIngredient(
                                "chromium", MaterialPrefixes.CURVED_PLATE),
                        "G", materialIngredient("graphite", MaterialPrefixes.ROD),
                        "P", materialIngredient(
                                "battery_alloy", MaterialPrefixes.CURVED_PLATE)),
                Map.of());
    }

    private static void batteryCellRecipe(
            RecipeOutput output,
            String family,
            List<String> pattern,
            Map<String, Ingredient> ingredients,
            Map<String, Ingredient> catalysts) {
        output.accept(
                id("battery_cells/" + family + "_empty"),
                new ShapedCatalystRecipe(
                        pattern,
                        ingredients,
                        catalysts,
                        new ItemStack(ModItems.batteryCell(
                                family + "_cell_empty").get())),
                null);
    }

    private static Ingredient circuitIngredient(String path) {
        return Ingredient.of(ModItems.technologicalPart(path).get());
    }

    private static void emitBatteryRecipe(
            RecipeOutput output,
            EnergyBatteryTierCatalog.Entry entry) {
        String path = entry.id().getPath();
        Item result = ModItems.batteryItemsById().get(entry.id()).get();
        java.util.LinkedHashMap<Character, Ingredient> keys =
                new java.util.LinkedHashMap<>();
        for (var key : entry.recipe().keys().entrySet()) {
            Ingredient item = resolveBatteryIngredient(key.getValue());
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
                unlock = unlockSample(key.getValue());
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
        java.util.LinkedHashMap<Character, Ingredient> keys =
                new java.util.LinkedHashMap<>();
        for (var key : entry.recipe().keys().entrySet()) {
            Ingredient item = resolveTransformerIngredient(key.getValue());
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
                unlock = unlockSample(key.getValue());
            }
        }
        if (unlock == null) {
            return;
        }
        builder.unlockedBy("has_part", has(unlock)).save(output, id(path));
    }

    private static void addHeatExchangerRecipes(RecipeOutput output) {
        for (HeatExchangerProfile profile : HeatExchangerCatalog.profiles()) {
            emitHeatExchangerRecipe(output, profile);
        }
    }

    private static void emitHeatExchangerRecipe(
            RecipeOutput output, HeatExchangerProfile profile) {
        LinkedHashMap<String, Ingredient> ingredients = new LinkedHashMap<>();
        for (var entry : profile.recipe().keys().entrySet()) {
            Ingredient item = resolveHeatExchangerIngredient(entry.getValue());
            if (item == null) {
                return;
            }
            ingredients.put(entry.getKey(), item);
        }
        acceptShapedCatalyst(
                output,
                profile.id().getPath(),
                profile.recipe().pattern(),
                ingredients,
                Map.of(
                        profile.recipe().catalyst(),
                        CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(
                        ModItems.heatExchangerItemsById()
                                .get(profile.id())
                                .get()));
    }

    private static Ingredient resolveHeatExchangerIngredient(
            HeatExchangerProfile.Ingredient ingredient) {
        MaterialPrefix prefix;
        try {
            prefix = new MaterialPrefix("cruciblecraft:" + ingredient.prefix());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return MaterialLookup.ingredient(ingredient.material(), prefix).orElse(null);
    }

    private static void addCoolerRecipes(RecipeOutput output) {
        for (CoolerProfile profile : CoolerCatalog.profiles()) {
            emitCoolerRecipe(output, profile);
        }
    }

    private static void emitCoolerRecipe(
            RecipeOutput output, CoolerProfile profile) {
        LinkedHashMap<String, Ingredient> ingredients = new LinkedHashMap<>();
        for (var entry : profile.recipe().keys().entrySet()) {
            Ingredient item = resolveCoolerIngredient(entry.getValue());
            if (item == null) {
                return;
            }
            ingredients.put(entry.getKey(), item);
        }
        LinkedHashMap<String, Ingredient> catalysts = new LinkedHashMap<>();
        for (String tool : profile.recipe().catalysts()) {
            Item catalyst = catalystItem(tool);
            if (catalyst == null) {
                return;
            }
            catalysts.put(tool, CraftingTools.of(catalyst));
        }
        acceptShapedCatalyst(
                output,
                profile.id().getPath(),
                profile.recipe().pattern(),
                ingredients,
                catalysts,
                new ItemStack(
                        ModItems.coolerItemsById()
                                .get(profile.id())
                                .get()));
    }

    private static Ingredient resolveCoolerIngredient(
            CoolerProfile.Ingredient ingredient) {
        if (ingredient.item() != null && !ingredient.item().isBlank()) {
            return MaterialLookup.ingredientFromLogicalId(ingredient.item())
                    .orElse(null);
        }
        MaterialPrefix prefix;
        try {
            prefix = new MaterialPrefix("cruciblecraft:" + ingredient.prefix());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return MaterialLookup.ingredient(ingredient.material(), prefix).orElse(null);
    }

    private static void addFluxConverterRecipes(RecipeOutput output) {
        for (FluxProfile profile : FluxCatalog.profiles()) {
            if (!profile.recipeLive()) {
                continue;
            }
            emitFluxRecipe(output, profile);
        }
    }

    private static void emitFluxRecipe(
            RecipeOutput output, FluxProfile profile) {
        LinkedHashMap<String, Ingredient> ingredients = new LinkedHashMap<>();
        for (var entry : profile.recipe().keys().entrySet()) {
            Ingredient item = resolveFluxIngredient(entry.getValue());
            if (item == null) {
                return;
            }
            ingredients.put(entry.getKey(), item);
        }
        boolean catalystPattern = profile.recipe().pattern().size() >= 2
                && profile.recipe().pattern().size() <= 3
                && profile.recipe().pattern().stream()
                        .allMatch(row -> row.length() == 3);
        if (!catalystPattern) {
            if (!profile.recipe().catalysts().isEmpty()) {
                throw new IllegalArgumentException(
                        "Flux recipe with catalysts has an incompatible shaped pattern: "
                                + profile.id());
            }
            ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(
                    RecipeCategory.MISC,
                    ModItems.fluxItemsById().get(profile.id()).get());
            profile.recipe().pattern().forEach(builder::pattern);
            ingredients.forEach((symbol, ingredient) ->
                    builder.define(symbol.charAt(0), ingredient));
            Item unlock = Items.CRAFTING_TABLE;
            for (Ingredient ingredient : ingredients.values()) {
                if (ingredient.getItems().length > 0) {
                    unlock = ingredient.getItems()[0].getItem();
                    break;
                }
            }
            builder.unlockedBy("has_ingredient", has(unlock))
                    .save(output, id(profile.id().getPath()));
            return;
        }
        acceptShapedCatalyst(
                output,
                profile.id().getPath(),
                profile.recipe().pattern(),
                ingredients,
                new LinkedHashMap<>(),
                new ItemStack(
                        ModItems.fluxItemsById()
                                .get(profile.id())
                                .get()));
    }

    private static Ingredient resolveFluxIngredient(
            FluxProfile.Ingredient ingredient) {
        if (ingredient.item() != null && !ingredient.item().isBlank()) {
            return MaterialLookup.ingredientFromLogicalId(ingredient.item())
                    .orElse(null);
        }
        MaterialPrefix prefix;
        try {
            prefix = new MaterialPrefix("cruciblecraft:" + ingredient.prefix());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return MaterialLookup.ingredient(ingredient.material(), prefix).orElse(null);
    }

    private static void addLargeHeatExchangerRecipes(RecipeOutput output) {
        LargeHeatExchangerProfile profile = LargeHeatExchangerCatalog.profile();
        emitCatalogShaped(
                output,
                profile.id().getPath(),
                profile.recipe(),
                new ItemStack(ModItems.LARGE_HEAT_EXCHANGER.get()));
        var transmitter = ModItems.mteInPlaceItemsById().get(profile.transmitterId());
        if (transmitter != null) {
            emitCatalogShaped(
                    output,
                    "multiblock/heat_transmitter",
                    profile.transmitterRecipe(),
                    new ItemStack(transmitter.get()));
        }
    }

    private static void addMteInPlaceAcquisitionRecipes(RecipeOutput output) {
        for (MteInPlaceAcquisitionCatalog.Recipe recipe
                : MteInPlaceAcquisitionCatalog.recipes()) {
            if (!BuiltInRegistries.ITEM.containsKey(recipe.resultId())) {
                continue;
            }
            Item result = BuiltInRegistries.ITEM.get(recipe.resultId());
            if (result == Items.AIR) {
                continue;
            }
            boolean missing = false;
            LinkedHashMap<String, Ingredient> ingredients = new LinkedHashMap<>();
            for (var entry : recipe.ingredients().entrySet()) {
                Ingredient ingredient = mteAcquisitionIngredient(entry.getValue());
                if (ingredient == null) {
                    missing = true;
                    break;
                }
                ingredients.put(entry.getKey(), ingredient);
            }
            if (missing) {
                continue;
            }
            LinkedHashMap<String, Ingredient> catalysts = new LinkedHashMap<>();
            for (var entry : recipe.catalysts().entrySet()) {
                Ingredient ingredient = mteAcquisitionIngredient(entry.getValue());
                if (ingredient == null) {
                    missing = true;
                    break;
                }
                catalysts.put(entry.getKey(), ingredient);
            }
            if (missing) {
                continue;
            }
            acceptShapedCatalyst(
                    output,
                    recipe.path(),
                    recipe.pattern(),
                    ingredients,
                    catalysts,
                    new ItemStack(result, recipe.count()));
        }
    }

    private static Ingredient mteAcquisitionIngredient(
            MteInPlaceAcquisitionCatalog.Slot slot) {
        if (slot == null) {
            return null;
        }
        if (slot.tag() != null && !slot.tag().isBlank()) {
            ResourceLocation tag = ResourceLocation.tryParse(slot.tag());
            if (tag == null) {
                return null;
            }
            return Ingredient.of(TagKey.create(Registries.ITEM, tag));
        }
        if (slot.item() == null || slot.item().isBlank()) {
            return null;
        }
        return MaterialLookup.ingredientFromLogicalId(slot.item()).orElse(null);
    }

    private static void addSteamTurbineRecipes(RecipeOutput output) {
        for (SteamTurbineCatalog.Profile profile : SteamTurbineCatalog.profiles()) {
            var item = ModItems.mteInPlaceItemsById().get(profile.id());
            if (item == null) {
                continue;
            }
            emitSteamShaped(
                    output,
                    profile.id().getPath(),
                    profile.recipe(),
                    new ItemStack(item.get()));
        }
    }

    private static void addLargeGasTurbineRecipes(RecipeOutput output) {
        for (LargeGasTurbineCatalog.Profile profile : LargeGasTurbineCatalog.profiles()) {
            var item = ModItems.mteInPlaceItemsById().get(profile.id());
            if (item == null) {
                continue;
            }
            emitGasTurbineShaped(
                    output,
                    profile.id().getPath(),
                    profile.recipe(),
                    new ItemStack(item.get()));
        }
    }

    private static void emitCatalogShaped(
            RecipeOutput output,
            String path,
            LargeHeatExchangerProfile.Recipe recipe,
            ItemStack result) {
        LinkedHashMap<String, Ingredient> ingredients = new LinkedHashMap<>();
        for (var entry : recipe.keys().entrySet()) {
            Ingredient item = resolveFlexibleIngredient(
                    entry.getValue().item(),
                    entry.getValue().prefix(),
                    entry.getValue().material());
            if (item == null) {
                return;
            }
            ingredients.put(entry.getKey(), item);
        }
        LinkedHashMap<String, Ingredient> catalysts = new LinkedHashMap<>();
        for (String tool : recipe.catalysts()) {
            Item catalyst = catalystItem(tool);
            if (catalyst == null) {
                return;
            }
            catalysts.put(tool, CraftingTools.of(catalyst));
        }
        acceptShapedCatalyst(output, path, recipe.pattern(), ingredients, catalysts, result);
    }

    private static void emitSteamShaped(
            RecipeOutput output,
            String path,
            SteamTurbineCatalog.Recipe recipe,
            ItemStack result) {
        if (recipe.pattern().isEmpty()) {
            return;
        }
        LinkedHashMap<String, Ingredient> ingredients = new LinkedHashMap<>();
        for (var entry : recipe.keys().entrySet()) {
            Ingredient item = resolveFlexibleIngredient(
                    entry.getValue().item(),
                    entry.getValue().prefix(),
                    entry.getValue().material());
            if (item == null) {
                return;
            }
            ingredients.put(entry.getKey(), item);
        }
        LinkedHashMap<String, Ingredient> catalysts = new LinkedHashMap<>();
        for (String tool : recipe.catalysts()) {
            Item catalyst = catalystItem(tool);
            if (catalyst == null) {
                return;
            }
            catalysts.put(tool, CraftingTools.of(catalyst));
        }
        acceptShapedCatalyst(output, path, recipe.pattern(), ingredients, catalysts, result);
    }

    private static void emitGasTurbineShaped(
            RecipeOutput output,
            String path,
            LargeGasTurbineCatalog.Recipe recipe,
            ItemStack result) {
        if (recipe.pattern().isEmpty()) {
            return;
        }
        LinkedHashMap<String, Ingredient> ingredients = new LinkedHashMap<>();
        for (var entry : recipe.keys().entrySet()) {
            Ingredient item = resolveFlexibleIngredient(
                    entry.getValue().item(),
                    entry.getValue().prefix(),
                    entry.getValue().material());
            if (item == null) {
                return;
            }
            ingredients.put(entry.getKey(), item);
        }
        LinkedHashMap<String, Ingredient> catalysts = new LinkedHashMap<>();
        for (String tool : recipe.catalysts()) {
            Item catalyst = catalystItem(tool);
            if (catalyst == null) {
                return;
            }
            catalysts.put(tool, CraftingTools.of(catalyst));
        }
        acceptShapedCatalyst(output, path, recipe.pattern(), ingredients, catalysts, result);
    }

    private static Ingredient resolveFlexibleIngredient(
            String itemId, String prefix, String material) {
        if (itemId != null && !itemId.isBlank()) {
            return MaterialLookup.ingredientFromLogicalId(itemId).orElse(null);
        }
        if (prefix == null || material == null) {
            return null;
        }
        MaterialPrefix parsed;
        try {
            parsed = new MaterialPrefix("cruciblecraft:" + prefix);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return MaterialLookup.ingredient(material, parsed).orElse(null);
    }

    private static Item catalystItem(String key) {
        return switch (key) {
            case "a" -> ModItems.MATERIAL_AXE.get();
            case "c" -> ModItems.MATERIAL_CROWBAR.get();
            case "d" -> ModItems.MATERIAL_SCREWDRIVER.get();
            case "f" -> ModItems.MATERIAL_FILE.get();
            case "h" -> ModItems.SMITHING_HAMMER.get();
            case "k" -> ModItems.MATERIAL_KNIFE.get();
            case "n" -> ModItems.MATERIAL_MONKEY_WRENCH.get();
            case "o" -> ModItems.MATERIAL_BENDING_CYLINDER_SMALL.get();
            case "q" -> ModItems.MATERIAL_SCISSORS.get();
            case "r" -> ModItems.MATERIAL_SOFT_HAMMER.get();
            case "s" -> ModItems.MATERIAL_SAW.get();
            case "w" -> ModItems.MATERIAL_WRENCH.get();
            case "x" -> ModItems.MATERIAL_WIRE_CUTTER.get();
            case "y" -> ModItems.MATERIAL_CHISEL.get();
            case "z" -> ModItems.MATERIAL_BENDING_CYLINDER.get();
            default -> null;
        };
    }

    private static Ingredient resolveTransformerIngredient(
            EnergyTransformerTierCatalog.Ingredient ingredient) {
        if (ingredient.item() != null) {
            return MaterialLookup.ingredientFromLogicalId(ingredient.item())
                    .orElse(null);
        }
        MaterialPrefix prefix;
        try {
            prefix = new MaterialPrefix("cruciblecraft:" + ingredient.prefix());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return MaterialLookup.ingredient(ingredient.material(), prefix).orElse(null);
    }

    private static Ingredient resolveBatteryIngredient(
            EnergyBatteryTierCatalog.Ingredient ingredient) {
        if (ingredient.item() != null) {
            return MaterialLookup.ingredientFromLogicalId(ingredient.item())
                    .orElse(null);
        }
        MaterialPrefix prefix;
        try {
            prefix = new MaterialPrefix("cruciblecraft:" + ingredient.prefix());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return MaterialLookup.ingredient(ingredient.material(), prefix).orElse(null);
    }

    private static Ingredient resolveConverterIngredient(
            EnergyConverterTierCatalog.Entry entry,
            EnergyConverterTierCatalog.Ingredient ingredient) {
        if (ingredient.item() != null) {
            if ("minecraft:flint_and_steel".equals(ingredient.item())) {
                return Ingredient.of(ModItemTags.CRAFTING_FIRESTARTER);
            }
            return MaterialLookup.ingredientFromLogicalId(ingredient.item())
                    .orElse(null);
        }
        String material = "variant".equals(ingredient.material())
                ? entry.material()
                : ingredient.material();
        if ("clay_brick".equals(material) && "ingot".equals(ingredient.prefix())) {
            return Ingredient.of(Items.BRICK);
        }
        MaterialPrefix prefix;
        try {
            prefix = new MaterialPrefix("cruciblecraft:" + ingredient.prefix());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return MaterialLookup.ingredient(material, prefix).orElse(null);
    }

    private static final String[] COMPACT_TIER_NAMES = {
            "ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "puv1",
            "puv2", "puv3", "puv4", "puv5", "omega"
    };

    private static int compactIndexFromVariant(String id) {
        if (id.endsWith("_puv2")) {
            return 10;
        }
        if (id.endsWith("_puv3")) {
            return 11;
        }
        if (id.endsWith("_puv4")) {
            return 12;
        }
        if (id.endsWith("_puv5")) {
            return 13;
        }
        if (id.endsWith("_omega")) {
            return 14;
        }
        return -1;
    }

    private static int compactIndexOrThrow(String id) {
        int index = compactIndexFromVariant(id);
        if (index < 0) {
            throw new IllegalStateException("Unknown compact-tier variant: " + id);
        }
        return index;
    }

    private static String compactPartOrThrow(String id, String family) {
        return family + "_" + COMPACT_TIER_NAMES[compactIndexOrThrow(id)];
    }

    private static Item technologicalCompact(String family, int tier) {
        if (tier < 1 || tier >= COMPACT_TIER_NAMES.length) {
            return null;
        }
        try {
            return ModItems.technologicalPart(
                    family + "_" + COMPACT_TIER_NAMES[tier]).get();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String circuitPath(int tier) {
        return switch (tier) {
            case 1 -> "circuit_basic";
            case 2 -> "circuit_good";
            case 3 -> "circuit_advanced";
            case 4 -> "circuit_elite";
            case 5 -> "circuit_master";
            default -> "circuit_quantum";
        };
    }

    private static boolean isExplicitProcessingKind(String kindPath) {
        return switch (kindPath) {
            case "printer",
                    "scanner",
                    "autocrafter",
                    "electric_mixer",
                    "boxinator",
                    "lightning",
                    "plantalyzer",
                    "bumblelyzer",
                    "massfab",
                    "replicator",
                    "freezer",
                    "cryo_mixer",
                    "polarizer",
                    "magnetic_separator" -> true;
            default -> false;
        };
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
        if (!material.furnaceSmeltable()) {
            return;
        }
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
        Ingredient ingredient = MaterialLookup.ingredient(material.id(), inputForm)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing " + inputForm.serializedName() + " item for " + material.id()));
        ItemStack result = MaterialLookup.stack(material.id(), MaterialPrefixes.INGOT);

        output.accept(
                recipeId(material, inputForm, "smelting"),
                new net.minecraft.world.item.crafting.SmeltingRecipe(
                        COMPAT_SHORTCUT_GROUP,
                        net.minecraft.world.item.crafting.CookingBookCategory.MISC,
                        ingredient,
                        result,
                        ORE_EXPERIENCE,
                        SMELTING_TIME),
                null);
        output.accept(
                recipeId(material, inputForm, "blasting"),
                new net.minecraft.world.item.crafting.BlastingRecipe(
                        COMPAT_SHORTCUT_GROUP,
                        net.minecraft.world.item.crafting.CookingBookCategory.MISC,
                        ingredient,
                        result.copy(),
                        ORE_EXPERIENCE,
                        BLASTING_TIME),
                null);
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
        return materialRule(
                target, input, output, inputCount, outputCount, duration, eut,
                specialValue, durationOverrides, List.of());
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
            Map<String, Integer> durationOverrides,
            List<String> conditions) {
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
                conditions,
                java.util.Optional.empty(),
                List.of()));
    }

    private static MaterialRuleRecipe materialRule(
            ResourceLocation target,
            List<MaterialRule.ItemResource> inputs,
            List<MaterialRule.ItemResource> outputs,
            int duration,
            long eut,
            long specialValue,
            List<String> conditions) {
        return new MaterialRuleRecipe(new MaterialRule(
                java.util.Optional.of(target),
                inputs,
                outputs,
                List.of(),
                List.of(),
                Integer.toString(duration),
                Long.toString(eut),
                Long.toString(specialValue),
                true,
                java.util.Optional.empty(),
                Map.of(),
                conditions,
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
