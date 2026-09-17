package com.masson.cruciblecraft.content.block;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6 {@code BlockFluidFinite} / {@code BlockBaseFluid}: metadata 0–15 is 1–16
 * quanta. Worldgen muffins place meta 15. Vanilla {@link FlowingFluid} ticks
 * are never scheduled.
 */
public final class SpringLiquidBlock extends LiquidBlock {
    public static final IntegerProperty META =
            IntegerProperty.create("gt6_meta", 0, 15);
    public static final int QUANTA_PER_BLOCK = FiniteFluidQuanta.QUANTA_PER_BLOCK;
    public static final int FULL_META = FiniteFluidQuanta.FULL_META;
    public static final int DRIP_META = FiniteFluidQuanta.DRIP_META;
    private static final int FLUID_UPDATE_FLAGS = Block.UPDATE_CLIENTS;
    private static final Vec3 WEB_STUCK = new Vec3(0.25D, 0.05D, 0.25D);

    private final FlowingFluid flowing;
    private final Supplier<Item> bucket;
    private final int flammability;
    private final boolean web;
    private final SpringLiquidContact contact;
    private final boolean lighterThanWater;
    private final int density;
    private final int densityDir;
    private final int tickRate;

    public SpringLiquidBlock(
            FlowingFluid fluid,
            Properties properties,
            Supplier<Item> bucket,
            int density,
            int viscosity,
            int flammability,
            boolean web,
            SpringLiquidContact contact) {
        super(fluid, properties);
        this.flowing = fluid;
        this.bucket = bucket;
        this.flammability = flammability;
        this.web = web;
        this.contact = contact;
        this.lighterThanWater = true;
        this.density = density;
        this.densityDir = density > 0 ? -1 : 1;
        this.tickRate = Math.max(1, viscosity / 200);
        registerDefaultState(
                stateDefinition.any().setValue(LEVEL, 0).setValue(META, FULL_META));
    }

    public static int bind4(int value) {
        return FiniteFluidQuanta.bind4(value);
    }

    public static int quanta(int meta) {
        return FiniteFluidQuanta.quanta(meta);
    }

    public static int quanta(BlockState state) {
        return state.hasProperty(META) ? state.getValue(META) + 1 : 0;
    }

    public BlockState withMeta(int meta) {
        return defaultBlockState().setValue(META, bind4(meta));
    }

    public BlockState dripState() {
        return withMeta(DRIP_META);
    }

    public BlockState fullState() {
        return withMeta(FULL_META);
    }

