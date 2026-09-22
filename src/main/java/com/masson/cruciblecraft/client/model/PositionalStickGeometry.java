package com.masson.cruciblecraft.client.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.worldgen.StickShape;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;

/**
 * Bakes the coordinate-seeded 14×2 pixel rectangle used by GT6 sticks.
 */
public final class PositionalStickGeometry
        implements IUnbakedGeometry<PositionalStickGeometry> {
    public static final IGeometryLoader<PositionalStickGeometry> LOADER =
            PositionalStickGeometry::read;

    private PositionalStickGeometry() {}

    private static PositionalStickGeometry read(
            JsonObject json, JsonDeserializationContext context) {
        return new PositionalStickGeometry();
    }

    @Override
    public BakedModel bake(
            IGeometryBakingContext context,
            ModelBaker baker,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelState,
            ItemOverrides overrides) {
        TextureAtlasSprite sprite = spriteGetter.apply(context.getMaterial("stick"));
        return new Baked(
                sprite,
                context.useAmbientOcclusion(),
                context.isGui3d(),
                context.useBlockLight(),
                context.getTransforms(),
                overrides,
                modelState);
    }

    private static final class Baked implements IDynamicBakedModel {
        private static final ModelProperty<Integer> PACKED = new ModelProperty<>();

        private final IdentityHashMap<TextureAtlasSprite, List<BakedQuad>[]>
                caches = new IdentityHashMap<>();
        private final TextureAtlasSprite sprite;
        private final boolean ambientOcclusion;
        private final boolean gui3d;
        private final boolean blockLight;
        private final ItemTransforms transforms;
        private final ItemOverrides overrides;
        private final ModelState modelState;

        private Baked(
                TextureAtlasSprite sprite,
                boolean ambientOcclusion,
                boolean gui3d,
                boolean blockLight,
                ItemTransforms transforms,
                ItemOverrides overrides,
                ModelState modelState) {
            this.sprite = sprite;
            this.ambientOcclusion = ambientOcclusion;
            this.gui3d = gui3d;
            this.blockLight = blockLight;
            this.transforms = transforms;
            this.overrides = overrides;
            this.modelState = modelState;
        }

        @Override
        public ModelData getModelData(
                BlockAndTintGetter level,
                BlockPos pos,
                BlockState state,
                ModelData modelData) {
            return modelData.derive()
                    .with(PACKED, StickShape.pack(pos))
                    .build();
        }

        @Override
        public List<BakedQuad> getQuads(
                @Nullable BlockState state,
                @Nullable Direction side,
                RandomSource rand,
                ModelData extraData,
                @Nullable RenderType renderType) {
            Integer packedFromPos = extraData.get(PACKED);
            int packed = packedFromPos != null
                    ? packedFromPos
                    : state == null
                            ? StickShape.ITEM_PACKED
                            : StickShape.pack(rand);
            List<BakedQuad> baked = quads(packed, sprite);
            if (side == null) {
                return baked;
            }
            List<BakedQuad> facing = new ArrayList<>(1);
            for (BakedQuad quad : baked) {
                if (quad.getDirection() == side) {
                    facing.add(quad);
                }
            }
            return facing;
        }

        private List<BakedQuad> quads(int packed, TextureAtlasSprite used) {
            List<BakedQuad>[] rows =
                    caches.computeIfAbsent(used, ignored -> newRows());
            List<BakedQuad> baked = rows[packed];
            if (baked != null) {
                return baked;
            }
            synchronized (rows) {
                baked = rows[packed];
                if (baked == null) {
                    baked = bakePacked(packed, used);
                    rows[packed] = baked;
                }
            }
            return baked;
        }

        @SuppressWarnings("unchecked")
        private static List<BakedQuad>[] newRows() {
            return (List<BakedQuad>[]) new List<?>[StickShape.MAX_PACKED + 1];
        }

        private List<BakedQuad> bakePacked(int packed, TextureAtlasSprite used) {
            StickShape.Pixels pixels = StickShape.pixels(packed);
            Vector3f from = new Vector3f(pixels.minX(), 0.0F, pixels.minZ());
            Vector3f to = new Vector3f(
                    pixels.maxX(), pixels.maxY(), pixels.maxZ());
            Map<Direction, BlockElementFace> faces =
                    new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                faces.put(
                        direction,
                        new BlockElementFace(
                                null,
                                -1,
                                "#stick",
                                uv(direction, pixels)));
            }
            BlockElement element = new BlockElement(from, to, faces, null, true);
            List<BakedQuad> quads = new ArrayList<>(6);
            for (Direction direction : Direction.values()) {
                quads.add(UnbakedGeometryHelper.bakeElementFace(
                        element,
                        faces.get(direction),
                        used,
                        direction,
                        modelState));
            }
            return List.copyOf(quads);
        }

        private static BlockFaceUV uv(
                Direction direction, StickShape.Pixels pixels) {
            float[] uvs = switch (direction) {
                case DOWN, UP -> new float[] {
                    pixels.minX(),
                    pixels.minZ(),
                    pixels.maxX(),
                    pixels.maxZ()
                };
                case NORTH, SOUTH -> new float[] {
                    pixels.minX(),
                    16.0F - pixels.maxY(),
                    pixels.maxX(),
                    16.0F
                };
                case WEST, EAST -> new float[] {
                    pixels.minZ(),
                    16.0F - pixels.maxY(),
                    pixels.maxZ(),
                    16.0F
                };
            };
            return new BlockFaceUV(uvs, 0);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return ambientOcclusion;
        }

        @Override
        public boolean isGui3d() {
            return gui3d;
        }

        @Override
        public boolean usesBlockLight() {
            return blockLight;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return sprite;
        }

        @Override
        public ItemTransforms getTransforms() {
            return transforms;
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }
    }
}
