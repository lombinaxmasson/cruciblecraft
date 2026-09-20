package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.client.color.MachineBlockColor;
import com.masson.cruciblecraft.client.screen.MachineGuiTextures;

import net.minecraft.resources.ResourceLocation;

class Gt6VisualFixResourceTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static final Path ASSETS = ROOT.resolve("assets/cruciblecraft");
    private static final Path GENERATED = Path.of(
            "src/generated/resources/assets/cruciblecraft");
    private static final List<String> PLACEHOLDER_PREFIXES = List.of(
            "small_casing",
            "double_ingot",
            "triple_ingot",
            "quadruple_ingot",
            "quintuple_ingot",
            "storage_ingot",
            "storage_plate",
            "gem",
            "gem_chipped",
            "gem_flawed",
            "gem_flawless",
            "gem_exquisite",
            "gem_legendary",
            "chain",
            "minecart_wheels",
            "wire",
            "double_wire",
            "cable",
            "quadruple_fluid_pipe",
            "nonuple_fluid_pipe",
            "fluid_pipe");

    @Test
    void lensUsesGt6MetallicIconsetNotGlass() throws Exception {
        Path prefix = Path.of(
                "src/main/resources/data/cruciblecraft/material_prefixes/lens.json");
        var json = JsonParser.parseString(Files.readString(prefix)).getAsJsonObject();
        assertEquals(
                "cruciblecraft:item/material/lens",
                json.get("model_texture").getAsString());
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "assets/cruciblecraft/textures/item/material/lens.png")));
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "assets/cruciblecraft/textures/item/material/lens_overlay.png")));
        String manifest = Files.readString(ROOT.resolve(
                "assets/cruciblecraft/gt6_misc_prefix_art_manifest.json"));
        assertTrue(manifest.contains("materialicons/metallic/lens.png"));
        assertTrue(manifest.contains("materialicons/metallic/lens_overlay.png"));
    }

    @Test
    void bushModelIsDualLayerAndCrusherTintsFollowCatalog() throws Exception {
        String bush = Files.readString(ROOT.resolve(
                "assets/cruciblecraft/models/block/plant/gt_bush_cube_bare.json"));
        assertTrue(bush.contains("overlay/bush"));
        assertTrue(bush.contains("tintindex"));
        assertEquals("bronze", MachineBlockColor.casingMaterialId("bronze_crusher"));
        assertEquals(
                "tungstensteel",
                MachineBlockColor.casingMaterialId("tungstensteel_crusher"));
    }

    @Test
    void leftoverPrefixIconsAreGt6MaterialIconsNotVanillaPlaceholders()
            throws Exception {
        Path prefixes = Path.of(
                "src/main/resources/data/cruciblecraft/material_prefixes");
        Path textures = ASSETS.resolve("textures/item/material");
        for (String form : PLACEHOLDER_PREFIXES) {
            JsonObject json = JsonParser.parseString(
                    Files.readString(prefixes.resolve(form + ".json")))
                    .getAsJsonObject();
            String texture = json.get("model_texture").getAsString();
            assertEquals("cruciblecraft:item/material/" + form, texture, form);
            assertFalse(texture.startsWith("minecraft:"), form);
            assertTrue(
                    Files.isRegularFile(textures.resolve(form + ".png")),
                    form);
        }
        String manifest = Files.readString(
                ASSETS.resolve("gt6_placeholder_art_manifest.json"));
        assertTrue(manifest.contains("materialicons/metallic/casingsmall.png"));
        assertTrue(manifest.contains("materialicons/metallic/gemlegendary.png"));
        assertTrue(Files.isRegularFile(textures.resolve("ingot_hot_overlay.png")));
        assertTrue(Files.isRegularFile(
                textures.resolve("minecart_wheels_overlay.png")));
        assertTrue(Files.isRegularFile(textures.resolve("storage_plate_overlay.png")));
        String plateManifest = Files.readString(
                ASSETS.resolve("gt6_storage_plate_art_manifest.json"));
        assertTrue(plateManifest.contains("materialicons/metallic/blockplate.png"));
        assertTrue(plateManifest.contains(
                "materialicons/metallic/blockplate_overlay.png"));
    }

    @Test
    void mixerEmiChromeAndImportedFluidsUseGt6Pngs() throws Exception {
        assertEquals(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "textures/gui/machines/mixer.png"),
                MachineGuiTextures.forPath("electric_mixer"));
        assertEquals(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "textures/gui/machines/cryomixer.png"),
                MachineGuiTextures.forPath("cryo_mixer"));
        assertTrue(Files.isRegularFile(
                ASSETS.resolve("textures/gui/machines/mixer.png")));
        assertTrue(Files.isRegularFile(
                ASSETS.resolve("textures/gui/machines/cryomixer.png")));
        for (String fluid : List.of(
                "hot_molten_sodium",
                "hot_tritiated_water",
                "hot_molten_tin",
                "hot_helium",
                "hot_carbon_dioxide",
                "water_geothermal",
                "natural_gas",
                "oil_light",
                "oil_medium",
                "oil_heavy",
                "oil_extra_heavy")) {
            assertTrue(
                    Files.isRegularFile(ASSETS.resolve(
                            "textures/fluid/gt6_import/" + fluid + ".png")),
                    fluid);
        }
        String atlas = Files.readString(Path.of(
                "src/main/resources/assets/minecraft/atlases/blocks.json"));
        assertTrue(atlas.contains("\"source\": \"fluid\""));
        assertTrue(atlas.contains("\"prefix\": \"fluid/\""));
    }

    @Test
    void uniqueWiresPipesWoodAndPanelsBindSourceBackedModels() throws Exception {
        assertEquals(
                "cruciblecraft:conductor/wiregt02_item",
                parent(GENERATED.resolve("models/item/hslasteel/double_wire.json")));
        assertEquals(
                "cruciblecraft:conductor/cablegt01_item",
                parent(GENERATED.resolve(
                        "models/item/yttrium_barium_cuprate/cable.json")));
        assertEquals(
                "cruciblecraft:conductor/wiregt01_item",
                parent(GENERATED.resolve(
                        "models/item/electrotine_alloy/wire.json")));
        assertEquals(
                "cruciblecraft:pipe/fluid_quadruple_item",
                parent(GENERATED.resolve(
                        "models/item/wood/quadruple_fluid_pipe.json")));
        assertEquals(
                "cruciblecraft:pipe/fluid_quadruple_item",
                parent(GENERATED.resolve(
                        "models/item/wood_treated/quadruple_fluid_pipe.json")));
        assertEquals(
                "cruciblecraft:pipe/fluid_8_item",
                parent(GENERATED.resolve(
                        "models/item/steel_galvanized/fluid_pipe.json")));
        assertTrue(Files.isRegularFile(GENERATED.resolve(
                "blockstates/hslasteel/double_wire.json")));
        assertTrue(Files.isRegularFile(GENERATED.resolve(
                "blockstates/blue_alloy/cable.json")));
        assertTrue(Files.isRegularFile(GENERATED.resolve(
                "blockstates/wood_treated/fluid_pipe.json")));
        JsonObject wood = JsonParser.parseString(Files.readString(
                GENERATED.resolve("models/item/gt_wood/blue_mahoe_planks.json")))
                .getAsJsonObject();
        assertEquals(
                "cruciblecraft:gt_wood/blue_mahoe_planks",
                wood.get("parent").getAsString());
        assertTrue(Files.isRegularFile(ASSETS.resolve(
                "textures/block/gt6/iconsets/planks_bluemahoe.png")));
        assertTrue(Files.isRegularFile(ASSETS.resolve(
                "textures/block/gt6/iconsets/asphalt.png")));
        assertTrue(Files.isRegularFile(ASSETS.resolve(
                "textures/block/gt6/iconsets/concrete.png")));
        assertTrue(Files.isRegularFile(ASSETS.resolve(
                "textures/block/gt6/iconsets/cfoam_hardened.png")));
        assertTrue(Files.isRegularFile(ASSETS.resolve(
                "textures/block/gt6/iconsets/planks_treated.png")));
        assertTrue(Files.isRegularFile(ASSETS.resolve(
                "textures/block/gt6/iconsets/planks_rubber.png")));
        assertTrue(Files.isRegularFile(ASSETS.resolve(
                "textures/block/gt6/materialicons/metallic/blocksolid.png")));
        assertTrue(Files.isRegularFile(ASSETS.resolve(
                "textures/block/material/wire.png")));
        JsonObject spikes = JsonParser.parseString(Files.readString(
                GENERATED.resolve("models/spikes_fancy/fancy_spikes.json")))
                .getAsJsonObject();
        assertEquals(
                "cruciblecraft:block/spike_wall",
                spikes.get("parent").getAsString());
        assertEquals(
                "cruciblecraft:block/gt6/materialicons/metallic/blocksolid",
                spikes.getAsJsonObject("textures").get("all").getAsString());
        JsonObject omni = JsonParser.parseString(Files.readString(
                GENERATED.resolve("models/spikes_fancy/pink.json")))
                .getAsJsonObject();
        assertEquals(
                "cruciblecraft:block/spike_omni",
                omni.get("parent").getAsString());
        JsonObject woodBlock = JsonParser.parseString(Files.readString(
                GENERATED.resolve("models/gt_wood/blue_mahoe_planks.json")))
                .getAsJsonObject();
        assertEquals("minecraft:block/cube_all", woodBlock.get("parent").getAsString());
        assertEquals(
                "cruciblecraft:block/gt6/iconsets/planks_bluemahoe",
                woodBlock.getAsJsonObject("textures").get("all").getAsString());
        JsonObject panelItem = JsonParser.parseString(Files.readString(
                GENERATED.resolve("models/item/panel/asphalt_white.json")))
                .getAsJsonObject();
        assertEquals(
                "cruciblecraft:panel/asphalt_white",
                panelItem.get("parent").getAsString());
        assertTrue(Files.isRegularFile(ASSETS.resolve(
                "models/block/spike_wall.json")));
        assertTrue(Files.isRegularFile(ASSETS.resolve(
                "models/block/tinted_panel.json")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/generated/resources/data/cruciblecraft/loot_table/blocks"
                        + "/gt_wood/blue_mahoe_planks.json")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/generated/resources/data/cruciblecraft/loot_table/blocks"
                        + "/panel/asphalt_white.json")));
        long panelBlocks = 0;
        try (var paths = Files.list(GENERATED.resolve("blockstates/panel"))) {
            panelBlocks = paths
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith("asphalt_")
                                || name.startsWith("cfoam_")
                                || name.startsWith("concrete_");
                    })
                    .count();
        }
        assertEquals(48, panelBlocks);
        JsonObject fireproof = JsonParser.parseString(Files.readString(
                GENERATED.resolve("models/planks_fireproof/fireproof_planks.json")))
                .getAsJsonObject();
        assertEquals(
                "cruciblecraft:block/gt6/iconsets/planks_rubber",
                fireproof.getAsJsonObject("textures").get("all").getAsString());
    }

    private static String parent(Path model) throws Exception {
        assertTrue(Files.isRegularFile(model), model.toString());
        return JsonParser.parseString(Files.readString(model))
                .getAsJsonObject()
                .get("parent")
                .getAsString();
    }
}
