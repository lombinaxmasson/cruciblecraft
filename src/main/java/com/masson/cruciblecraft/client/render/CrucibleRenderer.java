package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class CrucibleRenderer implements BlockEntityRenderer<CrucibleBlockEntity> {
    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            CrucibleBlockEntity crucible,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        CrucibleInteriorRenderer.renderSmall(
                crucible.process(),
                crucible.temperature(),
                poseStack,
                buffers,
                packedLight);
    }
}
