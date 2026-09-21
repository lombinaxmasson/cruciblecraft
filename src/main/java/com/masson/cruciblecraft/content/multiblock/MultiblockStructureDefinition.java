package com.masson.cruciblecraft.content.multiblock;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Immutable, data-pack-owned geometry for one horizontally facing multiblock. */
public record MultiblockStructureDefinition(
        int schemaVersion,
        Map<String, PalettePredicate> palette,
        List<Element> structure,
        Map<String, Offset> anchors,
        Optional<Source> source) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int MAX_SCAN_VOLUME = 4_096;

    public static final Codec<MultiblockStructureDefinition> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf(
                                    "schema_version",
                                    CURRENT_SCHEMA_VERSION)
                            .forGetter(MultiblockStructureDefinition::schemaVersion),
                    Codec.unboundedMap(Codec.STRING, PalettePredicate.CODEC)
                            .fieldOf("palette")
                            .forGetter(MultiblockStructureDefinition::palette),
                    Element.CODEC.listOf()
                            .fieldOf("structure")
                            .forGetter(MultiblockStructureDefinition::structure),
                    Codec.unboundedMap(Codec.STRING, Offset.CODEC)
                            .optionalFieldOf("anchors", Map.of())
                            .forGetter(MultiblockStructureDefinition::anchors),
                    Source.CODEC.optionalFieldOf("source")
                            .forGetter(MultiblockStructureDefinition::source))
                    .apply(instance, MultiblockStructureDefinition::new));

    public MultiblockStructureDefinition {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported multiblock schema version " + schemaVersion);
        }
        palette = Map.copyOf(new LinkedHashMap<>(palette));
        structure = List.copyOf(structure);
        anchors = Map.copyOf(new LinkedHashMap<>(anchors));
        source = Objects.requireNonNull(source, "source");
        if (palette.isEmpty() || structure.isEmpty()) {
            throw new IllegalArgumentException(
                    "A multiblock needs a non-empty palette and structure");
        }

        Set<Offset> occupied = new LinkedHashSet<>();
        int controllers = 0;
        for (Element element : structure) {
            PalettePredicate predicate = palette.get(element.predicate());
            if (predicate == null) {
                throw new IllegalArgumentException(
                        "Unknown palette key '" + element.predicate() + "'");
            }
            if (!occupied.add(element.offset())) {
                throw new IllegalArgumentException(
                        "Duplicate structure offset " + element.offset());
            }
            if (predicate.kind() == PredicateKind.CONTROLLER) {
                controllers++;
                if (!element.offset().equals(Offset.ZERO)) {
                    throw new IllegalArgumentException(
                            "Controller predicate must be at local [0,0,0]");
                }
            }
        }
        if (controllers != 1) {
            throw new IllegalArgumentException(
                    "A multiblock needs exactly one controller predicate");
        }
        if (scanVolume(structure) > MAX_SCAN_VOLUME) {
            throw new IllegalArgumentException(
                    "Multiblock scan volume exceeds " + MAX_SCAN_VOLUME);
        }
    }

    public PalettePredicate predicate(Element element) {
        return Objects.requireNonNull(
                palette.get(element.predicate()), element.predicate());
    }

    public BlockPos worldPosition(
            BlockPos controller,
            Direction facing,
            Offset offset) {
        Offset rotated = offset.rotate(facing);
        return controller.offset(rotated.x(), rotated.y(), rotated.z());
    }

    public BlockPos anchor(
            String name,
            BlockPos controller,
            Direction facing) {
        Offset offset = anchors.get(name);
        if (offset == null) {
            throw new IllegalArgumentException("Unknown multiblock anchor " + name);
        }
        return worldPosition(controller, facing, offset);
    }

    public int scanVolume() {
        return scanVolume(structure);
    }

    public long portCount(PortType type) {
        return structure.stream()
                .map(this::predicate)
                .filter(predicate -> predicate.kind() == PredicateKind.PORT)
                .filter(predicate -> predicate.port().orElseThrow() == type)
                .count();
    }

    private static int scanVolume(List<Element> elements) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (Element element : elements) {
            Offset offset = element.offset();
            minX = Math.min(minX, offset.x());
            minY = Math.min(minY, offset.y());
            minZ = Math.min(minZ, offset.z());
            maxX = Math.max(maxX, offset.x());
            maxY = Math.max(maxY, offset.y());
            maxZ = Math.max(maxZ, offset.z());
        }
        return Math.multiplyExact(
                Math.multiplyExact(maxX - minX + 1, maxY - minY + 1),
                maxZ - minZ + 1);
    }

    public record Element(Offset offset, String predicate) {
        public static final Codec<Element> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Offset.CODEC.fieldOf("offset").forGetter(Element::offset),
                        Codec.STRING.fieldOf("predicate").forGetter(Element::predicate))
                        .apply(instance, Element::new));

        public Element {
            Objects.requireNonNull(offset, "offset");
            if (predicate == null || predicate.isBlank()) {
                throw new IllegalArgumentException(
                        "Structure predicate key must not be blank");
            }
        }
    }

    public record Offset(int x, int y, int z) {
        public static final Offset ZERO = new Offset(0, 0, 0);
        public static final Codec<Offset> CODEC = Codec.INT.listOf()
                .comapFlatMap(
                        values -> values.size() == 3
                                ? DataResult.success(new Offset(
                                        values.get(0),
                                        values.get(1),
                                        values.get(2)))
                                : DataResult.error(() ->
                                        "Offset must contain exactly three integers"),
                        offset -> List.of(offset.x(), offset.y(), offset.z()));

        /**
         * JSON is authored for a north-facing controller. Positive local Z is
         * behind that controller; rotations preserve Y.
         */
        public Offset rotate(Direction facing) {
            return switch (requireHorizontal(facing)) {
                case NORTH -> this;
                case EAST -> new Offset(-z, y, x);
                case SOUTH -> new Offset(-x, y, -z);
                case WEST -> new Offset(z, y, -x);
                default -> throw new IllegalStateException("Unreachable facing");
            };
        }

        private static Direction requireHorizontal(Direction facing) {
            if (facing == null || facing.getAxis().isVertical()) {
                throw new IllegalArgumentException(
                        "Multiblock facing must be horizontal");
            }
            return facing;
        }
    }

    public record PalettePredicate(
            PredicateKind kind,
            Optional<ResourceLocation> block,
            Optional<ResourceLocation> tag,
            Optional<PortType> port,
            Optional<String> uniformGroup) {
        public static final Codec<PalettePredicate> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        PredicateKind.CODEC.fieldOf("type")
                                .forGetter(PalettePredicate::kind),
                        ResourceLocation.CODEC.optionalFieldOf("block")
                                .forGetter(PalettePredicate::block),
                        ResourceLocation.CODEC.optionalFieldOf("tag")
                                .forGetter(PalettePredicate::tag),
                        PortType.CODEC.optionalFieldOf("port")
                                .forGetter(PalettePredicate::port),
                        Codec.STRING.optionalFieldOf("uniform_group")
                                .forGetter(PalettePredicate::uniformGroup))
                        .apply(instance, PalettePredicate::new));

        public PalettePredicate(
                PredicateKind kind,
                Optional<ResourceLocation> block,
                Optional<ResourceLocation> tag,
                Optional<PortType> port) {
            this(kind, block, tag, port, Optional.empty());
        }

        public PalettePredicate {
            Objects.requireNonNull(kind, "kind");
            block = Objects.requireNonNull(block, "block");
            tag = Objects.requireNonNull(tag, "tag");
            port = Objects.requireNonNull(port, "port");
            uniformGroup = Objects.requireNonNull(uniformGroup, "uniformGroup")
                    .filter(value -> !value.isBlank());
            switch (kind) {
                case BLOCK -> require(block.isPresent()
                        && tag.isEmpty() && port.isEmpty(), "block");
                case TAG -> require(tag.isPresent()
                        && block.isEmpty() && port.isEmpty(), "tag");
                case AIR -> require(block.isEmpty()
                        && tag.isEmpty() && port.isEmpty(), "air");
                case CONTROLLER -> require(
                        (block.isPresent() ^ tag.isPresent()) && port.isEmpty(),
                        "controller");
                case PORT -> require(block.isPresent()
                        && tag.isEmpty() && port.isPresent(), "port");
            }
            if (uniformGroup.isPresent()
                    && kind != PredicateKind.BLOCK
                    && kind != PredicateKind.TAG) {
                throw new IllegalArgumentException(
                        "uniform_group is only valid on block or tag predicates");
            }
        }

        public String description() {
            return switch (kind) {
                case BLOCK -> "block " + block.orElseThrow();
                case TAG -> "tag #" + tag.orElseThrow();
                case AIR -> "air";
                case CONTROLLER -> block.isPresent()
                        ? "controller " + block.orElseThrow()
                        : "controller tag #" + tag.orElseThrow();
                case PORT -> port.orElseThrow().serializedName()
                        + " port " + block.orElseThrow();
            };
        }

        private static void require(boolean valid, String kind) {
            if (!valid) {
                throw new IllegalArgumentException(
                        "Invalid " + kind + " palette predicate fields");
            }
        }
    }

    public enum PredicateKind {
        BLOCK("block"),
        TAG("tag"),
        AIR("air"),
        CONTROLLER("controller"),
        PORT("port");

        public static final Codec<PredicateKind> CODEC =
                Codec.STRING.comapFlatMap(
                        input -> java.util.Arrays.stream(values())
                                .filter(value -> value.serializedName.equals(input))
                                .findFirst()
                                .map(DataResult::success)
                                .orElseGet(() -> DataResult.error(
                                        () -> "Unknown predicate kind '"
                                                + input + "'")),
                        PredicateKind::serializedName);

        private final String serializedName;

        PredicateKind(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }
    }

    public enum PortType {
        ITEM_FLUID("item_fluid"),
        ITEM_FLUID_IN("item_fluid_in"),
        ITEM_FLUID_OUT("item_fluid_out"),
        ENERGY_INPUT("energy_input"),
        ITEM_FLUID_ENERGY("item_fluid_energy"),
        FLUID_OUT("fluid_out");

        public static final Codec<PortType> CODEC =
                Codec.STRING.comapFlatMap(
                        input -> java.util.Arrays.stream(values())
                                .filter(value -> value.serializedName.equals(input))
                                .findFirst()
                                .map(DataResult::success)
                                .orElseGet(() -> DataResult.error(
                                        () -> "Unknown port type '"
                                                + input + "'")),
                        PortType::serializedName);

        private final String serializedName;

        PortType(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }
    }

    public record Source(
            String repository,
            String revision,
            String className,
            String method) {
        public static final Codec<Source> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.STRING.fieldOf("repository")
                                .forGetter(Source::repository),
                        Codec.STRING.fieldOf("revision")
                                .forGetter(Source::revision),
                        Codec.STRING.fieldOf("class")
                                .forGetter(Source::className),
                        Codec.STRING.fieldOf("method")
                                .forGetter(Source::method))
                        .apply(instance, Source::new));

        public Source {
            if (repository == null || repository.isBlank()
                    || revision == null || revision.isBlank()
                    || className == null || className.isBlank()
                    || method == null || method.isBlank()) {
                throw new IllegalArgumentException(
                        "Source provenance fields must not be blank");
            }
        }
    }

}
