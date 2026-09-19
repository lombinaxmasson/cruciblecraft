package com.masson.cruciblecraft.client.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.joml.Vector3f;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.content.blockentity.CeramicMoldBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FoundryCastingBlockEntity;
import com.masson.cruciblecraft.content.mold.MoldRecipes;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
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

import org.jetbrains.annotations.Nullable;

/**
 * GT6 {@code MultiTileEntityMold} inner 5×5: unchiseled cells stay as a sheet;
 * a set bit hides that cell and exposes the neighbouring walls. Shared by
 * ceramic molds and foundry metal/stone molds.
 */
public final class CeramicMoldGeometry
        implements IUnbakedGeometry<CeramicMoldGeometry> {
    public static final IGeometryLoader<CeramicMoldGeometry> LOADER =
            CeramicMoldGeometry::read;
    public static final ModelProperty<Integer> PATTERN = new ModelProperty<>();

    private final List<BlockElement> frame;
    private final int defaultPattern;

    private CeramicMoldGeometry(List<BlockElement> frame, int defaultPattern) {
        this.frame = List.copyOf(frame);
        this.defaultPattern = defaultPattern;
    }

    private static CeramicMoldGeometry read(
            JsonObject json,
            JsonDeserializationContext context) {
        List<BlockElement> frame = new ArrayList<>();
        if (json.has("elements")) {
            for (JsonElement element : json.getAsJsonArray("elements")) {
                frame.add(context.deserialize(element, BlockElement.class));
            }
        }
        return new CeramicMoldGeometry(
                frame,
                GsonHelper.getAsInt(json, "default_pattern", 0));
    }

    @Override
    public BakedModel bake(
            IGeometryBakingContext context,
            ModelBaker baker,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelState,
            ItemOverrides overrides) {
        TextureAtlasSprite body = spriteGetter.apply(context.getMaterial("body"));
        List<BakedQuad> frameQuads = bakeElements(
                staticFrame(frame), context, spriteGetter, modelState);
        @SuppressWarnings("unchecked")
        List<BakedQuad>[] cellFaces =
                (List<BakedQuad>[]) new List<?>[MoldRecipes.CELL_COUNT];
        for (int index = 0; index < MoldRecipes.CELL_COUNT; index++) {
            cellFaces[index] = bakeCell(index, body, modelState);
        }
        return new Baked(
                body,
                frameQuads,
                cellFaces,
                defaultPattern,
                context.useAmbientOcclusion(),
                context.isGui3d(),
                context.useBlockLight(),
                context.getTransforms(),
                overrides);
    }

    /**
     * GT6 pass 0 is the inner sheet, and {@code getTexture2} only draws it
     * when molten. Baking that clay slab into the empty frame covers the 5×5
     * cells, so a chisel bit has nothing visible to punch through. Foundry
     * metal molds bake the same 5×5 as static cubes; drop those too so the
     * loader can punch holes.
     */
    static List<BlockElement> staticFrame(List<BlockElement> elements) {
        List<BlockElement> source = elements;
        if (!elements.isEmpty()
                && isGt6Pass0(elements.get(0))
                && !isMoltenSheet(elements.get(0))) {
            source = elements.subList(1, elements.size());
        }
        List<BlockElement> kept = new ArrayList<>(source.size());
        for (BlockElement element : source) {
            if (!isBakedInnerCell(element)) {
                kept.add(element);
            }
        }
        return List.copyOf(kept);
    }

    static boolean isGt6Pass0(BlockElement element) {
        return element.from.x() >= 0.99F
                && element.from.x() <= 1.01F
                && element.from.y() >= 0.99F
                && element.from.y() <= 1.01F
                && element.from.z() >= 0.99F
                && element.from.z() <= 1.01F
                && element.to.x() >= 14.99F
                && element.to.x() <= 15.01F
                && element.to.z() >= 14.99F
                && element.to.z() <= 15.01F;
    }

    /**
     * GT6 empty-state ISBRH inner 5×5: {@code 2.4} wide cubes from y 0–3
     * inside {@code 2..14}. Foundry walls and the full-block floor stay.
     */
    static boolean isBakedInnerCell(BlockElement element) {
        return element.from.y() <= 0.01F
                && element.to.y() >= 2.99F
                && element.to.y() <= 3.01F
                && element.from.x() >= 1.99F
                && element.from.z() >= 1.99F
                && element.to.x() <= 14.01F
                && element.to.z() <= 14.01F
                && element.to.x() - element.from.x() <= 2.5F
                && element.to.z() - element.from.z() <= 2.5F;
    }

    static boolean isMoltenSheet(BlockElement element) {
        BlockElementFace up = element.faces.get(Direction.UP);
        if (up == null) {
            return false;
        }
        String texture = up.texture();
        return texture != null && texture.contains("molten");
    }

    private static List<BakedQuad> bakeElements(
            List<BlockElement> elements,
            IGeometryBakingContext context,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelState) {
        List<BakedQuad> quads = new ArrayList<>();
        for (BlockElement element : elements) {
            for (Map.Entry<Direction, BlockElementFace> entry
                    : element.faces.entrySet()) {
                BlockElementFace face = entry.getValue();
                String texture = face.texture();
                if (!texture.isEmpty() && texture.charAt(0) == '#') {
                    texture = texture.substring(1);
                }
                TextureAtlasSprite sprite = spriteGetter.apply(
                        context.getMaterial(texture));
                quads.add(UnbakedGeometryHelper.bakeElementFace(
                        element,
                        face,
                        sprite,
                        entry.getKey(),
                        modelState));
            }
        }
        return List.copyOf(quads);
    }

    private static List<BakedQuad> bakeCell(
            int index,
            TextureAtlasSprite sprite,
            ModelState modelState) {
        int x = index / 5;
        int z = index % 5;
        float fromX = cellEdge(x);
        float fromZ = cellEdge(z);
        float toX = cellEdge(x + 1);
        float toZ = cellEdge(z + 1);
        Vector3f from = new Vector3f(fromX, 0.0F, fromZ);
        Vector3f to = new Vector3f(toX, 3.0F, toZ);
        Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            faces.put(
                    direction,
                    new BlockElementFace(
                            null,
                            0,
                            "#body",
                            cellUv(direction, fromX, fromZ, toX, toZ)));
        }
        BlockElement element = new BlockElement(from, to, faces, null, true);
        List<BakedQuad> quads = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            quads.add(UnbakedGeometryHelper.bakeElementFace(
                    element,
                    faces.get(direction),
                    sprite,
                    direction,
                    modelState));
        }
        return List.copyOf(quads);
    }

    private static float cellEdge(int axisIndex) {
        return (MoldRecipes.INNER_MIN
                + axisIndex * MoldRecipes.INNER_SPAN / 5.0F)
                * 16.0F;
    }

    private static BlockFaceUV cellUv(
            Direction direction,
            float fromX,
            float fromZ,
            float toX,
            float toZ) {
        float[] uvs = switch (direction) {
            case DOWN, UP -> new float[] {fromX, fromZ, toX, toZ};
            case NORTH, SOUTH -> new float[] {fromX, 13.0F, toX, 16.0F};
            case WEST, EAST -> new float[] {fromZ, 13.0F, toZ, 16.0F};
        };
        return new BlockFaceUV(uvs, 0);
    }

    private static final class Baked implements IDynamicBakedModel {
        private final TextureAtlasSprite particle;
        private final List<BakedQuad> frame;
        private final List<BakedQuad>[] cellFaces;
        private final int defaultPattern;
        private final boolean ambientOcclusion;
        private final boolean gui3d;
        private final boolean blockLight;
        private final ItemTransforms transforms;
        private final ItemOverrides overrides;

        private Baked(
                TextureAtlasSprite particle,
                List<BakedQuad> frame,
                List<BakedQuad>[] cellFaces,
                int defaultPattern,
                boolean ambientOcclusion,
                boolean gui3d,
                boolean blockLight,
                ItemTransforms transforms,
                ItemOverrides overrides) {
            this.particle = particle;
            this.frame = frame;
            this.cellFaces = cellFaces;
            this.defaultPattern = defaultPattern;
            this.ambientOcclusion = ambientOcclusion;
            this.gui3d = gui3d;
            this.blockLight = blockLight;
            this.transforms = transforms;
            this.overrides = overrides;
        }

        @Override
        public ModelData getModelData(
                BlockAndTintGetter level,
                BlockPos pos,
                BlockState state,
                ModelData modelData) {
            if (level.getBlockEntity(pos) instanceof CeramicMoldBlockEntity mold) {
                return modelData.derive().with(PATTERN, mold.pattern()).build();
            }
            if (level.getBlockEntity(pos) instanceof FoundryCastingBlockEntity mold) {
                return modelData.derive().with(PATTERN, mold.pattern()).build();
            }
            return modelData;
        }

        @Override
        public List<BakedQuad> getQuads(
                @Nullable BlockState state,
                @Nullable Direction side,
                RandomSource rand,
                ModelData extraData,
                @Nullable RenderType renderType) {
            Integer stored = extraData.get(PATTERN);
            int pattern = stored != null ? stored : defaultPattern;
            List<BakedQuad> quads = new ArrayList<>(frame.size() + 32);
            appendFacing(quads, frame, side);
            for (int index = 0; index < MoldRecipes.CELL_COUNT; index++) {
                for (BakedQuad quad : cellFaces[index]) {
                    if (!MoldRecipes.cellFaceVisible(
                            pattern, index, quad.getDirection())) {
                        continue;
                    }
                    if (side == null || quad.getDirection() == side) {
                        quads.add(quad);
                    }
                }
            }
            return quads;
        }

        private static void appendFacing(
                List<BakedQuad> destination,
                List<BakedQuad> source,
                @Nullable Direction side) {
            if (side == null) {
                destination.addAll(source);
                return;
            }
            for (BakedQuad quad : source) {
                if (quad.getDirection() == side) {
                    destination.add(quad);
                }
            }
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
            return particle;
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
