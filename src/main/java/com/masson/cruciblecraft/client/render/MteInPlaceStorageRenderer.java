package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class MteInPlaceStorageRenderer
        implements BlockEntityRenderer<MteInPlaceBlockEntity> {
    private final GtChestRenderer.Model chest;

    public MteInPlaceStorageRenderer(BlockEntityRendererProvider.Context context) {
        this.chest = new GtChestRenderer.Model(context.bakeLayer(GtChestRenderer.LAYER));
    }

    @Override
    public void render(
            MteInPlaceBlockEntity host,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        MteInPlaceKind kind = host.spec().kind();
        if (kind == MteInPlaceKind.CHEST) {
            GtChestRenderer.renderHost(
                    chest,
                    host,
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay,
                    partialTick);
            return;
        }
        if (kind == MteInPlaceKind.MASS_STORAGE) {
            var inventory = host.massStorage();
            if (inventory == null) {
                return;
            }
            MassStorageRenderer.renderFace(
                    host,
                    inventory.filter(),
                    inventory.stored(),
                    inventory.capacity(),
                    "mass_storage_standard",
                    poseStack,
                    buffers,
                    packedOverlay);
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

    @Override
    public AABB getRenderBoundingBox(MteInPlaceBlockEntity host) {
        if (host.spec().kind() == MteInPlaceKind.CHEST) {
            BlockPos pos = host.getBlockPos();
            return new AABB(
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    pos.getX() + 1.0,
                    pos.getY() + 2.0,
                    pos.getZ() + 1.0);
        }
        return BlockEntityRenderer.super.getRenderBoundingBox(host);
    }
}
