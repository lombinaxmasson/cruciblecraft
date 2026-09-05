package com.masson.cruciblecraft.client.render;

import java.util.Map;

import org.joml.Matrix4f;

import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverComponentTiers;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverItemFilters;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.displaycpu.DisplayCpuKinds;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** GT6-style face plates on item and fluid pipes. */
public final class PipeCoverRenderer<T extends BlockEntity>
        implements BlockEntityRenderer<T> {
    private static final ResourceLocation ATLAS =
            TextureAtlas.LOCATION_BLOCKS;
    private static final ResourceLocation BASE =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "block/gt6_import/covers/base");
    private static final float INSET = 0.002f;
    private static final float OVERLAY = 0.001f;

    public PipeCoverRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            T blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        Map<Direction, PipeCover> covers;
        if (blockEntity instanceof ItemPipeBlockEntity pipe) {
            covers = pipe.coverSnapshot();
        } else if (blockEntity instanceof FluidPipeBlockEntity pipe) {
            covers = pipe.coverSnapshot();
        } else {
            return;
        }
        if (covers.isEmpty()) {
            return;
        }
        BlockState state = blockEntity.getBlockState();
        int width = 8;
        if (state.getBlock() instanceof AbstractPipeBlock pipe) {
            width = pipe.pipe().width();
        }
        float half = Math.max(width / 32.0f, 0.25f);
        TextureAtlasSprite base = sprite(BASE);
        VertexConsumer vertices = buffers.getBuffer(
                RenderType.cutoutMipped());
        for (Map.Entry<Direction, PipeCover> entry : covers.entrySet()) {
            PipeCover cover = entry.getValue();
            pose(poseStack, vertices, entry.getKey(), half, 0.0f, base, packedLight);
            if (DisplayCpuKinds.isDisplay(cover.definitionId())) {
                String folder = "display_cpu_"
                        + cover.definitionId().getPath().substring(
                                "logistics_display_cpu_".length());
                pose(
                        poseStack,
                        vertices,
                        entry.getKey(),
                        half,
                        OVERLAY,
                        sprite(coverTexture(folder, "underlay")),
                        packedLight);
                pose(
                        poseStack,
                        vertices,
                        entry.getKey(),
                        half,
                        OVERLAY * 2,
                        sprite(coverTexture(
                                folder,
                                Integer.toString(cover.config().visual()))),
                        packedLight);
                continue;
            }
            ResourceLocation overlay = overlay(cover);
            if (overlay == null) {
                continue;
            }
            pose(
                    poseStack,
                    vertices,
                    entry.getKey(),
                    half,
                    OVERLAY,
                    sprite(overlay),
                    packedLight);
        }
    }

    private static ResourceLocation overlay(PipeCover cover) {
        String path = cover.definitionId().getPath();
        boolean invert = CoverItemFilters.inverted(cover.config());
        var tier = CoverComponentTiers.findByDefinition(cover.definitionId());
        if (tier.isPresent()) {
            path = switch (tier.orElseThrow().family()) {
                case CONVEYOR -> "conveyor";
                case ROBOT_ARM -> "robot_arm";
                case PUMP -> "pump";
            };
        }
        String name = switch (path) {
            case "filter" -> invert ? "filter_inverted" : "filter_normal";
            case "shutter" -> "shutter";
            case "pump" -> "pump_in";
            case "conveyor", "conveyor_fast" -> "conveyor_in";
            case "retriever_item" -> invert
                    ? "retriever_inverted"
                    : "retriever_normal";
            case "robot_arm" -> "robot_arm_in";
            case "pressure_valve" -> "pressure_valve";
            case "selector_manual" -> "selector";
            case "logistics_item_storage",
                    "logistics_item_import",
                    "logistics_item_export",
                    "logistics_fluid_storage",
                    "logistics_fluid_import",
                    "logistics_fluid_export",
                    "logistics_generic_storage",
                    "logistics_generic_import",
                    "logistics_generic_export",
                    "logistics_generic_dump" -> path;
            default -> null;
        };
        if (name == null) {
            return null;
        }
        return coverTexture(name, null);
    }

    private static ResourceLocation coverTexture(String name, String frame) {
        String path = frame == null
                ? "block/gt6_import/covers/" + name
                : "block/gt6_import/covers/" + name + "/" + frame;
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }

    private static TextureAtlasSprite sprite(ResourceLocation location) {
        return Minecraft.getInstance().getTextureAtlas(ATLAS).apply(location);
    }

    private static void pose(
            PoseStack poseStack,
            VertexConsumer vertices,
            Direction face,
            float half,
            float extraInset,
            TextureAtlasSprite sprite,
            int light) {
        float inset = INSET + extraInset;
        float min = 0.5f - half;
        float max = 0.5f + half;
        Matrix4f pose = poseStack.last().pose();
        switch (face) {
            case NORTH -> quad(
                    poseStack, vertices, pose, sprite, light, face,
                    max, min, inset,
                    max, max, inset,
                    min, max, inset,
                    min, min, inset);
            case SOUTH -> quad(
                    poseStack, vertices, pose, sprite, light, face,
                    min, min, 1.0f - inset,
                    min, max, 1.0f - inset,
                    max, max, 1.0f - inset,
                    max, min, 1.0f - inset);
            case WEST -> quad(
                    poseStack, vertices, pose, sprite, light, face,
                    inset, min, min,
                    inset, max, min,
                    inset, max, max,
                    inset, min, max);
            case EAST -> quad(
                    poseStack, vertices, pose, sprite, light, face,
                    1.0f - inset, min, max,
                    1.0f - inset, max, max,
                    1.0f - inset, max, min,
                    1.0f - inset, min, min);
            case DOWN -> quad(
                    poseStack, vertices, pose, sprite, light, face,
                    min, inset, max,
                    max, inset, max,
                    max, inset, min,
                    min, inset, min);
            case UP -> quad(
                    poseStack, vertices, pose, sprite, light, face,
                    min, 1.0f - inset, min,
                    max, 1.0f - inset, min,
                    max, 1.0f - inset, max,
                    min, 1.0f - inset, max);
        }
    }

    private static void quad(
            PoseStack poseStack,
            VertexConsumer vertices,
            Matrix4f pose,
            TextureAtlasSprite sprite,
            int light,
            Direction face,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4) {
        vertex(vertices, poseStack, pose, x1, y1, z1, sprite.getU0(), sprite.getV1(), light, face);
        vertex(vertices, poseStack, pose, x2, y2, z2, sprite.getU0(), sprite.getV0(), light, face);
        vertex(vertices, poseStack, pose, x3, y3, z3, sprite.getU1(), sprite.getV0(), light, face);
        vertex(vertices, poseStack, pose, x4, y4, z4, sprite.getU1(), sprite.getV1(), light, face);
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
            int light,
            Direction face) {
        vertices.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(
                        poseStack.last(),
                        face.getStepX(),
                        face.getStepY(),
                        face.getStepZ());
    }
}
