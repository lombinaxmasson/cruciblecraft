package com.masson.cruciblecraft.content.multiblock;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.FusionReactorBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.energy.bedrockdrill.BedrockDrillStructure;
import com.masson.cruciblecraft.energy.largegasturbine.LargeGasTurbineCatalog;
import com.masson.cruciblecraft.energy.largedynamo.LargeDynamoCatalog;
import com.masson.cruciblecraft.energy.largedynamo.LargeDynamoStructure;
import com.masson.cruciblecraft.energy.largeheatexchanger.LargeHeatExchangerCatalog;
import com.masson.cruciblecraft.energy.largeheatexchanger.LargeHeatExchangerStructure;
import com.masson.cruciblecraft.energy.lightningrod.LightningRodStructure;
import com.masson.cruciblecraft.energy.steam.SteamTurbineCatalog;
import com.masson.cruciblecraft.energy.steam.SteamTurbineStructure;
import com.masson.cruciblecraft.energy.vondagraagg.VonDaGraaggStructure;
import com.masson.cruciblecraft.fusion.FusionStructure;
import com.masson.cruciblecraft.logistics.core.LogisticsCoreGeometry;
import com.masson.cruciblecraft.logistics.core.LogisticsCoreStructure;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Explicit adapters for structures whose source geometry remains Java-defined.
 *
 * <p>These adapters intentionally share the production check classes'
 * coordinate helpers. They never place environment resources such as bedrock,
 * air cavities, or an unbounded lightning-rod column.</p>
 */
