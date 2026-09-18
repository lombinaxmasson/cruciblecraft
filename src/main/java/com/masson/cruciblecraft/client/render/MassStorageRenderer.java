package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.content.block.StorageHostBlock;
import com.masson.cruciblecraft.content.blockentity.MassStorageBlockEntity;
import com.masson.cruciblecraft.content.storage.StorageCountFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code MultiTileEntityRendererMassStorage}: GUI item on the front
 * window plus the stored count.
 */
public final class MassStorageRenderer
        implements BlockEntityRenderer<MassStorageBlockEntity> {
    public MassStorageRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            MassStorageBlockEntity storage,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        var inventory = storage.inventory();
        if (inventory.filter().isEmpty() || inventory.stored() <= 0) {
            return;
        }
        renderFace(
                storage,
                inventory.filter(),
                inventory.stored(),
                poseStack,
                buffers,
                packedOverlay);
    }

    static void renderFace(
            BlockEntity entity,
            ItemStack icon,
            int stored,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedOverlay) {
        if (icon.isEmpty() || stored <= 0) {
            return;
        }
        BlockState state = entity.getBlockState();
        Direction facing = facingOf(state);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotation(facing)));
        poseStack.translate(0.25, 0.125, -0.502);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.03125F, 0.03125F, 0.0001F);
        poseStack.scale(8.0F, 8.0F, 1.0F);
        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(
                        icon.copyWithCount(1),
                        ItemDisplayContext.GUI,
                        LightTexture.FULL_BRIGHT,
                        packedOverlay,
                        poseStack,
                        buffers,
                        entity.getLevel(),
                        0);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotation(facing)));
        poseStack.translate(0.42, 0.02, -0.504);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.012F, 0.012F, 0.012F);
        Font font = Minecraft.getInstance().font;
        String count = StorageCountFormat.format(stored);
        font.drawInBatch(
                count,
                -font.width(count),
                0.0F,
                0xFFFFFFFF,
                false,
                poseStack.last().pose(),
                buffers,
                Font.DisplayMode.NORMAL,
                0,
                LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    static float yRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    static Direction facingOf(BlockState state) {
        if (state.hasProperty(StorageHostBlock.FACING)) {
            return state.getValue(StorageHostBlock.FACING);
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            Direction facing =
                    state.getValue(
                            net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
            return facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        }
        if (state.hasProperty(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }
}
