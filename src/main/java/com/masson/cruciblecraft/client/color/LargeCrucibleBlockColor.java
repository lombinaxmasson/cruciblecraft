package com.masson.cruciblecraft.client.color;

import com.masson.cruciblecraft.content.block.LargeCrucibleHosts;
import com.masson.cruciblecraft.content.block.LargeCrucibleWalls;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.CrucibleWorldHazards;
import com.masson.cruciblecraft.content.blockentity.LargeCrucibleBlockEntity;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** tintindex 0 only. Generic controller uses MACHINE_MATERIAL; unique hosts use the baked id. */
public final class LargeCrucibleBlockColor {
    private LargeCrucibleBlockColor() {}

    public static int blockColor(
            BlockState state,
            net.minecraft.world.level.BlockAndTintGetter level,
            net.minecraft.core.BlockPos pos,
            int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        int color;
        LargeCrucibleBlockEntity crucible = null;
        if (level != null && pos != null) {
            if (level.getBlockEntity(pos) instanceof LargeCrucibleBlockEntity controller) {
                crucible = controller;
            } else {
                crucible = LargeCrucibleWalls.controllerAt(level, pos);
            }
        }
        if (crucible != null) {
            color = LargeCrucibleHosts.colorRgb(crucible.process().casing().materialId());
            return crucible.process().nearMeltdown()
                    ? CrucibleWorldHazards.meltDownTint(color)
                    : color;
        }
        return LargeCrucibleHosts.bakedMaterial(state)
                .map(LargeCrucibleHosts::colorRgb)
                .orElse(LargeCrucibleHosts.colorRgb(LargeCrucibleHosts.DEFAULT_MATERIAL));
    }

    public static int itemColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }
        Block block = Block.byItem(stack.getItem());
        if (block instanceof MteInPlaceBlock inplace) {
            if (LargeCrucibleHosts.isController(inplace.spec())
                    || LargeCrucibleHosts.isCatalogWall(inplace.spec())) {
                return LargeCrucibleHosts.colorRgb(
                        LargeCrucibleHosts.materialId(inplace.spec()));
            }
        }
        String material = stack.getOrDefault(
                ModComponents.MACHINE_MATERIAL.get(),
                LargeCrucibleHosts.DEFAULT_MATERIAL);
        return LargeCrucibleHosts.colorRgb(material);
    }

    public static Block[] tintedBlocks() {
        java.util.ArrayList<Block> blocks = new java.util.ArrayList<>();
        blocks.add(ModBlocks.LARGE_CRUCIBLE.get());
        ModBlocks.mteInPlaceBlocksById().values().forEach(holder -> {
            var spec = holder.get().spec();
            if (LargeCrucibleHosts.isController(spec)
                    || LargeCrucibleHosts.isCatalogWall(spec)) {
                blocks.add(holder.get());
            }
        });
        return blocks.toArray(Block[]::new);
    }
}
