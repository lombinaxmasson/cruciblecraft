package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.content.blockentity.BottleCrateBlockEntity;
import com.masson.cruciblecraft.content.storage.StorageBottleDisplay;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class BottleCrateRenderer
        implements BlockEntityRenderer<BottleCrateBlockEntity> {
    private static final ResourceLocation WATER =
            ResourceLocation.withDefaultNamespace("block/water_still");

    public BottleCrateRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            BottleCrateBlockEntity crate,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        renderBottles(
                crate, crate.inventory(), poseStack, buffers, LightTexture.FULL_BRIGHT);
        PipeCoverRenderer.renderMounted(
                crate, poseStack, buffers, packedLight);
    }

    static void renderBottles(
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
        TextureAtlasSprite water =
                Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WATER);
        int slots = Math.min(inventory.getSlots(), StorageBottleDisplay.SLOTS);
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!StorageBottleDisplay.present(stack)) {
                continue;
            }
            if (StorageBottleDisplay.hasFluid(stack)) {
                int color = 0xFF000000 | StorageBottleDisplay.fluidColor(stack);
                VertexConsumer fluid =
                        buffers.getBuffer(
                                RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
                StorageVoxelBuffer.cube(
                        fluid,
                        poseStack,
                        StorageBottleDisplay.fluidBox(slot),
                        water,
                        color,
                        packedLight);
            }
            VertexConsumer glass =
                    buffers.getBuffer(
                            RenderType.entityTranslucent(
                                    StorageVoxelBuffer.blockTexture(
                                            "gt6_import/storage/bottle/bottlecrate_bottle_sides")));
            StorageVoxelBuffer.cube(
                    glass,
                    poseStack,
                    StorageBottleDisplay.glassBox(slot),
                    0xFFFFFFFF,
                    packedLight);
            VertexConsumer cap =
                    buffers.getBuffer(
                            RenderType.entityCutout(
                                    StorageVoxelBuffer.blockTexture(
                                            "gt6_import/storage/bottle/bottlecrate_bottle_cap")));
            StorageVoxelBuffer.cube(
                    cap,
                    poseStack,
                    StorageBottleDisplay.capBox(slot),
                    0xFFFFFFFF,
                    packedLight);
        }
        poseStack.popPose();
    }
}
