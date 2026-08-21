package com.masson.cruciblecraft.content.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Element;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PalettePredicate;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator.Status;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator.StructureAccess;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MultiblockStructureDefinitionTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void codecAndAllFourHorizontalRotationsValidate() {
        MultiblockStructureDefinition definition = simpleDefinition();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Map<BlockPos, BlockState> states = new LinkedHashMap<>();
            states.put(BlockPos.ZERO, Blocks.STONE.defaultBlockState());
            states.put(
                    definition.worldPosition(
                            BlockPos.ZERO, facing, new Offset(1, 0, 0)),
                    Blocks.DIRT.defaultBlockState());
            states.put(
                    definition.worldPosition(
                            BlockPos.ZERO, facing, new Offset(2, 0, 0)),
                    Blocks.AIR.defaultBlockState());
            var result = MultiblockStructureValidator.validate(
                    definition,
                    access(states),
                    BlockPos.ZERO,
                    facing);
            assertEquals(Status.VALID, result.status());
        }
    }

    @Test
    void unloadedPositionsAreReportedWithoutBeingRead() {
        MultiblockStructureDefinition definition = simpleDefinition();
        var result = MultiblockStructureValidator.validate(
                definition,
                new StructureAccess() {
                    @Override
                    public boolean isLoaded(BlockPos pos) {
                        return pos.equals(BlockPos.ZERO);
                    }

                    @Override
                    public BlockState blockState(BlockPos pos) {
                        if (!pos.equals(BlockPos.ZERO)) {
                            throw new AssertionError(
                                    "Validator read an unloaded position");
                        }
                        return Blocks.STONE.defaultBlockState();
                    }

                    @Override
                    public BlockEntity blockEntity(BlockPos pos) {
                        throw new AssertionError(
                                "No port block entity should be read");
                    }
                },
                BlockPos.ZERO,
                Direction.NORTH);
        assertEquals(Status.UNLOADED, result.status());
        assertEquals(2, result.diagnostics().size());
    }

    @Test
    void diagnosticsAreBounded() {
        Map<String, PalettePredicate> palette = Map.of(
                "C", predicate(PredicateKind.CONTROLLER, "minecraft:stone"),
                "B", predicate(PredicateKind.BLOCK, "minecraft:dirt"));
        java.util.ArrayList<Element> elements = new java.util.ArrayList<>();
        elements.add(new Element(Offset.ZERO, "C"));
        for (int x = 1; x <= 32; x++) {
            elements.add(new Element(new Offset(x, 0, 0), "B"));
        }
        MultiblockStructureDefinition definition =
                new MultiblockStructureDefinition(
                        1, palette, elements, Map.of(), java.util.Optional.empty());
        var result = MultiblockStructureValidator.validate(
                definition,
                access(Map.of(BlockPos.ZERO, Blocks.STONE.defaultBlockState())),
                BlockPos.ZERO,
                Direction.NORTH);
        assertEquals(Status.INVALID, result.status());
        assertEquals(
                MultiblockStructureValidator.MAX_DIAGNOSTICS,
                result.diagnostics().size());
    }

    @Test
    void reloadPublicationRemainsAtomicWhenOneFileIsInvalid() {
        ResourceLocation validId = id("atomic_valid");
        Map<ResourceLocation, JsonElement> valid =
                Map.of(validId, JsonParser.parseString(simpleJson()));
        MultiblockStructureCatalog.replaceFromJson(valid);
        long revision = MultiblockStructureCatalog.revision();
        Map<ResourceLocation, MultiblockStructureDefinition> before =
                MultiblockStructureCatalog.all();

        Map<ResourceLocation, JsonElement> broken = new LinkedHashMap<>(valid);
        broken.put(id("broken"), JsonParser.parseString(
                "{\"schema_version\":1,\"palette\":{},\"structure\":[]}"));
        assertThrows(
                RuntimeException.class,
                () -> MultiblockStructureCatalog.replaceFromJson(broken));
        assertEquals(revision, MultiblockStructureCatalog.revision());
        assertEquals(before, MultiblockStructureCatalog.all());
    }

    @Test
    void largeCentrifugeMatchesThePinnedGt6SourceShape() {
        MultiblockStructureDefinition definition =
                resourceDefinition("large_centrifuge");
        assertEquals(18, definition.structure().size());
        assertEquals(18, definition.scanVolume());
        assertEquals(15, definition.portCount(PortType.ITEM_FLUID));
        assertEquals(2, definition.portCount(PortType.ENERGY_INPUT));
        assertEquals(
                1,
                definition.structure().stream()
                        .filter(element -> definition.predicate(element).kind()
                                == PredicateKind.CONTROLLER)
                        .count());
        var source = definition.source().orElseThrow();
        assertEquals(
                "3703e40308c8c030763fd6297dea8b210d2a77b1",
                source.revision());
        assertEquals(
                "gregtech.tileentity.multiblocks.MultiTileEntityCentrifuge",
                source.className());
        assertEquals("checkStructure2", source.method());
    }

    @Test
    void unknownSchemaVersionFailsClosed() {
        String v2 = """
                {
                  "schema_version": 2,
                  "palette": {
                    "C": {"type":"controller","block":"minecraft:stone"}
                  },
                  "structure": [
                    {"offset":[0,0,0],"predicate":"C"}
                  ]
                }
                """;
        assertThrows(
                RuntimeException.class,
                () -> MultiblockStructureDefinition.CODEC.parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString(v2)));
    }

    @Test
    void oldStructuresLoadUntouchedAtSchemaV1() {
        MultiblockStructureDefinition cokeOven =
                resourceDefinition("coke_oven");
        assertEquals(1, cokeOven.schemaVersion());
        assertEquals(27, cokeOven.structure().size());
        assertEquals(
                1,
                cokeOven.structure().stream()
                        .filter(element -> cokeOven.predicate(element).kind()
                                == PredicateKind.CONTROLLER)
                        .count());

        MultiblockStructureDefinition centrifuge =
                resourceDefinition("large_centrifuge");
        assertEquals(1, centrifuge.schemaVersion());
        assertEquals(18, centrifuge.structure().size());
    }

    @Test
    void largeCrucibleIsTwentySevenPositionsWithBottomCenterController() {
        MultiblockStructureDefinition definition =
                resourceDefinition("large_crucible");
        assertEquals(1, definition.schemaVersion());
        assertEquals(27, definition.structure().size());
        assertEquals(8, definition.portCount(PortType.ENERGY_INPUT));
        assertEquals(8, definition.portCount(PortType.ITEM_FLUID));
        assertEquals(
                1,
                definition.structure().stream()
                        .filter(element -> definition.predicate(element).kind()
                                == PredicateKind.CONTROLLER)
                        .count());
        assertEquals(
                2,
                definition.structure().stream()
                        .filter(element -> definition.predicate(element).kind()
                                == PredicateKind.AIR)
                        .count());
        assertEquals(
                Offset.ZERO,
                definition.structure().stream()
                        .filter(element -> definition.predicate(element).kind()
                                == PredicateKind.CONTROLLER)
                        .findFirst()
                        .orElseThrow()
                        .offset());
        assertEquals(
                "cruciblecraft:large_crucible",
                definition.predicate(definition.structure().getFirst())
                        .block()
                        .orElseThrow()
                        .toString());
        java.util.Set<Offset> air = definition.structure().stream()
                .filter(element -> definition.predicate(element).kind()
                        == PredicateKind.AIR)
                .map(Element::offset)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(
                java.util.Set.of(new Offset(0, 1, 0), new Offset(0, 2, 0)),
                air);
        var source = definition.source().orElseThrow();
        assertEquals(
                "3703e40308c8c030763fd6297dea8b210d2a77b1",
                source.revision());
        assertEquals(
                "gregtech.tileentity.multiblocks.MultiTileEntityCrucible",
                source.className());
    }

    @Test
    void malformedTypedPortIsRejectedByTheCodec() {
        String malformed = """
                {
                  "schema_version": 1,
                  "palette": {
                    "C": {"type":"controller","block":"minecraft:stone"},
                    "P": {"type":"port","block":"minecraft:dirt"}
                  },
                  "structure": [
                    {"offset":[0,0,0],"predicate":"C"},
                    {"offset":[1,0,0],"predicate":"P"}
                  ]
                }
                """;
        assertThrows(
                RuntimeException.class,
                () -> MultiblockStructureDefinition.CODEC.parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString(malformed)));
    }

    private static MultiblockStructureDefinition simpleDefinition() {
        return MultiblockStructureDefinition.CODEC
                .parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString(simpleJson()))
                .getOrThrow(AssertionError::new);
    }

    private static String simpleJson() {
        return """
                {
                  "schema_version": 1,
                  "palette": {
                    "C": {"type":"controller","block":"minecraft:stone"},
                    "B": {"type":"block","block":"minecraft:dirt"},
                    "A": {"type":"air"}
                  },
                  "structure": [
                    {"offset":[0,0,0],"predicate":"C"},
                    {"offset":[1,0,0],"predicate":"B"},
                    {"offset":[2,0,0],"predicate":"A"}
                  ],
                  "anchors": {"end":[2,0,0]}
                }
                """;
    }

    private static PalettePredicate predicate(
            PredicateKind kind, String block) {
        return new PalettePredicate(
                kind,
                java.util.Optional.of(ResourceLocation.parse(block)),
                java.util.Optional.empty(),
                java.util.Optional.empty());
    }

    private static StructureAccess access(Map<BlockPos, BlockState> states) {
        return new StructureAccess() {
            @Override
            public boolean isLoaded(BlockPos pos) {
                return true;
            }

            @Override
            public BlockState blockState(BlockPos pos) {
                return states.getOrDefault(
                        pos, Blocks.COBBLESTONE.defaultBlockState());
            }

            @Override
            public BlockEntity blockEntity(BlockPos pos) {
                return null;
            }
        };
    }

    private static MultiblockStructureDefinition resourceDefinition(
            String name) {
        var stream = MultiblockStructureDefinitionTest.class
                .getResourceAsStream(
                        "/data/cruciblecraft/multiblock_structures/"
                                + name + ".json");
        if (stream == null) {
            throw new AssertionError("Missing multiblock " + name);
        }
        try (var reader = new InputStreamReader(
                stream, StandardCharsets.UTF_8)) {
            return MultiblockStructureDefinition.CODEC
                    .parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                    .getOrThrow(AssertionError::new);
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", path);
    }
}
