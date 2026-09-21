package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.energy.steam.SteamTurbinePresentation;
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
        } else if (kind.massStorage()) {
            var inventory = host.massStorage();
            if (inventory != null) {
                MassStorageRenderer.renderFace(
                        host,
                        inventory.filter(),
                        inventory.stored(),
                        inventory.capacity(),
                        kind == MteInPlaceKind.BARREL
                                ? "mass_storage_barrel"
                                : "mass_storage_standard",
                        poseStack,
                        buffers,
                        packedOverlay);
            }
        } else if (kind == MteInPlaceKind.BOOKSHELF) {
            BookshelfRenderer.renderBooks(
                    host, host.items(), poseStack, buffers, LightTexture.FULL_BRIGHT);
        } else if (kind == MteInPlaceKind.BOTTLE_CRATE) {
            BottleCrateRenderer.renderBottles(
                    host, host.items(), poseStack, buffers, LightTexture.FULL_BRIGHT);
        } else if (SteamTurbinePresentation.large(host.spec()) && host.formed()) {
            boolean running = host.getBlockState().hasProperty(MteInPlaceBlock.LIT)
                    && host.getBlockState().getValue(MteInPlaceBlock.LIT);
            LargeTurbineFanRenderer.render(
                    poseStack,
                    buffers,
                    packedLight,
                    host.getBlockState().getValue(MteInPlaceBlock.FACING),
                    LargeTurbineFanRenderer.STEAM_IDLE,
                    LargeTurbineFanRenderer.STEAM_ACTIVE,
                    running);
        }
        PipeCoverRenderer.renderMounted(host, poseStack, buffers, packedLight);
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
        if (SteamTurbinePresentation.large(host.spec()) && host.formed()) {
            return new AABB(host.getBlockPos()).inflate(1.05);
        }
        return BlockEntityRenderer.super.getRenderBoundingBox(host);
    }

    @Override
    public boolean shouldRenderOffScreen(MteInPlaceBlockEntity host) {
        return SteamTurbinePresentation.large(host.spec()) && host.formed();
    }
}
