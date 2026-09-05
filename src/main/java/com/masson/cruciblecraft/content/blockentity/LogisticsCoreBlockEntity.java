package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.logistics.core.LogisticsCoreDump;
import com.masson.cruciblecraft.logistics.core.LogisticsCoreGeometry;
import com.masson.cruciblecraft.logistics.core.LogisticsCoreScan;
import com.masson.cruciblecraft.logistics.core.LogisticsCoreStructure;
import com.masson.cruciblecraft.logistics.displaycpu.DisplayCpuWriteback;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/** Non-processing Logistics Core: structure, EU, scan, dump pass. */
public final class LogisticsCoreBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private long energy;
    private int cpuLogic;
    private int cpuControl;
    private int cpuStorage;
    private int cpuConversion;
    private int usedLogic;
    private int usedControl;
    private int usedStorage;
    private int usedConversion;
    private boolean formed;
    private List<BlockPos> boundWalls = List.of();

    public LogisticsCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOGISTICS_CORE.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LogisticsCoreBlockEntity core) {
        core.tickServer(level, pos, state);
    }

    public BlockPos centerPos() {
        return LogisticsCoreStructure.center(
                worldPosition, facing());
    }

    public boolean formed() {
        return formed;
    }

    public int cpuLogic() {
        return cpuLogic;
    }

    public int cpuControl() {
        return cpuControl;
    }

    public int cpuConversion() {
        return cpuConversion;
    }

    public int cpuStorage() {
        return cpuStorage;
    }

    public void unbindAll() {
        if (level != null) {
            LogisticsCoreStructure.unbindWalls(level, boundWalls);
        }
        boundWalls = List.of();
        formed = false;
    }

    public Direction facing() {
        return LogisticsCoreStructure.facingOf(getBlockState());
    }

    public List<Component> statusLines() {
        ArrayList<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Power: " + energy + " EU"));
        lines.add(Component.literal(
                "Consumption: "
                        + LogisticsCoreGeometry.idleDrain(
                                cpuLogic, cpuControl, cpuStorage, cpuConversion)
                        + " EU/t, plus more per moved Item"));
        lines.add(Component.literal(cpuLogic + " Logic Processors"));
        lines.add(Component.literal(
                cpuControl
                        + " Control Processors (Range: "
                        + (LogisticsCoreGeometry.HALF + cpuControl)
                        + "m, Cubic AoE)"));
        lines.add(Component.literal(
                cpuStorage + " Storage Processors (Note: For now useless)"));
        lines.add(Component.literal(
                cpuConversion + " Conversion Processors"));
        return List.copyOf(lines);
    }

    private void tickServer(Level level, BlockPos pos, BlockState state) {
        LogisticsCoreStructure.Result structure =
                LogisticsCoreStructure.check(level, pos, facing());
        if (structure.formed() != formed
                || !structure.walls().equals(boundWalls)) {
            LogisticsCoreStructure.unbindWalls(level, boundWalls);
            if (structure.formed()) {
                LogisticsCoreStructure.bindWalls(
                        level, pos, structure.walls());
            }
            boundWalls = structure.walls();
        }
        formed = structure.formed();
        cpuLogic = structure.logic();
        cpuControl = structure.control();
        cpuStorage = structure.storage();
        cpuConversion = structure.conversion();
        if (level.getGameTime() % LogisticsCoreGeometry.SYNC_INTERVAL == 0) {
            DisplayCpuWriteback.Load snapshot = new DisplayCpuWriteback.Load(
                    usedLogic,
                    cpuLogic,
                    usedControl,
                    cpuControl,
                    usedStorage,
                    cpuStorage,
                    usedConversion,
                    cpuConversion);
            usedLogic = 0;
            usedControl = 0;
            usedStorage = 0;
            usedConversion = 0;
            if (formed
                    && energy >= LogisticsCoreGeometry.operateThreshold(
                            cpuLogic, cpuConversion)) {
                LogisticsCoreScan.Result scan = LogisticsCoreScan.scan(
                        level, centerPos(), cpuControl);
                usedControl = scan.controlUsed();
                for (LogisticsCoreScan.Endpoint display : scan.displays()) {
                    DisplayCpuWriteback.write(
                            level, display.pipe(), display.side(), snapshot);
                }
                DumpUse dumped = dumpPass(level, scan);
                usedLogic = dumped.logicUsed();
                usedConversion = dumped.conversionUsed();
            }
        }
        energy -= LogisticsCoreGeometry.idleDrain(
                cpuLogic, cpuControl, cpuStorage, cpuConversion);
        if (energy < 0L) {
            energy = 0L;
        }
        setChanged();
    }

    private DumpUse dumpPass(Level level, LogisticsCoreScan.Result scan) {
        int conversions = Math.max(1, cpuConversion);
        int logicUsed = 0;
        int conversionUsed = 0;
        for (LogisticsCoreScan.Endpoint storage : scan.genericItemStorage()) {
            IItemHandler from = LogisticsCoreScan.inventoryAt(
                    level, storage.pipe(), storage.side());
            if (from == null) {
                continue;
            }
            for (LogisticsCoreScan.Endpoint dump : scan.dumps()) {
                if (dump.networkId() != storage.networkId()) {
                    continue;
                }
                IItemHandler to = LogisticsCoreScan.inventoryAt(
                        level, dump.pipe(), dump.side());
                if (to == null) {
                    continue;
                }
                boolean movedAny = false;
                for (int step = 0; step < conversions; step++) {
                    int moved = LogisticsCoreDump.moveLeftover(
                            from,
                            to,
                            scan.filteredFor(),
                            LogisticsCoreGeometry.DUMP_MAX_MOVE);
                    if (moved <= 0) {
                        break;
                    }
                    energy -= moved;
                    if (energy < 0L) {
                        energy = 0L;
                    }
                    conversionUsed = Math.max(conversionUsed, step + 1);
                    logicUsed = 1;
                    movedAny = true;
                }
                if (movedAny) {
                    return new DumpUse(logicUsed, conversionUsed);
                }
            }
        }
        return DumpUse.NONE;
    }

    private record DumpUse(int logicUsed, int conversionUsed) {
        private static final DumpUse NONE = new DumpUse(0, 0);
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return formed && type == EnergyType.ELECTRIC;
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
        long magnitude = EnergyPackets.magnitude(size);
        if (magnitude < LogisticsCoreGeometry.ENERGY_INPUT_MIN) {
            return 0L;
        }
        if (magnitude > LogisticsCoreGeometry.ENERGY_INPUT_MAX) {
            if (!simulate && level != null && !level.isClientSide) {
                explode();
            }
            return amount;
        }
        long cap = LogisticsCoreGeometry.capacity(cpuLogic, cpuConversion);
        if (energy > cap) {
            return 0L;
        }
        if (!simulate) {
            energy += EnergyPackets.units(size, amount);
            setChanged();
        }
        return amount;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.ELECTRIC ? energy : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.ELECTRIC
                ? LogisticsCoreGeometry.capacity(cpuLogic, cpuConversion)
                : 0L;
    }

    private void explode() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockPos pos = worldPosition;
        level.explode(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                6.0F,
                Level.ExplosionInteraction.TNT);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("energy", energy);
        tag.putInt("logic", cpuLogic);
        tag.putInt("control", cpuControl);
        tag.putInt("storage", cpuStorage);
        tag.putInt("conversion", cpuConversion);
        tag.putInt("usedLogic", usedLogic);
        tag.putInt("usedControl", usedControl);
        tag.putInt("usedStorage", usedStorage);
        tag.putInt("usedConversion", usedConversion);
        tag.putBoolean("formed", formed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = tag.getLong("energy");
        cpuLogic = tag.getInt("logic");
        cpuControl = tag.getInt("control");
        cpuStorage = tag.getInt("storage");
        cpuConversion = tag.getInt("conversion");
        usedLogic = tag.getInt("usedLogic");
        usedControl = tag.getInt("usedControl");
        usedStorage = tag.getInt("usedStorage");
        usedConversion = tag.getInt("usedConversion");
        formed = tag.getBoolean("formed");
    }
}
