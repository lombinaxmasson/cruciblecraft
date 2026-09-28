package com.masson.cruciblecraft.compat.emi.multiblock;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws a {@link PreviewBlockGetter} into its own colour and depth target
 * with a perspective camera, then blits that picture into the GUI. Block
 * layers go through the chunk render types so faces between neighbours are
 * culled and ambient occlusion reads the stand-in world.
 */
final class PreviewRenderer {
    private static TextureTarget target;
    /** Block entity types whose renderer threw without a live level. */
    private static final Set<Object> FAILED_RENDERERS = new HashSet<>();

    private PreviewRenderer() {}

    static void draw(
            PreviewBlockGetter world,
            PreviewCamera camera,
            @Nullable BlockPos highlight,
            int pixelWidth,
            int pixelHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        TextureTarget canvas = canvas(pixelWidth, pixelHeight);
        canvas.setClearColor(0f, 0f, 0f, 0f);
        canvas.clear(Minecraft.ON_OSX);
        canvas.bindWrite(true);

        Matrix4f savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting savedSorting = RenderSystem.getVertexSorting();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(
                camera.projection(),
                VertexSorting.byDistance(
                        camera.view().transformPosition(new org.joml.Vector3f())));
        FogRenderer.setupNoFog();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        try {
            PoseStack pose = new PoseStack();
            pose.mulPose(camera.view());
            ByteBufferBuilder builder = new ByteBufferBuilder(256 * 1024);
            try {
                MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(builder);
                drawBlocks(world, pose, buffers);
                drawBlockEntities(world, pose, buffers);
                if (highlight != null) {
                    LevelRenderer.renderLineBox(
                            pose,
                            buffers.getBuffer(RenderType.lines()),
                            new AABB(highlight).inflate(0.004),
                            1f,
                            1f,
                            1f,
                            1f);
                    buffers.endBatch(RenderType.lines());
                }
                buffers.endBatch();
            } finally {
                builder.close();
            }
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(savedProjection, savedSorting);
            main.bindWrite(true);
        }
    }

    /** Draws the last canvas over a GUI rectangle in the current pose. */
    static void blit(Matrix4f pose, float x, float y, float width, float height) {
        if (target == null) {
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(pose, x, y, 0f).setUv(0f, 1f);
        builder.addVertex(pose, x, y + height, 0f).setUv(0f, 0f);
        builder.addVertex(pose, x + width, y + height, 0f).setUv(1f, 0f);
        builder.addVertex(pose, x + width, y, 0f).setUv(1f, 1f);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private static void drawBlocks(
            PreviewBlockGetter world,
            PoseStack pose,
            MultiBufferSource.BufferSource buffers) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        RandomSource random = RandomSource.create();
        for (RenderType layer : RenderType.chunkBufferLayers()) {
            for (Map.Entry<BlockPos, BlockState> entry : world.states().entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();
                if (state.getRenderShape() != RenderShape.MODEL) {
                    continue;
                }
                BakedModel model = dispatcher.getBlockModel(state);
                BlockEntity blockEntity = world.getBlockEntity(pos);
                ModelData data = model.getModelData(
                        world,
                        pos,
                        state,
                        blockEntity == null ? ModelData.EMPTY : blockEntity.getModelData());
                random.setSeed(state.getSeed(pos));
                if (!model.getRenderTypes(state, random, data).contains(layer)) {
                    continue;
                }
                pose.pushPose();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                dispatcher.renderBatched(
                        state,
                        pos,
                        world,
                        pose,
                        buffers.getBuffer(layer),
                        true,
                        random,
                        data,
                        layer);
                pose.popPose();
            }
            buffers.endBatch(layer);
        }
    }

    private static void drawBlockEntities(
            PreviewBlockGetter world,
            PoseStack pose,
            MultiBufferSource.BufferSource buffers) {
        var dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        for (Map.Entry<BlockPos, BlockEntity> entry : world.blockEntities().entrySet()) {
            BlockEntity blockEntity = entry.getValue();
            if (FAILED_RENDERERS.contains(blockEntity.getType())) {
                continue;
            }
            BlockEntityRenderer<BlockEntity> renderer = dispatcher.getRenderer(blockEntity);
            if (renderer == null) {
                continue;
            }
            BlockPos pos = entry.getKey();
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            try {
                renderer.render(
                        blockEntity,
                        0f,
                        pose,
                        buffers,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY);
            } catch (RuntimeException exception) {
                FAILED_RENDERERS.add(blockEntity.getType());
            } finally {
                pose.popPose();
            }
        }
        buffers.endBatch();
    }

    private static TextureTarget canvas(int width, int height) {
        int w = Math.max(width, 1);
        int h = Math.max(height, 1);
        if (target == null) {
            target = new TextureTarget(w, h, true, Minecraft.ON_OSX);
        } else if (target.width != w || target.height != h) {
            target.resize(w, h, Minecraft.ON_OSX);
        }
        return target;
    }
}
