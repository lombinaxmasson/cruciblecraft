package com.masson.cruciblecraft.registry;

import java.util.List;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.recipe.AnvilMode;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.resources.ResourceLocation;

public final class ModRecipeMaps {
    /*
     * These maps are process-wide snapshots, not side-specific registries.
     * Integrated play reloads them from both client and server events. This is
     * safe because datapack recipes and synchronized tags are equivalent after
     * configuration, and RecipeMap publishes each replacement atomically.
     */
    public static final RecipeMap COKE_OVEN = create("coke_oven");
    public static final RecipeMap CRUSHER = create("crusher");
    public static final RecipeMap ANVIL = create("anvil");
    public static final RecipeMap ANVIL_BEND_SMALL = create("anvil_bend_small");
    public static final RecipeMap ANVIL_BEND_BIG = create("anvil_bend_big");
    public static final RecipeMap SLUICE = create("sluice");
    public static final RecipeMap BATH = create("bath");
    public static final RecipeMap CENTRIFUGE = create("centrifuge");
    public static final RecipeMap SHREDDER = create("shredder");
    public static final RecipeMap SIFTER = create("sifter");
    public static final RecipeMap SMELTER = create("smelter");
    public static final RecipeMap MORTAR = create("mortar");
    public static final RecipeMap EXTRUDER = create("extruder");
    public static final RecipeMap CUTTER = create("cutter");
    public static final RecipeMap LATHE = create("lathe");
    public static final RecipeMap ROLLINGMILL = create("rollingmill");
    public static final RecipeMap ROLLBENDER = create("rollbender");
    public static final RecipeMap WIREMILL = create("wiremill");
    public static final RecipeMap BENDER = create("bender");
    public static final RecipeMap ASSEMBLER = create("assembler");
    public static final RecipeMap WELDER = create("welder");
    public static final RecipeMap PRESS = create("press");

    public static final List<RecipeMap> ALL = List.of(
            COKE_OVEN,
            CRUSHER,
            ANVIL,
            ANVIL_BEND_SMALL,
            ANVIL_BEND_BIG,
            SLUICE,
            BATH,
            CENTRIFUGE,
            SHREDDER,
            SIFTER,
            SMELTER,
            MORTAR,
            EXTRUDER,
            CUTTER,
            LATHE,
            ROLLINGMILL,
            ROLLBENDER,
            WIREMILL,
            BENDER,
            ASSEMBLER,
            WELDER,
            PRESS);

    private ModRecipeMaps() {}

    public static RecipeMap anvil(AnvilMode mode) {
        return switch (mode) {
            case ANVIL -> ANVIL;
            case BEND_SMALL -> ANVIL_BEND_SMALL;
            case BEND_BIG -> ANVIL_BEND_BIG;
        };
    }

    private static RecipeMap create(String path) {
        return new RecipeMap(ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path));
    }
}
