package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.block.AnvilHosts;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

public final class AnvilRenderer implements BlockEntityRenderer<AnvilBlockEntity> {
    private static final ResourceLocation WORKPIECE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "block/gt6_import/mte/faucet");

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
            poseStack.translate(x, 0.0, z);
            if (facing.getAxis() == net.minecraft.core.Direction.Axis.X) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
            }
            if (AnvilHosts.isHammer(anvil.workpiece(slot))) {
                poseStack.translate(0.0, 1.03, 0.0);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
                poseStack.scale(0.48f, 0.48f, 0.48f);
                Minecraft.getInstance().getItemRenderer().renderStatic(
                        anvil.workpiece(slot),
                        ItemDisplayContext.FIXED,
                        packedLight,
                        packedOverlay,
                        poseStack,
                        buffers,
                        anvil.getLevel(),
                        slot);
            } else {
                renderWorkpiece(
                        anvil,
                        slot,
                        poseStack,
                        buffers,
                        packedLight);
            }
            poseStack.popPose();
        }
    }

    private static void renderWorkpiece(
            AnvilBlockEntity anvil,
            int slot,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        var stack = anvil.workpiece(slot);
        var entry = MaterialUnits.resolve(stack).orElse(null);
        String form = entry == null ? "" : entry.form().serializedId();
        int color = entry == null
                ? AnvilHosts.STONE_COLOR
                : 0xFF000000 | entry.material().colorRgb();
        float[] box = workpieceBox(form);
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(WORKPIECE_TEXTURE);
        VertexConsumer vertices = buffers.getBuffer(
                RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        StorageVoxelBuffer.cube(
                vertices,
                poseStack,
                box,
                sprite,
                color,
                packedLight);
    }

    private static float[] workpieceBox(String form) {
        if (form.contains("plate") || form.contains("foil")) {
            return new float[] {-0.22f, 0.75f, -0.34f, 0.22f, 0.82f, 0.34f};
        }
        if (form.contains("rod") || form.contains("wire")) {
            return new float[] {-0.36f, 0.75f, -0.09f, 0.36f, 0.84f, 0.09f};
        }
        if (form.contains("ring")) {
            return new float[] {-0.22f, 0.75f, -0.22f, 0.22f, 0.87f, 0.22f};
        }
        if (form.contains("gem")) {
            return new float[] {-0.20f, 0.75f, -0.20f, 0.20f, 0.95f, 0.20f};
        }
        if (form.contains("ore") || form.contains("rock") || form.contains("chunk")) {
            return new float[] {-0.24f, 0.75f, -0.24f, 0.24f, 0.92f, 0.24f};
        }
        return new float[] {-0.24f, 0.75f, -0.18f, 0.24f, 0.91f, 0.18f};
    }
}
