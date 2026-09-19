package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.block.AnvilHosts;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;

public final class AnvilRenderer implements BlockEntityRenderer<AnvilBlockEntity> {
    public AnvilRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            AnvilBlockEntity anvil,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        var facing = AnvilHosts.horizontalFacing(anvil.getBlockState());
        for (int slot = 0; slot < 2; slot++) {
            if (anvil.workpiece(slot).isEmpty()) {
                continue;
            }
            double offset = slot == 0 ? -0.22 : 0.22;
            double x = facing.getAxis() == net.minecraft.core.Direction.Axis.Z ? 0.5 + offset : 0.5;
            double z = facing.getAxis() == net.minecraft.core.Direction.Axis.X ? 0.5 + offset : 0.5;
            poseStack.pushPose();
            poseStack.translate(x, 1.03, z);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            poseStack.scale(0.5f, 0.5f, 0.5f);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    anvil.workpiece(slot),
                    ItemDisplayContext.FIXED,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    buffers,
                    anvil.getLevel(),
                    slot);
            poseStack.popPose();
        }
    }
}
