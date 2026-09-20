package com.masson.cruciblecraft.client.render;

import java.util.Map;

import org.joml.Matrix4f;

import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.RedstoneWireBlockEntity;
import com.masson.cruciblecraft.client.color.MaterialItemColor;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverVisuals;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverComponentTiers;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverItemFilters;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverTextureCycle;
import com.masson.cruciblecraft.logistics.pipe.cover.DecorativeCovers;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.cover.PlateCovers;
import com.masson.cruciblecraft.logistics.displaycpu.DisplayCpuKinds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** GT6-style face plates on pipes and processing machines. */
public final class PipeCoverRenderer<T extends BlockEntity>
        implements BlockEntityRenderer<T> {
    private static final ResourceLocation ATLAS =
            TextureAtlas.LOCATION_BLOCKS;
    private static final ResourceLocation BASE =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "block/gt6_import/covers/base");
    private static final float INSET = 0.002f;
    private static final float OVERLAY = 0.001f;
    private static final float COVER_THICKNESS = 1.0f / 16.0f;

    public PipeCoverRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            T blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        renderMounted(blockEntity, poseStack, buffers, packedLight);
    }

    public static void renderMounted(
            BlockEntity blockEntity,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        Map<Direction, PipeCover> covers;
        float half = 0.5f;
        if (blockEntity instanceof ItemPipeBlockEntity pipe) {
            covers = pipe.coverSnapshot();
        } else if (blockEntity instanceof FluidPipeBlockEntity pipe) {
            covers = pipe.coverSnapshot();
        } else if (blockEntity instanceof RedstoneWireBlockEntity wire) {
            covers = wire.covers().snapshot();
        } else if (blockEntity instanceof CableBlockEntity cable) {
            covers = cable.covers().snapshot();
        } else if (blockEntity instanceof MachineCoverHost machine) {
            covers = machine.covers().snapshot();
        } else {
            return;
        }
        if (covers.isEmpty()) {
            return;
        }
        BlockState state = blockEntity.getBlockState();
        if (state.getBlock() instanceof AbstractPipeBlock pipe) {
            half = Math.max(pipe.pipe().width() / 32.0f, 0.25f);
        }
        TextureAtlasSprite base = sprite(BASE);
        VertexConsumer vertices = buffers.getBuffer(
                RenderType.cutoutMipped());
        for (Map.Entry<Direction, PipeCover> entry : covers.entrySet()) {
            PipeCover cover = entry.getValue();
            Direction face = entry.getKey();
            if (DisplayCpuKinds.isDisplay(cover.definitionId())) {
                pose(poseStack, vertices, face, half, 0.0f, base, packedLight);
                String folder = "display_cpu_"
                        + cover.definitionId().getPath().substring(
                                "logistics_display_cpu_".length());
                pose(
                        poseStack,
                        vertices,
                        face,
                        half,
                        OVERLAY,
                        sprite(coverTexture(folder, "underlay")),
                        packedLight);
                pose(
                        poseStack,
                        vertices,
                        face,
                        half,
                        OVERLAY * 2,
                        sprite(coverTexture(
                                folder,
                                Integer.toString(cover.config().visual()))),
                        packedLight);
                continue;
            }
            if (renderWireTorch(
                    poseStack,
                    vertices,
                    cover,
                    face,
                    packedLight)) {
                continue;
            }
            if (renderRemainder(
                    poseStack,
                    vertices,
                    cover,
                    face,
                    half,
                    base,
                    packedLight)) {
                continue;
            }
            if (renderPlate(
                    poseStack,
                    vertices,
                    cover,
                    face,
                    half,
                    packedLight,
                    blockEntity)) {
                continue;
            }
            if (renderDecorative(
                    poseStack,
                    vertices,
                    cover,
                    face,
                    half,
                    packedLight,
                    blockEntity)) {
                continue;
            }
            pose(poseStack, vertices, face, half, 0.0f, base, packedLight);
            ResourceLocation overlay = overlay(cover);
            if (overlay == null) {
                continue;
            }
            pose(
                    poseStack,
                    vertices,
                    face,
                    half,
                    OVERLAY,
                    sprite(overlay),
                    packedLight);
        }
    }

    private static boolean renderPlate(
            PoseStack poseStack,
            VertexConsumer vertices,
            PipeCover cover,
            Direction face,
            float half,
            int packedLight,
            BlockEntity blockEntity) {
        if (!PlateCovers.isPlate(cover)) {
            return false;
        }
        ItemStack stack = CoverTextureCycle.plateSkin(cover);
        if (stack.isEmpty()) {
            return false;
        }
        BakedModel model = Minecraft.getInstance()
                .getItemRenderer()
                .getModel(stack, blockEntity.getLevel(), null, 0);
        TextureAtlasSprite sprite = model.getParticleIcon();
        int color = MaterialItemColor.color(stack, 0);
        pose(
                poseStack,
                vertices,
                face,
                half,
                0.0f,
                sprite,
                packedLight,
                color);
        return true;
    }

    private static boolean renderDecorative(
            PoseStack poseStack,
            VertexConsumer vertices,
            PipeCover cover,
            Direction face,
            float half,
            int packedLight,
            BlockEntity blockEntity) {
        if (!DecorativeCovers.isDecorative(cover)) {
            return false;
        }
        ItemStack stack = DecorativeCovers.stackFor(cover);
        if (stack.isEmpty()) {
            return false;
        }
        BakedModel model = Minecraft.getInstance()
                .getItemRenderer()
                .getModel(stack, blockEntity.getLevel(), null, 0);
        TextureAtlasSprite sprite = model.getParticleIcon();
        pose(
                poseStack,
                vertices,
                face,
                half,
                0.0f,
                sprite,
                packedLight);
        return true;
    }

    private static boolean renderWireTorch(
            PoseStack poseStack,
            VertexConsumer vertices,
            PipeCover cover,
            Direction face,
            int packedLight) {
        if (!MachineCoverKinds.isWireOnlyCover(cover.definitionId())) {
            return false;
        }
        String folder = cover.definitionId().getPath();
        String state = cover.config().visual() == 0 ? "on" : "off";
        poseCoverSlab(
                poseStack,
                vertices,
                face,
                1.0f / 16.0f,
                0.0f,
                0.5f,
                sprite(coverTexture(folder + "/" + state, "front")),
                sprite(coverTexture(folder + "/" + state, "side")),
                sprite(coverTexture(folder + "/" + state, "side")),
                packedLight);
        return true;
    }

    private static boolean renderRemainder(
            PoseStack poseStack,
            VertexConsumer vertices,
            PipeCover cover,
            Direction face,
            float half,
            TextureAtlasSprite sharedBase,
            int packedLight) {
        if (!MachineCoverKinds.isRemainder(cover.definitionId())) {
            return false;
        }
        String path = cover.definitionId().getPath();
        if ("controller_display".equals(path)) {
            int visual = cover.config().visual();
            String skin = MachineCoverVisuals.displaySkin(visual);
            pose(
                    poseStack,
                    vertices,
                    face,
                    half,
                    0.0f,
                    sprite(coverTexture(
                            "controller_display/" + skin, "base")),
                    packedLight);
            float extra = OVERLAY;
            for (int light = 0; light < 4; light++) {
                if (!MachineCoverVisuals.displayLightPresent(visual, light)) {
                    continue;
                }
                boolean on = MachineCoverVisuals.displayLightOn(visual, light);
                pose(
                        poseStack,
                        vertices,
                        face,
                        half,
                        extra,
                        sprite(coverTexture(
                                "controller_display/" + skin,
                                (light + 1) + (on ? "_on" : "_off"))),
                        packedLight);
                extra += OVERLAY;
            }
            return true;
        }
        if ("display_energy".equals(path)
                || "redstone_emitter".equals(path)
                || "selector_redstone".equals(path)
                || "selector_tag".equals(path)
                || "selector_button_panel".equals(path)) {
            int frame;
            String underlay = "underlay";
            if ("display_energy".equals(path)) {
                frame = Math.max(0, Math.min(10, cover.config().visual()));
            } else if ("selector_button_panel".equals(path)) {
                frame = MachineCoverVisuals.buttonMode(cover.config().visual());
                underlay = MachineCoverVisuals.buttonUnderlay(
                        cover.config().visual());
            } else {
                frame = Math.max(0, Math.min(15, cover.config().redstone()));
            }
            pose(
                    poseStack,
                    vertices,
                    face,
                    half,
                    0.0f,
                    sprite(coverTexture(path, underlay)),
                    packedLight);
            pose(
                    poseStack,
                    vertices,
                    face,
                    half,
                    OVERLAY,
                    sprite(coverTexture(path, Integer.toString(frame))),
                    packedLight);
            return true;
        }
        if ("controller_covers".equals(path)) {
            pose(
                    poseStack,
                    vertices,
                    face,
                    half,
                    0.0f,
                    sprite(coverTexture(path, "base")),
                    packedLight);
            pose(
                    poseStack,
                    vertices,
                    face,
                    half,
                    OVERLAY,
                    sprite(coverTexture(path, "circuit")),
                    packedLight);
            return true;
        }
        if ("vent".equals(path) || "cover_drain".equals(path)) {
            poseCoverSlab(
                    poseStack,
                    vertices,
                    face,
                    half,
                    0.0f,
                    sprite(coverTexture(path, "front")),
                    sprite(coverTexture(path, "back")),
                    sprite(coverTexture(path, "sides")),
                    packedLight);
            return true;
        }
        pose(poseStack, vertices, face, half, 0.0f, sharedBase, packedLight);
        ResourceLocation overlay = remainderOverlay(cover);
        if (overlay != null) {
            pose(
                    poseStack,
                    vertices,
                    face,
                    half,
                    OVERLAY,
                    sprite(overlay),
                    packedLight);
        }
        return true;
    }

    private static ResourceLocation remainderOverlay(PipeCover cover) {
        String path = cover.definitionId().getPath();
        if ("cover_blank".equals(path) || "cover_crafting".equals(path)) {
            int frame = Math.floorMod(cover.config().visual(), 6);
            return coverTexture(path, Integer.toString(frame));
        }
        if ("cover_warning".equals(path)) {
            int frame = Math.floorMod(cover.config().visual(), 20);
            return coverTexture(path, Integer.toString(frame));
        }
        if ("filter_fluid".equals(path)) {
            return coverTexture(
                    path,
                    CoverItemFilters.inverted(cover.config())
                            ? "inverted"
                            : "normal");
        }
        if ("redstone_conductor_in".equals(path)) {
            return coverTexture(path, "in");
        }
        if ("redstone_conductor_out".equals(path)) {
            return coverTexture(path, "out");
        }
        return switch (path) {
            case "controller_auto",
                    "controller_redstone",
                    "controller_auto_redstone",
                    "controller_auto_timer_1m",
                    "controller_auto_timer_5m",
                    "controller_auto_timer_10m",
                    "controller_auto_timer_20m",
                    "controller_auto_timer_30m",
                    "scale_energy",
                    "scale_progress",
                    "detector_running_possible",
                    "detector_running_passively",
                    "detector_running_actively",
                    "detector_running_successfully" ->
                    coverTexture(path, "circuit");
            default -> null;
        };
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

    private static void poseCoverSlab(
            PoseStack poseStack,
            VertexConsumer vertices,
            Direction face,
            float half,
            float extraInset,
            TextureAtlasSprite front,
            TextureAtlasSprite back,
            TextureAtlasSprite sides,
            int light) {
        poseCoverSlab(
                poseStack,
                vertices,
                face,
                half,
                extraInset,
                COVER_THICKNESS,
                front,
                back,
                sides,
                light);
    }

    private static void poseCoverSlab(
            PoseStack poseStack,
            VertexConsumer vertices,
            Direction face,
            float half,
            float extraInset,
            float thickness,
            TextureAtlasSprite front,
            TextureAtlasSprite back,
            TextureAtlasSprite sides,
            int light) {
        float outer = INSET + extraInset;
        float inner = outer + thickness;
        float min = 0.5f - half;
        float max = 0.5f + half;
        Matrix4f pose = poseStack.last().pose();
        switch (face) {
            case NORTH -> {
                quad(poseStack, vertices, pose, front, light, Direction.NORTH,
                        max, min, outer, max, max, outer, min, max, outer, min, min, outer);
                quad(poseStack, vertices, pose, back, light, Direction.SOUTH,
                        min, min, inner, min, max, inner, max, max, inner, max, min, inner);
                quad(poseStack, vertices, pose, sides, light, Direction.UP,
                        min, max, outer, max, max, outer, max, max, inner, min, max, inner);
                quad(poseStack, vertices, pose, sides, light, Direction.DOWN,
                        min, min, inner, max, min, inner, max, min, outer, min, min, outer);
                quad(poseStack, vertices, pose, sides, light, Direction.WEST,
                        min, min, inner, min, max, inner, min, max, outer, min, min, outer);
                quad(poseStack, vertices, pose, sides, light, Direction.EAST,
                        max, min, outer, max, max, outer, max, max, inner, max, min, inner);
            }
            case SOUTH -> {
                float southOuter = 1.0f - outer;
                float southInner = 1.0f - inner;
                quad(poseStack, vertices, pose, front, light, Direction.SOUTH,
                        min, min, southOuter, min, max, southOuter, max, max, southOuter, max, min, southOuter);
                quad(poseStack, vertices, pose, back, light, Direction.NORTH,
                        max, min, southInner, max, max, southInner, min, max, southInner, min, min, southInner);
                quad(poseStack, vertices, pose, sides, light, Direction.UP,
                        max, max, southOuter, min, max, southOuter, min, max, southInner, max, max, southInner);
                quad(poseStack, vertices, pose, sides, light, Direction.DOWN,
                        max, min, southInner, min, min, southInner, min, min, southOuter, max, min, southOuter);
                quad(poseStack, vertices, pose, sides, light, Direction.WEST,
                        min, min, southOuter, min, max, southOuter, min, max, southInner, min, min, southInner);
                quad(poseStack, vertices, pose, sides, light, Direction.EAST,
                        max, min, southInner, max, max, southInner, max, max, southOuter, max, min, southOuter);
            }
            case WEST -> {
                quad(poseStack, vertices, pose, front, light, Direction.WEST,
                        outer, min, min, outer, max, min, outer, max, max, outer, min, max);
                quad(poseStack, vertices, pose, back, light, Direction.EAST,
                        inner, min, max, inner, max, max, inner, max, min, inner, min, min);
                quad(poseStack, vertices, pose, sides, light, Direction.UP,
                        outer, max, min, inner, max, min, inner, max, max, outer, max, max);
                quad(poseStack, vertices, pose, sides, light, Direction.DOWN,
                        outer, min, max, inner, min, max, inner, min, min, outer, min, min);
                quad(poseStack, vertices, pose, sides, light, Direction.NORTH,
                        outer, min, min, outer, max, min, inner, max, min, inner, min, min);
                quad(poseStack, vertices, pose, sides, light, Direction.SOUTH,
                        inner, min, max, inner, max, max, outer, max, max, outer, min, max);
            }
            case EAST -> {
                float eastOuter = 1.0f - outer;
                float eastInner = 1.0f - inner;
                quad(poseStack, vertices, pose, front, light, Direction.EAST,
                        eastOuter, min, max, eastOuter, max, max, eastOuter, max, min, eastOuter, min, min);
                quad(poseStack, vertices, pose, back, light, Direction.WEST,
                        eastInner, min, min, eastInner, max, min, eastInner, max, max, eastInner, min, max);
                quad(poseStack, vertices, pose, sides, light, Direction.UP,
                        eastInner, max, min, eastOuter, max, min, eastOuter, max, max, eastInner, max, max);
                quad(poseStack, vertices, pose, sides, light, Direction.DOWN,
                        eastInner, min, max, eastOuter, min, max, eastOuter, min, min, eastInner, min, min);
                quad(poseStack, vertices, pose, sides, light, Direction.NORTH,
                        eastInner, min, min, eastInner, max, min, eastOuter, max, min, eastOuter, min, min);
                quad(poseStack, vertices, pose, sides, light, Direction.SOUTH,
                        eastOuter, min, max, eastOuter, max, max, eastInner, max, max, eastInner, min, max);
            }
            case DOWN -> {
                quad(poseStack, vertices, pose, front, light, Direction.DOWN,
                        min, outer, max, max, outer, max, max, outer, min, min, outer, min);
                quad(poseStack, vertices, pose, back, light, Direction.UP,
                        min, inner, min, max, inner, min, max, inner, max, min, inner, max);
                quad(poseStack, vertices, pose, sides, light, Direction.NORTH,
                        max, outer, min, min, outer, min, min, inner, min, max, inner, min);
                quad(poseStack, vertices, pose, sides, light, Direction.SOUTH,
                        min, outer, max, max, outer, max, max, inner, max, min, inner, max);
                quad(poseStack, vertices, pose, sides, light, Direction.WEST,
                        min, outer, min, min, outer, max, min, inner, max, min, inner, min);
                quad(poseStack, vertices, pose, sides, light, Direction.EAST,
                        max, outer, max, max, outer, min, max, inner, min, max, inner, max);
            }
            case UP -> {
                float upOuter = 1.0f - outer;
                float upInner = 1.0f - inner;
                quad(poseStack, vertices, pose, front, light, Direction.UP,
                        min, upOuter, min, max, upOuter, min, max, upOuter, max, min, upOuter, max);
                quad(poseStack, vertices, pose, back, light, Direction.DOWN,
                        min, upInner, max, max, upInner, max, max, upInner, min, min, upInner, min);
                quad(poseStack, vertices, pose, sides, light, Direction.NORTH,
                        min, upOuter, min, max, upOuter, min, max, upInner, min, min, upInner, min);
                quad(poseStack, vertices, pose, sides, light, Direction.SOUTH,
                        max, upOuter, max, min, upOuter, max, min, upInner, max, max, upInner, max);
                quad(poseStack, vertices, pose, sides, light, Direction.WEST,
                        min, upOuter, max, min, upOuter, min, min, upInner, min, min, upInner, max);
                quad(poseStack, vertices, pose, sides, light, Direction.EAST,
                        max, upOuter, min, max, upOuter, max, max, upInner, max, max, upInner, min);
            }
        }
    }

    private static void pose(
            PoseStack poseStack,
            VertexConsumer vertices,
            Direction face,
            float half,
            float extraInset,
            TextureAtlasSprite sprite,
            int light) {
        pose(poseStack, vertices, face, half, extraInset, sprite, light, 0xFFFFFFFF);
    }

    private static void pose(
            PoseStack poseStack,
            VertexConsumer vertices,
            Direction face,
            float half,
            float extraInset,
            TextureAtlasSprite sprite,
            int light,
            int color) {
        float inset = INSET + extraInset;
        float min = 0.5f - half;
        float max = 0.5f + half;
        Matrix4f pose = poseStack.last().pose();
        switch (face) {
            case NORTH -> quad(
                    poseStack, vertices, pose, sprite, light, face, color,
                    max, min, inset,
                    max, max, inset,
                    min, max, inset,
                    min, min, inset);
            case SOUTH -> quad(
                    poseStack, vertices, pose, sprite, light, face, color,
                    min, min, 1.0f - inset,
                    min, max, 1.0f - inset,
                    max, max, 1.0f - inset,
                    max, min, 1.0f - inset);
            case WEST -> quad(
                    poseStack, vertices, pose, sprite, light, face, color,
                    inset, min, min,
                    inset, max, min,
                    inset, max, max,
                    inset, min, max);
            case EAST -> quad(
                    poseStack, vertices, pose, sprite, light, face, color,
                    1.0f - inset, min, max,
                    1.0f - inset, max, max,
                    1.0f - inset, max, min,
                    1.0f - inset, min, min);
            case DOWN -> quad(
                    poseStack, vertices, pose, sprite, light, face, color,
                    min, inset, max,
                    max, inset, max,
                    max, inset, min,
                    min, inset, min);
            case UP -> quad(
                    poseStack, vertices, pose, sprite, light, face, color,
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
        quad(
                poseStack, vertices, pose, sprite, light, face, 0xFFFFFFFF,
                x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4);
    }

    private static void quad(
            PoseStack poseStack,
            VertexConsumer vertices,
            Matrix4f pose,
            TextureAtlasSprite sprite,
            int light,
            Direction face,
            int color,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4) {
        vertex(vertices, poseStack, pose, x1, y1, z1, sprite.getU0(), sprite.getV1(), light, face, color);
        vertex(vertices, poseStack, pose, x2, y2, z2, sprite.getU0(), sprite.getV0(), light, face, color);
        vertex(vertices, poseStack, pose, x3, y3, z3, sprite.getU1(), sprite.getV0(), light, face, color);
        vertex(vertices, poseStack, pose, x4, y4, z4, sprite.getU1(), sprite.getV1(), light, face, color);
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
            Direction face,
            int color) {
        vertices.addVertex(pose, x, y, z)
                .setColor(
                        (color >> 16) & 0xFF,
                        (color >> 8) & 0xFF,
                        color & 0xFF,
                        (color >> 24) & 0xFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(
                        poseStack.last(),
                        face.getStepX(),
                        face.getStepY(),
                        face.getStepZ());
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
        vertex(
                vertices, poseStack, pose, x, y, z, u, v, light, face,
                0xFFFFFFFF);
    }
}
