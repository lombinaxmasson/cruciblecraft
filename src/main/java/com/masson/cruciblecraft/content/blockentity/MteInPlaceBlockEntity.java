package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.mold.CruciblePour;
import com.masson.cruciblecraft.content.mold.MoldCastingRules;
import com.masson.cruciblecraft.content.mold.MoldHost;
import com.masson.cruciblecraft.content.mte.MteFoundryTanks;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.storage.StorageClientSync;
import com.masson.cruciblecraft.energy.steam.SteamTurbineCatalog;
import com.masson.cruciblecraft.energy.steam.SteamTurbineStructure;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Shared host for in-place GT6 MTE families. Kind-specific transfer is not a
 * pipe cover, KU axle, vanilla tool, or the ceramic crucible.
 */
public final class MteInPlaceBlockEntity extends BlockEntity
        implements IEnergyHandler, MoldHost {
    public static final int TRANSFER_MB = 1000;
    public static final long ENERGY_CAPACITY = 16_384L;

    private final ItemStackHandler items;
    private final FluidTank tank;
    private final FluidTank distilled;
    private final long energyCapacity;
    private long storedEnergy;
    private long steamCounter;
    private boolean formed;

    public MteInPlaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MTE_INPLACE.get(), pos, state);
        MteInPlaceSpec spec = specOf(state);
        int slots = Math.max(1, spec.kind().slots());
        this.items = new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                MteInPlaceBlockEntity.this.setChanged();
                MteInPlaceKind kind = spec().kind();
                if (kind == MteInPlaceKind.MASS_STORAGE
                        || kind == MteInPlaceKind.BOOKSHELF
                        || kind == MteInPlaceKind.BOTTLE_CRATE) {
                    StorageClientSync.send(MteInPlaceBlockEntity.this);
                }
            }
        };
        SteamTurbineCatalog.Profile turbine =
                spec.kind() == MteInPlaceKind.STEAM_TURBINE
                        ? SteamTurbineCatalog.find(spec.id()).orElse(null)
                        : null;
        int tankCap = spec.kind().foundryTank()
                ? MteFoundryTanks.capacityMb(spec)
                : turbine != null ? turbine.tankCapacityMb() : 1;
        this.tank = new FluidTank(tankCap) {
            @Override
            protected void onContentsChanged() {
                MteInPlaceBlockEntity.this.setChanged();
            }
        };
        this.distilled = new FluidTank(Math.max(1, tankCap)) {
            @Override
            protected void onContentsChanged() {
                MteInPlaceBlockEntity.this.setChanged();
            }
        };
        this.energyCapacity = turbine != null
                ? turbine.energyCapacity()
                : ENERGY_CAPACITY;
    }

    public MteInPlaceSpec spec() {
        return specOf(getBlockState());
    }

    public ItemStackHandler items() {
        return items;
    }

    public FluidTank tank() {
        return tank;
    }

    public FluidTank distilledTank() {
        return distilled;
    }

    /**
     * GT6 TurbineSteam trashes the steam tank. LargeTurbineSteam trashes steam
     * first, then DistW.
     */
    public boolean trashWithPlunger() {
        if (spec().kind() != MteInPlaceKind.STEAM_TURBINE) {
            return false;
        }
        if (!tank.isEmpty()) {
            tank.setFluid(FluidStack.EMPTY);
            setChanged();
            return true;
        }
        SteamTurbineCatalog.Profile profile =
                SteamTurbineCatalog.find(spec().id()).orElse(null);
        if (profile == null || !profile.large() || distilled.isEmpty()) {
            return false;
        }
        distilled.setFluid(FluidStack.EMPTY);
        setChanged();
        return true;
    }

    public boolean formed() {
        return formed;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            MteInPlaceBlockEntity host) {
        if (host.spec().kind().attachment()
                && level.hasNeighborSignal(pos)) {
            host.transferOnce();
        }
        if (host.spec().kind().extender()) {
            host.pushExtender();
        }
        if (host.spec().kind() == MteInPlaceKind.STEAM_TURBINE) {
            host.updateSteamFormed();
            host.consumeSteam();
        }
        if (host.spec().kind().drive()) {
            host.pushDrive();
        }
    }

    public void transferOnce() {
        if (level == null || level.isClientSide) {
            return;
        }
        Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
        switch (spec().kind()) {
            case FAUCET -> pourFaucet(facing);
            case TAP, NOZZLE -> pourDown(facing);
            case FUNNEL, CAP_NOZZLE -> fillAttached(facing);
            default -> {
            }
        }
    }

    public IFluidHandler fluidHandler(Direction side) {
        if (spec().kind().extender()) {
            return new ExtenderHandler(side);
        }
        if (spec().kind().foundryTank()) {
            return tank;
        }
        if (spec().kind() == MteInPlaceKind.STEAM_TURBINE) {
            return new SteamHandler(side);
        }
        return null;
    }

    public net.neoforged.neoforge.items.IItemHandler itemHandler(Direction side) {
        return spec().kind().inventory() ? items : null;
    }

    public void dropContents() {
        if (level == null || !spec().kind().inventory()) {
            return;
        }
        for (int slot = 0; slot < items.getSlots(); slot++) {
            Containers.dropItemStack(
                    level,
                    worldPosition.getX(),
                    worldPosition.getY(),
                    worldPosition.getZ(),
                    items.getStackInSlot(slot));
        }
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return spec().kind().energy() && type == spec().kind().energyType();
    }

    @Override
    public long stored(EnergyType type) {
        return handles(type, Direction.NORTH) ? storedEnergy : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return handles(type, Direction.NORTH) ? energyCapacity : 0L;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || amount <= 0L) {
            return 0L;
        }
        long room = Math.max(0L, energyCapacity - storedEnergy);
        long accepted = Math.min(amount, room);
        if (!simulate) {
            storedEnergy += accepted;
            setChanged();
        }
        return accepted;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maxAmount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || maxAmount <= 0L) {
            return 0L;
        }
        long taken = Math.min(maxAmount, storedEnergy);
        if (!simulate) {
            storedEnergy -= taken;
            setChanged();
        }
        return taken;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putLong("StoredEnergy", storedEnergy);
        tag.putLong("SteamCounter", steamCounter);
        tag.putBoolean("Formed", formed);
        tag.put("distilled", distilled.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            items.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("tank")) {
            tank.readFromNBT(registries, tag.getCompound("tank"));
        }
        storedEnergy = tag.getLong("StoredEnergy");
        steamCounter = tag.getLong("SteamCounter");
        formed = tag.getBoolean("Formed");
        if (tag.contains("distilled")) {
            distilled.readFromNBT(registries, tag.getCompound("distilled"));
        }
    }

    @Override
    public boolean isMoldInputSide(Direction side) {
        return spec().kind() == MteInPlaceKind.FAUCET
                && side == getBlockState().getValue(MteInPlaceBlock.FACING);
    }

    @Override
    public float moldMaxTemperatureCelsius() {
        String materialId = faucetMaterialId();
        float melting = MaterialCatalog.contains(materialId)
                ? (float) MaterialCatalog.require(materialId).thermal().meltingPoint()
                : (float) MaterialCatalog.require("stone").thermal().meltingPoint();
        return MoldCastingRules.maximumTemperature(melting);
    }

    @Override
    public int moldRequiredMaterialUnits() {
        if (level == null) {
            return 0;
        }
        BlockPos dest = faucetDestination();
        if (level.getBlockEntity(dest) instanceof MoldHost mold) {
            return mold.moldRequiredMaterialUnits();
        }
        return 0;
    }

    @Override
    public int fillMold(
            String materialId,
            int availableUnits,
            float temperature,
            Direction side) {
        if (spec().kind() != MteInPlaceKind.FAUCET
                || !isMoldInputSide(side)
                || materialId == null
                || materialId.isEmpty()
                || availableUnits <= 0
                || level == null) {
            return 0;
        }
        if (temperature > moldMaxTemperatureCelsius()) {
            level.setBlock(worldPosition, Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
            return 0;
        }
        BlockPos dest = faucetDestination();
        if (level.getBlockEntity(dest) instanceof MoldHost mold) {
            return mold.fillMold(materialId, availableUnits, temperature, Direction.UP);
        }
        return 0;
    }

    @Override
    public ItemStack takeOutput(Player player, boolean causeDamage) {
        return ItemStack.EMPTY;
    }

    private void pourFaucet(Direction facing) {
        if (level.getBlockEntity(worldPosition.relative(facing)) instanceof CruciblePour crucible) {
            crucible.fillMoldAtSide(this, facing.getOpposite(), facing);
            return;
        }
        pourDown(facing);
    }

    private BlockPos faucetDestination() {
        BlockPos dest = worldPosition.below();
        while (level != null
                && dest.getY() > level.getMinBuildHeight()
                && level.getBlockEntity(dest) instanceof MteInPlaceBlockEntity other
                && other.spec().kind() == MteInPlaceKind.FAUCET) {
            dest = dest.below();
        }
        return dest;
    }

    private String faucetMaterialId() {
        String path = spec().registryPath();
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int under = name.lastIndexOf('_');
        String candidate = under >= 0 ? name.substring(under + 1) : name;
        return MaterialCatalog.contains(candidate) ? candidate : "stone";
    }

    private void pourDown(Direction facing) {
        IFluidHandler source = neighborFluid(facing, facing.getOpposite());
        if (source == null) {
            return;
        }
        BlockPos destPos = worldPosition.below();
        while (level.getBlockEntity(destPos) instanceof MteInPlaceBlockEntity other
                && other.spec().kind() == MteInPlaceKind.FAUCET) {
            destPos = destPos.below();
        }
        IFluidHandler dest = capabilityFluid(destPos, Direction.UP);
        move(source, dest);
    }

    private void fillAttached(Direction facing) {
        IFluidHandler dest = neighborFluid(facing, facing.getOpposite());
        IFluidHandler source = neighborFluid(Direction.UP, Direction.DOWN);
        move(source, dest);
    }

    private void pushExtender() {
        Direction output = getBlockState().getValue(MteInPlaceBlock.FACING);
        Direction input = output.getOpposite();
        move(neighborFluid(input, output), neighborFluid(output, input));
        if (spec().kind() == MteInPlaceKind.TANK_BRIDGE) {
            move(neighborFluid(output, input), neighborFluid(input, output));
        }
    }

    private void updateSteamFormed() {
        SteamTurbineCatalog.Profile profile =
                SteamTurbineCatalog.find(spec().id()).orElse(null);
        if (profile == null || !profile.large()) {
            formed = true;
            return;
        }
        Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
        formed = SteamTurbineStructure.check(
                level, worldPosition, facing, profile.wallId());
    }

    private void consumeSteam() {
        SteamTurbineCatalog.Profile profile =
                SteamTurbineCatalog.find(spec().id()).orElse(null);
        if (profile == null || (profile.large() && !formed)) {
            return;
        }
        Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
        Direction inputSide = facing.getOpposite();
        IFluidHandler source = neighborFluid(inputSide, facing);
        if (source != null && tank.getSpace() > 0) {
            FluidStack pulled = source.drain(
                    new FluidStack(
                            ModFluids.STEAM_SOURCE.get(),
                            Math.min(profile.steamInputMax(), tank.getSpace())),
                    IFluidHandler.FluidAction.EXECUTE);
            if (!pulled.isEmpty()) {
                tank.fill(pulled, IFluidHandler.FluidAction.EXECUTE);
            }
        }
        int available = tank.getFluidAmount();
        if (available < profile.steamPerEu() * 2
                || storedEnergy >= energyCapacity) {
            return;
        }
        FluidStack drained = tank.drain(
                available, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return;
        }
        long ru = drained.getAmount() / (long) profile.steamPerEu();
        storedEnergy = Math.min(energyCapacity, storedEnergy + ru);
        steamCounter += drained.getAmount();
        if (steamCounter >= profile.steamPerWater()) {
            int water = (int) (steamCounter / profile.steamPerWater());
            steamCounter %= profile.steamPerWater();
            ModFluids.chemical("water_distilled").ifPresent(entry ->
                    distilled.fill(
                            new FluidStack(entry.source().get(), water),
                            IFluidHandler.FluidAction.EXECUTE));
        }
        setChanged();
    }

    private void pushDrive() {
        Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
        if (!(level.getBlockEntity(worldPosition.relative(facing))
                instanceof MteInPlaceBlockEntity other)
                || !other.spec().kind().drive()
                || storedEnergy <= 0L) {
            return;
        }
        long moved = other.insert(
                EnergyType.KINETIC_ROTATION,
                1L,
                storedEnergy,
                facing.getOpposite(),
                false);
        if (moved > 0L) {
            storedEnergy -= moved;
            setChanged();
        }
    }

    private void move(IFluidHandler source, IFluidHandler dest) {
        if (source == null || dest == null) {
            return;
        }
        FluidStack drained = source.drain(TRANSFER_MB, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) {
            return;
        }
        int filled = dest.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            source.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            setChanged();
        }
    }

    private IFluidHandler neighborFluid(Direction step, Direction access) {
        if (level == null) {
            return null;
        }
        return capabilityFluid(worldPosition.relative(step), access);
    }

    private IFluidHandler capabilityFluid(BlockPos pos, Direction access) {
        if (level == null) {
            return null;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, access);
    }

    private static MteInPlaceSpec specOf(BlockState state) {
        if (!(state.getBlock() instanceof MteInPlaceBlock block)) {
            throw new IllegalStateException(
                    "In-place MTE entity bound to " + state.getBlock());
        }
        return block.spec();
    }

    private final class ExtenderHandler implements IFluidHandler {
        private final Direction side;

        private ExtenderHandler(Direction side) {
            this.side = side;
        }

        private IFluidHandler target() {
            Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
            Direction step = side == facing ? facing.getOpposite() : facing;
            return neighborFluid(step, step.getOpposite());
        }

        @Override
        public int getTanks() {
            IFluidHandler target = target();
            return target == null ? 0 : target.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            IFluidHandler target = target();
            return target == null ? FluidStack.EMPTY : target.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            IFluidHandler target = target();
            return target == null ? 0 : target.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            IFluidHandler target = target();
            return target != null && target.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            IFluidHandler target = target();
            return target == null ? 0 : target.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            IFluidHandler target = target();
            return target == null ? FluidStack.EMPTY : target.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            IFluidHandler target = target();
            return target == null ? FluidStack.EMPTY : target.drain(maxDrain, action);
        }
    }

    private final class SteamHandler implements IFluidHandler {
        private SteamHandler(Direction side) {
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            if (index == 0) {
                return tank.getFluid();
            }
            return index == 1 ? distilled.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int index) {
            if (index == 0) {
                return tank.getCapacity();
            }
            return index == 1 ? distilled.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return index == 0
                    && !stack.isEmpty()
                    && stack.is(ModFluids.STEAM_SOURCE.get());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !resource.is(ModFluids.STEAM_SOURCE.get())) {
                return 0;
            }
            return tank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return distilled.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return distilled.drain(maxDrain, action);
        }
    }
}
