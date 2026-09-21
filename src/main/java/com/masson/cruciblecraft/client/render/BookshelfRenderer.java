package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.content.storage.StorageBookDisplay;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.masson.cruciblecraft.content.blockentity.BookshelfBlockEntity;

public final class BookshelfRenderer
        implements BlockEntityRenderer<BookshelfBlockEntity> {
    public BookshelfRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            BookshelfBlockEntity shelf,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        renderBooks(
                shelf,
                shelf.inventory(),
                poseStack,
                buffers,
                LightTexture.FULL_BRIGHT);
        PipeCoverRenderer.renderMounted(
                shelf, poseStack, buffers, packedLight);
    }

    static void renderBooks(
            BlockEntity entity,
            ItemStackHandler inventory,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        Direction facing = MassStorageRenderer.facingOf(entity.getBlockState());
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(MassStorageRenderer.yRotation(facing)));
        poseStack.translate(-0.5, 0.0, -0.5);
        int slots = Math.min(inventory.getSlots(), StorageBookDisplay.SLOTS);
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            int index = StorageBookDisplay.index(stack);
            if (index == StorageBookDisplay.EMPTY) {
                continue;
            }
            String stem = StorageBookDisplay.textureStem(index);
            float[] box = StorageBookDisplay.box(slot);
            VertexConsumer sides =
                    buffers.getBuffer(
                            RenderType.entityCutout(
                                    StorageVoxelBuffer.blockTexture(
                                            "gt6_import/storage/books/" + stem + "_side")));
            StorageVoxelBuffer.cube(sides, poseStack, box, 0xFFFFFFFF, packedLight);
            VertexConsumer back =
                    buffers.getBuffer(
                            RenderType.entityCutout(
                                    StorageVoxelBuffer.blockTexture(
                                            "gt6_import/storage/books/" + stem + "_back")));
            StorageVoxelBuffer.cubeNorthSouth(back, poseStack, box, 0xFFFFFFFF, packedLight);
        }
        poseStack.popPose();
    }
}
