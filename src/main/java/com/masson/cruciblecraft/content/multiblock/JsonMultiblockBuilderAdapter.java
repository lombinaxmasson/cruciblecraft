package com.masson.cruciblecraft.content.multiblock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Element;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PalettePredicate;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Builder adapter for data-pack-owned structure definitions.
 *
 * <p>Resolution is definition-driven: a controller or any matching element
 * can identify a target. No registry-name prefix or Java class-name guessing
 * is used.</p>
 */
public final class JsonMultiblockBuilderAdapter
        implements MultiblockBuilderAdapter {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "json");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Optional<MultiblockBuilderTarget> resolve(
            Level level,
            BlockPos clicked) {
        if (level == null || clicked == null || !level.hasChunkAt(clicked)) {
            return Optional.empty();
        }

        Optional<MultiblockBuilderTarget> bound =
                resolveBound(level, clicked);
        if (bound.isPresent()) {
            return bound;
        }

        BlockState clickedState = level.getBlockState(clicked);
        for (Map.Entry<ResourceLocation, MultiblockStructureDefinition> entry
                : MultiblockStructureCatalog.all().entrySet()) {
            MultiblockStructureDefinition definition = entry.getValue();
            if (controllerMatches(
                    definition,
                    level,
                    clicked,
                    clickedState)) {
                return target(
                        entry.getKey(),
                        definition,
                        level,
                        clicked,
                        clicked);
            }
        }

        /*
         * A casing/port may be clicked before the controller has formed and
         * before a persistent partial claim exists. Search only positions
         * implied by the explicit definition and six face rotations.
         */
        for (Map.Entry<ResourceLocation, MultiblockStructureDefinition> entry
                : MultiblockStructureCatalog.all().entrySet()) {
            MultiblockStructureDefinition definition = entry.getValue();
            for (Direction facing : allDirections()) {
                for (Element element : definition.structure()) {
                    BlockPos controller = clicked.subtract(
                            definition.worldPosition(
                                    BlockPos.ZERO,
                                    facing,
                                    element.offset()));
                    if (!level.hasChunkAt(controller)
                            || !controllerMatches(
                                    definition,
                                    level,
                                    controller)) {
                        continue;
                    }
                    BlockPos expected = definition.worldPosition(
                            controller,
                            facing,
                            element.offset());
                    if (expected.equals(clicked)
                            && matchesElement(
                                    definition,
                                    element,
                                    level,
                                    clicked,
                                    controller)) {
                        return target(
                                entry.getKey(),
                                definition,
                                level,
                                clicked,
                                controller);
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public MultiblockBuildPlan plan(
            Level level,
            MultiblockBuilderTarget target) {
        MultiblockStructureDefinition definition =
                MultiblockStructureCatalog.require(target.structureId());
        Map<String, Block> uniform = uniformBlocks(
                level,
                definition,
                target);
        java.util.ArrayList<MultiblockBuildCell> cells =
                new java.util.ArrayList<>();
        for (Element element : definition.structure()) {
            BlockPos position = definition.worldPosition(
                    target.controller(),
                    target.facing(),
                    element.offset());
            PalettePredicate predicate = definition.predicate(element);
            Predicate<BlockState> stateMatcher = state -> {
                net.minecraft.world.level.block.entity.BlockEntity blockEntity =
                        predicate.kind() == PredicateKind.PORT
                                ? level.getBlockEntity(position)
                                : null;
                boolean matches = MultiblockStructureValidator.matches(
                        predicate,
                        state,
                        blockEntity,
                        position,
                        target.controller());
                Block uniformBlock = uniform.get(
                                predicate.uniformGroup().orElse(""));
                return matches
                                && (uniformBlock == null
                                        || predicate.kind() != PredicateKind.TAG
                                        || state.getBlock() == uniformBlock);
            };
            Predicate<ItemStack> itemMatcher = itemMatcher(
                    predicate,
                    uniform.get(predicate.uniformGroup().orElse("")));
            boolean placeable = predicate.kind() == PredicateKind.BLOCK
                    || predicate.kind() == PredicateKind.TAG
                    || predicate.kind() == PredicateKind.PORT;
            cells.add(new MultiblockBuildCell(
                    position,
                    stateMatcher,
                    itemMatcher,
                    placeable,
                    predicate.description(),
                    predicate.uniformGroup()));
        }
        return new MultiblockBuildPlan(target, cells);
    }

    @Override
    public void recheck(
            Level level,
            MultiblockBuilderTarget target) {
        if (level != null
                && !level.isClientSide
                && level.getBlockEntity(target.controller())
                        instanceof MultiblockBuilderRecheckable recheckable) {
            recheckable.requestBuilderRecheck();
        }
        MultiblockBuilderAdapter.super.recheck(level, target);
    }

    private Optional<MultiblockBuilderTarget> resolveBound(
            Level level,
            BlockPos clicked) {
        if (!(level.getBlockEntity(clicked)
                instanceof MultiblockPort port)) {
            if (level.getBlockEntity(clicked)
                    instanceof MultiblockControllerBinding binding) {
                ResourceLocation id = binding.structureId();
                if (MultiblockStructureCatalog.find(id).isPresent()) {
                    return target(
                            id,
                            MultiblockStructureCatalog.require(id),
                            level,
                            clicked,
                            clicked);
                }
            }
            return Optional.empty();
        }
        if (port.controllerPosition().isEmpty()
                || port.structureId().isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation id = port.structureId().orElseThrow();
        Optional<MultiblockStructureDefinition> definition =
                MultiblockStructureCatalog.find(id);
        BlockPos controller = port.controllerPosition().orElseThrow();
        return definition.isEmpty() || !level.hasChunkAt(controller)
                ? Optional.empty()
                : target(
                        id,
                        definition.orElseThrow(),
                        level,
                        clicked,
                        controller);
    }

    private Optional<MultiblockBuilderTarget> target(
            ResourceLocation id,
            MultiblockStructureDefinition definition,
            Level level,
            BlockPos clicked,
            BlockPos controller) {
        Direction facing = facing(level.getBlockState(controller));
        return facing == null
                ? Optional.empty()
                : Optional.of(new MultiblockBuilderTarget(
                        this,
                        id,
                        controller,
                        facing,
                        clicked));
    }

    private static boolean controllerMatches(
            MultiblockStructureDefinition definition,
            Level level,
            BlockPos controller) {
        return controllerMatches(
                definition,
                level,
                controller,
                level.getBlockState(controller));
    }

    private static boolean controllerMatches(
            MultiblockStructureDefinition definition,
            Level level,
            BlockPos controller,
            BlockState state) {
        return definition.structure().stream()
                .filter(element -> element.offset().equals(
                        MultiblockStructureDefinition.Offset.ZERO))
                .map(definition::predicate)
                .anyMatch(predicate -> predicate.kind() == PredicateKind.CONTROLLER
                        && MultiblockStructureValidator.matches(
                                predicate,
                                state,
                                level.getBlockEntity(controller),
                                controller,
                                controller));
    }

    private static boolean matchesElement(
            MultiblockStructureDefinition definition,
            Element element,
            Level level,
            BlockPos position,
            BlockPos controller) {
        PalettePredicate predicate = definition.predicate(element);
        return MultiblockStructureValidator.matches(
                predicate,
                level.getBlockState(position),
                predicate.kind() == PredicateKind.PORT
                        ? level.getBlockEntity(position)
                        : null,
                position,
                controller);
    }

    private static Direction facing(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof DirectionProperty directionProperty
                    && property.getName().equals("facing")) {
                Direction direction = state.getValue(directionProperty);
                return direction;
            }
        }
        return Direction.NORTH;
    }

    private static Map<String, Block> uniformBlocks(
            Level level,
            MultiblockStructureDefinition definition,
            MultiblockBuilderTarget target) {
        Map<String, Block> result = new LinkedHashMap<>();
        for (Element element : definition.structure()) {
            PalettePredicate predicate = definition.predicate(element);
            if (predicate.uniformGroup().isEmpty()) {
                continue;
            }
            BlockPos position = definition.worldPosition(
                    target.controller(),
                    target.facing(),
                    element.offset());
            BlockState state = level.getBlockState(position);
            if ((predicate.kind() == PredicateKind.BLOCK
                    || predicate.kind() == PredicateKind.TAG
                    || predicate.kind() == PredicateKind.PORT)
                    && MultiblockStructureValidator.matches(
                            predicate,
                            state,
                            predicate.kind() == PredicateKind.PORT
                                    ? level.getBlockEntity(position)
                                    : null,
                            position,
                            target.controller())) {
                result.putIfAbsent(
                        predicate.uniformGroup().orElseThrow(),
                        state.getBlock());
            }
        }
        return result;
    }

    private static Predicate<ItemStack> itemMatcher(
            PalettePredicate predicate,
            Block uniformBlock) {
        if ((predicate.kind() == PredicateKind.BLOCK
                || predicate.kind() == PredicateKind.PORT)
                && predicate.block().isPresent()) {
            Block expected = BuiltInRegistries.BLOCK.getOptional(
                            predicate.block().orElseThrow())
                    .orElse(null);
            return stack -> isBlockItem(stack, expected);
        }
        if (predicate.kind() != PredicateKind.TAG
                && !(predicate.kind() == PredicateKind.PORT
                        && predicate.tag().isPresent())) {
            return stack -> false;
        }
        TagKey<Block> tag = TagKey.create(
                Registries.BLOCK,
                predicate.tag().orElseThrow());
        return stack -> {
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                return false;
            }
            Block block = blockItem.getBlock();
            return (uniformBlock == null || uniformBlock == block)
                    && block.defaultBlockState().is(tag);
        };
    }

    private static boolean isBlockItem(ItemStack stack, Block expected) {
        return expected != null
                && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() == expected;
    }

    private static Direction[] allDirections() {
        return new Direction[] {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.UP,
            Direction.DOWN
        };
    }
}