public final class SpecialMultiblockBuilderAdapter
        implements MultiblockBuilderAdapter {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "java_special");

    private interface Descriptor {
        ResourceLocation id();

        boolean isController(Level level, BlockPos position);

        List<Direction> candidateFacings();

        List<MultiblockBuildCell> cells(
                Level level,
                MultiblockBuilderTarget target);

        default Direction facing(
                Level level,
                BlockPos controller,
                Direction candidate) {
            return stateFacing(level.getBlockState(controller), candidate);
        }

        default void recheck(
                Level level,
                MultiblockBuilderTarget target) {
            if (level != null && !level.isClientSide) {
                level.updateNeighborsAt(
                        target.controller(),
                        level.getBlockState(target.controller()).getBlock());
            }
        }
    }

    private final List<Descriptor> descriptors = List.of(
            matterFabricator(),
            fusion(),
            logisticsCore(),
            bedrockDrill(),
            lightningRod(),
            vonDaGraagg(),
            largeHeatExchanger(),
            largeDynamo(),
            steamTurbine(),
            gasTurbine());

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
        for (Descriptor descriptor : descriptors) {
            Optional<MultiblockBuilderTarget> target =
                    resolve(descriptor, level, clicked);
            if (target.isPresent()) {
                return target;
            }
        }
        return Optional.empty();
    }

    @Override
    public MultiblockBuildPlan plan(
            Level level,
            MultiblockBuilderTarget target) {
        Descriptor descriptor = descriptor(target.structureId());
        return new MultiblockBuildPlan(
                target,
                descriptor.cells(level, target));
    }

    @Override
    public void recheck(
            Level level,
            MultiblockBuilderTarget target) {
        descriptor(target.structureId()).recheck(level, target);
    }

    private Optional<MultiblockBuilderTarget> resolve(
            Descriptor descriptor,
            Level level,
            BlockPos clicked) {
        if (descriptor.isController(level, clicked)) {
            Direction facing = descriptor.facing(
                    level,
                    clicked,
                    Direction.NORTH);
            return Optional.of(new MultiblockBuilderTarget(
                    this,
                    descriptor.id(),
                    clicked,
                    facing,
                    clicked));
        }

        for (Direction candidate : descriptor.candidateFacings()) {
            MultiblockBuilderTarget origin = new MultiblockBuilderTarget(
                    this,
                    descriptor.id(),
                    BlockPos.ZERO,
                    candidate,
                    BlockPos.ZERO);
            List<BlockPos> offsets = descriptor.cells(level, origin).stream()
                    .map(MultiblockBuildCell::position)
                    .distinct()
                    .toList();
            for (BlockPos offset : offsets) {
                BlockPos controller = clicked.subtract(offset);
                if (!level.hasChunkAt(controller)
                        || !descriptor.isController(level, controller)) {
                    continue;
                }
                Direction facing = descriptor.facing(
                        level,
                        controller,
                        candidate);
                if (facing != candidate
                        && !facing.equals(candidate)) {
                    continue;
                }
                MultiblockBuilderTarget target = new MultiblockBuilderTarget(
                        this,
                        descriptor.id(),
                        controller,
                        facing,
                        clicked);
                boolean matches = descriptor.cells(level, target).stream()
                        .filter(cell -> cell.position().equals(clicked))
                        .anyMatch(cell -> cell.matches(
                                level.getBlockState(clicked)));
                if (matches) {
                    return Optional.of(target);
                }
            }
        }
        return Optional.empty();
    }

    private Descriptor descriptor(ResourceLocation id) {
        return descriptors.stream()
                .filter(descriptor -> descriptor.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown special builder structure " + id));
    }

    private static Descriptor matterFabricator() {
        ResourceLocation id = CoilHosts.id("large_matter_fabricator");
        return new Descriptor() {
            @Override public ResourceLocation id() { return id; }

            @Override
            public boolean isController(Level level, BlockPos position) {
                return mteKind(level, position, MteInPlaceKind.MATTER_FABRICATOR)
                        && stateFacing(
                                level.getBlockState(position),
                                Direction.NORTH)
                                .getAxis()
                                .isHorizontal();
            }

            @Override
            public List<Direction> candidateFacings() {
                return horizontal();
            }

            @Override
            public List<MultiblockBuildCell> cells(
                    Level level,
                    MultiblockBuilderTarget target) {
                Block lead = CoilHosts.block(CoilHosts.DENSE_LEAD);
                Block coil = CoilHosts.block(CoilHosts.OSMIUM);
                Block vent = ModBlocks.VENTILATION_UNIT.get();
                Block versatile = ModBlocks.VERSATILE_PROCESSOR_UNIT.get();
                Block control = ModBlocks.CONTROL_PROCESSOR_UNIT.get();
                Block conversion = ModBlocks.CONVERSION_PROCESSOR_UNIT.get();
                BlockPos origin = MatterFabricatorStructure.origin(
                        target.controller(),
                        target.facing());
                Map<BlockPos, MultiblockBuildCell> cells =
                        new LinkedHashMap<>();
                int controlPresent = 0;
                int conversionPresent = 0;
                for (int x = 1; x <= 3; x++) {
                    for (int z = 1; z <= 3; z++) {
                        if (x == 2 && z == 2) {
                            continue;
                        }
                        Block found = level.getBlockState(
                                origin.offset(x, 5, z)).getBlock();
                        if (found == control) {
                            controlPresent++;
                        } else if (found == conversion) {
                            conversionPresent++;
                        }
                    }
                }
                int controlNeed = Math.max(0, 4 - controlPresent);
                int conversionNeed = Math.max(0, 4 - conversionPresent);
                for (int y = 0; y <= 4; y++) {
                    for (int x = 0; x <= 4; x++) {
                        for (int z = 0; z <= 4; z++) {
                            BlockPos pos = origin.offset(x, y, z);
                            if (pos.equals(target.controller())) {
                                continue;
                            }
                            boolean inner = x >= 1 && x <= 3
                                    && z >= 1 && z <= 3
                                    && y >= 1 && y <= 3;
                            boolean center = x == 2 && y == 2 && z == 2;
                            if (center) {
                                add(
                                        cells,
                                        air(pos),
                                        pos);
                            } else {
                                add(
                                        cells,
                                        exact(pos, inner ? coil : lead, true,
                                                inner ? "osmium coil" : "dense lead wall"),
                                        pos);
                            }
                        }
                    }
                }
                for (int x = 0; x <= 4; x++) {
                    for (int z = 0; z <= 4; z++) {
                        BlockPos pos = origin.offset(x, 5, z);
                        if (x == 0 || x == 4 || z == 0 || z == 4) {
                            add(cells, exact(pos, vent, true, "ventilation unit"), pos);
                        } else if (x == 2 && z == 2) {
                            add(cells, exact(pos, versatile, true, "versatile processor"), pos);
                        } else {
                            Block found = level.getBlockState(pos).getBlock();
                            List<Block> processors;
                            if (found == control || found == conversion) {
                                processors = List.of(control, conversion);
                            } else if (controlNeed > 0) {
                                processors = List.of(control);
                                controlNeed--;
                            } else if (conversionNeed > 0) {
                                processors = List.of(conversion);
                                conversionNeed--;
                            } else {
                                processors = List.of(control, conversion);
                            }
                            add(
                                    cells,
                                    oneOf(
                                            pos,
                                            processors,
                                            true,
                                            "control or conversion processor"),
                                    pos);
                        }
                    }
                }
                return List.copyOf(cells.values());
            }
        };
    }

    private static Descriptor fusion() {
        ResourceLocation id = CoilHosts.id("fusion_reactor");
        return new Descriptor() {
            @Override public ResourceLocation id() { return id; }

            @Override
            public boolean isController(Level level, BlockPos position) {
                return level.getBlockState(position).is(
                        ModBlocks.FUSION_REACTOR.get());
            }

            @Override
            public List<Direction> candidateFacings() {
                return horizontal();
            }

            @Override
            public List<MultiblockBuildCell> cells(
                    Level level,
                    MultiblockBuilderTarget target) {
                Block galvanized = ModBlocks.mteBlock("multiblock/galvanized_steel_wall");
                Block ventilation = ModBlocks.VENTILATION_UNIT.get();
                Block versatile = ModBlocks.VERSATILE_PROCESSOR_UNIT.get();
                Block logic = ModBlocks.LOGIC_PROCESSOR_UNIT.get();
                Block control = ModBlocks.CONTROL_PROCESSOR_UNIT.get();
                Block tungsten = ModBlocks.mteBlock("tungstensteel/wall");
                Block stainless = ModBlocks.mteBlock("stainless_steel/wall");
                Block coil = CoilHosts.block(CoilHosts.IRIDIUM);
                Map<BlockPos, MultiblockBuildCell> cells =
                        new LinkedHashMap<>();
                BlockPos center = FusionStructure.center(
                        target.controller(),
                        target.facing());
                int versatilePresent = 0;
                int logicPresent = 0;
                int controlPresent = 0;
                for (int i = -2; i <= 2; i++) {
                    for (int j = -2; j <= 2; j++) {
                        for (int k = -2; k <= 2; k++) {
                            if (i * i + j * j + k * k >= 4) {
                                continue;
                            }
                            Block found = level.getBlockState(
                                    center.offset(i, j, k)).getBlock();
                            if (found == versatile) {
                                versatilePresent++;
                            } else if (found == logic) {
                                logicPresent++;
                            } else if (found == control) {
                                controlPresent++;
                            }
                        }
                    }
                }
                int versatileNeed = Math.max(
                        0,
                        FusionStructure.VERSATILE_PROCESSORS
                                - versatilePresent);
                BlockPos frontProcessor = center.relative(
                        target.facing().getOpposite());
                boolean reserveFront = versatileNeed > 0;
                if (reserveFront) {
                    versatileNeed--;
                }
                int logicNeed = Math.max(
                        0,
                        FusionStructure.LOGIC_PROCESSORS - logicPresent);
                int controlNeed = Math.max(
                        0,
                        FusionStructure.CONTROL_PROCESSORS - controlPresent);
                for (int i = -2; i <= 2; i++) {
                    for (int j = -2; j <= 2; j++) {
                        for (int k = -2; k <= 2; k++) {
                            BlockPos pos = center.offset(i, j, k);
                            if (pos.equals(target.controller())) {
                                continue;
                            }
                            int radius = i * i + j * j + k * k;
                            boolean plus = j == 0
                                    && ((Math.abs(i) == 2 && k == 0)
                                            || (Math.abs(k) == 2 && i == 0));
                            if (radius < 4) {
                                Block found = level.getBlockState(pos).getBlock();
                                List<Block> processors;
                                if (reserveFront && pos.equals(frontProcessor)) {
                                    processors = List.of(versatile);
                                } else if (found == versatile
                                        || found == logic
                                        || found == control) {
                                    processors = List.of(
                                            versatile,
                                            logic,
                                            control);
                                } else if (versatileNeed > 0) {
                                    processors = List.of(versatile);
                                    versatileNeed--;
                                } else if (logicNeed > 0) {
                                    processors = List.of(logic);
                                    logicNeed--;
                                } else if (controlNeed > 0) {
                                    processors = List.of(control);
                                    controlNeed--;
                                } else {
                                    processors = List.of();
                                }
                                add(
                                        cells,
                                        oneOf(
                                                pos,
                                                processors,
                                                true,
                                                "fusion processor"),
                                        pos);
                            } else if (radius > 6 || plus) {
                                add(cells, exact(pos, galvanized, true, "galvanized wall"), pos);
                            } else {
                                add(cells, exact(pos, ventilation, true, "ventilation unit"), pos);
                            }
                        }
                    }
                }
                for (int[] extra : new int[][] {
                        {-3, 0, 0}, {-4, 0, 0},
                        {3, 0, 0}, {4, 0, 0},
                        {0, 0, -3}, {0, 0, -4},
                        {0, 0, 3}, {0, 0, 4}}) {
                    boolean required = extra[0] < 0
                            ? target.facing() != Direction.WEST
                            : extra[0] > 0
                                    ? target.facing() != Direction.EAST
                                    : extra[2] < 0
                                            ? target.facing() != Direction.NORTH
                                            : target.facing() != Direction.SOUTH;
                    if (required) {
                        add(
                                cells,
                                exact(
                                        center.offset(extra[0], extra[1], extra[2]),
                                        galvanized,
                                        true,
                                        "fusion outer galvanized wall"),
                                center.offset(extra[0], extra[1], extra[2]));
                    }
                }
                BlockPos origin = center.offset(-9, 0, -9);
                for (int i = 0; i < 19; i++) {
                    for (int j = 0; j < 19; j++) {
                        if (FusionStructure.occupiedCell(0, i, j)) {
                            addLayer(cells, origin, i, j, -1, tungsten);
                            addLayer(cells, origin, i, j, 0, tungsten);
                            addLayer(cells, origin, i, j, 1, tungsten);
                        }
                        if (FusionStructure.occupiedCell(1, i, j)) {
                            addLayer(cells, origin, i, j, -2, tungsten);
                            addLayer(cells, origin, i, j, -1, tungsten);
                            addLayer(cells, origin, i, j, 0, coil);
                            addLayer(cells, origin, i, j, 1, tungsten);
                            addLayer(cells, origin, i, j, 2, tungsten);
                        }
                        if (FusionStructure.occupiedCell(2, i, j)) {
                            addLayer(cells, origin, i, j, -2, tungsten);
                            addLayer(cells, origin, i, j, -1, coil);
                            addLayer(cells, origin, i, j, 0, stainless);
                            addLayer(cells, origin, i, j, 1, coil);
                            addLayer(cells, origin, i, j, 2, tungsten);
                        }
                    }
                }
                return List.copyOf(cells.values());
            }

            @Override
            public void recheck(
                    Level level,
                    MultiblockBuilderTarget target) {
                if (level.getBlockEntity(target.controller())
                        instanceof FusionReactorBlockEntity reactor) {
                    reactor.requestStructureCheck();
                }
            }
        };
    }

    private static Descriptor logisticsCore() {
        ResourceLocation id = CoilHosts.id("logistics_core");
        return new Descriptor() {
            @Override public ResourceLocation id() { return id; }

            @Override
            public boolean isController(Level level, BlockPos position) {
                return level.getBlockState(position).is(
                        ModBlocks.LOGISTICS_CORE.get());
            }

            @Override
            public List<Direction> candidateFacings() {
                return List.of(Direction.values());
            }

            @Override
            public List<MultiblockBuildCell> cells(
                    Level level,
                    MultiblockBuilderTarget target) {
                Map<BlockPos, MultiblockBuildCell> cells =
                        new LinkedHashMap<>();
                BlockPos center = LogisticsCoreStructure.center(
                        target.controller(),
                        target.facing());
                Block wall = ModBlocks.mteBlock("multiblock/galvanized_steel_wall");
                Block vent = ModBlocks.VENTILATION_UNIT.get();
                List<Block> inner = List.of(
                        wall,
                        ModBlocks.VERSATILE_PROCESSOR_UNIT.get(),
                        ModBlocks.LOGIC_PROCESSOR_UNIT.get(),
                        ModBlocks.CONTROL_PROCESSOR_UNIT.get(),
                        ModBlocks.STORAGE_PROCESSOR_UNIT.get(),
                        ModBlocks.CONVERSION_PROCESSOR_UNIT.get());
                for (int i = -LogisticsCoreGeometry.HALF;
                        i <= LogisticsCoreGeometry.HALF;
                        i++) {
                    for (int j = -LogisticsCoreGeometry.HALF;
                            j <= LogisticsCoreGeometry.HALF;
                            j++) {
                        for (int k = -LogisticsCoreGeometry.HALF;
                                k <= LogisticsCoreGeometry.HALF;
                                k++) {
                            BlockPos pos = center.offset(i, j, k);
                            if (pos.equals(target.controller())) {
                                continue;
                            }
                            switch (LogisticsCoreGeometry.cell(i, j, k)) {
                                case WALL -> add(
                                        cells,
                                        exact(pos, wall, true, "logistics wall"),
                                        pos);
                                case VENT -> add(
                                        cells,
                                        exact(pos, vent, true, "ventilation unit"),
                                        pos);
                                case INNER -> add(
                                        cells,
                                        oneOf(pos, inner, true, "logistics processor"),
                                        pos);
                            }
                        }
                    }
                }
                return List.copyOf(cells.values());
            }
        };
    }

    private static Descriptor bedrockDrill() {
        ResourceLocation id = CoilHosts.id("bedrock_drill");
        return new Descriptor() {
            @Override public ResourceLocation id() { return id; }

            @Override
            public boolean isController(Level level, BlockPos position) {
                return position.getY() >= level.getMinBuildHeight() + 5
                        && level.getBlockState(position).is(
                                ModBlocks.BEDROCK_DRILL.get());
            }

            @Override
            public List<Direction> candidateFacings() {
                return List.of(Direction.values());
            }

            @Override
            public List<MultiblockBuildCell> cells(
                    Level level,
                    MultiblockBuilderTarget target) {
                Block head = ModBlocks.BEDROCK_DRILL_HEAD.get();
                Block wall = mte(BedrockDrillStructure.WALL_ID);
                Map<BlockPos, MultiblockBuildCell> cells =
                        new LinkedHashMap<>();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos floor = target.controller().offset(dx, -5, dz);
                        add(
                                cells,
                                predicate(
                                        floor,
                                        state -> BedrockDrillStructure.isFloor(state),
                                        stack -> false,
                                        false,
                                        "bedrock or bedrock ore floor"),
                                floor);
                        BlockPos drillHead = target.controller().offset(dx, -4, dz);
                        add(cells, exact(drillHead, head, true, "bedrock drill head"), drillHead);
                        for (int dy = -3; dy <= 0; dy++) {
                            BlockPos part = target.controller().offset(dx, dy, dz);
                            if (dy == 0 && dx == 0 && dz == 0) {
                                continue;
                            }
                            add(cells, exact(part, wall, true, "dense titanium wall"), part);
                        }
                    }
                }
                return List.copyOf(cells.values());
            }
        };
    }

    private static Descriptor lightningRod() {
        ResourceLocation id = CoilHosts.id("lightning_rod");
        return new Descriptor() {
            @Override public ResourceLocation id() { return id; }

            @Override
            public boolean isController(Level level, BlockPos position) {
                return mteKind(level, position, MteInPlaceKind.LIGHTNING_ROD);
            }

            @Override
            public List<Direction> candidateFacings() {
                return List.of(Direction.values());
            }

            @Override
            public List<MultiblockBuildCell> cells(
                    Level level,
                    MultiblockBuilderTarget target) {
                Block wall = CoilHosts.block(CoilHosts.TUNGSTEN_WALL);
                Block coil = CoilHosts.block(CoilHosts.NIOBIUM_TITANIUM);
                Block rod = CoilHosts.block(CoilHosts.LIGHTNING_ROD_PART);
                Map<BlockPos, MultiblockBuildCell> cells =
                        new LinkedHashMap<>();
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos base = target.controller().offset(x, 0, z);
                        if (!(x == 0 && z == 0)) {
                            add(cells, exact(base, wall, true, "tungsten wall"), base);
                        }
                        add(cells, exact(base.above(), coil, true, "niobium-titanium coil"), base.above());
                        add(cells, exact(base.above(2), wall, true, "tungsten wall"), base.above(2));
                        add(cells, exact(base.above(3), coil, true, "niobium-titanium coil"), base.above(3));
                        add(cells, exact(base.above(4), wall, true, "tungsten wall"), base.above(4));
                    }
                }
                /*
                 * The rod section is unbounded. It is included only as a
                 * validation/resolution cell, never as a buildable demand.
                 */
                for (int y = LightningRodStructure.BASE_LAYERS;
                        y < LightningRodStructure.BASE_LAYERS + 64;
                        y++) {
                    BlockPos pos = target.controller().above(y);
                    add(cells, exact(pos, rod, false, "lightning rod segment"), pos);
                }
                return List.copyOf(cells.values());
            }
        };
    }

    private static Descriptor vonDaGraagg() {
        ResourceLocation id = CoilHosts.id("von_da_graagg");
        return new Descriptor() {
            @Override public ResourceLocation id() { return id; }

            @Override
            public boolean isController(Level level, BlockPos position) {
                return mteKind(level, position, MteInPlaceKind.VON_DA_GRAAGG);
            }

            @Override
            public List<Direction> candidateFacings() {
                return List.of(Direction.values());
            }

            @Override
            public List<MultiblockBuildCell> cells(
                    Level level,
                    MultiblockBuilderTarget target) {
                Block galvanized = CoilHosts.block(CoilHosts.DENSE_GALVANIZED);
                Block coil = CoilHosts.block(CoilHosts.COPPER);
                Block steel = CoilHosts.block(CoilHosts.DENSE_STEEL);
                Map<BlockPos, MultiblockBuildCell> cells =
                        new LinkedHashMap<>();
                for (int i = -2; i <= 2; i++) {
                    for (int j = -2; j <= 2; j++) {
                        if (Math.abs(i * j) >= 4) {
                            continue;
                        }
                        for (int y = 0; y <= 1; y++) {
                            BlockPos pos = target.controller().offset(i, y, j);
                            if (!pos.equals(target.controller())) {
                                add(cells, exact(pos, galvanized, true, "dense galvanized wall"), pos);
                            }
                        }
                    }
                }
                for (int y = 2; y <= 6; y++) {
                    BlockPos pos = target.controller().above(y);
                    add(cells, exact(pos, coil, true, "copper coil"), pos);
                }
                add(cells, exact(target.controller().above(7), steel, true, "dense steel cap"), target.controller().above(7));
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        if (i == 0 && j == 0) {
                            continue;
                        }
                        BlockPos y6 = target.controller().offset(i, 6, j);
                        add(cells, exact(y6, steel, true, "dense steel cap"), y6);
                        if (i * j == 0) {
                            BlockPos y5 = target.controller().offset(i, 5, j);
                            BlockPos y7 = target.controller().offset(i, 7, j);
                            add(cells, exact(y5, steel, true, "dense steel cap"), y5);
                            add(cells, exact(y7, steel, true, "dense steel cap"), y7);
                        }
                    }
                }
                return List.copyOf(cells.values());
            }
        };
    }

    private static Descriptor largeHeatExchanger() {
        ResourceLocation id = CoilHosts.id("large_heat_exchanger");
        return new Descriptor() {
            @Override public ResourceLocation id() { return id; }

            @Override
            public boolean isController(Level level, BlockPos position) {
                return level.getBlockState(position).is(
                        ModBlocks.LARGE_HEAT_EXCHANGER.get());
            }

            @Override
            public List<Direction> candidateFacings() {
                return horizontal();
            }

            @Override
            public List<MultiblockBuildCell> cells(
                    Level level,
                    MultiblockBuilderTarget target) {
                Block wall = mte(LargeHeatExchangerCatalog.profile().wallId());
                Block transmitter = mte(
                        LargeHeatExchangerCatalog.profile().transmitterId());
                Map<BlockPos, MultiblockBuildCell> cells =
                        new LinkedHashMap<>();
                for (Vec3i offset : new Vec3i[] {
                        new Vec3i(-1, 0, -1), new Vec3i(0, 0, -1),
                        new Vec3i(1, 0, -1), new Vec3i(-1, 0, 0),
                        new Vec3i(1, 0, 0), new Vec3i(-1, 0, 1),
                        new Vec3i(0, 0, 1), new Vec3i(1, 0, 1),
                        new Vec3i(0, 1, 0)}) {
                    BlockPos pos = target.controller().offset(offset);
                    add(cells, exact(pos, wall, true, "dense tungsten wall"), pos);
                }
                for (Vec3i offset : LargeHeatExchangerStructure.transmitters()) {
                    BlockPos pos = target.controller().offset(offset);
                    add(cells, exact(pos, transmitter, true, "heat transmitter"), pos);
                }
                return List.copyOf(cells.values());
            }
        };
    }

    private static Descriptor largeDynamo() {
        ResourceLocation id = CoilHosts.id("large_dynamo");
        return new Descriptor() {
            @Override public ResourceLocation id() { return id; }

            @Override
            public boolean isController(Level level, BlockPos position) {
                return mteKind(level, position, MteInPlaceKind.LARGE_DYNAMO);
            }

            @Override
            public List<Direction> candidateFacings() {
                return horizontal();
            }

            @Override
            public List<MultiblockBuildCell> cells(
                    Level level,
                    MultiblockBuilderTarget target) {
                Block wall = largeDynamoWall(level, target.controller());
                Block coil = CoilHosts.block(CoilHosts.COPPER);
                Map<BlockPos, MultiblockBuildCell> cells =
                        new LinkedHashMap<>();
                SteamTurbineStructure.Cell cell =
                        SteamTurbineStructure.cell(
                                target.controller(),
                                target.facing());
                for (int x = cell.minX(); x <= cell.maxX(); x++) {
                    for (int y = cell.minY(); y <= cell.maxY(); y++) {
                        for (int z = cell.minZ(); z <= cell.maxZ(); z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (pos.equals(target.controller())) {
                                continue;
                            }
                            Block expected = LargeDynamoStructure.interior(
                                    target.controller(),
                                    target.facing(),
                                    pos) ? coil : wall;
                            add(
                                    cells,
                                    exact(
                                            pos,
                                            expected,
                                            true,
                                            expected == coil
                                                    ? "copper coil"
                                                    : "dynamo wall"),
                                    pos);
                        }
                    }
                }
                return List.copyOf(cells.values());
            }
        };
    }

    private static Descriptor steamTurbine() {
        ResourceLocation id = CoilHosts.id("steam_turbine");
        return turbineDescriptor(
                id,
                MteInPlaceKind.STEAM_TURBINE,
                (level, target) -> steamWall(level, target.controller()));
    }

    private static Descriptor gasTurbine() {
        ResourceLocation id = CoilHosts.id("gas_turbine");
        return turbineDescriptor(
                id,
                MteInPlaceKind.GAS_TURBINE,
                (level, target) -> gasWall(level, target.controller()));
    }

    private static Descriptor turbineDescriptor(
            ResourceLocation id,
            MteInPlaceKind kind,
            WallResolver wallResolver) {
        return new Descriptor() {
            @Override public ResourceLocation id() { return id; }

            @Override
            public boolean isController(Level level, BlockPos position) {
                if (!mteKind(level, position, kind)) {
                    return false;
                }
                if (kind == MteInPlaceKind.STEAM_TURBINE
                        && level.getBlockState(position).getBlock()
                                instanceof MteInPlaceBlock block) {
                    return SteamTurbineCatalog.find(block.spec().id())
                            .map(SteamTurbineCatalog.Profile::large)
                            .orElse(false);
                }
                return true;
            }

            @Override
            public List<Direction> candidateFacings() {
                return List.of(Direction.values());
            }

            @Override
            public List<MultiblockBuildCell> cells(
                    Level level,
                    MultiblockBuilderTarget target) {
                Block wall = wallResolver.wall(level, target);
                Map<BlockPos, MultiblockBuildCell> cells =
                        new LinkedHashMap<>();
                SteamTurbineStructure.Cell cell =
                        SteamTurbineStructure.cell(
                                target.controller(),
                                target.facing());
                for (int x = cell.minX(); x <= cell.maxX(); x++) {
                    for (int y = cell.minY(); y <= cell.maxY(); y++) {
                        for (int z = cell.minZ(); z <= cell.maxZ(); z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (!pos.equals(target.controller())) {
                                add(cells, exact(pos, wall, true, "turbine wall"), pos);
                            }
                        }
                    }
                }
                return List.copyOf(cells.values());
            }
        };
    }

    @FunctionalInterface
    private interface WallResolver {
        Block wall(Level level, MultiblockBuilderTarget target);
    }

    private static Block largeDynamoWall(Level level, BlockPos controller) {
        MteInPlaceSpec spec = mteSpec(level, controller);
        return spec == null
                ? mte(CoilHosts.DENSE_STAINLESS)
                : LargeDynamoCatalog.find(spec.id())
                        .map(profile -> mte(profile.wallId()))
                        .orElse(mte(CoilHosts.DENSE_STAINLESS));
    }

    private static Block steamWall(Level level, BlockPos controller) {
        MteInPlaceSpec spec = mteSpec(level, controller);
        return spec == null
                ? mte(CoilHosts.DENSE_STAINLESS)
                : SteamTurbineCatalog.find(spec.id())
                        .map(profile -> mte(profile.wallId()))
                        .orElse(mte(CoilHosts.DENSE_STAINLESS));
    }

    private static Block gasWall(Level level, BlockPos controller) {
        MteInPlaceSpec spec = mteSpec(level, controller);
        return spec == null
                ? mte(CoilHosts.DENSE_STAINLESS)
                : LargeGasTurbineCatalog.find(spec.id())
                        .map(profile -> mte(profile.wallId()))
                        .orElse(mte(CoilHosts.DENSE_STAINLESS));
    }

    private static void addLayer(
            Map<BlockPos, MultiblockBuildCell> cells,
            BlockPos origin,
            int x,
            int z,
            int y,
            Block block) {
        BlockPos pos = origin.offset(x, y, z);
        add(cells, exact(pos, block, true, "fusion wall or coil"), pos);
    }

    private static void add(
            Map<BlockPos, MultiblockBuildCell> cells,
            MultiblockBuildCell cell,
            BlockPos position) {
        cells.putIfAbsent(position.immutable(), cell);
    }

    private static MultiblockBuildCell exact(
            BlockPos position,
            Block expected,
            boolean placeable,
            String description) {
        return predicate(
                position,
                state -> expected != null && state.is(expected),
                stack -> expected != null
                        && stack.getItem() instanceof BlockItem blockItem
                        && blockItem.getBlock() == expected,
                placeable,
                description);
    }

    private static MultiblockBuildCell oneOf(
            BlockPos position,
            List<Block> expected,
            boolean placeable,
            String description) {
        List<Block> blocks = expected.stream().filter(block -> block != null).toList();
        return predicate(
                position,
                state -> blocks.stream().anyMatch(state::is),
                stack -> stack.getItem() instanceof BlockItem blockItem
                        && blocks.contains(blockItem.getBlock()),
                placeable,
                description);
    }

    private static MultiblockBuildCell air(BlockPos position) {
        return predicate(
                position,
                BlockState::isAir,
                stack -> false,
                false,
                "air cavity");
    }

    private static MultiblockBuildCell predicate(
            BlockPos position,
            Predicate<BlockState> stateMatcher,
            Predicate<ItemStack> itemMatcher,
            boolean placeable,
            String description) {
        return new MultiblockBuildCell(
                position,
                stateMatcher,
                itemMatcher,
                placeable,
                description);
    }

    private static boolean mteKind(
            Level level,
            BlockPos position,
            MteInPlaceKind kind) {
        return level.getBlockState(position).getBlock()
                        instanceof MteInPlaceBlock block
                && block.spec().kind() == kind;
    }

    private static MteInPlaceSpec mteSpec(Level level, BlockPos position) {
        return level.getBlockState(position).getBlock()
                        instanceof MteInPlaceBlock block
                ? block.spec()
                : null;
    }

    private static Block mte(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        var holder = ModBlocks.mteInPlaceBlocksById().get(id);
        return holder == null ? null : holder.get();
    }

    private static Direction stateFacing(
            BlockState state,
            Direction fallback) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof DirectionProperty directionProperty
                    && property.getName().equals("facing")) {
                return state.getValue(directionProperty);
            }
        }
        return fallback;
    }

    private static List<Direction> horizontal() {
        return List.of(
                Direction.NORTH,
                Direction.EAST,
                Direction.SOUTH,
                Direction.WEST);
    }
}
