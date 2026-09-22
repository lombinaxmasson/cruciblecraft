package com.masson.cruciblecraft.content.blockentity;

import java.util.Arrays;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;

import net.minecraft.resources.ResourceLocation;

/**
 * The source-backed tier data for GT6's large steam boilers.
 *
 * <p>The five controller identities are deliberately kept distinct. The
 * controller and the dense wall must come from the same row; a generic
 * "boiler" profile would make mixed-material structures silently valid.</p>
 */
public enum LargeBoilerTier {
    STAINLESS_STEEL(
            "stainless_steel/boiler_main_barometer",
            "large_boiler_stainless_steel",
            "multiblock/dense_stainless_steel_wall",
            17201,
            8_192L),
    INVAR(
            "invar/boiler_main_barometer",
            "large_boiler_invar",
            "multiblock/dense_invar_wall",
            17205,
            8_192L),
    TITANIUM(
            "titanium/boiler_main_barometer",
            "large_boiler_titanium",
            "multiblock/dense_titanium_wall",
            17202,
            16_384L),
    TUNGSTENSTEEL(
            "tungstensteel/boiler_main_barometer",
            "large_boiler_tungstensteel",
            "multiblock/dense_tungstensteel_wall",
            17203,
            32_768L),
    ADAMANTIUM(
            "adamantium/boiler_main_barometer",
            "large_boiler_adamantium",
            "multiblock/dense_adamantium_wall",
            17204,
            262_144L);

    public static final long WATER_CAPACITY = 128_000L;
    public static final long COOL_DOWN_RESET_TICKS = 128L;
    public static final long HEAT_PER_WATER = 80L;
    public static final long STEAM_PER_WATER = 160L;

    private final ResourceLocation controllerId;
    private final ResourceLocation structureId;
    private final ResourceLocation wallId;
    private final int meta;
    private final long steamOutput;

    LargeBoilerTier(
            String controllerPath,
            String structurePath,
            String wallPath,
            int meta,
            long steamOutput) {
        this.controllerId = id(controllerPath);
        this.structureId = id(structurePath);
        this.wallId = id(wallPath);
        this.meta = meta;
        this.steamOutput = steamOutput;
    }

    public ResourceLocation controllerId() {
        return controllerId;
    }

    public ResourceLocation structureId() {
        return structureId;
    }

    public ResourceLocation wallId() {
        return wallId;
    }

    public int meta() {
        return meta;
    }

    /** GT6 NBT_OUTPUT_SU: steam produced per nominal tick. */
    public long steamOutput() {
        return steamOutput;
    }

    /** GT6 recommended HU packet size: mOutput / STEAM_PER_EU. */
    public long heatOutput() {
        return steamOutput / 2L;
    }

    /** GT6 mCapacity and the steam-tank capacity. */
    public long capacity() {
        return Math.multiplyExact(steamOutput, 10_000L);
    }

    public static Optional<LargeBoilerTier> byControllerId(
            ResourceLocation controllerId) {
        return Arrays.stream(values())
                .filter(tier -> tier.controllerId.equals(controllerId))
                .findFirst();
    }

    public static Optional<LargeBoilerTier> bySpec(MteInPlaceSpec spec) {
        return spec == null ? Optional.empty() : byControllerId(spec.id());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }
}
