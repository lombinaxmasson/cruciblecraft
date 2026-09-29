package com.masson.cruciblecraft.datagen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.energy.longdistance.LongDistanceTransformerCatalog;
import com.masson.cruciblecraft.machine.processing.MachineTierCatalog;
import com.masson.cruciblecraft.energy.longdistance.LongDistanceTransformerProfile;
import com.masson.cruciblecraft.energy.longdistance.LongDistanceWireProfile;
import com.masson.cruciblecraft.energy.quantum.QuantumEnergizerCatalog;
import com.masson.cruciblecraft.energy.quantum.QuantumEnergizerProfile;
import com.masson.cruciblecraft.recipe.crafting.CraftingTools;
import com.masson.cruciblecraft.recipe.crafting.ShapedCatalystRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeEntry;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

/** PUV2+/OMEGA lane recipes. Skip any row whose GT6 grid cannot resolve. */
final class PuvOmegaRecipes {
    private static final String[] TIERS = {
            "ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "puv1",
            "puv2", "puv3", "puv4", "puv5", "omega"
    };
    private static final String[] ELECTRIC = {
            "tin_alloy", "steel_galvanized", "aluminium", "stainless_steel",
            "chromium", "titanium", "iridium", "osmium_elemental",
            "trinitanium", "trinaquadalloy", "neutronium", "neutronium",
            "neutronium", "neutronium", "neutronium"
    };
    private static final MaterialPrefix[] MOTOR_WINDING = {
            MaterialPrefixes.FINE_WIRE, MaterialPrefixes.WIRE,
            MaterialPrefixes.DOUBLE_WIRE, MaterialPrefixes.TRIPLE_WIRE,
            MaterialPrefixes.QUADRUPLE_WIRE, MaterialPrefixes.QUINTUPLE_WIRE,
            MaterialPrefixes.SEXTUPLE_WIRE, MaterialPrefixes.SEPTUPLE_WIRE,
            MaterialPrefixes.OCTUPLE_WIRE, MaterialPrefixes.NONUPLE_WIRE,
            MaterialPrefixes.HEXADECUPLE_WIRE, MaterialPrefixes.HEXADECUPLE_WIRE,
            MaterialPrefixes.HEXADECUPLE_WIRE, MaterialPrefixes.HEXADECUPLE_WIRE,
            MaterialPrefixes.HEXADECUPLE_WIRE
    };
    private static final MaterialPrefix[] FIELD_OSMIUM = {
            MaterialPrefixes.FINE_WIRE, MaterialPrefixes.WIRE,
            MaterialPrefixes.DOUBLE_WIRE, MaterialPrefixes.QUADRUPLE_WIRE,
            MaterialPrefixes.SEXTUPLE_WIRE, MaterialPrefixes.OCTUPLE_WIRE,
            MaterialPrefixes.DECUPLE_WIRE, MaterialPrefixes.DODECUPLE_WIRE,
            MaterialPrefixes.TETRADECUPLE_WIRE, MaterialPrefixes.HEXADECUPLE_WIRE,
            MaterialPrefixes.HEXADECUPLE_WIRE, MaterialPrefixes.HEXADECUPLE_WIRE,
            MaterialPrefixes.HEXADECUPLE_WIRE, MaterialPrefixes.HEXADECUPLE_WIRE,
            MaterialPrefixes.HEXADECUPLE_WIRE
    };
    private static final String[] CIRCUITS = {
            "circuit_basic", "circuit_basic", "circuit_good", "circuit_advanced",
            "circuit_elite", "circuit_master", "circuit_ultimate",
            "circuit_quantum", "circuit_quantum", "circuit_quantum",
            "circuit_quantum", "circuit_quantum", "circuit_quantum",
            "circuit_quantum", "circuit_quantum"
    };

    private PuvOmegaRecipes() {}

    static void addAll(RecipeOutput output) {
        addQuantumEnergizerRecipes(output);
        addMassfabRecipe(output);
        addGrapheneNanofab(output);
        addMagneticSeparator(output);
        addLightningAdamantium(output);
        addPolarizerRecipes(output);
        addFreezerRecipes(output);
        addCryoMixerRecipes(output);
        addAlloyMixers(output);
        addCompactParts(output);
        addCrystalAndLaser(output);
        addFusionHullRecipes(output);
        addBedrockDrillRecipes(output);
        addLongDistanceRecipes(output);
        addProcessingHostRecipes(output);
    }

    private static final java.util.Set<String> HOST_KINDS = java.util.Set.of(
            "printer",
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
            "magnetic_separator");

    private static void addProcessingHostRecipes(RecipeOutput output) {
        for (MachineTierCatalog.Entry entry : MachineTierCatalog.entries()) {
            String kind = entry.kindId().getPath();
            if (!HOST_KINDS.contains(kind)) {
                continue;
            }
            var holder = ModItems.tieredProcessingItemsById().get(entry.variantId());
            if (holder == null) {
                continue;
            }
            int tier = entry.sourceTier();
            if (tier < 1 || tier >= TIERS.length) {
                continue;
            }
            String material = materialPath(entry.tierBand().materialId());
            Item result = holder.get();
            switch (kind) {
                case "printer" -> emitPrinter(output, entry, result, material, tier);
                case "scanner" -> emitScanner(output, entry, result, material, tier);
                case "autocrafter" -> emitAutocrafter(
                        output, entry, result, material, tier);
                case "electric_mixer" -> emitElectricMixer(
                        output, entry, result, material, tier);
                case "boxinator" -> emitBoxinator(output, entry, result, material, tier);
                case "lightning" -> emitLightning(output, entry, result, material, tier);
                case "plantalyzer" -> emitAnalyzer(
                        output, entry, result, material, tier, Items.OAK_SAPLING);
                case "bumblelyzer" -> emitAnalyzer(
                        output, entry, result, material, tier, Items.HONEY_BOTTLE);
                case "massfab" -> emitMassfab(output, entry, result, material, tier);
                case "replicator" -> emitReplicator(
                        output, entry, result, material, tier);
                case "freezer" -> emitFreezer(output, entry, result, material, tier);
                case "cryo_mixer" -> emitCryoMixer(output, entry, result, material, tier);
                case "polarizer" -> emitMagnetMachine(
                        output, entry, result, material, List.of("TwT", "PMP", "TdT"));
                case "magnetic_separator" -> emitMagnetMachine(
                        output, entry, result, material, List.of("TwT", "TdT", "PMP"));
                default -> {
                }
            }
        }
    }

    private static String materialPath(String materialId) {
        int colon = materialId.indexOf(':');
        return colon < 0 ? materialId : materialId.substring(colon + 1);
    }

    private static Item compact(String family, int tier) {
        return part(family + "_" + TIERS[tier]);
    }

