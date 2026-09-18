package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;

public final class MteInPlaceStorageRenderer
        implements BlockEntityRenderer<MteInPlaceBlockEntity> {
    public MteInPlaceStorageRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            MteInPlaceBlockEntity host,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        MteInPlaceKind kind = host.spec().kind();
        if (kind == MteInPlaceKind.MASS_STORAGE) {
            ItemStack stack = host.items().getStackInSlot(0);
            MassStorageRenderer.renderFace(
                    host, stack, stack.getCount(), poseStack, buffers, packedOverlay);
            return;
        }
        if (kind == MteInPlaceKind.BOOKSHELF) {
            BookshelfRenderer.renderBooks(
                    host, host.items(), poseStack, buffers, LightTexture.FULL_BRIGHT);
            return;
        }
        if (kind == MteInPlaceKind.BOTTLE_CRATE) {
            BottleCrateRenderer.renderBottles(
                    host, host.items(), poseStack, buffers, LightTexture.FULL_BRIGHT);
        }
    }
}
