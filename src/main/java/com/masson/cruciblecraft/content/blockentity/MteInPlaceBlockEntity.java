package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.mold.CruciblePour;
import com.masson.cruciblecraft.content.mold.MoldCastingRules;
import com.masson.cruciblecraft.content.mold.MoldHost;
import com.masson.cruciblecraft.content.menu.StorageMenu;
import com.masson.cruciblecraft.content.mte.MteFoundryTanks;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.storage.MassStorageFace;
import com.masson.cruciblecraft.content.storage.MassStorageHandler;
import com.masson.cruciblecraft.content.storage.MassStorageSidedHandler;
import com.masson.cruciblecraft.content.storage.StorageClientSync;
import com.masson.cruciblecraft.content.storage.StorageFilters;
import com.masson.cruciblecraft.energy.steam.SteamTurbineCatalog;
import com.masson.cruciblecraft.energy.steam.SteamTurbineStructure;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Shared host for in-place GT6 MTE families. Kind-specific transfer is not a
 * pipe cover, KU axle, vanilla tool, or the ceramic crucible.
 */
public final class MteInPlaceBlockEntity extends BlockEntity
        implements IEnergyHandler, MoldHost, MenuProvider {
    public static final int TRANSFER_MB = 1000;
    public static final long ENERGY_CAPACITY = 16_384L;
    public static final int MASS_CAPACITY = 1_000_000;
    private static final int AUTO_OUTPUT = 1;
    private static final int RESET_FILTER = 2;
    private static final int EMIT_OVERFLOW = 4;
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.FEET,
            EquipmentSlot.LEGS,
            EquipmentSlot.CHEST,
            EquipmentSlot.HEAD
    };

    private final ItemStackHandler items;
    private final MassStorageHandler massStorage;
    private final FluidTank tank;
    private final FluidTank distilled;
    private final long energyCapacity;
    private long storedEnergy;
    private long steamCounter;
    private boolean formed;
    private int massMode;
    private boolean massInventoryChanged;
    private int drawerCompartment;
    private int usingPlayers;
    private float lidAngle;
    private float oldLidAngle;

    public MteInPlaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MTE_INPLACE.get(), pos, state);
        MteInPlaceSpec spec = specOf(state);
        int slots = Math.max(1, spec.kind().slots());
        this.items = new ItemStackHandler(slots) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return switch (spec().kind()) {
                    case BOOKSHELF -> StorageFilters.book(stack);
                    case BOTTLE_CRATE -> StorageFilters.bottle(stack);
                    default -> true;
                };
            }

            @Override
            protected void onContentsChanged(int slot) {
                MteInPlaceBlockEntity.this.setChanged();
                MteInPlaceKind kind = spec().kind();
                if (kind == MteInPlaceKind.BOOKSHELF
                        || kind == MteInPlaceKind.BOTTLE_CRATE
                        || kind == MteInPlaceKind.MASS_STORAGE) {
                    StorageClientSync.send(MteInPlaceBlockEntity.this);
                }
            }
        };
        this.massStorage = spec.kind() == MteInPlaceKind.MASS_STORAGE
                ? new MassStorageHandler(MASS_CAPACITY, () -> {
                    massInventoryChanged = true;
                    setChanged();
                    StorageClientSync.send(this);
                })
                : null;
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

    public MassStorageHandler massStorage() {
        return massStorage;
    }

    public int drawerCompartment() {
        return drawerCompartment;
    }

    public void setDrawerCompartment(int compartment) {
        if (compartment < 0 || compartment >= DrawerBlockEntity.COMPARTMENTS) {
            throw new IllegalArgumentException("drawer compartment " + compartment);
        }
        this.drawerCompartment = compartment;
    }

    public boolean stillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5) <= 64.0;
    }

    public void startOpen(Player player) {
        if (spec().kind() != MteInPlaceKind.CHEST
                || player.isSpectator()
                || level == null
                || level.isClientSide) {
            return;
        }
        usingPlayers = Mth.clamp(usingPlayers + 1, 0, 127);
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, usingPlayers);
    }

    public void stopOpen(Player player) {
        if (spec().kind() != MteInPlaceKind.CHEST
                || player.isSpectator()
                || level == null
                || level.isClientSide) {
            return;
        }
        usingPlayers = Mth.clamp(usingPlayers - 1, 0, 127);
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, usingPlayers);
    }

    public float lidOpenness(float partialTick) {
        return oldLidAngle + (lidAngle - oldLidAngle) * partialTick;
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            usingPlayers = Mth.clamp(type, 0, 127);
            return true;
        }
        return super.triggerEvent(id, type);
    }

    public static void clientTick(
            Level level,
            BlockPos pos,
            BlockState state,
            MteInPlaceBlockEntity host) {
        if (host.spec().kind() != MteInPlaceKind.CHEST) {
            return;
        }
        host.oldLidAngle = host.lidAngle;
        if (host.usingPlayers > 0) {
            host.lidAngle = Math.min(1.0F, host.lidAngle + 0.1F);
            if (host.lidAngle > 0.1F && host.oldLidAngle <= 0.1F) {
                level.playLocalSound(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        SoundEvents.CHEST_OPEN,
                        SoundSource.BLOCKS,
                        0.5F,
                        level.random.nextFloat() * 0.1F + 0.9F,
                        false);
            }
        } else {
            host.lidAngle = Math.max(0.0F, host.lidAngle - 0.1F);
            if (host.lidAngle < 0.5F && host.oldLidAngle >= 0.5F) {
                level.playLocalSound(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        SoundEvents.CHEST_CLOSE,
                        SoundSource.BLOCKS,
                        0.5F,
                        level.random.nextFloat() * 0.1F + 0.9F,
                        false);
            }
        }
    }

    public float enchantPower() {
        float points = 0.0F;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            points += StorageFilters.enchantedBook(stack) ? 2.0F : 1.0F;
        }
        return points / 12.0F;
    }

    public void swapArmor(Player player) {
        for (int slot = 0; slot < ARMOR.length; slot++) {
            ItemStack stored = items.getStackInSlot(slot);
            ItemStack worn = player.getItemBySlot(ARMOR[slot]);
            items.setStackInSlot(slot, worn.copy());
            player.setItemSlot(ARMOR[slot], stored.copy());
        }
        setChanged();
    }

    public boolean massStorageActivated(
            Player player, ItemStack held, BlockHitResult hit) {
        if (massStorage == null) {
            return false;
        }
        Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
        boolean used = MassStorageFace.onActivated(
                massStorage,
                player,
                held,
                hit,
                facing,
                worldPosition,
                stack -> MassStorageFace.ejectInFront(
                        level, worldPosition, facing, stack));
        if (used) {
            setChanged();
        }
        return used;
    }

    public void giveMassToPlayer(Player player) {
        if (massStorage == null) {
            return;
        }
        MassStorageFace.giveToPlayer(massStorage, player);
        setChanged();
    }

    public void dumpMassInFront() {
        if (massStorage == null) {
            return;
        }
        Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
        MassStorageFace.dumpInFront(
                massStorage,
                stack -> MassStorageFace.ejectInFront(
                        level, worldPosition, facing, stack));
        setChanged();
    }

    public void clearMassContents() {
        if (massStorage == null) {
            return;
        }
        massStorage.clearAll();
        setChanged();
    }

    public boolean autoOutput() {
        return (massMode & AUTO_OUTPUT) != 0;
    }

    public boolean emitOverflow() {
        return (massMode & EMIT_OVERFLOW) != 0;
    }

    public boolean resetFilterWhenEmpty() {
        return (massMode & RESET_FILTER) != 0;
    }

    public void toggleAutoOutput() {
        massMode ^= AUTO_OUTPUT;
        setChanged();
    }

    public void toggleResetFilterWhenEmpty() {
        massMode ^= RESET_FILTER;
        if (massStorage != null) {
            massStorage.setKeepFilterWhenEmpty(!resetFilterWhenEmpty());
        }
        setChanged();
    }

    public void toggleOverflow() {
        massMode ^= EMIT_OVERFLOW;
        if (massStorage != null) {
            massStorage.setOverflowBonus(
                    emitOverflow() ? MassStorageHandler.OVERFLOW_BONUS : 0);
        }
        setChanged();
    }

    public Component autoOutputMessage() {
        return Component.translatable(
                autoOutput()
                        ? "message.cruciblecraft.mass_storage.auto_output_on"
                        : "message.cruciblecraft.mass_storage.auto_output_off");
    }

    public Component filterMessage() {
        return Component.translatable(
                resetFilterWhenEmpty()
                        ? "message.cruciblecraft.mass_storage.filter_reset"
                        : "message.cruciblecraft.mass_storage.filter_stay");
    }

    public Component overflowMessage() {
        return Component.translatable(
                emitOverflow()
                        ? "message.cruciblecraft.mass_storage.overflow_on"
                        : "message.cruciblecraft.mass_storage.overflow_off");
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int id, Inventory playerInventory, Player player) {
        MteInPlaceKind kind = spec().kind();
        if (!kind.playerInventoryGui()) {
            return null;
        }
        int visible = kind == MteInPlaceKind.DRAWER
                ? DrawerBlockEntity.COMPARTMENT_SLOTS
                : kind.slots();
        int offset = kind == MteInPlaceKind.DRAWER
                ? drawerCompartment * DrawerBlockEntity.COMPARTMENT_SLOTS
                : 0;
        return new StorageMenu(
                ModMenus.MTE_STORAGE.get(),
                id,
                playerInventory,
                this,
                visible,
                offset);
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
        if (host.spec().kind() == MteInPlaceKind.MASS_STORAGE) {
            host.tickMassStorage(level, pos);
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

    public IItemHandler itemHandler(Direction side) {
        if (massStorage != null) {
            return new MassStorageSidedHandler(
                    massStorage, autoOutput() && side == Direction.DOWN);
        }
        return spec().kind().inventory() ? items : null;
    }

    public void dropContents() {
        if (level == null || !spec().kind().inventory()) {
            return;
        }
        if (massStorage != null) {
            MassStorageFace.dropContents(massStorage, level, worldPosition);
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
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        if (packet.getTag() != null) {
            handleUpdateTag(packet.getTag(), registries);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (massStorage != null) {
            if (!massStorage.filter().isEmpty()) {
                tag.put("filter", massStorage.filter().save(registries));
            }
            tag.putInt("stored", massStorage.stored());
            tag.putLong("partial", massStorage.partialUnits());
            tag.putByte("mode", (byte) massMode);
        } else {
            tag.put("inventory", items.serializeNBT(registries));
            tag.putInt("compartment", drawerCompartment);
        }
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putLong("StoredEnergy", storedEnergy);
        tag.putLong("SteamCounter", steamCounter);
        tag.putBoolean("Formed", formed);
        tag.put("distilled", distilled.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (massStorage != null) {
            loadMassStorage(tag, registries);
        } else if (tag.contains("inventory")) {
            loadClampedInventory(tag.getCompound("inventory"), registries);
            if (tag.contains("compartment")) {
                drawerCompartment = Math.floorMod(
                        tag.getInt("compartment"), DrawerBlockEntity.COMPARTMENTS);
            }
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

    private void tickMassStorage(Level level, BlockPos pos) {
        if (massStorage == null) {
            return;
        }
        massStorage.convertPartials();
        boolean pulse = massInventoryChanged || level.getGameTime() % 100L == 0L;
        massInventoryChanged = false;
        if (!pulse) {
            return;
        }
        if (autoOutput() && massStorage.stored() > 0) {
            MassStorageFace.pushBelow(massStorage, level, pos);
        } else if (emitOverflow()
                && massStorage.stored() > massStorage.capacity()) {
            MassStorageFace.emitOverflowBelow(massStorage, level, pos);
        }
    }

    private void loadMassStorage(CompoundTag tag, HolderLookup.Provider registries) {
        massMode = tag.getByte("mode");
        massStorage.setKeepFilterWhenEmpty(!resetFilterWhenEmpty());
        massStorage.setOverflowBonus(
                emitOverflow() ? MassStorageHandler.OVERFLOW_BONUS : 0);
        if (tag.contains("stored") || tag.contains("filter")) {
            ItemStack filter = tag.contains("filter")
                    ? ItemStack.parseOptional(registries, tag.getCompound("filter"))
                    : ItemStack.EMPTY;
            massStorage.load(filter, tag.getInt("stored"), tag.getLong("partial"));
            return;
        }
        if (!tag.contains("inventory")) {
            return;
        }
        ItemStackHandler loaded = new ItemStackHandler();
        loaded.deserializeNBT(registries, tag.getCompound("inventory"));
        if (loaded.getSlots() <= 0) {
            return;
        }
        ItemStack stack = loaded.getStackInSlot(0);
        if (!stack.isEmpty()) {
            massStorage.load(stack.copyWithCount(1), stack.getCount());
        }
    }

    private void loadClampedInventory(
            CompoundTag nbt, HolderLookup.Provider registries) {
        ItemStackHandler loaded = new ItemStackHandler();
        loaded.deserializeNBT(registries, nbt);
        int n = Math.min(items.getSlots(), loaded.getSlots());
        for (int slot = 0; slot < n; slot++) {
            items.setStackInSlot(slot, loaded.getStackInSlot(slot));
        }
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
