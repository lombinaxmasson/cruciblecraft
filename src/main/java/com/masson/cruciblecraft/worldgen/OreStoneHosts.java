package com.masson.cruciblecraft.worldgen;

import com.masson.cruciblecraft.content.block.GtHostedOreBlock;
import com.masson.cruciblecraft.content.block.GtSmallOreBlock;
import com.masson.cruciblecraft.content.block.MaterialOreBlock;
import com.masson.cruciblecraft.content.block.OreStoneHost;
import com.masson.cruciblecraft.content.block.StoneLayerRockOreBlock;
import com.masson.cruciblecraft.content.block.StoneLayerStoneBlock;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Maps replaced cubes and layer materials onto {@link OreStoneHost}. */
public final class OreStoneHosts {
    private OreStoneHosts() {}

    public static OreStoneHost ofLayer(String material) {
        return OreStoneHost.ofLayer(material);
    }

    public static OreStoneHost of(BlockState replaced) {
        if (replaced.is(Blocks.NETHERRACK)
                || replaced.is(BlockTags.BASE_STONE_NETHER)) {
            return OreStoneHost.NETHERRACK;
        }
        if (replaced.is(Blocks.DEEPSLATE)
                || replaced.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
            return OreStoneHost.DEEPSLATE;
        }
        if (replaced.is(Blocks.GRANITE)) {
            return OreStoneHost.GRANITE;
        }
        if (replaced.is(Blocks.DIORITE)) {
            return OreStoneHost.DIORITE;
        }
        if (replaced.is(Blocks.ANDESITE)) {
            return OreStoneHost.ANDESITE;
        }
        if (replaced.getBlock() instanceof StoneLayerStoneBlock
                || replaced.getBlock() instanceof StoneLayerRockOreBlock) {
            return OreStoneHost.ofLayer(StoneLayerStones.materialOf(replaced));
        }
        if (replaced.getBlock() instanceof MaterialOreBlock) {
            return replaced.getValue(MaterialOreBlock.HOST);
        }
        if (replaced.getBlock() instanceof GtHostedOreBlock) {
            return replaced.getValue(GtHostedOreBlock.HOST);
        }
        if (replaced.getBlock() instanceof GtSmallOreBlock) {
            return replaced.getValue(GtSmallOreBlock.HOST);
        }
        return OreStoneHost.STONE;
    }

    public static boolean isUniqueOre(BlockState state) {
        return uniqueMaterial(state.getBlock()) != null;
    }

    public static String uniqueMaterial(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !"cruciblecraft".equals(id.getNamespace())) {
            return null;
        }
        String path = id.getPath();
        if (!path.endsWith("_ore")) {
            return null;
        }
        if (path.startsWith("deepslate_")) {
            String material = path.substring(
                    "deepslate_".length(), path.length() - "_ore".length());
            return ModBlocks.hasOreBlock(material, Host.DEEPSLATE) ? material : null;
        }
        String material = path.substring(0, path.length() - "_ore".length());
        return ModBlocks.hasOreBlock(material, Host.STONE) ? material : null;
    }

    public static boolean isDeepslateUnique(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null
                && "cruciblecraft".equals(id.getNamespace())
                && id.getPath().startsWith("deepslate_")
                && id.getPath().endsWith("_ore")
                && uniqueMaterial(block) != null;
    }
}
