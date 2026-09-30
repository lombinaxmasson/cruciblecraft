package com.masson.cruciblecraft.worldgen;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.GtStoneBlock;
import com.masson.cruciblecraft.content.block.StoneLayerRockOreBlock;
import com.masson.cruciblecraft.content.block.StoneLayerStoneBlock;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * No-mod GT6 {@code BlocksGT} stone/cobble/mossy cubes, vanilla stone /
 * deepslate mappings, and {@code BlockRockOres} dense cubes.
 */
public final class StoneLayerStones {
    public enum Role {
        STONE,
        COBBLE,
        MOSSY_COBBLE,
        SMALL_BRICKS;

        public String id() {
            return switch (this) {
                case STONE -> "stone";
                case COBBLE -> "cobble";
                case MOSSY_COBBLE -> "mossy_cobble";
                case SMALL_BRICKS -> "small_bricks";
            };
        }

        public static Role fromId(String id) {
            return switch (id) {
                case "stone" -> STONE;
                case "cobble" -> COBBLE;
                case "mossy_cobble" -> MOSSY_COBBLE;
                case "small_bricks" -> SMALL_BRICKS;
                default -> throw new IllegalArgumentException(
                        "unknown stone-layer role " + id);
            };
        }

        public boolean naturalLayer() {
            return this != SMALL_BRICKS;
        }
    }

    public record Cube(
            String material,
            Role role,
            String registryPath,
            ResourceLocation id,
            String texture,
            int harvestLevel,
            float hardness,
            float resistance,
            String english,
            String chinese,
            boolean denseOre,
            int flammability) {}

    private static final List<Cube> CUBES = load("stone_blocks", false);
    private static final List<Cube> ROCK_ORES = load("rock_ores", true);
    private static final List<Cube> VILLAGE_BRICKS = load("village_bricks", false);
    private static final Map<String, Cube> BY_PATH = index(allRegistered());
    private static final Map<String, Cube> DENSE_BY_MATERIAL = denseIndex(ROCK_ORES);

    private StoneLayerStones() {}

    public static List<Cube> cubes() {
        return CUBES;
    }

    public static List<Cube> rockOres() {
        return ROCK_ORES;
    }

    public static List<Cube> villageBricks() {
        return VILLAGE_BRICKS;
    }

    public static List<Cube> registeredCubes() {
        return allRegistered();
    }

    public static boolean isRegisteredCube(String registryPath) {
        return BY_PATH.containsKey(registryPath);
    }

    public static boolean isDenseOre(String material) {
        return DENSE_BY_MATERIAL.containsKey(material);
    }

    public static Cube require(String registryPath) {
        Cube cube = BY_PATH.get(registryPath);
        if (cube == null) {
            throw new IllegalArgumentException(
                    "Unknown stone-layer cube " + registryPath);
        }
        return cube;
    }

    public static BlockState cube(String material, Role role) {
        if (material == null || material.isEmpty()) {
            return Blocks.STONE.defaultBlockState();
        }
        Cube dense = DENSE_BY_MATERIAL.get(material);
        if (dense != null) {
            return switch (role) {
                case STONE -> ModBlocks.layerOrExistingStone(dense.registryPath())
                        .get()
                        .defaultBlockState();
                case COBBLE -> Blocks.COBBLESTONE.defaultBlockState();
                case MOSSY_COBBLE -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                case SMALL_BRICKS -> villageBrickState(material);
            };
        }
        if (StoneLayerCatalog.DEEPSLATE.equals(material)) {
            return switch (role) {
                case STONE -> Blocks.DEEPSLATE.defaultBlockState();
                case COBBLE, MOSSY_COBBLE ->
                        Blocks.COBBLED_DEEPSLATE.defaultBlockState();
                case SMALL_BRICKS -> villageBrickState(material);
            };
        }
        if ("stone".equals(material)) {
            return switch (role) {
                case STONE -> Blocks.STONE.defaultBlockState();
                case COBBLE -> Blocks.COBBLESTONE.defaultBlockState();
                case MOSSY_COBBLE -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                case SMALL_BRICKS -> villageBrickState(material);
            };
        }
        return ModBlocks.layerOrExistingStone(material + "/" + role.id())
                .get()
                .defaultBlockState();
    }

