package com.masson.cruciblecraft.client.render;

import org.joml.Vector3f;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client-side pressure readout for the GT6 large boiler controller.
 *
 * <p>GT6 composes its barometer from a base icon and a 32-frame scale
 * overlay. The reference tree contains the source paths but no image assets,
 * so this renderer keeps the same 0..31 state and pressure thresholds as a
 * small geometry gauge instead of introducing an unbacked texture alias.</p>
 */
public final class LargeBoilerRenderer
        implements BlockEntityRenderer<LargeBoilerBlockEntity> {
    private static final float FACE_EPSILON = 0.004F;

    public LargeBoilerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            LargeBoilerBlockEntity boiler,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        PipeCoverRenderer.renderMounted(
                boiler, poseStack, buffers, packedLight);

        BlockState state = boiler.getBlockState();
        Direction facing = state.hasProperty(MteInPlaceBlock.FACING)
                ? state.getValue(MteInPlaceBlock.FACING)
                : state.getValue(ProcessingMachineBlock.FACING);
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        PoseStack.Pose pose = poseStack.last();
        int pressure = Math.max(0, Math.min(31, boiler.barometer()));

        RenderSystem.lineWidth(2.0F);
        for (int tick = 0; tick <= 31; tick++) {
            float v = 0.20F + 0.60F * tick / 31.0F;
            float intensity = tick <= pressure ? 1.0F : 0.28F;
            int color = tick <= pressure
                    ? pressure >= 16 ? 0xFFFF3030 : 0xFF40E060
                    : 0xFF606060;
            drawLine(
                    pose,
                    lines,
                    facePoint(facing, 0.30F, v),
                    facePoint(facing, 0.40F, v),
                    color,
                    intensity);
        }
        float marker = 0.20F + 0.60F * pressure / 31.0F;
        drawLine(
                pose,
                lines,
                facePoint(facing, 0.44F, 0.20F),
                facePoint(facing, 0.44F, marker),
                pressure >= 16 ? 0xFFFF3030 : 0xFF40E060,
                1.0F);
        RenderSystem.lineWidth(1.0F);
    }

    private static Vector3f facePoint(
            Direction face,
            float u,
            float v) {
        float n = FACE_EPSILON;
        return switch (face) {
            case DOWN -> new Vector3f(u, -n, v);
            case UP -> new Vector3f(u, 1.0F + n, v);
            case NORTH -> new Vector3f(u, v, -n);
            case SOUTH -> new Vector3f(u, v, 1.0F + n);
            case WEST -> new Vector3f(-n, v, u);
            case EAST -> new Vector3f(1.0F + n, v, u);
        };
    }

    private static void drawLine(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            Vector3f from,
            Vector3f to,
            int color,
            float intensity) {
        Vector3f normal = new Vector3f(to).sub(from);
        if (normal.lengthSquared() < 1.0E-6F) {
            normal.set(0.0F, 1.0F, 0.0F);
        } else {
            normal.normalize();
        }
        float red = ((color >> 16) & 0xFF) / 255.0F * intensity;
        float green = ((color >> 8) & 0xFF) / 255.0F * intensity;
        float blue = (color & 0xFF) / 255.0F * intensity;
        buffer.addVertex(pose, from.x, from.y, from.z)
                .setColor(red, green, blue, 1.0F)
                .setNormal(pose, normal.x, normal.y, normal.z);
        buffer.addVertex(pose, to.x, to.y, to.z)
                .setColor(red, green, blue, 1.0F)
                .setNormal(pose, normal.x, normal.y, normal.z);
    }
}
