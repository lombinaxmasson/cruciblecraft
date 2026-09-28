package com.masson.cruciblecraft.client.multiblockpreview;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import com.masson.cruciblecraft.compat.emi.multiblock.MultiblockProjectionGrid;
import com.masson.cruciblecraft.compat.emi.multiblock.PreviewBlockGetter;
import com.masson.cruciblecraft.compat.emi.multiblock.PreviewScene;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * One client-side structure overlay anchored on a controller block. Empty
 * cells draw as a translucent ghost; cells holding the wrong block draw a
 * red outline. Nothing is placed and nothing is sent to the server.
 */
public final class WorldStructurePreview {
    public static final int DURATION_TICKS = 30 * 20;

    private final MultiblockProjectionGrid grid;
    private final BlockPos controller;
    private final Direction facing;
    private final long expiresAtGameTime;
    private final PreviewBlockGetter ghosts;

    public WorldStructurePreview(
            MultiblockProjectionGrid grid,
            BlockPos controller,
            Direction facing,
            long expiresAtGameTime) {
        this.grid = grid;
        this.controller = controller.immutable();
        this.facing = facing;
        this.expiresAtGameTime = expiresAtGameTime;
        this.ghosts = PreviewBlockGetter.of(PreviewScene.of(grid, null));
    }

    public MultiblockProjectionGrid grid() {
        return grid;
    }

    public boolean expired(long gameTime) {
        return gameTime >= expiresAtGameTime;
    }

    public void render(
            Level level,
            Vec3 camera,
            PoseStack pose,
            Matrix4f cameraRotation,
            MultiBufferSource.BufferSource buffers) {
        pose.pushPose();
        pose.mulPose(cameraRotation);
        pose.translate(-camera.x, -camera.y, -camera.z);
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        PreviewBlockGetter placed = ghosts.placed(controller, facing);
        RandomSource random = RandomSource.create();
        List<BlockPos> wrong = new ArrayList<>();
        for (MultiblockProjectionGrid.Cell cell : grid.cells()) {
            Offset rotated = cell.offset().rotate(facing);
            BlockPos pos = controller.offset(rotated.x(), rotated.y(), rotated.z());
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState actual = level.getBlockState(pos);
            if (matches(cell, actual, pos)) {
                continue;
            }
            if (actual.isAir()) {
                drawGhost(dispatcher, cell, pos, pose, buffers, random, placed);
            } else {
                wrong.add(pos);
            }
        }
        if (!wrong.isEmpty()) {
            VertexConsumer lines = buffers.getBuffer(RenderType.lines());
            for (BlockPos pos : wrong) {
                AABB box = new AABB(pos).move(-camera.x, -camera.y, -camera.z).inflate(0.004);
                com.masson.cruciblecraft.client.multiblockpreview.PreviewOutlines
                        .box(cameraRotation, lines, box, 0.85f, 0.1f, 0.1f, 1f);
            }
            buffers.endBatch(RenderType.lines());
        }
        buffers.endBatch();
        pose.popPose();
    }

    private boolean matches(
            MultiblockProjectionGrid.Cell cell, BlockState actual, BlockPos pos) {
        if (pos.equals(controller)) {
            return grid.controllers().contains(
                    BuiltInRegistries.BLOCK.getKey(actual.getBlock()));
        }
        return grid.materials().stream()
                .filter(material -> material.paletteKey().equals(cell.paletteKey()))
                .findFirst()
                .map(material -> material.itemBlocks().contains(
                        BuiltInRegistries.BLOCK.getKey(actual.getBlock())))
                .orElse(false);
    }

    private static void drawGhost(
            BlockRenderDispatcher dispatcher,
            MultiblockProjectionGrid.Cell cell,
            BlockPos pos,
            PoseStack pose,
            MultiBufferSource.BufferSource buffers,
            RandomSource random,
            PreviewBlockGetter ghosts) {
        BlockState state = BuiltInRegistries.BLOCK.getOptional(cell.block())
                .map(block -> block.defaultBlockState())
                .orElse(Blocks.AIR.defaultBlockState());
        if (state.getRenderShape() != RenderShape.MODEL) {
            return;
        }
        BakedModel model = dispatcher.getBlockModel(state);
        random.setSeed(state.getSeed(pos));
        ModelData data = model.getModelData(null, pos, state, ModelData.EMPTY);
        pose.pushPose();
        pose.translate(pos.getX(), pos.getY(), pos.getZ());
        for (RenderType layer : model.getRenderTypes(state, random, data)) {
            dispatcher.renderBatched(
                    state,
                    pos,
                    ghosts,
                    pose,
                    buffers.getBuffer(RenderType.translucent()),
                    false,
                    random,
                    data,
                    layer);
        }
        pose.popPose();
    }

    /** Controller block the crosshair is on, if it belongs to this grid. */
    public static BlockPos aimedController(Level level, MultiblockProjectionGrid grid) {
        HitResult hit = Minecraft.getInstance().hitResult;
        if (!(hit instanceof BlockHitResult blockHit)
                || blockHit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = blockHit.getBlockPos();
        return grid.controllers().contains(
                BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()))
                        ? pos
                        : null;
    }
}
