package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.energy.largegasturbine.LargeGasTurbineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/**
 * GT6 {@code MultiTileEntityLargeTurbine} render pass 1: a 3x3 facing fan
 * slightly in front of the frontal hull.
 */
public final class LargeGasTurbineRenderer
        implements BlockEntityRenderer<LargeGasTurbineBlockEntity> {
    public LargeGasTurbineRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            LargeGasTurbineBlockEntity turbine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        PipeCoverRenderer.renderMounted(
                turbine, poseStack, buffers, packedLight);
        if (!turbine.formed()) {
            return;
        }
        Direction facing = turbine.getBlockState().getValue(MteInPlaceBlock.FACING);
        LargeTurbineFanRenderer.render(
                poseStack,
                buffers,
                packedLight,
                facing,
                LargeTurbineFanRenderer.GAS_IDLE,
                LargeTurbineFanRenderer.GAS_ACTIVE,
                turbine.running());
    }

    @Override
    public AABB getRenderBoundingBox(LargeGasTurbineBlockEntity turbine) {
        return new AABB(turbine.getBlockPos()).inflate(1.05);
    }

    @Override
    public boolean shouldRenderOffScreen(LargeGasTurbineBlockEntity turbine) {
        return turbine.formed();
    }
}
