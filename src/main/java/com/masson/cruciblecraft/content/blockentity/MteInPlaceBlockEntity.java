package com.masson.cruciblecraft.content.blockentity;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.api.fluid.LongFluidHandler;
import com.masson.cruciblecraft.content.block.LargeBoilerWallParts;
import com.masson.cruciblecraft.content.block.LargeCrucibleHosts;
import com.masson.cruciblecraft.content.block.LargeCrucibleWalls;
import com.masson.cruciblecraft.content.block.ImplosionCompressorWalls;
import com.masson.cruciblecraft.content.block.StainlessSteelMixerWalls;
import com.masson.cruciblecraft.content.block.CrusherWheels;
import com.masson.cruciblecraft.content.block.ShredderBlades;
import com.masson.cruciblecraft.content.block.AutoclaveWalls;
import com.masson.cruciblecraft.content.block.DenseLeadPorts;
import com.masson.cruciblecraft.content.block.ElectrolyzerParts;
import com.masson.cruciblecraft.content.block.GalvanizedGraaggWalls;
import com.masson.cruciblecraft.content.block.InvarOvenWalls;
import com.masson.cruciblecraft.content.block.TungstensteelCrusherWalls;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.TankWallParts;
import com.masson.cruciblecraft.content.multiblock.MultiblockPort;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.content.mold.CruciblePour;
import com.masson.cruciblecraft.content.mold.MoldCastingRules;
import com.masson.cruciblecraft.content.mold.MoldHost;
import com.masson.cruciblecraft.content.menu.StorageMenu;
import com.masson.cruciblecraft.content.mte.BathingPotRuntime;
import com.masson.cruciblecraft.content.mte.MteFoundryTanks;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.storage.MassStorageFace;
import com.masson.cruciblecraft.content.storage.MassStorageHandler;
import com.masson.cruciblecraft.content.storage.MassStorageSidedHandler;
import com.masson.cruciblecraft.content.storage.StorageClientSync;
import com.masson.cruciblecraft.content.storage.StorageFilters;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.energy.drive.RotationEngineCatalog;
import com.masson.cruciblecraft.energy.drive.RotationEngineConversion;
import com.masson.cruciblecraft.energy.largedynamo.LargeDynamoBlockEntity;
import com.masson.cruciblecraft.energy.largegasturbine.LargeGasTurbineBlockEntity;
import com.masson.cruciblecraft.energy.largegasturbine.LargeTurbineHatchRole;
import com.masson.cruciblecraft.energy.steam.SteamTurbineCatalog;
import com.masson.cruciblecraft.energy.steam.SteamTurbineConversion;
import com.masson.cruciblecraft.energy.steam.SteamTurbineHatchRole;
import com.masson.cruciblecraft.energy.steam.SteamTurbineHatches;
import com.masson.cruciblecraft.energy.steam.SteamTurbinePresentation;
import com.masson.cruciblecraft.energy.steam.SteamTurbineStructure;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModMenus;
import com.masson.cruciblecraft.steam.ExactFluidTransfer;
import com.masson.cruciblecraft.steam.MachineSideRules;
import com.masson.cruciblecraft.steam.SteamConversion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
public final class MteInPlaceBlockEntity extends MachineCoverHostBlockEntity
        implements IEnergyHandler, MoldHost, MenuProvider, MultiblockPort {
    public static final int TRANSFER_MB = 1000;
    public static final long ENERGY_CAPACITY = 16_384L;
    public static final int MASS_CAPACITY = 1_000_000;
    public static final int BARREL_CAPACITY = 10_000;
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
    private final BathingPotRuntime bathingPot;
    private final MassStorageHandler massStorage;
    private final FluidTank tank;
    private final FluidTank distilled;
    private final long energyCapacity;
    private final RotationEngineCatalog.Profile rotationEngine;
    private long storedEnergy;
    private long steamCounter;
    private long energyProducedNextTick;
    private boolean formed;
    private boolean stopped;
    private boolean overcharged;
    private boolean steamActivity;
    private boolean steamFast;
    private boolean steamCounterClockwise;
    private int steamExplosionPrevention;
    private int massMode;
    private boolean massInventoryChanged;
    private int drawerCompartment;
    private int usingPlayers;
    private float lidAngle;
    private float oldLidAngle;
    private BlockPos gasTurbineHost;
    private LargeTurbineHatchRole gasTurbineRole;
    private Direction gasTurbineOutward = Direction.NORTH;
    private BlockPos largeDynamoHost;
    private Direction largeDynamoOutward = Direction.NORTH;
    private BlockPos steamTurbineHost;
    private SteamTurbineHatchRole steamTurbineRole;
    private Direction steamTurbineOutward = Direction.NORTH;
    private BlockPos mixerController;
    private ResourceLocation mixerStructure;
    private PortType mixerPortType;
    private final Set<BlockPos> boundSteamHatches = new LinkedHashSet<>();
    private final IFluidHandler steamFillView = new SteamFillView();
    private final IFluidHandler steamDrainView = new SteamDrainView();
    private final IFluidHandler steamIoView = new SteamIoView();

    public MteInPlaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MTE_INPLACE.get(), pos, state);
        MteInPlaceSpec spec = specOf(state);
        this.bathingPot = BathingPotRuntime.hosts(spec)
                ? new BathingPotRuntime(spec, this::setChanged)
                : null;
        int slots = Math.max(1, spec.kind().slots());
        this.items = bathingPot != null
                ? bathingPot.items()
                : new ItemStackHandler(slots) {
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
                        || kind.massStorage()) {
                    StorageClientSync.send(MteInPlaceBlockEntity.this);
                }
            }
        };
        this.massStorage = spec.kind().massStorage()
                ? new MassStorageHandler(massStorageCapacity(spec.kind()), () -> {
                    massInventoryChanged = true;
                    setChanged();
                    StorageClientSync.send(this);
                })
                : null;
        SteamTurbineCatalog.Profile turbine =
                spec.kind() == MteInPlaceKind.STEAM_TURBINE
                        ? SteamTurbineCatalog.find(spec.id()).orElse(null)
                        : null;
        this.rotationEngine = spec.kind().rotationEngine()
                ? RotationEngineCatalog.require(spec.id())
                : null;
        int tankCap = turbine != null ? turbine.tankCapacityMb() : 1;
        this.tank = new FluidTank(tankCap, stack -> turbine == null
                || stack.is(ModFluids.STEAM_SOURCE.get())) {
            @Override
            protected void onContentsChanged() {
                MteInPlaceBlockEntity.this.setChanged();
            }
        };
        this.distilled = new FluidTank(
                Math.max(1, tankCap), SteamConversion::isDistilledWater) {
            @Override
            protected void onContentsChanged() {
                MteInPlaceBlockEntity.this.setChanged();
            }
        };
        this.energyCapacity = rotationEngine != null
                ? rotationEngine.capacity()
                : turbine != null ? turbine.energyCapacity() : ENERGY_CAPACITY;
    }

    public MteInPlaceSpec spec() {
        return specOf(getBlockState());
    }

    public void bindGasTurbine(
            BlockPos controller,
            LargeTurbineHatchRole role,
            Direction outward) {
        BlockPos immutable = controller.immutable();
        Direction face = outward == null ? Direction.NORTH : outward;
        if (!immutable.equals(gasTurbineHost)
                || gasTurbineRole != role
                || gasTurbineOutward != face) {
            gasTurbineHost = immutable;
            gasTurbineRole = role;
            gasTurbineOutward = face;
            setChanged();
            invalidateGasTurbineCaps();
        }
    }

    public void unbindGasTurbine() {
        if (gasTurbineHost != null || gasTurbineRole != null) {
            gasTurbineHost = null;
            gasTurbineRole = null;
            setChanged();
            invalidateGasTurbineCaps();
        }
    }

    public LargeTurbineHatchRole gasTurbineRole() {
        return gasTurbineRole;
    }

    public LargeGasTurbineBlockEntity boundGasTurbine() {
        if (level == null || gasTurbineHost == null || gasTurbineRole == null) {
            return null;
        }
        if (level.getBlockEntity(gasTurbineHost)
                instanceof LargeGasTurbineBlockEntity turbine
                && turbine.formed()) {
            return turbine;
        }
        return null;
    }

    public boolean forwardsGasTurbineEnergy(Direction side) {
        return gasTurbineRole != null
                && gasTurbineRole.energyOut()
                && boundGasTurbine() != null
                && (side == null || side == gasTurbineOutward);
    }

    public IFluidHandler gasTurbineFluids() {
        if (gasTurbineRole == null || !(gasTurbineRole.fill() || gasTurbineRole.drain())) {
            return null;
        }
        LargeGasTurbineBlockEntity host = boundGasTurbine();
        return host == null ? null : host.hatchFluids(gasTurbineRole);
    }

    public void bindLargeDynamo(BlockPos controller, Direction outward) {
        BlockPos immutable = controller.immutable();
        Direction face = outward == null ? Direction.NORTH : outward;
        if (!immutable.equals(largeDynamoHost) || largeDynamoOutward != face) {
            largeDynamoHost = immutable;
            largeDynamoOutward = face;
            setChanged();
            invalidateGasTurbineCaps();
        }
    }

    public void unbindLargeDynamo() {
        if (largeDynamoHost != null) {
            largeDynamoHost = null;
            setChanged();
            invalidateGasTurbineCaps();
        }
    }

    public LargeDynamoBlockEntity boundLargeDynamo() {
        if (level == null || largeDynamoHost == null) {
            return null;
        }
        if (level.getBlockEntity(largeDynamoHost)
                instanceof LargeDynamoBlockEntity dynamo
                && dynamo.formed()) {
            return dynamo;
        }
        return null;
    }

    public boolean forwardsLargeDynamoEnergy(Direction side) {
        return largeDynamoHost != null
                && boundLargeDynamo() != null
                && (side == null || side == largeDynamoOutward);
    }

    public void bindSteamTurbine(
            BlockPos controller,
            SteamTurbineHatchRole role,
            Direction outward) {
        BlockPos immutable = controller.immutable();
        Direction face = outward == null ? Direction.NORTH : outward;
        if (!immutable.equals(steamTurbineHost)
                || steamTurbineRole != role
                || steamTurbineOutward != face) {
            steamTurbineHost = immutable;
            steamTurbineRole = role;
            steamTurbineOutward = face;
            setChanged();
            invalidateGasTurbineCaps();
        }
    }

    public void unbindSteamTurbine() {
        if (steamTurbineHost != null || steamTurbineRole != null) {
            steamTurbineHost = null;
            steamTurbineRole = null;
            setChanged();
            invalidateGasTurbineCaps();
        }
    }

    public SteamTurbineHatchRole steamTurbineRole() {
        return steamTurbineRole;
    }

    public MteInPlaceBlockEntity boundSteamTurbine() {
        if (level == null || steamTurbineHost == null || steamTurbineRole == null) {
            return null;
        }
        if (level.getBlockEntity(steamTurbineHost)
                instanceof MteInPlaceBlockEntity turbine
                && turbine.spec().kind() == MteInPlaceKind.STEAM_TURBINE
                && turbine.formed()) {
            return turbine;
        }
        return null;
    }

    public boolean forwardsSteamTurbineEnergy(Direction side) {
        return steamTurbineRole != null
                && steamTurbineRole.energyOut()
                && boundSteamTurbine() != null
                && (side == null || side == steamTurbineOutward);
    }

    public IFluidHandler steamTurbineFluids() {
        if (steamTurbineRole == null
                || !(steamTurbineRole.fill() || steamTurbineRole.drain())) {
            return null;
        }
        MteInPlaceBlockEntity host = boundSteamTurbine();
        return host == null ? null : host.hatchFluids(steamTurbineRole);
    }

    public IFluidHandler hatchFluids(SteamTurbineHatchRole role) {
        if (role == null) {
            return null;
        }
        return switch (role) {
            case FLUID_IN -> steamFillView;
            case FLUID_OUT -> steamDrainView;
            case FLUID -> steamIoView;
            case ENERGY_OUT, NOTHING -> null;
        };
    }

    @Override
    public void setRemoved() {
        unbindSteamHatches();
        unbindSteamTurbine();
        super.setRemoved();
    }

    private void invalidateGasTurbineCaps() {
        if (level != null && !level.isClientSide) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    public ItemStackHandler items() {
        return items;
    }

    public BathingPotRuntime bathingPot() {
        return bathingPot;
    }

    public MassStorageHandler massStorage() {
        return massStorage;
    }

    private static int massStorageCapacity(MteInPlaceKind kind) {
        return kind == MteInPlaceKind.BARREL ? BARREL_CAPACITY : MASS_CAPACITY;
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

    public boolean rotationEngineStopped() {
        return stopped;
    }

    public boolean rotationEngineOvercharged() {
        return overcharged;
    }

    public boolean toggleRotationEngineStopped() {
        if (rotationEngine == null) {
            return true;
        }
        stopped = !stopped;
        setChanged();
        return !stopped;
    }

    public boolean toggleSteamTurbineStopped() {
        if (spec().kind() != MteInPlaceKind.STEAM_TURBINE) {
            return true;
        }
        stopped = !stopped;
        setChanged();
        return !stopped;
    }

    public boolean toggleSteamCounterClockwise() {
        if (spec().kind() != MteInPlaceKind.STEAM_TURBINE) {
            return false;
        }
        steamCounterClockwise = !steamCounterClockwise;
        storedEnergy = 0L;
        setChanged();
        syncSteamVisuals();
        return steamCounterClockwise;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            MteInPlaceBlockEntity host) {
        host.tickCovers();
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
        if (host.spec().kind().driveTransmit()) {
            host.pushDrive();
        }
        if (host.rotationEngine != null) {
            host.convertRotationEngine();
        }
        if (host.spec().kind().massStorage()) {
            host.tickMassStorage(level, pos);
        }
        if (host.bathingPot != null) {
            host.bathingPot.serverTick(level, pos);
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
        if (bathingPot != null) {
            return bathingPot.fluidHandler(
                    getBlockState().getValue(MteInPlaceBlock.FACING), side);
        }
        if (spec().kind().extender()) {
            return new ExtenderHandler(side);
        }
        if (spec().kind() == MteInPlaceKind.STEAM_TURBINE) {
            return new SteamHandler(side);
        }
        if (gasTurbineRole != null) {
            return gasTurbineFluids();
        }
        if (steamTurbineRole != null) {
            return steamTurbineFluids();
        }
        IFluidHandler implosion = ImplosionCompressorWalls.fluids(this);
        if (implosion != null) {
            return implosion;
        }
        IFluidHandler autoclave = AutoclaveWalls.fluids(this);
        if (autoclave != null) {
            return autoclave;
        }
        IFluidHandler largeBoiler = LargeBoilerWallParts.fluids(this);
        if (largeBoiler != null) {
            return largeBoiler;
        }
        IFluidHandler tankWall = TankWallParts.fluids(this);
        if (tankWall != null) {
            return tankWall;
        }
        IFluidHandler mixer = StainlessSteelMixerWalls.fluids(this);
        if (mixer != null) {
            return mixer;
        }
        IFluidHandler electrolyzer = ElectrolyzerParts.fluids(this);
        if (electrolyzer != null) {
            return electrolyzer;
        }
        IFluidHandler oven = InvarOvenWalls.fluids(this);
        if (oven != null) {
            return oven;
        }
        IFluidHandler crusherWall = TungstensteelCrusherWalls.fluids(this);
        if (crusherWall != null) {
            return crusherWall;
        }
        IFluidHandler crusherWheel = CrusherWheels.fluids(this);
        if (crusherWheel != null) {
            return crusherWheel;
        }
        IFluidHandler shredderBlade = ShredderBlades.fluids(this);
        if (shredderBlade != null) {
            return shredderBlade;
        }
        IFluidHandler massfab = DenseLeadPorts.fluids(this);
        if (massfab != null) {
            return massfab;
        }
        return LargeCrucibleWalls.fluids(this);
    }

    public LongFluidHandler longFluidHandler(Direction side) {
        return LargeBoilerWallParts.longFluids(this);
    }

    public IItemHandler itemHandler(Direction side) {
        if (bathingPot != null) {
            return bathingPot.itemHandler(
                    getBlockState().getValue(MteInPlaceBlock.FACING), side);
        }
        if (massStorage != null) {
            return new MassStorageSidedHandler(
                    massStorage, autoOutput() && side == Direction.DOWN);
        }
        IItemHandler implosion = ImplosionCompressorWalls.items(this);
        if (implosion != null) {
            return implosion;
        }
        IItemHandler autoclave = AutoclaveWalls.items(this);
        if (autoclave != null) {
            return autoclave;
        }
        if (LargeBoilerWallParts.isWall(spec())) {
            return null;
        }
        if (TankWallParts.isWall(spec())) {
            return null;
        }
        if (spec().kind().inventory()) {
            return items;
        }
        if (gasTurbineRole != null) {
            return null;
        }
        if (steamTurbineRole != null) {
            return null;
        }
        IItemHandler mixer = StainlessSteelMixerWalls.items(this);
        if (mixer != null) {
            return mixer;
        }
        IItemHandler electrolyzer = ElectrolyzerParts.items(this);
        if (electrolyzer != null) {
            return electrolyzer;
        }
        IItemHandler oven = InvarOvenWalls.items(this);
        if (oven != null) {
            return oven;
        }
        IItemHandler crusherWall = TungstensteelCrusherWalls.items(this);
        if (crusherWall != null) {
            return crusherWall;
        }
        IItemHandler crusherWheel = CrusherWheels.items(this);
        if (crusherWheel != null) {
            return crusherWheel;
        }
        IItemHandler shredderBlade = ShredderBlades.items(this);
        if (shredderBlade != null) {
            return shredderBlade;
        }
        IItemHandler massfab = DenseLeadPorts.items(this);
        if (massfab != null) {
            return massfab;
        }
        return LargeCrucibleWalls.items(this);
    }

    public PortType mixerPortType() {
        return mixerPortType;
    }

    public Optional<BlockPos> mixerControllerPosition() {
        return Optional.ofNullable(mixerController);
    }

    public Optional<ResourceLocation> mixerStructureId() {
        return Optional.ofNullable(mixerStructure);
    }

    @Override
    public PortType portType() {
        if (mixerPortType != null) {
            return mixerPortType;
        }
        if (TankWallParts.isWall(spec())) {
            return TankWallParts.defaultType(spec());
        }
        if (LargeBoilerWallParts.isWall(spec())) {
            return LargeBoilerWallParts.defaultType(spec());
        }
        return PortType.ITEM_FLUID_IN;
    }

    @Override
    public boolean accepts(PortType type) {
        if (mixerPortType != null) {
            return mixerPortType == type;
        }
        return ImplosionCompressorWalls.accepts(spec(), type)
                || StainlessSteelMixerWalls.accepts(spec(), type)
                || ElectrolyzerParts.accepts(spec(), type)
                || InvarOvenWalls.accepts(spec(), type)
                || AutoclaveWalls.accepts(spec(), type)
                || TungstensteelCrusherWalls.accepts(spec(), type)
                || CrusherWheels.accepts(spec(), type)
                || ShredderBlades.accepts(spec(), type)
                || DenseLeadPorts.accepts(spec(), type)
                || LargeBoilerWallParts.accepts(spec(), type)
                || TankWallParts.accepts(spec(), type)
                || GalvanizedGraaggWalls.accepts(spec(), type);
    }

    @Override
    public void bind(BlockPos controller, ResourceLocation structureId) {
        bind(controller, structureId, portType());
    }

    @Override
    public void bind(
            BlockPos controller,
            ResourceLocation structureId,
            PortType type) {
        if (!StainlessSteelMixerWalls.isWall(spec())
                && !ElectrolyzerParts.isPart(spec())
                && !InvarOvenWalls.isWall(spec())
                && !AutoclaveWalls.isWall(spec())
                && !ImplosionCompressorWalls.isWall(spec())
                && !TungstensteelCrusherWalls.isWall(spec())
                && !CrusherWheels.isPart(spec())
                && !ShredderBlades.isPart(spec())
                && !DenseLeadPorts.isPort(spec())
                && !LargeBoilerWallParts.isWall(spec())
                && !TankWallParts.isWall(spec())
                && !GalvanizedGraaggWalls.isWall(spec())) {
            return;
        }
        BlockPos immutable = controller.immutable();
        boolean changed = !immutable.equals(mixerController)
                || !structureId.equals(mixerStructure)
                || mixerPortType != type;
        mixerController = immutable;
        mixerStructure = structureId;
        mixerPortType = type;
        if (changed) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.invalidateCapabilities(worldPosition);
            }
        }
    }

    /** Structure-only processing parts keep a controller pointer for walk damage. */
    public void bindStructureMember(
            BlockPos controller, ResourceLocation structureId) {
        if ((!CrusherWheels.isPart(spec())
                && !ShredderBlades.isPart(spec()))
                || mixerPortType != null) {
            return;
        }
        BlockPos immutable = controller.immutable();
        boolean changed = !immutable.equals(mixerController)
                || !structureId.equals(mixerStructure);
        mixerController = immutable;
        mixerStructure = structureId;
        if (changed) {
            setChanged();
        }
    }

    @Override
    public void unbind(BlockPos controller) {
        if (controller.equals(mixerController)) {
            mixerController = null;
            mixerStructure = null;
            mixerPortType = null;
            setChanged();
            if (level != null && !level.isClientSide) {
                level.invalidateCapabilities(worldPosition);
            }
        }
    }

    @Override
    public Optional<BlockPos> controllerPosition() {
        return Optional.ofNullable(mixerController);
    }

    @Override
    public Optional<ResourceLocation> structureId() {
        return Optional.ofNullable(mixerStructure);
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        if (bathingPot != null) {
            bathingPot.drop(level, worldPosition);
            return;
        }
        if (!spec().kind().inventory()) {
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
        if (forwardsLargeDynamoEnergy(side)) {
            return type == EnergyType.ELECTRIC;
        }
        if (gasTurbineRole != null) {
            return type == EnergyType.KINETIC_ROTATION
                    && forwardsGasTurbineEnergy(side);
        }
        if (steamTurbineRole != null) {
            return type == EnergyType.KINETIC_ROTATION
                    && forwardsSteamTurbineEnergy(side);
        }
        if (ImplosionCompressorWalls.hostReady(this)
                && mixerPortType == PortType.ITEM_FLUID_ENERGY) {
            return type == EnergyType.TIME;
        }
        if (StainlessSteelMixerWalls.forwardsEnergy(this)) {
            return type == EnergyType.KINETIC_ROTATION;
        }
        if (ElectrolyzerParts.forwardsEnergy(this)) {
            return type == EnergyType.ELECTRIC;
        }
        if (InvarOvenWalls.forwardsEnergy(this)) {
            return type == EnergyType.ELECTRIC;
        }
        if (AutoclaveWalls.forwardsEnergy(this)) {
            return type == EnergyType.TIME;
        }
        if (ImplosionCompressorWalls.forwardsEnergy(this)) {
            return type == EnergyType.TIME;
        }
        if (TungstensteelCrusherWalls.forwardsEnergy(this)) {
            return type == EnergyType.KINETIC_ROTATION;
        }
        if (DenseLeadPorts.forwardsEnergy(this)) {
            return type == EnergyType.QUANTUM;
        }
        if (GalvanizedGraaggWalls.forwardsEnergy(this)) {
            return type == EnergyType.ELECTRIC;
        }
        if (LargeCrucibleHosts.isWall(spec())) {
            LargeCrucibleBlockEntity host = LargeCrucibleWalls.controllerAt(
                    getLevel(), getBlockPos());
            return host != null
                    && LargeCrucibleWalls.forwardsEnergy(this)
                    && host.handles(type, side);
        }
        if (rotationEngine != null) {
            return type == EnergyType.KINETIC_ROTATION
                    && !stopped
                    && rotationInputSide(side);
        }
        if (spec().kind() == MteInPlaceKind.STEAM_TURBINE) {
            return type == EnergyType.KINETIC_ROTATION
                    && (side == null
                            || side == getBlockState().getValue(MteInPlaceBlock.FACING));
        }
        return spec().kind().energy() && type == spec().kind().energyType();
    }

    @Override
    public long stored(EnergyType type) {
        if (gasTurbineRole != null || steamTurbineRole != null) {
            return 0L;
        }
        if (rotationEngine != null
                || spec().kind() == MteInPlaceKind.STEAM_TURBINE) {
            return type == EnergyType.KINETIC_ROTATION ? storedEnergy : 0L;
        }
        if (StainlessSteelMixerWalls.forwardsEnergy(this)) {
            return StainlessSteelMixerWalls.storedEnergy(this, type);
        }
        if (ElectrolyzerParts.forwardsEnergy(this)) {
            return ElectrolyzerParts.storedEnergy(this, type);
        }
        if (InvarOvenWalls.forwardsEnergy(this)) {
            return InvarOvenWalls.storedEnergy(this, type);
        }
        if (AutoclaveWalls.forwardsEnergy(this)) {
            return AutoclaveWalls.storedEnergy(this, type);
        }
        if (ImplosionCompressorWalls.forwardsEnergy(this)) {
            return ImplosionCompressorWalls.storedEnergy(this, type);
        }
        if (TungstensteelCrusherWalls.forwardsEnergy(this)) {
            return TungstensteelCrusherWalls.storedEnergy(this, type);
        }
        if (DenseLeadPorts.forwardsEnergy(this)) {
            return DenseLeadPorts.storedEnergy(this, type);
        }
        if (GalvanizedGraaggWalls.forwardsEnergy(this)) {
            return GalvanizedGraaggWalls.storedEnergy(this, type);
        }
        return handles(type, Direction.NORTH) ? storedEnergy : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        if (gasTurbineRole != null || steamTurbineRole != null) {
            return 0L;
        }
        if (rotationEngine != null
                || spec().kind() == MteInPlaceKind.STEAM_TURBINE) {
            return type == EnergyType.KINETIC_ROTATION ? energyCapacity : 0L;
        }
        if (StainlessSteelMixerWalls.forwardsEnergy(this)) {
            return StainlessSteelMixerWalls.energyCapacity(this, type);
        }
        if (ElectrolyzerParts.forwardsEnergy(this)) {
            return ElectrolyzerParts.energyCapacity(this, type);
        }
        if (InvarOvenWalls.forwardsEnergy(this)) {
            return InvarOvenWalls.energyCapacity(this, type);
        }
        if (AutoclaveWalls.forwardsEnergy(this)) {
            return AutoclaveWalls.energyCapacity(this, type);
        }
        if (ImplosionCompressorWalls.forwardsEnergy(this)) {
            return ImplosionCompressorWalls.energyCapacity(this, type);
        }
        if (TungstensteelCrusherWalls.forwardsEnergy(this)) {
            return TungstensteelCrusherWalls.energyCapacity(this, type);
        }
        if (DenseLeadPorts.forwardsEnergy(this)) {
            return DenseLeadPorts.energyCapacity(this, type);
        }
        if (GalvanizedGraaggWalls.forwardsEnergy(this)) {
            return GalvanizedGraaggWalls.energyCapacity(this, type);
        }
        return handles(type, Direction.NORTH) ? energyCapacity : 0L;
    }

    @Override
    public boolean allowCover(Direction side) {
        Direction facing = getBlockState().hasProperty(MteInPlaceBlock.FACING)
                ? getBlockState().getValue(MteInPlaceBlock.FACING)
                : Direction.NORTH;
        return spec().kind().allowCover(facing, side);
    }

    @Override
    public boolean hasEnergyBuffer() {
        return spec().kind().energy();
    }

    @Override
    public long energyStored() {
        return storedEnergy;
    }

    @Override
    public long energyCapacity() {
        return energyCapacity;
    }

    @Override
    public boolean hasFluidTanks() {
        return spec().kind() == MteInPlaceKind.STEAM_TURBINE;
    }

    @Override
    public boolean runningPossible() {
        return spec().kind() == MteInPlaceKind.STEAM_TURBINE
                || rotationEngine != null;
    }

    @Override
    public boolean runningActively() {
        if (spec().kind() == MteInPlaceKind.STEAM_TURBINE) {
            return steamActivity;
        }
        return rotationEngine != null && !stopped && storedEnergy > 0L;
    }

    @Override
    public boolean switchableOnOff() {
        return spec().kind() == MteInPlaceKind.STEAM_TURBINE
                || rotationEngine != null;
    }

    @Override
    public boolean getStateOnOff() {
        return !stopped;
    }

    @Override
    public boolean setStateOnOff(boolean on) {
        if (stopped == !on) {
            return on;
        }
        stopped = !on;
        setChanged();
        return !stopped;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        if (forwardsLargeDynamoEnergy(side)) {
            LargeDynamoBlockEntity host = boundLargeDynamo();
            return host == null ? 0L : host.hatchOutputSize(type, side);
        }
        if (forwardsGasTurbineEnergy(side)) {
            LargeGasTurbineBlockEntity host = boundGasTurbine();
            return host == null ? 0L : host.hatchOutputSize(type, side);
        }
        if (type != EnergyType.KINETIC_ROTATION) {
            return 0L;
        }
        if (forwardsSteamTurbineEnergy(side)) {
            MteInPlaceBlockEntity host = boundSteamTurbine();
            return host == null ? 0L : host.ruPacketSize();
        }
        if (spec().kind() == MteInPlaceKind.STEAM_TURBINE
                && (side == null
                        || side == getBlockState().getValue(MteInPlaceBlock.FACING))) {
            return ruPacketSize();
        }
        if (!forwardsGasTurbineEnergy(side)) {
            return 0L;
        }
        LargeGasTurbineBlockEntity host = boundGasTurbine();
        return host == null ? 0L : host.hatchOutputSize(type, side);
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (gasTurbineRole != null
                || steamTurbineRole != null
                || largeDynamoHost != null
                || spec().kind() == MteInPlaceKind.STEAM_TURBINE) {
            return 0L;
        }
        if (ImplosionCompressorWalls.hostReady(this)
                && mixerPortType == PortType.ITEM_FLUID_ENERGY) {
            return ImplosionCompressorWalls.insertEnergy(
                    this, type, size, amount, simulate);
        }
        if (StainlessSteelMixerWalls.forwardsEnergy(this)) {
            return StainlessSteelMixerWalls.insertEnergy(
                    this, type, size, amount, simulate);
        }
        if (ElectrolyzerParts.forwardsEnergy(this)) {
            return ElectrolyzerParts.insertEnergy(
                    this, type, size, amount, simulate);
        }
        if (InvarOvenWalls.forwardsEnergy(this)) {
            return InvarOvenWalls.insertEnergy(
                    this, type, size, amount, simulate);
        }
        if (AutoclaveWalls.forwardsEnergy(this)) {
            return AutoclaveWalls.insertEnergy(
                    this, type, size, amount, simulate);
        }
        if (ImplosionCompressorWalls.forwardsEnergy(this)) {
            return ImplosionCompressorWalls.insertEnergy(
                    this, type, size, amount, simulate);
        }
        if (TungstensteelCrusherWalls.forwardsEnergy(this)) {
            return TungstensteelCrusherWalls.insertEnergy(
                    this, type, size, amount, simulate);
        }
        if (DenseLeadPorts.forwardsEnergy(this)) {
            return DenseLeadPorts.insertEnergy(
                    this, type, size, amount, simulate);
        }
        if (GalvanizedGraaggWalls.forwardsEnergy(this)) {
            return GalvanizedGraaggWalls.insertEnergy(
                    this, type, size, amount, simulate);
        }
        if (LargeCrucibleHosts.isWall(spec())) {
            return LargeCrucibleWalls.insertEnergy(
                    this, type, size, amount, side, simulate);
        }
        if (rotationEngine != null) {
            return insertRotationRu(type, size, amount, side, simulate);
        }
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
        if (forwardsLargeDynamoEnergy(side)) {
            if (maxAmount <= 0L) {
                return 0L;
            }
            LargeDynamoBlockEntity host = boundLargeDynamo();
            return host == null
                    ? 0L
                    : host.hatchExtract(type, size, maxAmount, simulate);
        }
        if (gasTurbineRole != null) {
            if (!forwardsGasTurbineEnergy(side) || maxAmount <= 0L) {
                return 0L;
            }
            LargeGasTurbineBlockEntity host = boundGasTurbine();
            return host == null
                    ? 0L
                    : host.hatchExtract(type, size, maxAmount, simulate);
        }
        if (steamTurbineRole != null
                || spec().kind() == MteInPlaceKind.STEAM_TURBINE) {
            return 0L;
        }
        if (rotationEngine != null) {
            return 0L;
        }
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
        if (bathingPot != null) {
            bathingPot.save(tag, registries);
        } else if (massStorage != null) {
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
        tag.putLong("EnergyProducedNextTick", energyProducedNextTick);
        tag.putBoolean("Formed", formed);
        tag.putBoolean("Stopped", stopped);
        tag.putBoolean("Overcharged", overcharged);
        tag.putBoolean("SteamActivity", steamActivity);
        tag.putBoolean("SteamFast", steamFast);
        tag.putBoolean("CounterClockwise", steamCounterClockwise);
        tag.putInt("ExplosionPrevention", steamExplosionPrevention);
        tag.put("distilled", distilled.writeToNBT(registries, new CompoundTag()));
        if (gasTurbineHost != null && gasTurbineRole != null) {
            tag.putLong("lgt.controller", gasTurbineHost.asLong());
            tag.putString("lgt.role", gasTurbineRole.name());
            tag.putString("lgt.outward", gasTurbineOutward.getSerializedName());
        }
        if (largeDynamoHost != null) {
            tag.putLong("ldy.controller", largeDynamoHost.asLong());
            tag.putString("ldy.outward", largeDynamoOutward.getSerializedName());
        }
        if (steamTurbineHost != null && steamTurbineRole != null) {
            tag.putLong("st.controller", steamTurbineHost.asLong());
            tag.putString("st.role", steamTurbineRole.name());
            tag.putString("st.outward", steamTurbineOutward.getSerializedName());
        }
        if (mixerController != null && mixerStructure != null
                && mixerPortType != null) {
            tag.putLong("mixer.controller", mixerController.asLong());
            tag.putString("mixer.structure", mixerStructure.toString());
            tag.putString("mixer.port", mixerPortType.serializedName());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (bathingPot != null) {
            bathingPot.load(tag, registries);
        } else if (massStorage != null) {
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
        energyProducedNextTick = tag.getLong("EnergyProducedNextTick");
        formed = tag.getBoolean("Formed");
        stopped = tag.getBoolean("Stopped");
        overcharged = tag.getBoolean("Overcharged");
        steamActivity = tag.getBoolean("SteamActivity");
        steamFast = tag.getBoolean("SteamFast");
        steamCounterClockwise = tag.getBoolean("CounterClockwise");
        steamExplosionPrevention = Math.max(0, tag.getInt("ExplosionPrevention"));
        if (tag.contains("distilled")) {
            distilled.readFromNBT(registries, tag.getCompound("distilled"));
        }
        if (tag.contains("lgt.controller") && tag.contains("lgt.role")) {
            gasTurbineHost = BlockPos.of(tag.getLong("lgt.controller"));
            try {
                gasTurbineRole = LargeTurbineHatchRole.valueOf(tag.getString("lgt.role"));
            } catch (IllegalArgumentException ignored) {
                gasTurbineRole = null;
                gasTurbineHost = null;
            }
            Direction parsed = Direction.byName(tag.getString("lgt.outward"));
            gasTurbineOutward = parsed == null ? Direction.NORTH : parsed;
        } else {
            gasTurbineHost = null;
            gasTurbineRole = null;
        }
        if (tag.contains("ldy.controller")) {
            largeDynamoHost = BlockPos.of(tag.getLong("ldy.controller"));
            Direction dynamoOut = Direction.byName(tag.getString("ldy.outward"));
            largeDynamoOutward = dynamoOut == null ? Direction.NORTH : dynamoOut;
        } else {
            largeDynamoHost = null;
        }
        if (tag.contains("st.controller") && tag.contains("st.role")) {
            steamTurbineHost = BlockPos.of(tag.getLong("st.controller"));
            try {
                steamTurbineRole = SteamTurbineHatchRole.valueOf(
                        tag.getString("st.role"));
            } catch (IllegalArgumentException ignored) {
                steamTurbineRole = null;
                steamTurbineHost = null;
            }
            Direction parsed = Direction.byName(tag.getString("st.outward"));
            steamTurbineOutward = parsed == null ? Direction.NORTH : parsed;
        } else {
            steamTurbineHost = null;
            steamTurbineRole = null;
        }
        mixerController = null;
        mixerStructure = null;
        mixerPortType = null;
        ResourceLocation mixerParsed = tag.contains("mixer.structure")
                ? ResourceLocation.tryParse(tag.getString("mixer.structure"))
                : null;
        if (tag.contains("mixer.controller") && mixerParsed != null) {
            mixerController = BlockPos.of(tag.getLong("mixer.controller"));
            mixerStructure = mixerParsed;
            String name = tag.getString("mixer.port");
            for (PortType type : PortType.values()) {
                if (type.serializedName().equals(name)) {
                    mixerPortType = type;
                    break;
                }
            }
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
        MoldHost mold = MoldHost.at(level, dest);
        if (mold != null) {
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
            level.setBlock(
                    worldPosition,
                    CrucibleWorldHazards.meltdownLavaState(),
                    Block.UPDATE_ALL);
            return 0;
        }
        BlockPos dest = faucetDestination();
        MoldHost mold = MoldHost.at(level, dest);
        if (mold != null) {
            return mold.fillMold(materialId, availableUnits, temperature, Direction.UP);
        }
        return 0;
    }

    @Override
    public ItemStack takeOutput(Player player, boolean causeDamage) {
        return ItemStack.EMPTY;
    }

    private void pourFaucet(Direction facing) {
        CruciblePour crucible = CruciblePour.at(level, worldPosition.relative(facing));
        if (crucible != null) {
            crucible.fillMoldAtSide(this, facing.getOpposite(), facing);
            return;
        }
        pourDown(facing);
    }

    private BlockPos faucetDestination() {
        BlockPos dest = worldPosition.below();
        while (level != null && dest.getY() > level.getMinBuildHeight()) {
            if (level.getBlockEntity(dest) instanceof MteInPlaceBlockEntity other
                    && other.spec().kind() == MteInPlaceKind.FAUCET) {
                dest = dest.below();
                continue;
            }
            if (MoldHost.at(level, dest) != null) {
                break;
            }
            if (level.getBlockState(dest).getCollisionShape(level, dest).isEmpty()) {
                dest = dest.below();
                continue;
            }
            break;
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
        if (!SteamTurbineStructure.aabbLoaded(level, worldPosition, facing)) {
            return;
        }
        boolean ok = SteamTurbineStructure.check(
                level, worldPosition, facing, profile.wallId());
        if (ok) {
            boolean becameFormed = !formed;
            formed = true;
            bindSteamHatches(facing);
            if (becameFormed) {
                setChanged();
                notifySteamClients();
            }
        } else if (formed) {
            formed = false;
            unbindSteamHatches();
            setChanged();
            notifySteamClients();
        } else {
            unbindSteamHatches();
        }
    }

    private void bindSteamHatches(Direction facing) {
        if (level == null) {
            return;
        }
        Set<BlockPos> desired = new LinkedHashSet<>();
        for (SteamTurbineHatches.Hatch hatch
                : SteamTurbineHatches.hatches(worldPosition, facing)) {
            desired.add(hatch.pos());
            bindSteamHatch(hatch.pos(), hatch.role(), facing.getOpposite());
        }
        for (BlockPos previous : List.copyOf(boundSteamHatches)) {
            if (!desired.contains(previous)) {
                unbindSteamHatch(previous);
            }
        }
    }

    private void unbindSteamHatches() {
        for (BlockPos previous : List.copyOf(boundSteamHatches)) {
            unbindSteamHatch(previous);
        }
    }

    private void bindSteamHatch(
            BlockPos pos, SteamTurbineHatchRole role, Direction outward) {
        if (level == null || !level.hasChunkAt(pos)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity wall
                && wall.spec().kind() != MteInPlaceKind.STEAM_TURBINE) {
            wall.bindSteamTurbine(worldPosition, role, outward);
            boundSteamHatches.add(pos.immutable());
        }
    }

    private void unbindSteamHatch(BlockPos pos) {
        boundSteamHatches.remove(pos);
        if (level != null
                && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity wall) {
            wall.unbindSteamTurbine();
        }
    }

    private void consumeSteam() {
        SteamTurbineCatalog.Profile profile =
                SteamTurbineCatalog.find(spec().id()).orElse(null);
        if (profile == null || (profile.large() && !formed)) {
            return;
        }
        if (energyProducedNextTick > 0L) {
            storedEnergy += energyProducedNextTick;
            energyProducedNextTick = 0L;
        } else if (!(profile.large() && stopped)
                && tank.getFluidAmount()
                        >= SteamTurbineConversion.conversionThresholdMb(profile)) {
            int steam = tank.getFluidAmount();
            tank.drain(steam, IFluidHandler.FluidAction.EXECUTE);
            steamCounter += steam;
            long ru = SteamTurbineConversion.ruFromSteam(steam, profile);
            storedEnergy += ru;
            energyProducedNextTick += ru;
            if (steamCounter >= profile.steamPerWater()) {
                int water = (int) (steamCounter / profile.steamPerWater());
                steamCounter %= profile.steamPerWater();
                recoverDistilled(profile, water);
            }
        }
        emitSteamRu(profile);
        setChanged();
    }

    private void recoverDistilled(SteamTurbineCatalog.Profile profile, int water) {
        FluidStack distilledWater = SteamConversion.distilledExhaust(water);
        if (distilledWater.isEmpty()) {
            return;
        }
        if (profile.large()) {
            distilled.fill(distilledWater, IFluidHandler.FluidAction.EXECUTE);
            return;
        }
        distilled.setFluid(distilledWater);
        pushDistilledToSides();
        distilled.setFluid(FluidStack.EMPTY);
    }

    /**
     * GT6 singles: DistW to {@code FACING_SIDES}, leftover {@code GarbageGT.trash}.
     */
    private void pushDistilledToSides() {
        if (level == null || level.isClientSide || distilled.isEmpty()) {
            return;
        }
        Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
        for (Direction side : Direction.values()) {
            if (distilled.isEmpty()) {
                break;
            }
            if (!MachineSideRules.engineExposesExhaust(facing, side)) {
                continue;
            }
            BlockPos target = worldPosition.relative(side);
            if (!level.hasChunkAt(target)) {
                continue;
            }
            IFluidHandler neighbor = level.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    target,
                    side.getOpposite());
            if (neighbor == null) {
                continue;
            }
            ExactFluidTransfer.move(
                    distilled, neighbor, distilled.getFluidAmount());
        }
    }

    private void emitSteamRu(SteamTurbineCatalog.Profile profile) {
        if (level == null || level.isClientSide) {
            return;
        }
        SteamTurbineConversion.Tick tick =
                SteamTurbineConversion.emit(storedEnergy, profile);
        if (tick.overloaded()) {
            storedEnergy = 0L;
            overcharged = true;
            steamActivity = false;
            steamFast = false;
            overloadSteam(tick.packetSize());
            syncSteamVisuals();
            return;
        }
        steamActivity = tick.canEmit();
        steamFast = tick.fast();
        if (tick.canEmit()) {
            Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
            BlockPos emitFrom = profile.large()
                    ? SteamTurbineStructure.energyOut(worldPosition, facing)
                    : worldPosition;
            Direction emitSide = profile.large() ? facing.getOpposite() : facing;
            EnergyEmitter.pushToSide(
                    level,
                    emitFrom,
                    EnergyType.KINETIC_ROTATION,
                    tick.packetSize(),
                    1L,
                    emitSide);
            if (level.getGameTime() % 20L == 0L) {
                level.playSound(
                        null,
                        worldPosition,
                        SoundEvents.MINECART_RIDING,
                        SoundSource.BLOCKS,
                        0.5F,
                        1.0F);
            }
        }
        storedEnergy = tick.storedAfterWaste();
        syncSteamVisuals();
    }

    /**
     * GT6 {@code TileEntityBase10EnergyConverter.overload}: count to 100,
     * then {@code overcharge(size, RU)}.
     */
    private void overloadSteam(long size) {
        if (steamExplosionPrevention
                < SteamTurbinePresentation.OVERLOAD_EXPLOSION_THRESHOLD) {
            steamExplosionPrevention++;
            return;
        }
        float strength =
                SteamTurbinePresentation.overchargeExplosionStrength(size);
        BlockPos pos = worldPosition;
        level.removeBlock(pos, false);
        if (strength >= 1.0F && level instanceof ServerLevel server) {
            server.explode(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    strength,
                    Level.ExplosionInteraction.TNT);
        }
    }

    private void syncSteamVisuals() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        BlockState next = state;
        if (state.hasProperty(MteInPlaceBlock.LIT)) {
            next = next.setValue(MteInPlaceBlock.LIT, steamActivity);
        }
        if (state.hasProperty(MteInPlaceBlock.FAST)) {
            next = next.setValue(MteInPlaceBlock.FAST, steamFast);
        }
        if (state.hasProperty(MteInPlaceBlock.COUNTERCLOCKWISE)) {
            next = next.setValue(
                    MteInPlaceBlock.COUNTERCLOCKWISE, steamCounterClockwise);
        }
        if (next != state) {
            level.setBlock(worldPosition, next, Block.UPDATE_CLIENTS);
        }
    }

    private void notifySteamClients() {
        if (level == null || level.isClientSide) {
            return;
        }
        level.sendBlockUpdated(
                worldPosition,
                getBlockState(),
                getBlockState(),
                Block.UPDATE_CLIENTS);
    }

    long ruPacketSize() {
        SteamTurbineCatalog.Profile profile =
                SteamTurbineCatalog.find(spec().id()).orElse(null);
        if (profile == null) {
            return 0L;
        }
        SteamTurbineConversion.Tick tick =
                SteamTurbineConversion.emit(storedEnergy, profile);
        return tick.canEmit() ? tick.packetSize() : 0L;
    }

    private boolean rotationInputSide(Direction side) {
        if (side == null) {
            return false;
        }
        Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
        return side.getAxis() != facing.getAxis();
    }

    /**
     * GT6 {@code TE_Behavior_Energy_Stats.doInject}: packet size is voltage,
     * amount is amperage. Oversize packets are consumed and overload.
     */
    private long insertRotationRu(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (type != EnergyType.KINETIC_ROTATION
                || !handles(type, side)
                || amount <= 0L) {
            return 0L;
        }
        long magnitude = EnergyPackets.magnitude(size);
        if (magnitude == 0L) {
            return 0L;
        }
        if (magnitude > rotationEngine.inputMax()) {
            if (!simulate) {
                overcharged = true;
                storedEnergy = 0L;
                setChanged();
            }
            return amount;
        }
        if (storedEnergy >= energyCapacity) {
            return 0L;
        }
        long room = energyCapacity - storedEnergy;
        long offered = EnergyPackets.units(size, amount);
        long tInput = Math.min(room, offered);
        long consumed = Math.min(
                amount,
                tInput / magnitude + (tInput % magnitude != 0L ? 1L : 0L));
        if (!simulate && consumed > 0L) {
            storedEnergy += EnergyPackets.units(size, consumed);
            setChanged();
        }
        return consumed;
    }

    /**
     * GT6 {@code MultiTileEntityEngineRotation.doConversion}: bipolar KU to
     * front/back, swapping sign every 16 ticks, then waste remaining RU.
     */
    private void convertRotationEngine() {
        if (level == null || level.isClientSide || storedEnergy <= 0L) {
            return;
        }
        RotationEngineConversion.Tick tick = RotationEngineConversion.tick(
                storedEnergy, rotationEngine, 0);
        if (tick.overloaded()) {
            storedEnergy = 0L;
            overcharged = true;
            setChanged();
            return;
        }
        boolean emitted = false;
        if (tick.canEmit()) {
            Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
            boolean firstHalf = level.getGameTime() % 32L < 16L;
            Direction positive = firstHalf ? facing.getOpposite() : facing;
            Direction negative = positive.getOpposite();
            EnergyEmitter.pushToSide(
                    level,
                    worldPosition,
                    EnergyType.KINETIC_PUSH,
                    tick.outputSize(),
                    1L,
                    positive);
            EnergyEmitter.pushToSide(
                    level,
                    worldPosition,
                    EnergyType.KINETIC_PUSH,
                    -tick.outputSize(),
                    1L,
                    negative);
            emitted = true;
        }
        if (storedEnergy != tick.storedAfterWaste() || emitted) {
            storedEnergy = tick.storedAfterWaste();
            setChanged();
        }
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
        private final Direction side;

        private SteamHandler(Direction side) {
            this.side = side;
        }

        private SteamTurbineCatalog.Profile profile() {
            return SteamTurbineCatalog.find(spec().id()).orElse(null);
        }

        private boolean fillSide() {
            SteamTurbineCatalog.Profile profile = profile();
            if (profile == null || stopped) {
                return false;
            }
            if (profile.large()) {
                return true;
            }
            Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
            return side == null || side == facing.getOpposite();
        }

        private boolean drainSide() {
            SteamTurbineCatalog.Profile profile = profile();
            if (profile == null || !profile.large()) {
                return false;
            }
            return true;
        }

        private boolean exposeTank() {
            SteamTurbineCatalog.Profile profile = profile();
            if (profile == null) {
                return false;
            }
            if (profile.large()) {
                return true;
            }
            Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
            return side != facing;
        }

        @Override
        public int getTanks() {
            return exposeTank() ? 2 : 0;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            if (!exposeTank()) {
                return FluidStack.EMPTY;
            }
            if (index == 0) {
                return tank.getFluid();
            }
            return index == 1 ? distilled.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int index) {
            if (!exposeTank()) {
                return 0;
            }
            if (index == 0) {
                return tank.getCapacity();
            }
            return index == 1 ? distilled.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return index == 0
                    && fillSide()
                    && !stack.isEmpty()
                    && stack.is(ModFluids.STEAM_SOURCE.get());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!fillSide()
                    || resource.isEmpty()
                    || !resource.is(ModFluids.STEAM_SOURCE.get())) {
                return 0;
            }
            return tank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return drainSide()
                    ? distilled.drain(resource, action)
                    : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return drainSide()
                    ? distilled.drain(maxDrain, action)
                    : FluidStack.EMPTY;
        }
    }

    private final class SteamFillView implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return index == 0 ? tank.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int index) {
            return index == 0 ? tank.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return index == 0 && tank.isFluidValid(stack) && !stopped;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return stopped ? 0 : tank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    private final class SteamDrainView implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return index == 0 ? distilled.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int index) {
            return index == 0 ? distilled.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
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

    private final class SteamIoView implements IFluidHandler {
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
            return index == 0 && tank.isFluidValid(stack) && !stopped;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return stopped ? 0 : tank.fill(resource, action);
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