    public void scheduleFlow(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, tickRate);
        }
    }

    public void flowTick(Level level, BlockPos pos, RandomSource random) {
        if (level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) {
            return;
        }
        if (flammability > 0) {
            for (Direction direction : Direction.values()) {
                BlockState neighbor = level.getBlockState(pos.relative(direction));
                if (neighbor.is(this)) {
                    continue;
                }
                if (neighbor.is(Blocks.FIRE) || neighbor.is(Blocks.LAVA)
                        || neighbor.getFluidState().is(FluidTags.LAVA)) {
                    level.setBlock(pos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
                    return;
                }
            }
        }

        int remaining = quanta(state);
        int y = pos.getY();
        if (y <= level.getMinBuildHeight() || y + 1 >= level.getMaxBuildHeight()) {
            if (level.setBlock(
                    pos, Blocks.AIR.defaultBlockState(), FLUID_UPDATE_FLAGS | 1)) {
                updateFluidBlocks(level, pos, true);
            }
            return;
        }

        int original = remaining;
        remaining = tryToFlowVerticallyInto(level, pos, remaining);
        if (remaining < 1) {
            updateFluidBlocks(level, pos, false);
            return;
        }

        boolean changed = remaining != original;
        if (remaining == 1) {
            if (changed) {
                set(level, pos, remaining - 1, false);
                updateFluidBlocks(level, pos, false);
                return;
            }
            if (!isLiquid(level, pos.offset(0, densityDir, 0))) {
                Direction[] sides = horizontalOrder(random);
                for (Direction side : sides) {
                    BlockPos hop = pos.relative(side);
                    BlockPos underHop = hop.offset(0, densityDir, 0);
                    if (isLoaded(level, hop)
                            && !hasCollide(level, underHop)
                            && displaceIfPossible(level, hop)
                            && set(level, hop, remaining - 1, false)) {
                        scheduleFlow(level, hop);
                        level.setBlock(
                                pos,
                                Blocks.AIR.defaultBlockState(),
                                FLUID_UPDATE_FLAGS | 1);
                        updateFluidBlocks(level, pos, true);
                        return;
                    }
                }
            }
            return;
        }

        BlockPos north = pos.north();
        BlockPos south = pos.south();
        BlockPos west = pos.west();
        BlockPos east = pos.east();
        if (isLoaded(level, north) && displaceIfPossible(level, north)) {
            clearToAir(level, north);
        }
        if (isLoaded(level, south) && displaceIfPossible(level, south)) {
            clearToAir(level, south);
        }
        if (isLoaded(level, west) && displaceIfPossible(level, west)) {
            clearToAir(level, west);
        }
        if (isLoaded(level, east) && displaceIfPossible(level, east)) {
            clearToAir(level, east);
        }

        int total = remaining;
        int count = 1;
        int northQ = getQuantaValueBelow(level, north, remaining - 1);
        int southQ = getQuantaValueBelow(level, south, remaining - 1);
        int westQ = getQuantaValueBelow(level, west, remaining - 1);
        int eastQ = getQuantaValueBelow(level, east, remaining - 1);
        if (northQ >= 0) {
            count++;
            total += northQ;
        }
        if (southQ >= 0) {
            count++;
            total += southQ;
        }
        if (westQ >= 0) {
            count++;
            total += westQ;
        }
        if (eastQ >= 0) {
            count++;
            total += eastQ;
        }
        if (count == 1) {
            if (changed) {
                set(level, pos, remaining - 1, false);
                updateFluidBlocks(level, pos, false);
            }
            return;
        }

        int spread = total / count;
        int remainder = total % count;
        if (northQ >= 0) {
            remainder = spreadSide(
                    level, north, northQ, spread, remainder, count, random);
            count--;
        }
        if (southQ >= 0) {
            remainder = spreadSide(
                    level, south, southQ, spread, remainder, count, random);
            count--;
        }
        if (westQ >= 0) {
            remainder = spreadSide(
                    level, west, westQ, spread, remainder, count, random);
            count--;
        }
        if (eastQ >= 0) {
            remainder = spreadSide(
                    level, east, eastQ, spread, remainder, count, random);
        }
        set(level, pos, remainder > 0 ? spread : spread - 1, false);
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(META);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        int amount = Mth.clamp(quanta(state), 1, QUANTA_PER_BLOCK);
        if (amount >= QUANTA_PER_BLOCK) {
            return flowing.getSource(false);
        }
        return flowing.getFlowing(amount, false);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public ItemStack pickupBlock(
            @Nullable Player player,
            LevelAccessor level,
            BlockPos pos,
            BlockState state) {
        if (state.getValue(META) < DRIP_META) {
            return ItemStack.EMPTY;
        }
        int leftover = quanta(state) - FiniteFluidQuanta.BUCKET_QUANTA;
        if (leftover > 0) {
            level.setBlock(pos, withMeta(leftover - 1), 11);
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        }
        if (level instanceof Level world) {
            updateFluidBlocks(world, pos, true);
        }
        return new ItemStack(bucket.get());
    }

    @Override
    public int getFlammability(
            BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return flammability;
    }

    @Override
    public int getFireSpreadSpeed(
            BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return flammability;
    }

    @Override
    protected void entityInside(
            BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (web) {
            entity.makeStuckInBlock(state, WEB_STUCK);
        }
        contact.apply(this, level, entity);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return false;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        scheduleFlow(level, pos);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston) {
        scheduleFlow(level, pos);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        return state;
    }

    @Override
    protected void tick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        flowTick(level, pos, random);
    }

    private int tryToFlowVerticallyInto(Level level, BlockPos pos, int amount) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (lighterThanWater) {
            int tY = y;
            int topY = level.getMaxBuildHeight();
            while (++tY < topY && isAnyWater(level, new BlockPos(x, tY, z))) {
                // climb the water column
            }
            if (tY - 1 > y) {
                BlockPos dest = new BlockPos(x, tY, z);
                BlockState destState = level.getBlockState(dest);
                if (destState.is(this)) {
                    int merged = 1 + destState.getValue(META) + amount;
                    if (merged > 16) {
                        set(level, dest, 15, true);
                        scheduleFlow(level, dest);
                        return merged - 16;
                    }
                    if (merged > 0) {
                        set(level, dest, merged - 1, true);
                        level.setBlock(
                                pos,
                                Blocks.AIR.defaultBlockState(),
                                FLUID_UPDATE_FLAGS | 1);
                        return 0;
                    }
                    return amount;
                }
                if (destState.isAir() || displaceIfPossible(level, dest)) {
                    set(level, dest, amount - 1, true);
                    scheduleFlow(level, dest);
                    return 0;
                }
            }
        }

        if (amount > QUANTA_PER_BLOCK) {
            BlockPos dest = pos.offset(0, -densityDir, 0);
            BlockState destState = level.getBlockState(dest);
            if (destState.getBlock() instanceof SpringLiquidBlock) {
                int destMeta = destState.getValue(META);
                if (destMeta > DRIP_META) {
                    return amount;
                }
                level.setBlock(pos, destState, FLUID_UPDATE_FLAGS | 1);
                set(level, dest, amount - 1, true);
                scheduleFlow(level, dest);
                return 0;
            }
            if (!lighterThanWater && isAnyWater(destState)) {
                level.setBlock(pos, destState, FLUID_UPDATE_FLAGS | 1);
                set(level, dest, amount - 1, true);
                scheduleFlow(level, dest);
                return 0;
            }
            if (destState.isAir() || displaceIfPossible(level, dest)) {
                scheduleFlowAt(level, pos, 128 - amount * 4);
                set(level, dest, amount - 2, true);
                scheduleFlowAt(level, dest, 1);
                updateFluidBlocks(level, pos, true);
                return 1;
            }
        }

        BlockPos dest = pos.offset(0, densityDir, 0);
        BlockState destState = level.getBlockState(dest);
        if (destState.is(this)) {
            int merged = 1 + destState.getValue(META) + amount;
            if (merged > QUANTA_PER_BLOCK) {
                set(level, dest, QUANTA_PER_BLOCK - 1, true);
                scheduleFlow(level, dest);
                return merged - QUANTA_PER_BLOCK;
            }
            if (merged > 0) {
                set(level, dest, merged - 1, true);
                level.setBlock(
                        pos, Blocks.AIR.defaultBlockState(), FLUID_UPDATE_FLAGS | 1);
                return 0;
            }
            return amount;
        }
        if (destState.getBlock() instanceof SpringLiquidBlock other) {
            int otherDensity = other.density;
            boolean denser = densityDir > 0
                    ? otherDensity > density
                    : otherDensity < density;
            if (denser) {
                level.setBlock(pos, destState, FLUID_UPDATE_FLAGS | 1);
                set(level, dest, amount - 1, true);
                scheduleFlow(level, dest);
                return 0;
            }
            return amount;
        }
        if (destState.isAir() || displaceIfPossible(level, dest)) {
            set(level, dest, amount - 1, true);
            level.setBlock(
                    pos, Blocks.AIR.defaultBlockState(), FLUID_UPDATE_FLAGS | 1);
            return 0;
        }
        return amount;
    }

    private int spreadSide(
            Level level,
            BlockPos neighbor,
            int neighborQuanta,
            int spread,
            int remainder,
            int count,
            RandomSource random) {
        int next = spread;
        if (remainder == count
                || remainder > 1 && random.nextInt(count - remainder) != 0) {
            next++;
            remainder--;
        }
        if (next != neighborQuanta) {
            if (next > 0) {
                if (set(level, neighbor, next - 1, false)) {
                    scheduleFlow(level, neighbor);
                }
            } else {
                clearToAir(level, neighbor);
            }
        }
        return remainder;
    }

    private boolean set(Level level, BlockPos pos, int meta, boolean blockUpdate) {
        meta = bind4(meta);
        BlockState current = level.getBlockState(pos);
        if (!current.is(this)) {
            int flags = blockUpdate ? Block.UPDATE_ALL : FLUID_UPDATE_FLAGS;
            return level.setBlock(pos, withMeta(meta), flags);
        }
        int old = current.getValue(META);
        if (old == meta) {
            return false;
        }
        int flags = meta >= DRIP_META && old >= DRIP_META
                ? (blockUpdate ? 5 : 4)
                : (blockUpdate ? Block.UPDATE_ALL : FLUID_UPDATE_FLAGS);
        return level.setBlock(
                pos, current.setValue(LEVEL, 0).setValue(META, meta), flags);
    }

    private void updateFluidBlocks(Level level, BlockPos origin, boolean all) {
        int originY = origin.getY();
        int minY = densityDir > 0 ? -1 : 0;
        int maxY = densityDir > 0 ? 0 : 1;
        int worldMin = level.getMinBuildHeight();
        int worldTop = level.getMaxBuildHeight();
        for (int j = minY; j <= maxY; j++) {
            int y = originY + j;
            if (y < worldMin || y >= worldTop) {
                continue;
            }
            for (int i = -4; i <= 4; i++) {
                for (int k = -4; k <= 4; k++) {
                    if (i == 0 && j == 0 && k == 0) {
                        continue;
                    }
                    BlockPos around = origin.offset(i, j, k);
                    BlockState state = level.getBlockState(around);
                    if (!state.is(this)) {
                        continue;
                    }
                    int meta = state.getValue(META);
                    if (all || meta > (j == 0 ? Math.abs(i) : 0)) {
                        scheduleFlow(level, around);
                    }
                }
            }
        }
    }

    private int getQuantaValue(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return 0;
        }
        if (!state.is(this)) {
            return -1;
        }
        return quanta(state);
    }

    private int getQuantaValueBelow(BlockGetter level, BlockPos pos, int belowThis) {
        int remaining = getQuantaValue(level, pos);
        if (remaining >= belowThis) {
            return -1;
        }
        return remaining;
    }

    private boolean canDisplace(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (state.is(this) || isLiquid(state) || state.blocksMotion()) {
            return false;
        }
        return true;
    }

    private boolean displaceIfPossible(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (!canDisplace(level, pos)) {
            return false;
        }
        if (level instanceof ServerLevel) {
            Block.dropResources(state, level, pos);
        }
        return true;
    }

    private void clearToAir(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir()) {
            level.setBlock(
                    pos, Blocks.AIR.defaultBlockState(), FLUID_UPDATE_FLAGS | 1);
        }
    }

    private void scheduleFlowAt(Level level, BlockPos pos, int delay) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, Math.max(1, delay));
        }
    }

    private static boolean isLoaded(Level level, BlockPos pos) {
        return level.isLoaded(pos);
    }

    private static boolean hasCollide(BlockGetter level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isLiquid(BlockGetter level, BlockPos pos) {
        return isLiquid(level.getBlockState(pos));
    }

    private static boolean isLiquid(BlockState state) {
        return state.liquid() || !state.getFluidState().isEmpty();
    }

    private static boolean isAnyWater(BlockGetter level, BlockPos pos) {
        return isAnyWater(level.getBlockState(pos));
    }

    private static boolean isAnyWater(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    private static Direction[] horizontalOrder(RandomSource random) {
        Direction[] order = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
        };
        int start = random.nextInt(order.length);
        if (start == 0) {
            return order;
        }
        Direction[] rotated = new Direction[order.length];
        for (int i = 0; i < order.length; i++) {
            rotated[i] = order[(start + i) % order.length];
        }
        return rotated;
    }
}
