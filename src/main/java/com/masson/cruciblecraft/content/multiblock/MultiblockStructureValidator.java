package com.masson.cruciblecraft.content.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Element;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PalettePredicate;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The only world scanner used by JSON-backed multiblock definitions. */
public final class MultiblockStructureValidator {
    public static final int MAX_DIAGNOSTICS = 16;

    private MultiblockStructureValidator() {}

    public static ValidationResult validate(
            MultiblockStructureDefinition definition,
            LevelReader level,
            BlockPos controller,
            Direction facing) {
        Objects.requireNonNull(level, "level");
        return validate(
                definition,
                new StructureAccess() {
                    @Override
                    public boolean isLoaded(BlockPos pos) {
                        return level.hasChunkAt(pos);
                    }

                    @Override
                    public BlockState blockState(BlockPos pos) {
                        return level.getBlockState(pos);
                    }

                    @Override
                    public BlockEntity blockEntity(BlockPos pos) {
                        return level.getBlockEntity(pos);
                    }
                },
                controller,
                facing);
    }

    public static ValidationResult validate(
            MultiblockStructureDefinition definition,
            StructureAccess access,
            BlockPos controller,
            Direction facing) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(controller, "controller");
        if (facing == null || facing.getAxis().isVertical()) {
            throw new IllegalArgumentException(
                    "Multiblock facing must be horizontal");
        }

        List<Diagnostic> diagnostics = new ArrayList<>();
        List<MatchedPort> ports = new ArrayList<>();
        java.util.Map<String, ResourceLocation> uniformBlocks = new java.util.LinkedHashMap<>();
        boolean unloaded = false;
        boolean invalid = false;
        for (Element element : definition.structure()) {
            BlockPos target = definition.worldPosition(
                    controller, facing, element.offset());
            PalettePredicate predicate = definition.predicate(element);
            if (!access.isLoaded(target)) {
                unloaded = true;
                addDiagnostic(
                        diagnostics,
                        new Diagnostic(
                                target,
                                predicate.description(),
                                "unloaded chunk"));
                continue;
            }

            BlockState state = access.blockState(target);
            BlockEntity blockEntity = predicate.kind()
                            == MultiblockStructureDefinition.PredicateKind.PORT
                    ? access.blockEntity(target)
                    : null;
            if (!matches(predicate, state, blockEntity, target, controller)) {
                invalid = true;
                addDiagnostic(
                        diagnostics,
                        new Diagnostic(
                                target,
                                predicate.description(),
                                actual(state, blockEntity)));
                continue;
            }
            if (predicate.kind()
                    == MultiblockStructureDefinition.PredicateKind.PORT) {
                ports.add(new MatchedPort(
                        target,
                        predicate.port().orElseThrow()));
            }
            if (predicate.uniformGroup().isPresent()) {
                String group = predicate.uniformGroup().orElseThrow();
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                ResourceLocation first = uniformBlocks.putIfAbsent(group, id);
                if (first != null && !first.equals(id)) {
                    invalid = true;
                    addDiagnostic(
                            diagnostics,
                            new Diagnostic(
                                    target,
                                    predicate.description(),
                                    "mixed " + group + " (" + first + " vs " + id + ")"));
                }
            }
        }

        Status status = unloaded
                ? Status.UNLOADED
                : invalid ? Status.INVALID : Status.VALID;
        return new ValidationResult(status, diagnostics, ports);
    }

    /**
     * Shared predicate evaluation for validation and builder planning.
     * Callers must pass the block entity when the predicate is a port.
     */
    public static boolean matches(
            PalettePredicate predicate,
            BlockState state,
            BlockEntity blockEntity,
            BlockPos target,
            BlockPos controller) {
        return switch (predicate.kind()) {
            case AIR -> state.isAir();
            case BLOCK -> BuiltInRegistries.BLOCK
                    .getOptional(predicate.block().orElseThrow())
                    .map(state::is)
                    .orElse(false);
            case TAG -> state.is(TagKey.create(
                    Registries.BLOCK, predicate.tag().orElseThrow()));
            case CONTROLLER -> target.equals(controller)
                    && (predicate.block().isPresent()
                            ? BuiltInRegistries.BLOCK
                                    .getOptional(predicate.block().orElseThrow())
                                    .map(state::is)
                                    .orElse(false)
                            : state.is(TagKey.create(
                                    Registries.BLOCK,
                                    predicate.tag().orElseThrow())));
            case PORT -> BuiltInRegistries.BLOCK
                            .getOptional(predicate.block().orElseThrow())
                            .map(state::is)
                            .orElse(false)
                    && blockEntity instanceof MultiblockPort port
                    && port.accepts(predicate.port().orElseThrow());
        };
    }

    private static String actual(
            BlockState state,
            BlockEntity blockEntity) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(
                state.getBlock());
        return blockEntity instanceof MultiblockPort port
                ? blockId + " (" + port.portType().serializedName() + " port)"
                : blockId.toString();
    }

    private static void addDiagnostic(
            List<Diagnostic> diagnostics,
            Diagnostic diagnostic) {
        if (diagnostics.size() < MAX_DIAGNOSTICS) {
            diagnostics.add(diagnostic);
        }
    }

    public enum Status {
        VALID,
        INVALID,
        UNLOADED
    }

    public record ValidationResult(
            Status status,
            List<Diagnostic> diagnostics,
            List<MatchedPort> ports) {
        public ValidationResult {
            Objects.requireNonNull(status, "status");
            diagnostics = List.copyOf(diagnostics);
            ports = List.copyOf(ports);
            if (diagnostics.size() > MAX_DIAGNOSTICS) {
                throw new IllegalArgumentException(
                        "Validation diagnostics are not bounded");
            }
        }

        public boolean valid() {
            return status == Status.VALID;
        }
    }

    public record Diagnostic(
            BlockPos position,
            String expected,
            String actual) {
        public Diagnostic {
            position = position.immutable();
            Objects.requireNonNull(expected, "expected");
            Objects.requireNonNull(actual, "actual");
        }
    }

    public record MatchedPort(BlockPos position, PortType type) {
        public MatchedPort {
            position = position.immutable();
            Objects.requireNonNull(type, "type");
        }
    }

    public interface StructureAccess {
        boolean isLoaded(BlockPos pos);

        BlockState blockState(BlockPos pos);

        BlockEntity blockEntity(BlockPos pos);
    }
}
