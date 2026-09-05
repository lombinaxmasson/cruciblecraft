package com.masson.cruciblecraft.energy.battery;

import com.masson.cruciblecraft.CrucibleCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/** GT6 EU charge bar as a second, height-scaled overlay pass. */
public final class BatteryRenderer
        implements BlockEntityRenderer<BatteryBlockEntity> {
    public BatteryRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            BatteryBlockEntity battery,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        EnergyBatteryProfile profile = battery.profile();
        if (!profile.hasBar() || battery.displayedEnergy() <= 0) {
            return;
        }
        AABB box = profile.shape().bounds();
        float height = (battery.displayedEnergy() + 1) / 16.0F;
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID,
                        profile.textureFolder() + "/bar"));
        VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());
        int color = profile.color();
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float minX = (float) box.minX - 0.002F;
        float maxX = (float) box.maxX + 0.002F;
        float minZ = (float) box.minZ - 0.002F;
        float maxZ = (float) box.maxZ + 0.002F;
        float minY = 1.0F / 16.0F;
        float maxY = Math.min((float) box.maxY, height);
        poseStack.pushPose();
        var pose = poseStack.last();
        quad(consumer, pose, minX, minY, minZ, maxX, maxY, minZ,
                red, green, blue, packedLight, sprite, Direction.NORTH);
        quad(consumer, pose, maxX, minY, maxZ, minX, maxY, maxZ,
                red, green, blue, packedLight, sprite, Direction.SOUTH);
        quad(consumer, pose, minX, minY, maxZ, minX, maxY, minZ,
                red, green, blue, packedLight, sprite, Direction.WEST);
        quad(consumer, pose, maxX, minY, minZ, maxX, maxY, maxZ,
                red, green, blue, packedLight, sprite, Direction.EAST);
        poseStack.popPose();
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float red,
            float green,
            float blue,
            int light,
            TextureAtlasSprite sprite,
            Direction face) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        vertex(consumer, pose, x1, y1, z1, u0, v1, red, green, blue, light, face);
        vertex(consumer, pose, x1, y2, z1, u0, v0, red, green, blue, light, face);
        vertex(consumer, pose, x2, y2, z2, u1, v0, red, green, blue, light, face);
        vertex(consumer, pose, x2, y1, z2, u1, v1, red, green, blue, light, face);
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            float red,
            float green,
            float blue,
            int light,
            Direction face) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, 1.0F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
    }
}
