package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.Gt6StyleConnections;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

import org.joml.Vector3f;

/**
 * GTM {@code BlockHighlightRenderer.drawGridOverlays} for pipes and cables.
 *
 * <p>Holding the matching tune tool (wrench on pipes, wire cutter on cables)
 * draws the 0.25 / 0.75 nine-grid on the aimed face and a connect/block icon
 * in each cell. Cell → side mapping is {@link Gt6StyleConnections#nineGridSideOnFace},
 * the same table the click uses.
 */
@EventBusSubscriber(
        modid = CrucibleCraft.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.GAME)
public final class ConnectionGridOverlay {
    private static final ResourceLocation CONNECT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID,
                    "textures/gui/overlay/tool_pipe_connect.png");
    private static final ResourceLocation BLOCK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID,
                    "textures/gui/overlay/tool_pipe_block.png");
    private static final float FACE_EPSILON = 0.01F;
    private static final float ICON_MARGIN = 0.2F / 16.0F;

    private ConnectionGridOverlay() {}

    @SubscribeEvent
    public static void onRenderHighlight(RenderHighlightEvent.Block event) {
        BlockHitResult hit = event.getTarget();
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (!Gt6StyleConnections.holdingMatchingTool(
                state, player.getMainHandItem())) {
            return;
        }
        drawGrid(
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getCamera().getPosition(),
                hit,
                state);
    }

    private static void drawGrid(
            PoseStack poseStack,
            MultiBufferSource buffers,
            Vec3 camera,
            BlockHitResult hit,
            BlockState state) {
        Direction face = hit.getDirection();
        BlockPos pos = hit.getBlockPos();
        Vec3 location = hit.getLocation();
        double uHit = faceU(face, location.x - pos.getX(), location.z - pos.getZ());
        double vHit = faceV(face, location.y - pos.getY(), location.z - pos.getZ());
        int hoveredU = band(uHit);
        int hoveredV = band(vHit);

        float pulse =
                0.2F
                        + (float) Math.sin(
                                        (System.currentTimeMillis()
                                                        % (Mth.PI * 800))
                                                / 800)
                                / 2;
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        RenderSystem.lineWidth(3);
        drawLine(
                pose,
                lines,
                facePoint(pos, face, 0.25F, 0.0F),
                facePoint(pos, face, 0.25F, 1.0F),
                pulse);
        drawLine(
                pose,
                lines,
                facePoint(pos, face, 0.75F, 0.0F),
                facePoint(pos, face, 0.75F, 1.0F),
                pulse);
        drawLine(
                pose,
                lines,
                facePoint(pos, face, 0.0F, 0.25F),
                facePoint(pos, face, 1.0F, 0.25F),
                pulse);
        drawLine(
                pose,
                lines,
                facePoint(pos, face, 0.0F, 0.75F),
                facePoint(pos, face, 1.0F, 0.75F),
                pulse);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int cu = 0; cu < 3; cu++) {
            for (int cv = 0; cv < 3; cv++) {
                Direction side = Gt6StyleConnections.nineGridSideOnFace(
                        face, cellCenter(cu), cellCenter(cv));
                boolean open = Gt6StyleConnections.isOpen(state, side);
                boolean hovered = cu == hoveredU && cv == hoveredV;
                int color = hovered ? 0xFFFFFFFF : 0x44FFFFFF;
                drawIcon(
                        poseStack,
                        buffers,
                        pos,
                        face,
                        cellStart(cu),
                        cellStart(cv),
                        cellEnd(cu),
                        cellEnd(cv),
                        open ? CONNECT_TEXTURE : BLOCK_TEXTURE,
                        color);
            }
        }
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        poseStack.popPose();
    }

    static double faceU(Direction face, double fx, double fz) {
        return face.getAxis() == Direction.Axis.X ? fz : fx;
    }

    static double faceV(Direction face, double fy, double fz) {
        return face.getAxis() == Direction.Axis.Y ? fz : fy;
    }

    static int band(double t) {
        if (t < 0.25) {
            return 0;
        }
        return t > 0.75 ? 2 : 1;
    }

    static float cellStart(int cell) {
        return cell == 0 ? 0.0F : cell == 1 ? 0.25F : 0.75F;
    }

    static float cellEnd(int cell) {
        return cell == 0 ? 0.25F : cell == 1 ? 0.75F : 1.0F;
    }

    static float cellCenter(int cell) {
        return (cellStart(cell) + cellEnd(cell)) * 0.5F;
    }

    private static Vector3f facePoint(
            BlockPos pos, Direction face, float u, float v) {
        float x = pos.getX();
        float y = pos.getY();
        float z = pos.getZ();
        float n = FACE_EPSILON;
        return switch (face) {
            case DOWN -> new Vector3f(x + u, y - n, z + v);
            case UP -> new Vector3f(x + u, y + 1.0F + n, z + v);
            case NORTH -> new Vector3f(x + u, y + v, z - n);
            case SOUTH -> new Vector3f(x + u, y + v, z + 1.0F + n);
            case WEST -> new Vector3f(x - n, y + v, z + u);
            case EAST -> new Vector3f(x + 1.0F + n, y + v, z + u);
        };
    }

    private static void drawLine(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            Vector3f from,
            Vector3f to,
            float pulse) {
        Vector3f normal = new Vector3f(from).sub(to);
        if (normal.lengthSquared() < 1.0E-6F) {
            normal.set(0.0F, 1.0F, 0.0F);
        } else {
            normal.normalize();
        }
        buffer.addVertex(pose, from.x, from.y, from.z)
                .setColor(pulse, pulse, 1.0F, 1.0F)
                .setNormal(pose, normal.x, normal.y, normal.z);
        buffer.addVertex(pose, to.x, to.y, to.z)
                .setColor(pulse, pulse, 1.0F, 1.0F)
                .setNormal(pose, normal.x, normal.y, normal.z);
    }

    private static void drawIcon(
            PoseStack poseStack,
            MultiBufferSource buffers,
            BlockPos pos,
            Direction face,
            float u0,
            float v0,
            float u1,
            float v1,
            ResourceLocation texture,
            int color) {
        float x0 = u0 + ICON_MARGIN;
        float y0 = v0 + ICON_MARGIN;
        float x1 = u1 - ICON_MARGIN;
        float y1 = v1 - ICON_MARGIN;
        Vector3f p00 = facePoint(pos, face, x0, y0);
        Vector3f p10 = facePoint(pos, face, x1, y0);
        Vector3f p11 = facePoint(pos, face, x1, y1);
        Vector3f p01 = facePoint(pos, face, x0, y1);
        boolean outwardPositive = face.getAxisDirection()
                == Direction.AxisDirection.POSITIVE;
        VertexConsumer consumer =
                buffers.getBuffer(RenderType.textSeeThrough(texture));
        PoseStack.Pose pose = poseStack.last();
        if (outwardPositive) {
            quad(consumer, pose, p00, p01, p11, p10, color);
        } else {
            quad(consumer, pose, p00, p10, p11, p01, color);
        }
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vector3f a,
            Vector3f b,
            Vector3f c,
            Vector3f d,
            int color) {
        vertex(consumer, pose, a, 0.0F, 1.0F, color);
        vertex(consumer, pose, b, 1.0F, 1.0F, color);
        vertex(consumer, pose, c, 1.0F, 0.0F, color);
        vertex(consumer, pose, d, 0.0F, 0.0F, color);
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vector3f point,
            float u,
            float v,
            int color) {
        consumer.addVertex(pose, point.x, point.y, point.z)
                .setColor(color)
                .setUv(u, v)
                .setLight(LightTexture.FULL_BRIGHT);
    }
}
