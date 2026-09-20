package com.masson.cruciblecraft.client.render;

import java.util.EnumSet;
import java.util.Set;

import org.joml.Matrix4f;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.LargeCrucibleHosts;
import com.masson.cruciblecraft.content.blockentity.LargeCrucibleBlockEntity;
import com.masson.cruciblecraft.machine.component.CrucibleInteriorGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code MultiTileEntityCrucible} passes 0–4: a 3x3x3 hollow hull the
 * controller draws after walls switch to empty design 4.
 */
final class LargeCrucibleHullRenderer {
    private static final ResourceLocation COLORED = texture("colored");
    private static final ResourceLocation COLORED_FRONT = texture("colored_front");
    private static final ResourceLocation OVERLAY = texture("overlay");
    private static final ResourceLocation OVERLAY_FRONT = texture("overlay_front");

    private LargeCrucibleHullRenderer() {}

    static void render(
            LargeCrucibleBlockEntity crucible,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        int tint = LargeCrucibleHosts.colorRgb(
                crucible.process().casing().materialId());
        Direction facing = facing(crucible.getBlockState());
        VertexConsumer vertices = buffers.getBuffer(
                RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        box(vertices, poseStack, packedLight, tint, facing,
                -0.999f, 0.0f, -0.999f, -0.500f, 3.0f, 1.999f,
                EnumSet.of(Direction.WEST, Direction.EAST, Direction.UP));
        box(vertices, poseStack, packedLight, tint, facing,
                -0.999f, 0.0f, -0.999f, 1.999f, 3.0f, -0.500f,
                EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.UP));
        box(vertices, poseStack, packedLight, tint, facing,
                1.500f, 0.0f, -0.999f, 1.999f, 3.0f, 1.999f,
                EnumSet.of(Direction.WEST, Direction.EAST, Direction.UP));
        box(vertices, poseStack, packedLight, tint, facing,
                -0.999f, 0.0f, 1.500f, 1.999f, 3.0f, 1.999f,
                EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.UP));
        float floor = CrucibleInteriorGeometry.LARGE_BASE_Y;
        box(vertices, poseStack, packedLight, tint, facing,
                -0.999f, 0.0f, -0.999f, 1.999f, floor, 1.999f,
                EnumSet.of(Direction.UP, Direction.DOWN));
    }

    private static Direction facing(BlockState state) {
        return LargeCrucibleHosts.horizontalFacing(state);
    }

    private static void box(
            VertexConsumer vertices,
            PoseStack poseStack,
            int packedLight,
            int tint,
            Direction facing,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            Set<Direction> faces) {
        if (faces.contains(Direction.DOWN)) {
            quad(vertices, poseStack, packedLight, tint, facing, Direction.DOWN,
                    x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
        }
        if (faces.contains(Direction.UP)) {
            quad(vertices, poseStack, packedLight, tint, facing, Direction.UP,
                    x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        }
        if (faces.contains(Direction.NORTH)) {
            quad(vertices, poseStack, packedLight, tint, facing, Direction.NORTH,
                    x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0);
        }
        if (faces.contains(Direction.SOUTH)) {
            quad(vertices, poseStack, packedLight, tint, facing, Direction.SOUTH,
                    x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1);
        }
        if (faces.contains(Direction.WEST)) {
            quad(vertices, poseStack, packedLight, tint, facing, Direction.WEST,
                    x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0);
        }
        if (faces.contains(Direction.EAST)) {
            quad(vertices, poseStack, packedLight, tint, facing, Direction.EAST,
                    x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1);
        }
    }

    private static void quad(
            VertexConsumer vertices,
            PoseStack poseStack,
            int packedLight,
            int tint,
            Direction facing,
            Direction side,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3) {
        TextureAtlasSprite colored = sprite(side, facing, true);
        TextureAtlasSprite overlay = sprite(side, facing, false);
        emit(vertices, poseStack, packedLight, tint, side, colored,
                x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
        emit(vertices, poseStack, packedLight, 0xFFFFFFFF, side, overlay,
                x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
    }

    private static void emit(
            VertexConsumer vertices,
            PoseStack poseStack,
            int packedLight,
            int argb,
            Direction side,
            TextureAtlasSprite sprite,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        Matrix4f pose = poseStack.last().pose();
        vertex(vertices, poseStack, pose, x0, y0, z0, u0, v0, side, argb, packedLight);
        vertex(vertices, poseStack, pose, x1, y1, z1, u1, v0, side, argb, packedLight);
        vertex(vertices, poseStack, pose, x2, y2, z2, u1, v1, side, argb, packedLight);
        vertex(vertices, poseStack, pose, x3, y3, z3, u0, v1, side, argb, packedLight);
    }

    private static void vertex(
            VertexConsumer vertices,
            PoseStack poseStack,
            Matrix4f pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            Direction side,
            int argb,
            int packedLight) {
        vertices.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(
                        poseStack.last(),
                        side.getStepX(),
                        side.getStepY(),
                        side.getStepZ());
    }

    private static TextureAtlasSprite sprite(
            Direction side, Direction facing, boolean colored) {
        ResourceLocation folder = colored
                ? (side == facing ? COLORED_FRONT : COLORED)
                : (side == facing ? OVERLAY_FRONT : OVERLAY);
        String face = switch (side) {
            case DOWN -> "bottom";
            case UP -> "top";
            default -> "side";
        };
        return Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(folder.withSuffix("/" + face));
    }

    private static ResourceLocation texture(String folder) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID,
                "block/gt6_import/multiblockmains/crucible/" + folder);
    }
}
