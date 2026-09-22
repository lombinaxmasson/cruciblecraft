package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.block.AnvilHosts;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code MultiTileEntityAnvil} workpiece passes. Boxes are the pixel bounds
 * from {@code setBlockBounds2}; textures are the GT6 metallic icon set.
 */
public final class AnvilRenderer implements BlockEntityRenderer<AnvilBlockEntity> {
    private static final ResourceLocation SOLID = texture("blocksolid");
    private static final ResourceLocation GEM = texture("blockgem");
    private static final ResourceLocation RAW = texture("blockraw");
    private static final float SURFACE = 0.001f;

    public AnvilRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            AnvilBlockEntity anvil,
            float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        boolean alongX = AnvilHosts.horizontalFacing(anvil.getBlockState())
                .getAxis() == Direction.Axis.X;
        for (int slot = 0; slot < 2; slot++) {
            ItemStack stack = anvil.workpiece(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int shape = shape(stack);
            WorkpieceBox box = WorkpieceBox.of(shape, slot == 1);
            ResourceLocation texture = shape == 6 ? GEM : shape == 7 ? RAW : SOLID;
            VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutout(texture));
            StorageVoxelBuffer.cube(
                    vertices,
                    poseStack,
                    box.bounds(alongX),
                    color(stack),
                    packedLight);
        }
    }

    private static int shape(ItemStack stack) {
        String form = MaterialUnits.resolve(stack)
                .map(entry -> entry.form().serializedId())
                .orElse("");
        if (form.contains("plate") || form.contains("foil") || form.contains("plank")) {
            return 2;
        }
        if (form.contains("rod")
                || form.contains("wire")
                || form.contains("bolt")
                || form.contains("screw")
                || form.contains("spring")) {
            return 3;
        }
        if (form.contains("chunk")) {
            return 4;
        }
        if (form.contains("ring")) {
            return 5;
        }
        if (form.contains("gem")) {
            return 6;
        }
        if (form.contains("ore") || form.contains("rock") || form.contains("raw")) {
            return 7;
        }
        if (form.contains("ingot") || form.contains("nugget") || form.contains("billet")) {
            return 1;
        }
        return AnvilHosts.isHammer(stack) ? 8 : 0;
    }

    private static int color(ItemStack stack) {
        return MaterialUnits.resolve(stack)
                .map(entry -> 0xFF000000 | entry.material().colorRgb())
                .orElse(AnvilHosts.STONE_COLOR);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft",
                "textures/block/gt6_import/anvil/" + name + ".png");
    }

    /**
     * Pixel indexes from GT6 {@code PX_P}/{@code PX_N} workpiece cases.
     * Long-axis indexes swap between the two slots.
     */
    private record WorkpieceBox(
            int crossMin,
            int crossMax,
            int longMin,
            int longMax,
            int yMin,
            int yMax) {
        static WorkpieceBox of(int shape, boolean secondSlot) {
            return switch (shape) {
                case 1 -> secondSlot
                        ? new WorkpieceBox(5, 5, 10, 3, 12, 1)
                        : new WorkpieceBox(5, 5, 3, 10, 12, 1);
                case 2 -> secondSlot
                        ? new WorkpieceBox(5, 5, 9, 1, 12, 3)
                        : new WorkpieceBox(5, 5, 1, 9, 12, 3);
                case 3 -> secondSlot
                        ? new WorkpieceBox(7, 7, 9, 1, 12, 2)
                        : new WorkpieceBox(7, 7, 1, 9, 12, 2);
                case 4 -> secondSlot
                        ? new WorkpieceBox(6, 6, 10, 2, 12, 2)
                        : new WorkpieceBox(6, 6, 2, 10, 12, 2);
                case 5 -> secondSlot
                        ? new WorkpieceBox(6, 6, 10, 2, 12, 3)
                        : new WorkpieceBox(6, 6, 2, 10, 12, 3);
                case 6 -> secondSlot
                        ? new WorkpieceBox(6, 6, 10, 2, 12, 0)
                        : new WorkpieceBox(6, 6, 2, 10, 12, 0);
                case 8 -> secondSlot
                        ? new WorkpieceBox(5, 5, 10, 2, 12, 0)
                        : new WorkpieceBox(5, 5, 2, 10, 12, 0);
                case 7 -> secondSlot
                        ? new WorkpieceBox(5, 5, 9, 1, 12, 0)
                        : new WorkpieceBox(5, 5, 1, 9, 12, 0);
                default -> secondSlot
                        ? new WorkpieceBox(5, 5, 9, 1, 12, 0)
                        : new WorkpieceBox(5, 5, 1, 9, 12, 0);
            };
        }

        float[] bounds(boolean alongX) {
            float minLong = pixel(longMin);
            float maxLong = pixelFromEnd(longMax);
            float minCross = pixel(crossMin);
            float maxCross = pixelFromEnd(crossMax);
            float minY = pixel(yMin) + SURFACE;
            float maxY = pixelFromEnd(yMax) + SURFACE;
            return alongX
                    ? new float[] {minCross, minY, minLong, maxCross, maxY, maxLong}
                    : new float[] {minLong, minY, minCross, maxLong, maxY, maxCross};
        }

        private static float pixel(int pixels) {
            return pixels / 16.0f;
        }

        private static float pixelFromEnd(int pixels) {
            return (16 - pixels) / 16.0f;
        }
    }
}
