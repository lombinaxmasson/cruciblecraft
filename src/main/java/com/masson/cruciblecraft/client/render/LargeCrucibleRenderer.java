package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.content.blockentity.LargeCrucibleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

public final class LargeCrucibleRenderer
        implements BlockEntityRenderer<LargeCrucibleBlockEntity> {
    public LargeCrucibleRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            LargeCrucibleBlockEntity crucible,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        if (!crucible.structureValid() || crucible.pluginQuarantined()) {
            return;
        }
        LargeCrucibleHullRenderer.render(
                crucible, poseStack, buffers, packedLight);
        CrucibleInteriorRenderer.renderLarge(
                crucible.process(),
                crucible.temperature(),
                poseStack,
                buffers,
                packedLight);
    }

    @Override
    public AABB getRenderBoundingBox(LargeCrucibleBlockEntity crucible) {
        return new AABB(
                crucible.getBlockPos().getX() - 1.05,
                crucible.getBlockPos().getY() - 0.05,
                crucible.getBlockPos().getZ() - 1.05,
                crucible.getBlockPos().getX() + 2.05,
                crucible.getBlockPos().getY() + 3.05,
                crucible.getBlockPos().getZ() + 2.05);
    }

    @Override
    public boolean shouldRenderOffScreen(LargeCrucibleBlockEntity crucible) {
        return true;
    }
}
