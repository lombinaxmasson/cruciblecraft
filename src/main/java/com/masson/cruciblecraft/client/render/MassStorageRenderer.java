package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.content.block.MassStorageBlock;
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
 * window plus the stored count. Item facing follows the gregtech6_w port
 * ({@code -toYRot} so GUI +Z points out of the face).
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
        String profile = storage.getBlockState().getBlock() instanceof MassStorageBlock block
                ? block.variant().modelProfile()
                : "mass_storage_standard";
        renderFace(
                storage,
                inventory.filter(),
                inventory.stored(),
                inventory.capacity(),
                profile,
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
        renderFace(
                entity,
                icon,
                stored,
                0,
                "",
                poseStack,
                buffers,
                packedOverlay);
    }

    static void renderFace(
            BlockEntity entity,
            ItemStack icon,
            int stored,
            int capacity,
            String modelProfile,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedOverlay) {
        if (icon.isEmpty() || stored <= 0) {
            return;
        }
        Direction facing = facingOf(entity.getBlockState());
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        // GT6 1.7.10 item center landed at Y=0.375; GUI models face +Z.
        poseStack.translate(0.0, -0.125, 0.51);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.4F, 0.4F, 0.0001F);
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
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(0.28, 0.3125, 0.51);
        poseStack.scale(0.0125F, -0.0125F, 0.0125F);
        Font font = Minecraft.getInstance().font;
        String count = StorageCountFormat.face(stored, capacity, modelProfile);
        int color = capacity > 0 && stored >= capacity ? 0xFFFF2020 : 0xFF202020;
        font.drawInBatch(
                count,
                -font.width(count),
                0.0F,
                color,
                false,
                poseStack.last().pose(),
                buffers,
                Font.DisplayMode.SEE_THROUGH,
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
