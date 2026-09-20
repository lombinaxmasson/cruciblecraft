package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.client.color.StorageArtColor;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code MultiTileEntityRendererChest}: 64x64 metalchest/woodchest/lootchest
 * colored+plain TESR, not a cube_all.
 */
public final class GtChestRenderer {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, "gt6_chest"),
            "main");
    /**
     * GT6 {@code COMPASS_FROM_SIDE}: D,U,N,S,W,E. {@code * 90 - 180} is the
     * TESR yaw; vanilla {@code toYRot() - 180} is 180° off this table.
     */
    private static final int[] COMPASS_FROM_SIDE = {0, 0, 0, 2, 3, 1, 0, 0};
    /**
     * GT6 {@code ITEM_CHEST_FACING} is NORTH so the latch faces the camera.
     */
    private static final Direction ITEM_FACING = Direction.NORTH;
    private static ItemRenderer itemRendererInstance;

    private GtChestRenderer() {}

    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRendererInstance == null) {
            itemRendererInstance = new ItemRenderer(
                    new Model(Minecraft.getInstance().getEntityModels().bakeLayer(LAYER)));
        }
        return itemRendererInstance;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                "lid",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -5.0F, -14.0F, 14.0F, 5.0F, 14.0F),
                PartPose.offset(1.0F, 7.0F, 15.0F));
        root.addOrReplaceChild(
                "knob",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.0F, -15.0F, 2.0F, 4.0F, 1.0F),
                PartPose.offset(8.0F, 7.0F, 15.0F));
        root.addOrReplaceChild(
                "bottom",
                CubeListBuilder.create().texOffs(0, 19).addBox(0.0F, 0.0F, 0.0F, 14.0F, 10.0F, 14.0F),
                PartPose.offset(1.0F, 6.0F, 1.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static final class Model {
        private final ModelPart lid;
        private final ModelPart knob;
        private final ModelPart bottom;

        public Model(ModelPart root) {
            this.lid = root.getChild("lid");
            this.knob = root.getChild("knob");
            this.bottom = root.getChild("bottom");
        }

        void render(
                PoseStack poseStack,
                VertexConsumer consumer,
                int packedLight,
                int packedOverlay,
                int color,
                float lidAngle) {
            lid.xRot = lidAngle;
            knob.xRot = lidAngle;
            lid.render(poseStack, consumer, packedLight, packedOverlay, color);
            knob.render(poseStack, consumer, packedLight, packedOverlay, color);
            bottom.render(poseStack, consumer, packedLight, packedOverlay, color);
        }
    }

    static String textureSet(MteInPlaceSpec spec) {
        String path = spec.registryPath();
        if (path.contains("reinforced_wooden")) {
            return "woodchest";
        }
        if (path.contains("stone_chest")) {
            return "lootchest";
        }
        return "metalchest";
    }

    static ResourceLocation texture(String set, String layer) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID,
                "textures/entity/gt6_import/chest/" + set + "_" + layer + ".png");
    }

    public static void render(
            Model model,
            MteInPlaceSpec spec,
            Direction facing,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        render(model, spec, facing, poseStack, buffers, packedLight, packedOverlay, 0.0F);
    }

    public static void render(
            Model model,
            MteInPlaceSpec spec,
            Direction facing,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay,
            float openness) {
        int color = 0xFF000000 | (StorageArtColor.materialColor(spec) & 0xFFFFFF);
        String set = textureSet(spec);
        // GT6 MultiTileEntityRendererChest: 1 - o, cubic ease, -PI/2.
        float closed = 1.0F - openness;
        float lidAngle = -((1.0F - closed * closed * closed) * (float) Math.PI) / 2.0F;
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 1.0F);
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotation(facing)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        model.render(
                poseStack,
                buffers.getBuffer(RenderType.entityCutout(texture(set, "colored"))),
                packedLight,
                packedOverlay,
                color,
                lidAngle);
        model.render(
                poseStack,
                buffers.getBuffer(RenderType.entityCutout(texture(set, "plain"))),
                packedLight,
                packedOverlay,
                0xFFFFFFFF,
                lidAngle);
        poseStack.popPose();
    }

    public static void renderHost(
            Model model,
            BlockEntity host,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay,
            float partialTick) {
        BlockState state = host.getBlockState();
        if (!(state.getBlock() instanceof MteInPlaceBlock block)
                || block.spec().kind() != MteInPlaceKind.CHEST) {
            return;
        }
        float openness = host instanceof MteInPlaceBlockEntity chest
                ? chest.lidOpenness(partialTick)
                : 0.0F;
        render(
                model,
                block.spec(),
                state.getValue(MteInPlaceBlock.FACING),
                poseStack,
                buffers,
                packedLight,
                packedOverlay,
                openness);
    }

    private static float yRotation(Direction facing) {
        int side = facing.get3DDataValue();
        if (side < 0 || side >= COMPASS_FROM_SIDE.length) {
            side = Direction.NORTH.get3DDataValue();
        }
        return COMPASS_FROM_SIDE[side] * 90.0F - 180.0F;
    }

    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        private final Model model;

        public ItemRenderer(BlockEntityRendererProvider.Context context) {
            super(
                    Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    context.getModelSet());
            this.model = new Model(context.bakeLayer(LAYER));
        }

        public ItemRenderer(Model model) {
            super(
                    Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels());
            this.model = model;
        }

        @Override
        public void renderByItem(
                ItemStack stack,
                ItemDisplayContext displayContext,
                PoseStack poseStack,
                MultiBufferSource buffers,
                int packedLight,
                int packedOverlay) {
            if (!(Block.byItem(stack.getItem()) instanceof MteInPlaceBlock block)
                    || block.spec().kind() != MteInPlaceKind.CHEST) {
                return;
            }
            render(
                    model,
                    block.spec(),
                    ITEM_FACING,
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay);
        }
    }
}
