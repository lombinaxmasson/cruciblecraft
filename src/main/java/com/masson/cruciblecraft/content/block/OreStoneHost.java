package com.masson.cruciblecraft.content.block;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;

import net.minecraft.util.StringRepresentable;

/**
 * PrefixBlock-style stone host for unique, hosted, small, and broken ores.
 * Layer cubes share this list so flecks sit on the same texture as the rock.
 */
public enum OreStoneHost implements StringRepresentable {
    STONE("stone", "minecraft:block/stone", "minecraft:block/cobblestone", true),
    DEEPSLATE(
            "deepslate",
            "minecraft:block/deepslate",
            "minecraft:block/cobbled_deepslate",
            false),
    NETHERRACK(
            "netherrack",
            "minecraft:block/netherrack",
            "minecraft:block/netherrack",
            false),
    GRANITE_BLACK(
            "granite_black",
            "cruciblecraft:block/gt6/stones/gt.stone.granite.black/stone",
            "cruciblecraft:block/gt6/stones/gt.stone.granite.black/cobble",
            true),
    GRANITE_RED(
            "granite_red",
            "cruciblecraft:block/gt6/stones/gt.stone.granite.red/stone",
            "cruciblecraft:block/gt6/stones/gt.stone.granite.red/cobble",
            true),
    BASALT(
            "basalt",
            "cruciblecraft:block/gt6/stones/gt.stone.basalt/stone",
            "cruciblecraft:block/gt6/stones/gt.stone.basalt/cobble",
            true),
    MARBLE(
            "marble",
            "cruciblecraft:block/gt6/stones/gt.stone.marble/stone",
            "cruciblecraft:block/gt6/stones/gt.stone.marble/cobble",
            true),
    LIMESTONE(
            "limestone",
            "cruciblecraft:block/gt6/stones/gt.stone.limestone/stone",
            "cruciblecraft:block/gt6/stones/gt.stone.limestone/cobble",
            true),
    GRANITE(
            "granite",
            "cruciblecraft:block/gt6/stones/gt.stone.granite/stone",
            "cruciblecraft:block/gt6/stones/gt.stone.granite/cobble",
            true),
    DIORITE(
            "diorite",
            "cruciblecraft:block/gt6/stones/gt.stone.diorite/stone",
            "cruciblecraft:block/gt6/stones/gt.stone.diorite/cobble",
            true),
    ANDESITE(
            "andesite",
            "cruciblecraft:block/gt6/stones/gt.stone.andesite/stone",
            "cruciblecraft:block/gt6/stones/gt.stone.andesite/cobble",
            true),
    KOMATIITE(
            "komatiite",
            "cruciblecraft:block/gt6/stones/gt.stone.komatiite/stone",
            "cruciblecraft:block/gt6/stones/gt.stone.komatiite/cobble",
            true),
    GREENSCHIST(
            "greenschist",
            "cruciblecraft:block/gt6/stones/gt.stone.greenschist/stone",
            "cruciblecraft:block/gt6/stones/gt.stone.greenschist/cobble",
            true),
    BLUESCHIST(
            "blueschist",
            "cruciblecraft:block/gt6/stones/gt.stone.blueschist/stone",
            "cruciblecraft:block/gt6/stones/gt.stone.blueschist/cobble",
            true),
    KIMBERLITE(
            "kimberlite",
            "cruciblecraft:block/gt6/stones/gt.stone.kimberlite/stone",
            "cruciblecraft:block/gt6/stones/gt.stone.kimberlite/cobble",
            true),
    QUARTZITE(
            "quartzite",
            "cruciblecraft:block/gt6/stones/gt.stone.quartzite/stone",
            "cruciblecraft:block/gt6/stones/gt.stone.quartzite/cobble",
            true),
    SLATE(
            "slate",
            "cruciblecraft:block/gt6/stones/gt.stone.slate/stone",
            "cruciblecraft:block/gt6/stones/gt.stone.slate/cobble",
            true),
    SHALE(
            "shale",
            "cruciblecraft:block/gt6/stones/gt.stone.shale/stone",
            "cruciblecraft:block/gt6/stones/gt.stone.shale/cobble",
            true),
    COAL(
            "coal",
            "cruciblecraft:block/gt6/rock_ores/ore_anthracite",
            "minecraft:block/cobblestone",
            true),
    LIGNITE(
            "lignite",
            "cruciblecraft:block/gt6/rock_ores/ore_lignite",
            "minecraft:block/cobblestone",
            true),
    SALT(
            "salt",
            "cruciblecraft:block/gt6/rock_ores/ore_salt",
            "minecraft:block/cobblestone",
            true),
    SYLVITE(
            "sylvite",
            "cruciblecraft:block/gt6/rock_ores/ore_rocksalt",
            "minecraft:block/cobblestone",
            true),
    BAUXITE(
            "bauxite",
            "cruciblecraft:block/gt6/rock_ores/ore_bauxite",
            "minecraft:block/cobblestone",
            true),
    OIL_SHALE(
            "oil_shale",
            "cruciblecraft:block/gt6/rock_ores/ore_oil",
            "minecraft:block/cobblestone",
            true),
    GYPSUM(
            "gypsum",
            "cruciblecraft:block/gt6/rock_ores/ore_gypsum",
            "minecraft:block/cobblestone",
            true),
    MILKY_QUARTZ(
            "milky_quartz",
            "cruciblecraft:block/gt6/rock_ores/ore_milkyquartz",
            "minecraft:block/cobblestone",
            true),
    NETHER_QUARTZ(
            "nether_quartz",
            "cruciblecraft:block/gt6/rock_ores/ore_netherquartz",
            "minecraft:block/netherrack",
            true);

