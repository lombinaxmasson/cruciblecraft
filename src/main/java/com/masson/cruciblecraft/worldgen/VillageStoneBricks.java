package com.masson.cruciblecraft.worldgen;

import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code GetVillageBlockID} / {@code GetVillageBlockMeta}: village
 * cobblestone becomes {@code BlocksGT.stones[(biomeId+6)%length]} meta
 * {@code BlockStones.SBRIK}. Null biome is Andesite.
 */
public final class VillageStoneBricks {
    public static final int ANDESITE_INDEX = 7;

    private VillageStoneBricks() {}

    public static BlockState brickForBiome(
            Holder<Biome> biome, RegistryAccess access) {
        int biomeId = -1;
        if (biome != null) {
            biomeId = access.registryOrThrow(Registries.BIOME).getId(biome.value());
        }
        return brickForBiomeId(biomeId);
    }

    public static BlockState brickForBiomeId(int biomeId) {
        var bricks = StoneLayerStones.villageBricks();
        int index = biomeId < 0
                ? ANDESITE_INDEX
                : (biomeId + 6) % bricks.size();
        return ModBlocks.layerOrExistingStone(bricks.get(index).registryPath())
                .get()
                .defaultBlockState();
    }
}