    private static void emitPrinter(
            RecipeOutput output,
            MachineTierCatalog.Entry entry,
            Item result,
            String material,
            int tier) {
        Item casing = material(material, MaterialPrefixes.MACHINE_CASING);
        Item conveyor = compact("compact_electric_conveyor", tier);
        Item circuit = part(CIRCUITS[tier]);
        Item cable = cableOrWire(tier);
        Item pipe = material("stainless_steel", MaterialPrefixes.TINY_FLUID_PIPE);
        if (anyNull(result, casing, conveyor, circuit, cable, pipe)) {
            return;
        }
        accept(
                output,
                "machines/" + entry.variantId().getPath(),
                List.of("CPC", "wXh", "WMW"),
                Map.of(
                        "C", Ingredient.of(circuit),
                        "P", keyedIngredient(pipe, "stainless_steel", MaterialPrefixes.TINY_FLUID_PIPE),
                        "X", Ingredient.of(conveyor),
                        "W", Ingredient.of(cable),
                        "M", keyedIngredient(casing, material, MaterialPrefixes.MACHINE_CASING)),
                Map.of(
                        "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get()),
                        "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get())),
                new ItemStack(result));
    }

    private static void emitScanner(
            RecipeOutput output,
            MachineTierCatalog.Entry entry,
            Item result,
            String material,
            int tier) {
        Item casing = material(material, MaterialPrefixes.MACHINE_CASING);
        Item conveyor = compact("compact_electric_conveyor", tier);
        Item circuit = part(CIRCUITS[tier]);
        Item cable = cableOrWire(tier);
        Item plate = material("lumium", MaterialPrefixes.PLATE);
        if (anyNull(result, casing, conveyor, circuit, cable, plate)) {
            return;
        }
        accept(
                output,
                "machines/" + entry.variantId().getPath(),
                List.of("CPC", "wXh", "WMW"),
                Map.of(
                        "C", Ingredient.of(circuit),
                        "P", keyedIngredient(plate, "lumium", MaterialPrefixes.PLATE),
                        "X", Ingredient.of(conveyor),
                        "W", Ingredient.of(cable),
                        "M", keyedIngredient(casing, material, MaterialPrefixes.MACHINE_CASING)),
                Map.of(
                        "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get()),
                        "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get())),
                new ItemStack(result));
    }

    private static void emitAutocrafter(
            RecipeOutput output,
            MachineTierCatalog.Entry entry,
            Item result,
            String material,
            int tier) {
        Item casing = material(material, MaterialPrefixes.MACHINE_CASING_DOUBLE);
        Item arm = compact("compact_electric_robot_arm", tier);
        Item circuit = part(CIRCUITS[tier]);
        Item cable = cableOrWire(tier);
        if (anyNull(result, casing, arm, circuit, cable)) {
            return;
        }
        accept(
                output,
                "machines/" + entry.variantId().getPath(),
                List.of("WRW", "RwR", "CMC"),
                Map.of(
                        "W", Ingredient.of(cable),
                        "R", Ingredient.of(arm),
                        "C", Ingredient.of(circuit),
                        "M", keyedIngredient(casing, material, MaterialPrefixes.MACHINE_CASING_DOUBLE)),
                Map.of("w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static void emitElectricMixer(
            RecipeOutput output,
            MachineTierCatalog.Entry entry,
            Item result,
            String material,
            int tier) {
        Item casing = material(material, MaterialPrefixes.MACHINE_CASING);
        Item motor = compact("compact_electric_motor", tier);
        Item rotor = material("stainless_steel", MaterialPrefixes.ROTOR);
        Item plate = material("stainless_steel", stackedPlate(tier, false));
        if (anyNull(result, casing, motor, rotor, plate)) {
            return;
        }
        accept(
                output,
                "machines/" + entry.variantId().getPath(),
                List.of("PMP", "PRP", "hSw"),
                Map.of(
                        "P", keyedIngredient(plate, "stainless_steel", stackedPlate(tier, false)),
                        "M", keyedIngredient(casing, material, MaterialPrefixes.MACHINE_CASING),
                        "R", keyedIngredient(rotor, "stainless_steel", MaterialPrefixes.ROTOR),
                        "S", Ingredient.of(motor)),
                Map.of(
                        "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get()),
                        "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static void emitBoxinator(
            RecipeOutput output,
            MachineTierCatalog.Entry entry,
            Item result,
            String material,
            int tier) {
        Item casing = material(material, MaterialPrefixes.MACHINE_CASING);
        Item piston = compact("compact_electric_piston", tier);
        Item conveyor = compact("compact_electric_conveyor", tier);
        Item circuit = part(CIRCUITS[tier]);
        if (anyNull(result, casing, piston, conveyor, circuit)) {
            return;
        }
        accept(
                output,
                "machines/" + entry.variantId().getPath(),
                List.of("wP ", "CY ", "CM "),
                Map.of(
                        "P", Ingredient.of(piston),
                        "C", Ingredient.of(circuit),
                        "Y", Ingredient.of(conveyor),
                        "M", keyedIngredient(casing, material, MaterialPrefixes.MACHINE_CASING)),
                Map.of("w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static void emitLightning(
            RecipeOutput output,
            MachineTierCatalog.Entry entry,
            Item result,
            String material,
            int tier) {
        Item casing = material(material, MaterialPrefixes.MACHINE_CASING);
        Item quad = material(wireMaterial(tier), MaterialPrefixes.QUADRUPLE_WIRE);
        Item iron = material("iron", lightningIron(tier));
        if (anyNull(result, casing, quad, iron)) {
            return;
        }
        accept(
                output,
                "machines/" + entry.variantId().getPath(),
                List.of("XxX", "WwW", "XMX"),
                Map.of(
                        "X", keyedIngredient(iron, "iron", lightningIron(tier)),
                        "W", keyedIngredient(quad, wireMaterial(tier), MaterialPrefixes.QUADRUPLE_WIRE),
                        "M", keyedIngredient(casing, material, MaterialPrefixes.MACHINE_CASING)),
                Map.of(
                        "x", CraftingTools.of(ModItems.MATERIAL_WIRE_CUTTER.get()),
                        "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static MaterialPrefix lightningIron(int tier) {
        return switch (tier) {
            case 1 -> MaterialPrefixes.WIRE;
            case 2 -> MaterialPrefixes.DOUBLE_WIRE;
            case 3 -> MaterialPrefixes.QUADRUPLE_WIRE;
            case 4 -> MaterialPrefixes.OCTUPLE_WIRE;
            default -> MaterialPrefixes.HEXADECUPLE_WIRE;
        };
    }

    private static void emitAnalyzer(
            RecipeOutput output,
            MachineTierCatalog.Entry entry,
            Item result,
            String material,
            int tier,
            Item specialty) {
        Item casing = material(material, MaterialPrefixes.MACHINE_CASING);
        Item circuit = part(CIRCUITS[tier]);
        Item cable = cableOrWire(tier);
        Item processor = part("processor_crystal_diamond");
        Item emitter = compact("compact_signal_emitter", tier);
        Item sensor = compact("compact_sensor", tier);
        if (anyNull(
                result, casing, circuit, cable, processor, emitter, sensor, specialty)) {
            return;
        }
        accept(
                output,
                "machines/" + entry.variantId().getPath(),
                List.of("WXW", "ZMP", "CYC"),
                Map.of(
                        "W", Ingredient.of(cable),
                        "X", Ingredient.of(emitter),
                        "Z", Ingredient.of(specialty),
                        "M", keyedIngredient(casing, material, MaterialPrefixes.MACHINE_CASING),
                        "P", Ingredient.of(processor),
                        "C", Ingredient.of(circuit),
                        "Y", Ingredient.of(sensor)),
                Map.of(),
                new ItemStack(result));
    }

    private static void emitMassfab(
            RecipeOutput output,
            MachineTierCatalog.Entry entry,
            Item result,
            String material,
            int tier) {
        Item casing = material(material, MaterialPrefixes.MACHINE_CASING);
        Item ruby = part("processor_crystal_ruby");
        Item sapphire = part("processor_crystal_sapphire");
        Item field = compact("compact_force_field_emitter", Math.min(tier, 14));
        if (anyNull(result, casing, ruby, sapphire, field)) {
            return;
        }
        accept(
                output,
                "machines/" + entry.variantId().getPath(),
                List.of("RFS", "FMF", "RFS"),
                Map.of(
                        "R", Ingredient.of(ruby),
                        "F", Ingredient.of(field),
                        "S", Ingredient.of(sapphire),
                        "M", keyedIngredient(casing, material, MaterialPrefixes.MACHINE_CASING)),
                Map.of(),
                new ItemStack(result));
    }

    private static void emitReplicator(
            RecipeOutput output,
            MachineTierCatalog.Entry entry,
            Item result,
            String material,
            int tier) {
        Item casing = material(material, MaterialPrefixes.MACHINE_CASING);
        Item emerald = part("processor_crystal_emerald");
        Item sapphire = part("processor_crystal_sapphire");
        int partTier = Math.min(tier, 14);
        Item emitter = compact("compact_signal_emitter", partTier);
        Item field = compact("compact_force_field_emitter", partTier);
        if (anyNull(result, casing, emerald, sapphire, emitter, field)) {
            return;
        }
        accept(
                output,
                "machines/" + entry.variantId().getPath(),
                List.of("EXE", "FMF", "SXS"),
                Map.of(
                        "E", Ingredient.of(emerald),
                        "X", Ingredient.of(emitter),
                        "F", Ingredient.of(field),
                        "M", keyedIngredient(casing, material, MaterialPrefixes.MACHINE_CASING),
                        "S", Ingredient.of(sapphire)),
                Map.of(),
                new ItemStack(result));
    }

    private static void emitFreezer(
            RecipeOutput output,
            MachineTierCatalog.Entry entry,
            Item result,
            String material,
            int tier) {
        Item casing = material(material, MaterialPrefixes.MACHINE_CASING);
        Item silicon = material("silicon", stackedPlate(tier, true));
        Item plate = material("stainless_steel", stackedPlate(tier, false));
        if (anyNull(result, casing, silicon, plate)) {
            return;
        }
        accept(
                output,
                "machines/" + entry.variantId().getPath(),
                List.of("hPw", "PMP", "PSP"),
                Map.of(
                        "P", keyedIngredient(plate, "stainless_steel", stackedPlate(tier, false)),
                        "M", keyedIngredient(casing, material, MaterialPrefixes.MACHINE_CASING),
                        "S", keyedIngredient(silicon, "silicon", stackedPlate(tier, true))),
                Map.of(
                        "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get()),
                        "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static void emitCryoMixer(
            RecipeOutput output,
            MachineTierCatalog.Entry entry,
            Item result,
            String material,
            int tier) {
        Item casing = material(material, MaterialPrefixes.MACHINE_CASING);
        Item rotor = material("stainless_steel", MaterialPrefixes.ROTOR);
        Item silicon = material("silicon", stackedPlate(tier, true));
        Item plate = material("stainless_steel", stackedPlate(tier, false));
        if (anyNull(result, casing, rotor, silicon, plate)) {
            return;
        }
        accept(
                output,
                "machines/" + entry.variantId().getPath(),
                List.of("PMP", "PRP", "hSw"),
                Map.of(
                        "P", keyedIngredient(plate, "stainless_steel", stackedPlate(tier, false)),
                        "M", keyedIngredient(casing, material, MaterialPrefixes.MACHINE_CASING),
                        "R", keyedIngredient(rotor, "stainless_steel", MaterialPrefixes.ROTOR),
                        "S", keyedIngredient(silicon, "silicon", stackedPlate(tier, true))),
                Map.of(
                        "h", CraftingTools.of(ModItems.SMITHING_HAMMER.get()),
                        "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static void emitMagnetMachine(
            RecipeOutput output,
            MachineTierCatalog.Entry entry,
            Item result,
            String material,
            List<String> pattern) {
        Item casing = material(material, MaterialPrefixes.MACHINE_CASING);
        Item plate = material(material, MaterialPrefixes.PLATE);
        Item screw = material(material, MaterialPrefixes.SCREW);
        if (anyNull(result, casing, plate, screw)) {
            return;
        }
        accept(
                output,
                "machines/" + entry.variantId().getPath(),
                pattern,
                Map.of(
                        "T", keyedIngredient(screw, material, MaterialPrefixes.SCREW),
                        "P", keyedIngredient(plate, material, MaterialPrefixes.PLATE),
                        "M", keyedIngredient(casing, material, MaterialPrefixes.MACHINE_CASING)),
                Map.of(
                        "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get()),
                        "d", CraftingTools.of(ModItems.MATERIAL_SCREWDRIVER.get())),
                new ItemStack(result));
    }

    private static MaterialPrefix stackedPlate(int tier, boolean silicon) {
        int rank = Math.min(tier, silicon ? 5 : 4);
        return switch (rank) {
            case 1 -> MaterialPrefixes.PLATE;
            case 2 -> MaterialPrefixes.DOUBLE_PLATE;
            case 3 -> MaterialPrefixes.TRIPLE_PLATE;
            case 4 -> MaterialPrefixes.QUADRUPLE_PLATE;
            default -> MaterialPrefixes.QUINTUPLE_PLATE;
        };
    }

    private static void addQuantumEnergizerRecipes(RecipeOutput output) {
        for (QuantumEnergizerProfile profile : QuantumEnergizerCatalog.profiles()) {
            var item = ModItems.quantumEnergizerItemsById().get(profile.id());
            if (item == null) {
                continue;
            }
            LinkedHashMap<String, Ingredient> ingredients = new LinkedHashMap<>();
            boolean complete = true;
            for (var entry : profile.recipe().keys().entrySet()) {
                Ingredient resolved = resolveIngredient(entry.getValue());
                if (resolved == null) {
                    complete = false;
                    break;
                }
                ingredients.put(entry.getKey(), resolved);
            }
            if (!complete) {
                continue;
            }
            accept(
                    output,
                    profile.id().getPath(),
                    profile.recipe().pattern(),
                    ingredients,
                    Map.of(),
                    new ItemStack(item.get()));
        }
    }

    private static Item resolve(QuantumEnergizerProfile.Ingredient ingredient) {
        if (ingredient.item() != null && !ingredient.item().isBlank()) {
            return registered(ingredient.item());
        }
        if (ingredient.prefix() == null || ingredient.material() == null) {
            return null;
        }
        try {
            return MaterialLookup.item(
                            ingredient.material(),
                            new MaterialPrefix("cruciblecraft:" + ingredient.prefix()))
                    .orElse(null);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Ingredient resolveIngredient(QuantumEnergizerProfile.Ingredient ingredient) {
        if (ingredient.item() != null && !ingredient.item().isBlank()) {
            Item item = registered(ingredient.item());
            return item == null ? null : Ingredient.of(item);
        }
        if (ingredient.prefix() == null || ingredient.material() == null) {
            return null;
        }
        try {
            return MaterialLookup.ingredient(
                            ingredient.material(),
                            new MaterialPrefix("cruciblecraft:" + ingredient.prefix()))
                    .orElse(null);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void addMassfabRecipe(RecipeOutput output) {
        Optional<Fluid> matter = ModFluids.chemical("matter_neutral")
                .map(entry -> entry.source().get());
        Optional<ItemStack> ingot = MaterialLookup.tryStack(
                "neutronium", MaterialPrefixes.INGOT, 1);
        if (matter.isEmpty() || ingot.isEmpty()) {
            return;
        }
        output.accept(
                id("massfab/neutral_matter_to_neutronium"),
                new GTRecipeEntry(
                        ModRecipeMaps.MASSFAB.id(),
                        new GTRecipe(
                                List.of(),
                                List.of(),
                                List.of(ingot.orElseThrow()),
                                List.of(new FluidStack(matter.orElseThrow(), 144)),
                                List.of(),
                                List.of(GTRecipe.GUARANTEED_CHANCE),
                                128,
                                4_096L,
                                0L)),
                null);
    }

    private static void addGrapheneNanofab(RecipeOutput output) {
        nanofabGraphene(
                output,
                "nanofab/graphene_plate_from_carbon_dust",
                1,
                256,
                MaterialPrefixes.DUST,
                1,
                MaterialPrefixes.PLATE,
                1);
        nanofabGraphene(
                output,
                "nanofab/graphene_wire_from_carbon_dust",
                5,
                256,
                MaterialPrefixes.DUST,
                1,
                MaterialPrefixes.WIRE,
                2);
    }

    private static void nanofabGraphene(
            RecipeOutput output,
            String path,
            int circuit,
            int duration,
            MaterialPrefix inputPrefix,
            int inputCount,
            MaterialPrefix outputPrefix,
            int outputCount) {
        Optional<ItemStack> carbon = MaterialLookup.tryStack(
                "carbon", inputPrefix, inputCount);
        Optional<ItemStack> graphene = MaterialLookup.tryStack(
                "graphene", outputPrefix, outputCount);
        if (carbon.isEmpty() || graphene.isEmpty()) {
            return;
        }
        output.accept(
                id(path),
                new GTRecipeEntry(
                        ModRecipeMaps.NANOFAB.id(),
                        new GTRecipe(
                                List.of(
                                        circuit(circuit),
                                        stackInput(carbon.orElseThrow())),
                                List.of(0, inputCount),
                                List.of(graphene.orElseThrow()),
                                List.of(),
                                List.of(),
                                List.of(GTRecipe.GUARANTEED_CHANCE),
                                duration,
                                16L,
                                0L)),
                null);
    }

    private static void addMagneticSeparator(RecipeOutput output) {
        Optional<ItemStack> bedrock = MaterialLookup.tryStack(
                "bedrock", MaterialPrefixes.DUST, 1);
        Optional<ItemStack> deepslate = MaterialLookup.tryStack(
                "deepslate", MaterialPrefixes.DUST, 1);
        Optional<ItemStack> adamantine = MaterialLookup.tryStack(
                "adamantine", MaterialPrefixes.TINY_DUST, 1);
        if (bedrock.isEmpty() || deepslate.isEmpty() || adamantine.isEmpty()) {
            return;
        }
        List<ItemStack> outputs = new ArrayList<>();
        List<Integer> chances = new ArrayList<>();
        outputs.add(deepslate.orElseThrow());
        chances.add(7_000);
        outputs.add(adamantine.orElseThrow());
        chances.add(3_000);
        addOptionalDust(outputs, chances, "atlantium", MaterialPrefixes.TINY_DUST, 3_000);
        addOptionalDust(outputs, chances, "rare_earth", MaterialPrefixes.TINY_DUST, 3_000);
        addOptionalDust(outputs, chances, "neodymium", MaterialPrefixes.TINY_DUST, 3_000);
        addOptionalDust(outputs, chances, "vanadium_pentoxide", MaterialPrefixes.TINY_DUST, 3_000);
        if (outputs.size() > 6) {
            outputs = new ArrayList<>(outputs.subList(0, 6));
            chances = new ArrayList<>(chances.subList(0, 6));
        }
        output.accept(
                id("magnetic_separator/bedrock_dust"),
                new GTRecipeEntry(
                        ModRecipeMaps.MAGNETIC_SEPARATOR.id(),
                        new GTRecipe(
                                List.of(stackInput(bedrock.orElseThrow())),
                                List.of(1),
                                outputs,
                                List.of(),
                                List.of(),
                                chances,
                                144,
                                64L,
                                0L)),
                null);
    }

    private static void addOptionalDust(
            List<ItemStack> outputs,
            List<Integer> chances,
            String material,
            MaterialPrefix prefix,
            int chance) {
        MaterialLookup.tryStack(material, prefix, 1).ifPresent(stack -> {
            outputs.add(stack);
            chances.add(chance);
        });
    }

    private static void addLightningAdamantium(RecipeOutput output) {
        Optional<ItemStack> adamantine = MaterialLookup.tryStack(
                "adamantine", MaterialPrefixes.DUST, 7);
        Optional<ItemStack> adamantium = MaterialLookup.tryStack(
                "adamantium", MaterialPrefixes.DUST, 3);
        Optional<Fluid> oxygen = ModFluids.chemical("oxygen")
                .map(entry -> entry.source().get());
        if (adamantine.isEmpty() || adamantium.isEmpty() || oxygen.isEmpty()) {
            return;
        }
        output.accept(
                id("lightning/adamantine_to_adamantium"),
                new GTRecipeEntry(
                        ModRecipeMaps.LIGHTNING.id(),
                        new GTRecipe(
                                List.of(
                                        circuit(1),
                                        stackInput(adamantine.orElseThrow())),
                                List.of(0, 7),
                                List.of(adamantium.orElseThrow()),
                                List.of(),
                                List.of(new FluidStack(oxygen.orElseThrow(), 4_000)),
                                List.of(GTRecipe.GUARANTEED_CHANCE),
                                128,
                                2_048L,
                                0L)),
                null);
    }

    private static void addPolarizerRecipes(RecipeOutput output) {
        polarize(
                output,
                "polarizer/iron_ingot",
                "iron",
                "iron_magnetic",
                16L);
        polarize(
                output,
                "polarizer/steel_ingot",
                "steel",
                "steel_magnetic",
                16L);
        polarize(
                output,
                "polarizer/neodymium_ingot",
                "neodymium",
                "neodymium_magnetic",
                128L);
    }

    private static void polarize(
            RecipeOutput output,
            String path,
            String inputMaterial,
            String resultMaterial,
            long eut) {
        Ingredient input = MaterialLookup.ingredient(
                inputMaterial, MaterialPrefixes.INGOT).orElse(null);
        ItemStack result = MaterialLookup.tryStack(
                resultMaterial, MaterialPrefixes.INGOT, 1).orElse(null);
        if (input == null || result == null) {
            if ("iron".equals(inputMaterial)) {
                input = Ingredient.of(Items.IRON_INGOT);
            }
        }
        if (input == null || result == null) {
            return;
        }
        output.accept(
                id(path),
                new GTRecipeEntry(
                        ModRecipeMaps.POLARIZER.id(),
                        new GTRecipe(
                                List.of(input),
                                List.of(1),
                                List.of(result),
                                List.of(),
                                List.of(),
                                List.of(GTRecipe.GUARANTEED_CHANCE),
                                144,
                                eut,
                                0L)),
                null);
    }

    private static void addFreezerRecipes(RecipeOutput output) {
        freezerWaterItem(
                output,
                "freezer/water_to_snowball",
                1,
                16,
                250,
                new ItemStack(Items.SNOWBALL));
        freezerWaterItem(
                output,
                "freezer/water_to_snow_layer",
                2,
                32,
                500,
                new ItemStack(Blocks.SNOW));
        freezerWaterItem(
                output,
                "freezer/water_to_snow_block",
                3,
                64,
                1_000,
                new ItemStack(Blocks.SNOW_BLOCK));
        freezerWaterItem(
                output,
                "freezer/water_to_ice_block",
                4,
                128,
                1_000,
                new ItemStack(Blocks.ICE));
    }

    private static void freezerWaterItem(
            RecipeOutput output,
            String path,
            int circuitConfig,
            int duration,
            int waterMb,
            ItemStack result) {
        output.accept(
                id(path),
                new GTRecipeEntry(
                        ModRecipeMaps.FREEZER.id(),
                        new GTRecipe(
                                List.of(circuit(circuitConfig)),
                                List.of(0),
                                List.of(result),
                                List.of(new FluidStack(Fluids.WATER, waterMb)),
                                List.of(),
                                List.of(GTRecipe.GUARANTEED_CHANCE),
                                duration,
                                16L,
                                0L)),
                null);
    }

    private static void addCryoMixerRecipes(RecipeOutput output) {
        cryoNiter(
                output,
                "cryo_mixer/cryotheum_from_niter",
                1,
                1,
                1,
                250,
                2,
                32);
        cryoNiter(
                output,
                "cryo_mixer/cryotheum_from_niter_x4",
                4,
                4,
                4,
                1_000,
                8,
                128);
        cryoSalt(
                output,
                "cryo_mixer/cryotheum_from_sodium_nitrate",
                "sodium_nitrate",
                1,
                250,
                2,
                32);
        cryoSalt(
                output,
                "cryo_mixer/cryotheum_from_potassium_nitrate",
                "potassium_nitrate",
                1,
                250,
                2,
                32);
    }

    private static void cryoNiter(
            RecipeOutput output,
            String path,
            int redstone,
            int blizzTiny,
            int niter,
            int waterMb,
            int cryotheum,
            int duration) {
        Item redstoneDust = material("redstone", MaterialPrefixes.DUST);
        Item blizz = material("blizz", MaterialPrefixes.TINY_DUST);
        Item niterDust = material("niter", MaterialPrefixes.DUST);
        Item cryotheumDust = material("cryotheum", MaterialPrefixes.DUST);
        if (anyNull(redstoneDust, blizz, niterDust, cryotheumDust)) {
            return;
        }
        output.accept(
                id(path),
                new GTRecipeEntry(
                        ModRecipeMaps.CRYO_MIXER.id(),
                        new GTRecipe(
                                List.of(
                                        keyedIngredient(redstoneDust, "redstone", MaterialPrefixes.DUST),
                                        keyedIngredient(blizz, "blizz", MaterialPrefixes.TINY_DUST),
                                        keyedIngredient(niterDust, "niter", MaterialPrefixes.DUST)),
                                List.of(redstone, blizzTiny, niter),
                                List.of(keyedStack(cryotheumDust, "cryotheum", MaterialPrefixes.DUST, cryotheum)),
                                List.of(new FluidStack(Fluids.WATER, waterMb)),
                                List.of(),
                                List.of(GTRecipe.GUARANTEED_CHANCE),
                                duration,
                                16L,
                                0L)),
                null);
    }

    private static void cryoSalt(
            RecipeOutput output,
            String path,
            String salt,
            int count,
            int waterMb,
            int cryotheum,
            int duration) {
        Item redstoneDust = material("redstone", MaterialPrefixes.DUST);
        Item blizz = material("blizz", MaterialPrefixes.TINY_DUST);
        Item saltDust = material(salt, MaterialPrefixes.DUST);
        Item cryotheumDust = material("cryotheum", MaterialPrefixes.DUST);
        if (anyNull(redstoneDust, blizz, saltDust, cryotheumDust)) {
            return;
        }
        output.accept(
                id(path),
                new GTRecipeEntry(
                        ModRecipeMaps.CRYO_MIXER.id(),
                        new GTRecipe(
                                List.of(
                                        keyedIngredient(redstoneDust, "redstone", MaterialPrefixes.DUST),
                                        keyedIngredient(blizz, "blizz", MaterialPrefixes.TINY_DUST),
                                        keyedIngredient(saltDust, salt, MaterialPrefixes.DUST)),
                                List.of(count, count, count),
                                List.of(keyedStack(cryotheumDust, "cryotheum", MaterialPrefixes.DUST, cryotheum)),
                                List.of(new FluidStack(Fluids.WATER, waterMb)),
                                List.of(),
                                List.of(GTRecipe.GUARANTEED_CHANCE),
                                duration,
                                16L,
                                0L)),
                null);
    }

    private static void addAlloyMixers(RecipeOutput output) {
        mixerDusts(
                output,
                "chemical/mixer/trinitanium",
                List.of(
                        dust("trinium", 2),
                        dust("titanium", 1)),
                dust("trinitanium", 3));
        mixerDusts(
                output,
                "chemical/mixer/trinaquadalloy",
                List.of(
                        dust("trinium", 6),
                        dust("naquadah", 2),
                        dust("carbon", 1)),
                dust("trinaquadalloy", 9));
        mixerDusts(
                output,
                "chemical/mixer/vibramantium",
                List.of(
                        dust("vibranium", 1),
                        dust("adamantium", 3)),
                dust("vibramantium", 4));
    }

    private record Counted(ItemStack stack) {}

    private static Counted dust(String material, int count) {
        return MaterialLookup.tryStack(material, MaterialPrefixes.DUST, count)
                .map(Counted::new)
                .orElse(null);
    }

    private static void mixerDusts(
            RecipeOutput output,
            String path,
            List<Counted> inputs,
            Counted result) {
        if (result == null || inputs.stream().anyMatch(value -> value == null)) {
            return;
        }
        output.accept(
                id(path),
                new GTRecipeEntry(
                        ModRecipeMaps.MIXER.id(),
                        new GTRecipe(
                                inputs.stream()
                                        .map(input -> stackInput(input.stack()))
                                        .toList(),
                                inputs.stream().map(input -> input.stack().getCount()).toList(),
                                List.of(result.stack().copy()),
                                List.of(),
                                List.of(),
                                List.of(GTRecipe.GUARANTEED_CHANCE),
                                16,
                                16L,
                                0L)),
                null);
    }

    private static void addCompactParts(RecipeOutput output) {
        for (int tier = 0; tier <= 14; tier++) {
            emitMotor(output, tier);
            emitPump(output, tier);
            emitConveyor(output, tier);
            emitPiston(output, tier);
            emitRobotArm(output, tier);
            emitField(output, tier);
            emitEmitter(output, tier);
            emitSensor(output, tier);
        }
    }

    private static void emitMotor(RecipeOutput output, int tier) {
        if (tier <= 1) {
            emitMotorVariant(output, tier, "iron_magnetic", "_iron_magnetic");
            emitMotorVariant(output, tier, "steel_magnetic", "_steel_magnetic");
            return;
        }
        String magnetic = tier <= 3 ? "steel_magnetic" : "neodymium_magnetic";
        emitMotorVariant(output, tier, magnetic, "");
    }

    private static void emitMotorVariant(
            RecipeOutput output, int tier, String magnetic, String recipeSuffix) {
        Item result = part("compact_electric_motor_" + TIERS[tier]);
        Item magnet = material(magnetic, magnetPrefix(tier));
        Item plate = material(ELECTRIC[tier], MaterialPrefixes.CURVED_PLATE);
        Item rod = material(ELECTRIC[tier], MaterialPrefixes.ROD);
        Item winding = material(windingMaterial(tier), MOTOR_WINDING[tier]);
        Item cable = cableOrWire(tier);
        if (anyNull(result, magnet, plate, rod, winding, cable)) {
            return;
        }
        accept(
                output,
                "compact_electric_motor_" + TIERS[tier] + recipeSuffix,
                List.of("CWR", "WIW", "PWC"),
                Map.of(
                        "I", keyedIngredient(magnet, magnetic, magnetPrefix(tier)),
                        "P", keyedIngredient(plate, ELECTRIC[tier], MaterialPrefixes.CURVED_PLATE),
                        "R", keyedIngredient(rod, ELECTRIC[tier], MaterialPrefixes.ROD),
                        "W", keyedIngredient(winding, windingMaterial(tier), MOTOR_WINDING[tier]),
                        "C", Ingredient.of(cable)),
                Map.of(),
                new ItemStack(result));
    }

    private static MaterialPrefix magnetPrefix(int tier) {
        if (tier == 0) {
            return MaterialPrefixes.BOLT;
        }
        if (tier <= 5) {
            return MaterialPrefixes.ROD;
        }
        return MaterialPrefixes.LONG_ROD;
    }

    private static String windingMaterial(int tier) {
        return tier <= 3 ? "copper" : "annealed_copper";
    }

    private static void emitPump(RecipeOutput output, int tier) {
        Item result = part("compact_electric_pump_" + TIERS[tier]);
        Item motor = part("compact_electric_motor_" + TIERS[tier]);
        Item ring = material("rubber", MaterialPrefixes.RING);
        Item rotor = material(ELECTRIC[tier], MaterialPrefixes.ROTOR);
        Item screw = material(ELECTRIC[tier], MaterialPrefixes.SCREW);
        Item plate = material(ELECTRIC[tier], MaterialPrefixes.CURVED_PLATE);
        if (anyNull(result, motor, ring, rotor, screw, plate)) {
            return;
        }
        accept(
                output,
                "compact_electric_pump_" + TIERS[tier],
                List.of("TXO", "dPw", "OMT"),
                Map.of(
                        "T", keyedIngredient(screw, ELECTRIC[tier], MaterialPrefixes.SCREW),
                        "X", keyedIngredient(rotor, ELECTRIC[tier], MaterialPrefixes.ROTOR),
                        "O", keyedIngredient(ring, "rubber", MaterialPrefixes.RING),
                        "P", keyedIngredient(plate, ELECTRIC[tier], MaterialPrefixes.CURVED_PLATE),
                        "M", Ingredient.of(motor)),
                Map.of(
                        "d", CraftingTools.of(ModItems.MATERIAL_SCREWDRIVER.get()),
                        "w", CraftingTools.of(ModItems.MATERIAL_WRENCH.get())),
                new ItemStack(result));
    }

    private static void emitConveyor(RecipeOutput output, int tier) {
        Item result = part("compact_electric_conveyor_" + TIERS[tier]);
        Item motor = part("compact_electric_motor_" + TIERS[tier]);
        Item rubber = material("rubber", MaterialPrefixes.PLATE);
        Item cable = cableOrWire(tier);
        if (anyNull(result, motor, rubber, cable)) {
            return;
        }
        accept(
                output,
                "compact_electric_conveyor_" + TIERS[tier],
                List.of("RRR", "MCM", "RRR"),
                Map.of(
                        "R", keyedIngredient(rubber, "rubber", MaterialPrefixes.PLATE),
                        "M", Ingredient.of(motor),
                        "C", Ingredient.of(cable)),
                Map.of(),
                new ItemStack(result));
    }

    private static void emitPiston(RecipeOutput output, int tier) {
        Item result = part("compact_electric_piston_" + TIERS[tier]);
        Item motor = part("compact_electric_motor_" + TIERS[tier]);
        Item plate = material(ELECTRIC[tier], MaterialPrefixes.PLATE);
        Item rod = material(ELECTRIC[tier], MaterialPrefixes.ROD);
        Item gear = material(ELECTRIC[tier], MaterialPrefixes.SMALL_GEAR);
        Item screw = material(ELECTRIC[tier], MaterialPrefixes.SCREW);
        if (anyNull(result, motor, plate, rod, gear, screw)) {
            return;
        }
        accept(
                output,
                "compact_electric_piston_" + TIERS[tier],
                List.of("TPP", "dSS", "TMG"),
                Map.of(
                        "T", keyedIngredient(screw, ELECTRIC[tier], MaterialPrefixes.SCREW),
                        "P", keyedIngredient(plate, ELECTRIC[tier], MaterialPrefixes.PLATE),
                        "S", keyedIngredient(rod, ELECTRIC[tier], MaterialPrefixes.ROD),
                        "M", Ingredient.of(motor),
                        "G", keyedIngredient(gear, ELECTRIC[tier], MaterialPrefixes.SMALL_GEAR)),
                Map.of("d", CraftingTools.of(ModItems.MATERIAL_SCREWDRIVER.get())),
                new ItemStack(result));
    }

    private static void emitRobotArm(RecipeOutput output, int tier) {
        Item result = part("compact_electric_robot_arm_" + TIERS[tier]);
        Item motor = part("compact_electric_motor_" + TIERS[tier]);
        Item piston = part("compact_electric_piston_" + TIERS[tier]);
        Item cable = cableOrWire(tier);
        Item circuit = part(CIRCUITS[tier]);
        Item rod = material(ELECTRIC[tier], MaterialPrefixes.ROD);
        if (anyNull(result, motor, piston, cable, circuit, rod)) {
            return;
        }
        accept(
                output,
                "compact_electric_robot_arm_" + TIERS[tier],
                List.of("CCC", "MSM", "PES"),
                Map.of(
                        "C", Ingredient.of(cable),
                        "M", Ingredient.of(motor),
                        "S", keyedIngredient(rod, ELECTRIC[tier], MaterialPrefixes.ROD),
                        "P", Ingredient.of(piston),
                        "E", Ingredient.of(circuit)),
                Map.of(),
                new ItemStack(result));
    }

    private static void emitField(RecipeOutput output, int tier) {
        Item result = part("compact_force_field_emitter_" + TIERS[tier]);
        Item wire = material("osmium_elemental", FIELD_OSMIUM[tier]);
        Item plate = material(ELECTRIC[tier], MaterialPrefixes.DOUBLE_PLATE);
        Item circuit = part(CIRCUITS[tier]);
        Item gem = fieldGem(tier);
        if (anyNull(result, wire, plate, circuit, gem)) {
            return;
        }
        accept(
                output,
                "compact_force_field_emitter_" + TIERS[tier],
                List.of("WPW", "CGC", "WPW"),
                Map.of(
                        "W", keyedIngredient(wire, "osmium_elemental", FIELD_OSMIUM[tier]),
                        "P", keyedIngredient(plate, ELECTRIC[tier], MaterialPrefixes.DOUBLE_PLATE),
                        "C", Ingredient.of(circuit),
                        "G", Ingredient.of(gem)),
                Map.of(),
                new ItemStack(result));
    }

    private static void emitEmitter(RecipeOutput output, int tier) {
        Item result = part("compact_signal_emitter_" + TIERS[tier]);
        Item sensorWire = material(wireMaterial(tier), MaterialPrefixes.QUADRUPLE_WIRE);
        Item plate = material(ELECTRIC[tier], MaterialPrefixes.CURVED_PLATE);
        Item circuit = part(CIRCUITS[tier]);
        Item cable = cableOrWire(tier);
        Item gem = sensorGem(tier);
        if (anyNull(result, sensorWire, plate, circuit, cable, gem)) {
            return;
        }
        accept(
                output,
                "compact_signal_emitter_" + TIERS[tier],
                List.of("SPC", "WQP", "CWS"),
                Map.of(
                        "S", keyedIngredient(sensorWire, wireMaterial(tier), MaterialPrefixes.QUADRUPLE_WIRE),
                        "P", keyedIngredient(plate, ELECTRIC[tier], MaterialPrefixes.CURVED_PLATE),
                        "C", Ingredient.of(circuit),
                        "W", Ingredient.of(cable),
                        "Q", Ingredient.of(gem)),
                Map.of(),
                new ItemStack(result));
    }

    private static void emitSensor(RecipeOutput output, int tier) {
        Item result = part("compact_sensor_" + TIERS[tier]);
        Item sensorWire = material(wireMaterial(tier), MaterialPrefixes.WIRE);
        Item plate = material(ELECTRIC[tier], MaterialPrefixes.CURVED_PLATE);
        Item circuit = part(CIRCUITS[tier]);
        Item gem = sensorGem(tier);
        if (anyNull(result, sensorWire, plate, circuit, gem)) {
            return;
        }
        accept(
                output,
                "compact_sensor_" + TIERS[tier],
                List.of("P Q", "PS ", "CPP"),
                Map.of(
                        "P", keyedIngredient(plate, ELECTRIC[tier], MaterialPrefixes.CURVED_PLATE),
                        "Q", Ingredient.of(gem),
                        "S", keyedIngredient(sensorWire, wireMaterial(tier), MaterialPrefixes.WIRE),
                        "C", Ingredient.of(circuit)),
                Map.of(),
                new ItemStack(result));
    }

    private static Item fieldGem(int tier) {
        if (tier <= 1) {
            return Items.ENDER_PEARL;
        }
        if (tier <= 3) {
            return Items.ENDER_EYE;
        }
        return Items.NETHER_STAR;
    }

    private static Item sensorGem(int tier) {
        if (tier <= 2) {
            return Items.QUARTZ;
        }
        if (tier == 3) {
            return Items.EMERALD;
        }
        if (tier == 4) {
            return Items.ENDER_PEARL;
        }
        if (tier == 5) {
            return Items.ENDER_EYE;
        }
        return Items.NETHER_STAR;
    }

    private static String wireMaterial(int tier) {
        if (tier <= 5) {
            return switch (tier) {
                case 0 -> "lead";
                case 1 -> "tin";
                case 2 -> "copper";
                case 3 -> "gold";
                case 4 -> "aluminium";
                default -> "platinum";
            };
        }
        return tier <= 10 ? "graphene" : "superconductor";
    }

    private static Item cableOrWire(int tier) {
        if (tier <= 5) {
            return material(wireMaterial(tier), MaterialPrefixes.CABLE);
        }
        return material(wireMaterial(tier), MaterialPrefixes.WIRE);
    }

    private static void addCrystalAndLaser(RecipeOutput output) {
        Item emptyLaser = part("laser_gas_empty");
        Item circuitGood = part("circuit_good");
        Item silver = material("silver", MaterialPrefixes.PLATE);
        Item screw = material("stainless_steel", MaterialPrefixes.SCREW);
        Item copperCable = material("copper", MaterialPrefixes.CABLE);
        if (!anyNull(emptyLaser, circuitGood, silver, screw, copperCable)) {
            accept(
                    output,
                    "laser_gas_empty",
                    List.of("CWM", "WGx", "MTd"),
                    Map.of(
                            "C", Ingredient.of(circuitGood),
                            "W", keyedIngredient(copperCable, "copper", MaterialPrefixes.CABLE),
                            "M", keyedIngredient(silver, "silver", MaterialPrefixes.PLATE),
                            "T", keyedIngredient(screw, "stainless_steel", MaterialPrefixes.SCREW),
                            "G", Ingredient.of(Items.GLASS)),
                    Map.of(
                            "x", CraftingTools.of(ModItems.MATERIAL_WIRE_CUTTER.get()),
                            "d", CraftingTools.of(ModItems.MATERIAL_SCREWDRIVER.get())),
                    new ItemStack(emptyLaser));
        }
        fillLaserGas(output, emptyLaser, "helium", "laser_gas_he");
        fillLaserGas(output, emptyLaser, "neon", "laser_gas_ne");
        fillLaserGas(output, emptyLaser, "argon", "laser_gas_ar");
        fillLaserGas(output, emptyLaser, "krypton", "laser_gas_kr");
        fillLaserGas(output, emptyLaser, "xenon", "laser_gas_xe");
        fillLaserGas(output, emptyLaser, "helium_neon", "laser_gas_hene");
        fillLaserGas(output, emptyLaser, "carbon_monoxide", "laser_gas_co");
        fillLaserGas(output, emptyLaser, "carbon_dioxide", "laser_gas_co2");
        Item filled = part("laser_gas_hene");
        Item emptyProcessor = part("processor_crystal_empty");
        Item ultimate = part("circuit_ultimate");
        Item plate = part("circuit_plate_platinum");
        if (!anyNull(emptyProcessor, ultimate, plate, filled)) {
            accept(
                    output,
                    "processor_crystal_empty",
                    List.of("CLC", "LBL", "CLC"),
                    Map.of(
                            "C", Ingredient.of(ultimate),
                            "L", Ingredient.of(filled),
                            "B", Ingredient.of(plate)),
                    Map.of(),
                    new ItemStack(emptyProcessor));
        }
        laserCrystal(output, "diamond", "circuit_crystal_diamond");
        laserCrystal(output, "ruby", "circuit_crystal_ruby");
        laserCrystal(output, "emerald", "circuit_crystal_emerald");
        laserCrystal(output, "sapphire", "circuit_crystal_sapphire");
        pressProcessor(output, "circuit_crystal_diamond", "processor_crystal_diamond");
        pressProcessor(output, "circuit_crystal_ruby", "processor_crystal_ruby");
        pressProcessor(output, "circuit_crystal_emerald", "processor_crystal_emerald");
        pressProcessor(output, "circuit_crystal_sapphire", "processor_crystal_sapphire");
    }

    private static void fillLaserGas(
            RecipeOutput output,
            Item emptyLaser,
            String fluidPath,
            String resultPath) {
        Optional<Fluid> gas = ModFluids.chemical(fluidPath)
                .map(entry -> entry.source().get());
        Item filled = part(resultPath);
        if (emptyLaser == null || filled == null || gas.isEmpty()) {
            return;
        }
        output.accept(
                id("chemical/canner/" + resultPath),
                new GTRecipeEntry(
                        ModRecipeMaps.CANNER.id(),
                        new GTRecipe(
                                List.of(Ingredient.of(emptyLaser)),
                                List.of(1),
                                List.of(new ItemStack(filled)),
                                List.of(new FluidStack(gas.orElseThrow(), 1_000)),
                                List.of(),
                                List.of(GTRecipe.GUARANTEED_CHANCE),
                                128,
                                16L,
                                0L)),
                null);
    }

    private static void laserCrystal(
            RecipeOutput output, String gem, String resultPath) {
        Item plate = material(gem, MaterialPrefixes.PLATE_GEM);
        String lensMaterial = "emerald";
        Item lens = material(lensMaterial, new MaterialPrefix("cruciblecraft:lens"));
        Item result = part(resultPath);
        if (anyNull(plate, lens, result)) {
            lensMaterial = "olivine";
            lens = material(lensMaterial, new MaterialPrefix("cruciblecraft:lens"));
        }
        if (anyNull(plate, lens, result)) {
            return;
        }
        output.accept(
                id("machine/laser_engraver/" + resultPath),
                new GTRecipeEntry(
                        ModRecipeMaps.LASER_ENGRAVER.id(),
                        new GTRecipe(
                                List.of(
                                        keyedIngredient(plate, gem, MaterialPrefixes.PLATE_GEM),
                                        keyedIngredient(
                                                lens,
                                                lensMaterial,
                                                new MaterialPrefix("cruciblecraft:lens"))),
                                List.of(1, 0),
                                List.of(new ItemStack(result)),
                                List.of(),
                                List.of(),
                                List.of(GTRecipe.GUARANTEED_CHANCE),
                                64,
                                256L,
                                0L)),
                null);
    }

    private static void pressProcessor(
            RecipeOutput output, String circuitPath, String resultPath) {
        Item empty = part("processor_crystal_empty");
        Item circuit = part(circuitPath);
        Item result = part(resultPath);
        if (anyNull(empty, circuit, result)) {
            return;
        }
        output.accept(
                id("machine/press/" + resultPath),
                new GTRecipeEntry(
                        ModRecipeMaps.PRESS.id(),
                        new GTRecipe(
                                List.of(Ingredient.of(empty), Ingredient.of(circuit)),
                                List.of(1, 1),
                                List.of(new ItemStack(result)),
                                List.of(),
                                List.of(),
                                List.of(GTRecipe.GUARANTEED_CHANCE),
                                16,
                                16L,
                                0L)),
                null);
    }

    private static void addFusionHullRecipes(RecipeOutput output) {
        Item casing = material("steel_galvanized", MaterialPrefixes.MACHINE_CASING);
        Item advanced = part("circuit_advanced");
        Item ultimate = part("circuit_ultimate");
        Item vent = part("vent_cover");
        Item motor = part("compact_electric_motor_lv");
        Item diamond = part("processor_crystal_diamond");
        Item ruby = part("processor_crystal_ruby");
        Item emerald = part("processor_crystal_emerald");
        Item sapphire = part("processor_crystal_sapphire");
        Item cutter = ModItems.MATERIAL_WIRE_CUTTER.get();
        Item screwdriver = ModItems.MATERIAL_SCREWDRIVER.get();
        Item ventilation = ModItems.VENTILATION_UNIT.get();
        Item versatile = ModItems.VERSATILE_PROCESSOR_UNIT.get();
        Item logic = ModItems.LOGIC_PROCESSOR_UNIT.get();
        Item control = ModItems.CONTROL_PROCESSOR_UNIT.get();
        Item storage = ModItems.STORAGE_PROCESSOR_UNIT.get();
        Item conversion = ModItems.CONVERSION_PROCESSOR_UNIT.get();
        if (!anyNull(
                ventilation, casing, advanced, vent, motor, cutter, screwdriver)) {
            accept(
                    output,
                    "ventilation_unit",
                    List.of("FwF", "CMC", "EdE"),
                    Map.of(
                            "F", Ingredient.of(vent),
                            "C", Ingredient.of(advanced),
                            "M", keyedIngredient(casing, "steel_galvanized", MaterialPrefixes.MACHINE_CASING),
                            "E", Ingredient.of(motor)),
                    Map.of(
                            "w", Ingredient.of(cutter),
                            "d", Ingredient.of(screwdriver)),
                    new ItemStack(ventilation));
        }
        if (!anyNull(
                versatile, casing, ultimate, diamond, sapphire, ruby, emerald)) {
            accept(
                    output,
                    "versatile_processor_unit",
                    List.of("DCS", "CMC", "RCE"),
                    Map.of(
                            "D", Ingredient.of(diamond),
                            "C", Ingredient.of(ultimate),
                            "S", Ingredient.of(sapphire),
                            "M", keyedIngredient(casing, "steel_galvanized", MaterialPrefixes.MACHINE_CASING),
                            "R", Ingredient.of(ruby),
                            "E", Ingredient.of(emerald)),
                    Map.of(),
                    new ItemStack(versatile));
        }
        emitQuadcoreProcessor(
                output, logic, "logic_processor_unit", casing, ultimate, diamond);
        emitQuadcoreProcessor(
                output, control, "control_processor_unit", casing, ultimate, ruby);
        emitQuadcoreProcessor(
                output, storage, "storage_processor_unit", casing, ultimate, emerald);
        emitQuadcoreProcessor(
                output,
                conversion,
                "conversion_processor_unit",
                casing,
                ultimate,
                sapphire);
    }

    private static void emitQuadcoreProcessor(
            RecipeOutput output,
            Item result,
            String path,
            Item casing,
            Item circuit,
            Item processor) {
        if (anyNull(result, casing, circuit, processor)) {
            return;
        }
        accept(
                output,
                path,
                List.of("PCP", "CMC", "PCP"),
                Map.of(
                        "P", Ingredient.of(processor),
                        "C", Ingredient.of(circuit),
                        "M", Ingredient.of(casing)),
                Map.of(),
                new ItemStack(result));
    }

    private static void addBedrockDrillRecipes(RecipeOutput output) {
        Item denseTi = material("titanium", MaterialPrefixes.MACHINE_CASING_DENSE);
        Item denseWs = material(
                "tungstensteel", MaterialPrefixes.MACHINE_CASING_DENSE);
        Item gear = material("tungstensteel", MaterialPrefixes.GEAR);
        Item drill = material(
                "tungstensteel", new MaterialPrefix("cruciblecraft:tool_head_drill"));
        Item diamond = Items.DIAMOND;
        Item ruby = part("processor_crystal_ruby");
        Item conveyor = part("compact_electric_conveyor_iv");
        Item circuit = part("circuit_ultimate");
        if (!anyNull(denseWs, gear, diamond) && ModItems.BEDROCK_DRILL_HEAD.get() != null) {
            LinkedHashMap<String, Ingredient> keys = new LinkedHashMap<>();
            keys.put("M", Ingredient.of(denseWs));
            keys.put("G", keyedIngredient(gear, "tungstensteel", MaterialPrefixes.GEAR));
            keys.put("D", Ingredient.of(diamond));
            if (drill != null) {
                keys.put("I", Ingredient.of(drill));
                accept(
                        output,
                        "bedrock_drill_head",
                        List.of("DID", "GMG", "DID"),
                        keys,
                        Map.of(),
                        new ItemStack(ModItems.BEDROCK_DRILL_HEAD.get()));
            }
        }
        if (!anyNull(denseTi, gear, ruby, conveyor, circuit)
                && drill != null) {
            accept(
                    output,
                    "bedrock_drill",
                    List.of("PYP", "CMC", "GIG"),
                    Map.of(
                            "P", Ingredient.of(ruby),
                            "Y", Ingredient.of(conveyor),
                            "C", Ingredient.of(circuit),
                            "M", keyedIngredient(denseTi, "titanium", MaterialPrefixes.MACHINE_CASING_DENSE),
                            "G", keyedIngredient(gear, "tungstensteel", MaterialPrefixes.GEAR),
                            "I", Ingredient.of(drill)),
                    Map.of(),
                    new ItemStack(ModItems.BEDROCK_DRILL.get()));
        }
    }

    private static void addLongDistanceRecipes(RecipeOutput output) {
        Item cable = material("annealed_copper", MaterialPrefixes.QUADRUPLE_CABLE);
        Item cutter = ModItems.MATERIAL_WIRE_CUTTER.get();
        if (cable != null && cutter != null) {
            for (LongDistanceTransformerProfile profile
                    : LongDistanceTransformerCatalog.endpoints()) {
                var item = ModItems.longDistanceTransformerItemsById()
                        .get(profile.id());
                Item host = registered(profile.hostTransformer().toString());
                if (item == null || host == null) {
                    continue;
                }
                accept(
                        output,
                        profile.id().getPath(),
                        List.of("WMW", "MxM", "WMW"),
                        Map.of(
                                "W", keyedIngredient(cable, "annealed_copper", MaterialPrefixes.QUADRUPLE_CABLE),
                                "M", Ingredient.of(host)),
                        Map.of("x", Ingredient.of(cutter)),
                        new ItemStack(item.get()));
            }
        }
        Item rubber = material("rubber", MaterialPrefixes.PLATE);
        Item copperCurve = material("copper", MaterialPrefixes.CURVED_PLATE);
        Item aluminiumCurve = material("aluminium", MaterialPrefixes.CURVED_PLATE);
        if (anyNull(rubber, copperCurve, aluminiumCurve)) {
            return;
        }
        for (LongDistanceWireProfile profile : LongDistanceTransformerCatalog.wires()) {
            var item = ModItems.longDistanceWireItemsById().get(profile.id());
            Item core = material(profile.core(), MaterialPrefixes.HEXADECUPLE_WIRE);
            if (item == null || core == null) {
                continue;
            }
            accept(
                    output,
                    profile.id().getPath(),
                    List.of("RSR", "PWP", "RSR"),
                    Map.of(
                            "R", keyedIngredient(rubber, "rubber", MaterialPrefixes.PLATE),
                            "S", keyedIngredient(aluminiumCurve, "aluminium", MaterialPrefixes.CURVED_PLATE),
                            "P", keyedIngredient(copperCurve, "copper", MaterialPrefixes.CURVED_PLATE),
                            "W", keyedIngredient(core, profile.core(), MaterialPrefixes.HEXADECUPLE_WIRE)),
                    Map.of(),
                    new ItemStack(item.get()));
        }
    }

    private static Ingredient circuit(int config) {
        return DataComponentIngredient.of(
                false,
                DataComponentPredicate.builder()
                        .expect(ModComponents.CIRCUIT_CONFIG.get(), config)
                        .build(),
                ModItems.PROGRAMMED_CIRCUIT.get());
    }

    private static Item part(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, path);
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? null : item;
    }

    private static Item registered(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null || !BuiltInRegistries.ITEM.containsKey(loc)) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(loc);
        return item == Items.AIR ? null : item;
    }

    private static Item material(String material, MaterialPrefix prefix) {
        try {
            return MaterialLookup.item(material, prefix).orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Ingredient materialIngredient(String material, MaterialPrefix prefix) {
        try {
            return MaterialLookup.ingredient(material, prefix).orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static ItemStack materialStack(String material, MaterialPrefix prefix) {
        return materialStack(material, prefix, 1);
    }

    private static ItemStack materialStack(String material, MaterialPrefix prefix, int count) {
        try {
            return MaterialLookup.tryStack(material, prefix, count).orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /**
     * DataComponentIngredient.of(stack) copies every default component, which
     * the recipe-map index rejects. Match the item, plus only prefix_material
     * for a shared prefix Item.
     */
    private static Ingredient stackInput(ItemStack stack) {
        String prefixMaterial = stack.get(ModComponents.PREFIX_MATERIAL.get());
        return prefixMaterial == null
                ? Ingredient.of(stack.getItem())
                : MaterialLookup.prefixMaterialIngredient(stack.getItem(), prefixMaterial);
    }

    private static Ingredient keyedIngredient(
            Item item, String material, MaterialPrefix prefix) {
        if (item == null) {
            return null;
        }
        if (item instanceof com.masson.cruciblecraft.content.item.PrefixMaterialItem) {
            return materialIngredient(material, prefix);
        }
        return Ingredient.of(item);
    }

    private static ItemStack keyedStack(
            Item item, String material, MaterialPrefix prefix, int count) {
        if (item == null) {
            return null;
        }
        if (item instanceof com.masson.cruciblecraft.content.item.PrefixMaterialItem) {
            return materialStack(material, prefix, count);
        }
        return new ItemStack(item, count);
    }

    private static boolean anyNull(Item... items) {
        for (Item item : items) {
            if (item == null) {
                return true;
            }
        }
        return false;
    }

    private static void accept(
            RecipeOutput output,
            String path,
            List<String> pattern,
            Map<String, Ingredient> ingredients,
            Map<String, Ingredient> catalysts,
            ItemStack result) {
        output.accept(
                id(path),
                new ShapedCatalystRecipe(pattern, ingredients, catalysts, result),
                null);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }
}
