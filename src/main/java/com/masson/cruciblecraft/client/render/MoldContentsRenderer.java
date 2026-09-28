package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.blockentity.CeramicMoldBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FoundryCastingBlockEntity;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
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

/**
 * Renders the material currently poured into a mold or basin.
 *
 * <p>The block model only describes the shell. A basin fills the inner cavity
 * as an opaque volume. Solidified basin contents use the metal
 * {@code blocksolid} icon plus the material color — not a translucent lid.</p>
 */
public final class MoldContentsRenderer<T extends BlockEntity>
        implements BlockEntityRenderer<T> {
    private static final ResourceLocation MOLTEN =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID,
                    "block/gt6_import/materialicons/rough_molten");
    private static final ResourceLocation SOLID =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID,
                    "block/gt6_import/materialicons/rough_block_raw");
    private static final ResourceLocation BLOCK_SOLID =
            StorageVoxelBuffer.blockTexture("gt6_import/anvil/blocksolid");
    /** Ceramic / foundry mold cavity. */
    private static final float MIN = 2.05F / 16.0F;
    private static final float MAX = 13.95F / 16.0F;
    private static final float Y = 3.01F / 16.0F;
    /**
     * GT6 basin pass 5 fills {@code PX_P[0]..PX_N[0]} x/z and
     * {@code PX_N[1]} high. The inner cavity is inside the one-pixel walls.
     */
    private static final float BASIN_INSET = 1.01F / 16.0F;
    private static final float BASIN_OUTER = 14.99F / 16.0F;
    private static final float BASIN_TOP = 15.0F / 16.0F;

    public MoldContentsRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            T blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        String materialId;
        float temperature;
        boolean solidified;
        int outputCount;
        boolean basin = false;
        if (blockEntity instanceof CeramicMoldBlockEntity mold) {
            materialId = mold.materialId();
            temperature = mold.temperature();
            solidified = mold.isSolidified();
            outputCount = mold.outputCount();
        } else if (blockEntity instanceof FoundryCastingBlockEntity mold) {
            materialId = mold.materialId();
            temperature = mold.temperature();
            solidified = mold.isSolidified();
            outputCount = mold.outputCount();
            basin = mold.basin();
        } else {
            return;
        }
        if (materialId.isEmpty() || outputCount <= 0
                || !MaterialCatalog.contains(materialId)) {
            return;
        }
        MaterialDefinition material = MaterialCatalog.require(materialId);
        boolean molten = !solidified
                && temperature >= material.thermal().meltingPoint();
        int color = 0xFF000000 | material.colorRgb();
        if (basin) {
            renderBasin(poseStack, buffers, packedLight, color, molten);
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(molten ? MOLTEN : SOLID);
        int light = molten ? LightTexture.FULL_BRIGHT : packedLight;
        int moldColor = molten ? color : 0xFF808080;
        poseStack.pushPose();
        VertexConsumer vertices = buffers.getBuffer(
                RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        quad(vertices, poseStack.last(), sprite, moldColor, light, MIN, Y, MIN, MAX, MAX);
        poseStack.popPose();
    }

    private static void renderBasin(
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int color,
            boolean molten) {
        float[] box = {
            BASIN_INSET,
            BASIN_INSET,
            BASIN_INSET,
            BASIN_OUTER,
            BASIN_TOP,
            BASIN_OUTER
        };
        poseStack.pushPose();
        if (molten) {
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .apply(MOLTEN);
            StorageVoxelBuffer.cube(
                    buffers.getBuffer(
                            RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)),
                    poseStack,
                    box,
                    sprite,
                    color,
                    LightTexture.FULL_BRIGHT);
        } else {
            StorageVoxelBuffer.cube(
                    buffers.getBuffer(RenderType.entityCutout(BLOCK_SOLID)),
                    poseStack,
                    box,
                    color,
                    packedLight);
        }
        poseStack.popPose();
    }

    private static void quad(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            int color,
            int light,
            float minX,
            float y,
            float minZ,
            float maxX,
            float maxZ) {
        vertex(vertices, pose, sprite, color, light, minX, y, minZ,
                sprite.getU0(), sprite.getV0());
        vertex(vertices, pose, sprite, color, light, minX, y, maxZ,
                sprite.getU0(), sprite.getV1());
        vertex(vertices, pose, sprite, color, light, maxX, y, maxZ,
                sprite.getU1(), sprite.getV1());
        vertex(vertices, pose, sprite, color, light, maxX, y, minZ,
                sprite.getU1(), sprite.getV0());
    }

    private static void vertex(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            int color,
            int light,
            float x,
            float y,
            float z,
            float u,
            float v) {
        vertices.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, Direction.UP.getStepX(),
                        Direction.UP.getStepY(), Direction.UP.getStepZ());
    }
}