    private static final Map<String, OreStoneHost> BY_NAME =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
                    OreStoneHost::getSerializedName,
                    Function.identity()));
    private static final List<OreStoneHost> UNIQUE_OVERWORLD =
            Arrays.stream(values()).filter(OreStoneHost::uniqueOverworld).toList();

    private final String serialized;
    private final String stoneTexture;
    private final String cobbleTexture;
    private final boolean uniqueOverworld;

    OreStoneHost(
            String serialized,
            String stoneTexture,
            String cobbleTexture,
            boolean uniqueOverworld) {
        this.serialized = serialized;
        this.stoneTexture = stoneTexture;
        this.cobbleTexture = cobbleTexture;
        this.uniqueOverworld = uniqueOverworld;
    }

    public boolean uniqueOverworld() {
        return uniqueOverworld;
    }

    public String stoneTexture() {
        return stoneTexture;
    }

    public String cobbleTexture() {
        return cobbleTexture;
    }

    public String stoneModel() {
        return "cruciblecraft:block/ore_host/" + serialized;
    }

    public String cobbleModel() {
        return "cruciblecraft:block/ore_host/" + serialized + "_cobble";
    }

    public Host catalogHost() {
        return this == DEEPSLATE ? Host.DEEPSLATE : Host.STONE;
    }

    public String secondaryDust() {
        return switch (this) {
            case STONE -> "stone";
            case DEEPSLATE -> "deepslate";
            case NETHERRACK -> "netherrack";
            default -> serialized;
        };
    }

    public static List<OreStoneHost> uniqueOverworldHosts() {
        return UNIQUE_OVERWORLD;
    }

    public static OreStoneHost byName(String serialized) {
        OreStoneHost host = BY_NAME.get(serialized);
        if (host == null) {
            throw new IllegalArgumentException("unknown ore host " + serialized);
        }
        return host;
    }

    public static OreStoneHost ofLayer(String material) {
        if (material == null || material.isEmpty()) {
            return STONE;
        }
        OreStoneHost host = BY_NAME.get(material.toLowerCase(Locale.ROOT));
        return host == null ? STONE : host;
    }

    @Override
    public String getSerializedName() {
        return serialized;
    }
}