    private static BlockState villageBrickState(String material) {
        throw new IllegalArgumentException(
                "village SBRIK is not a layer cube for " + material);
    }

    public static boolean isNaturalLayerCube(BlockState state) {
        if (state.getBlock() instanceof StoneLayerStoneBlock layer) {
            return layer.role().naturalLayer();
        }
        if (state.getBlock() instanceof StoneLayerRockOreBlock) {
            return true;
        }
        if (state.getBlock() instanceof GtStoneBlock gt) {
            return !gt.variant().slab() && gt.variant().meta() < 3;
        }
        return false;
    }

    public static String materialOf(BlockState state) {
        if (state.getBlock() instanceof StoneLayerStoneBlock layer) {
            return layer.materialId();
        }
        if (state.getBlock() instanceof StoneLayerRockOreBlock ore) {
            return ore.materialId();
        }
        if (state.getBlock() instanceof GtStoneBlock gt) {
            return gt.variant().stone();
        }
        return "stone";
    }

    private static List<Cube> allRegistered() {
        List<Cube> all = new ArrayList<>(
                CUBES.size() + ROCK_ORES.size() + VILLAGE_BRICKS.size());
        all.addAll(CUBES);
        all.addAll(ROCK_ORES);
        all.addAll(VILLAGE_BRICKS);
        return Collections.unmodifiableList(all);
    }

    private static Map<String, Cube> index(List<Cube> cubes) {
        LinkedHashMap<String, Cube> byPath = new LinkedHashMap<>();
        for (Cube cube : cubes) {
            if (byPath.put(cube.registryPath(), cube) != null) {
                throw new IllegalStateException(
                        "Duplicate stone-layer cube " + cube.registryPath());
            }
        }
        return Collections.unmodifiableMap(byPath);
    }

    private static Map<String, Cube> denseIndex(List<Cube> cubes) {
        LinkedHashMap<String, Cube> byMaterial = new LinkedHashMap<>();
        for (Cube cube : cubes) {
            if (byMaterial.put(cube.material(), cube) != null) {
                throw new IllegalStateException(
                        "Duplicate dense layer material " + cube.material());
            }
        }
        return Collections.unmodifiableMap(byMaterial);
    }

    private static List<Cube> load(String key, boolean denseOre) {
        try (InputStream in = StoneLayerStones.class.getResourceAsStream(
                "/data/cruciblecraft/worldgen_catalog/stone_layer_rocks.json")) {
            if (in == null) {
                throw new IllegalStateException("missing stone_layer_rocks.json");
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonArray array = root.getAsJsonArray(key);
            if (array == null) {
                throw new IllegalStateException("missing " + key);
            }
            List<Cube> cubes = new ArrayList<>(array.size());
            for (int i = 0; i < array.size(); i++) {
                JsonObject row = array.get(i).getAsJsonObject();
                String path = row.get("registry_path").getAsString();
                cubes.add(new Cube(
                        row.get("material").getAsString(),
                        Role.fromId(row.get("role").getAsString()),
                        path,
                        ResourceLocation.fromNamespaceAndPath(
                                CrucibleCraft.MODID, path),
                        row.get("texture").getAsString(),
                        row.get("harvest_level").getAsInt(),
                        row.get("hardness").getAsFloat(),
                        row.get("resistance").getAsFloat(),
                        row.get("english").getAsString(),
                        row.get("chinese").getAsString(),
                        denseOre
                                || (row.has("dense_ore")
                                        && row.get("dense_ore").getAsBoolean()),
                        row.has("flammability")
                                ? row.get("flammability").getAsInt()
                                : 0));
            }
            if (cubes.isEmpty()) {
                throw new IllegalStateException("empty " + key);
            }
            return Collections.unmodifiableList(cubes);
        } catch (IOException error) {
            throw new IllegalStateException("stone layer cubes", error);
        }
    }

    public static String harvestTag(int harvestLevel) {
        return switch (harvestLevel) {
            case 0 -> "";
            case 1 -> "stone";
            case 2 -> "iron";
            default -> "diamond";
        };
    }

    public static String modelName(String registryPath) {
        return registryPath.toLowerCase(Locale.ROOT);
    }
}
