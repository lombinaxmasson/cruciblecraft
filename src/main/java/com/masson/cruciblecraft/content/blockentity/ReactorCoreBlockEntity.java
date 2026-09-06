package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.content.block.ReactorCoreBlock;
import com.masson.cruciblecraft.content.item.ReactorRodItem;
import com.masson.cruciblecraft.nuclear.ReactorCoolant;
import com.masson.cruciblecraft.nuclear.ReactorCoreHost;
import com.masson.cruciblecraft.nuclear.ReactorRodPhysics;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/** GT6 reactor core: 20-tick emit/reflect then heat + coolant conversion. */
public final class ReactorCoreBlockEntity extends BlockEntity
        implements ReactorCoreHost {
    public static final int COOLANT_CAPACITY = 64_000;
    private static final int[] S2103 = {0, 0, 2, 1, 0, 3, 0};
    private static final int[] S0312 = {0, 0, 0, 3, 1, 2, 0};

    private final int slots;
    private final ItemStackHandler inventory;
    private final FluidTank coolant = new FluidTank(COOLANT_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return ReactorCoolant.of(stack) != null;
        }
    };
    private final FluidTank output = new FluidTank(
            COOLANT_CAPACITY * ReactorCoolant.STEAM_PER_WATER);
    private final int[] neutrons = new int[4];
    private final int[] oldNeutrons = new int[4];
    private long heat;
    private long lastHeat;
    private boolean stopped = true;
    private boolean running;
    private byte mode;

    public ReactorCoreBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, slotsOf(state));
    }

    public ReactorCoreBlockEntity(BlockPos pos, BlockState state, int slots) {
        super(ModBlockEntities.REACTOR_CORE.get(), pos, state);
        this.slots = slots;
        this.inventory = new ItemStackHandler(slots) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem() instanceof ReactorRodItem;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            public ItemStack insertItem(
                    int slot, ItemStack stack, boolean simulate) {
                ItemStack remaining = super.insertItem(slot, stack, simulate);
                if (!simulate && remaining.getCount() < stack.getCount()) {
                    stopped = true;
                }
                return remaining;
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ReactorCoreBlockEntity core) {
        core.onTick(level.getGameTime());
        boolean lit = core.running && !core.stopped;
        if (state.getValue(ReactorCoreBlock.LIT) != lit) {
            level.setBlock(
                    pos, state.setValue(ReactorCoreBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    public void onTick(long gameTime) {
        emitAndReflect(gameTime);
        reactAndConvert(gameTime);
    }

    public int slots() {
        return slots;
    }

    public boolean stopped() {
        return stopped;
    }

    public boolean running() {
        return running;
    }

    public long lastHeat() {
        return lastHeat;
    }

    public int neutrons(int slot) {
        return oldNeutrons[slot];
    }

    public ItemStack rod(int slot) {
        return inventory.getStackInSlot(slot);
    }

    public FluidTank coolantTank() {
        return coolant;
    }

    public FluidTank outputTank() {
        return output;
    }

    public IFluidHandler fluids() {
        return new Fluids();
    }

    public ItemStackHandler items() {
        return inventory;
    }

    public void toggleStopped() {
        stopped = !stopped;
        setChanged();
    }

    public void setStopped(boolean value) {
        stopped = value;
        setChanged();
    }

    public boolean insertRod(int slot, ItemStack stack, Player player) {
        if (slot < 0 || slot >= slots || !inventory.getStackInSlot(slot).isEmpty()) {
            return false;
        }
        if (!(stack.getItem() instanceof ReactorRodItem)) {
            return false;
        }
        inventory.setStackInSlot(slot, stack.split(1));
        stopped = true;
        setChanged();
        return true;
    }

    public ItemStack extractRod(int slot, Player player) {
        if (slot < 0 || slot >= slots || (!stopped && (mode & (1 << slot)) == 0)) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = inventory.getStackInSlot(slot);
        if (extracted.isEmpty()) {
            return ItemStack.EMPTY;
        }
        inventory.setStackInSlot(slot, ItemStack.EMPTY);
        if (player != null && !player.addItem(extracted.copy())) {
            player.drop(extracted.copy(), false);
        }
        setChanged();
        return extracted;
    }

    @Override
    public ReactorCoolant coolant() {
        return ReactorCoolant.of(coolant.getFluid());
    }

    @Override
    public int oldNeutrons(int slot) {
        return oldNeutrons[slot];
    }

    @Override
    public void addNeutrons(int slot, long amount) {
        neutrons[slot] = (int) Math.min(
                Integer.MAX_VALUE, neutrons[slot] + Math.max(0L, amount));
    }

    @Override
    public void addHeat(long amount) {
        long added = Math.max(0L, amount);
        if (added > 0L && heat > Long.MAX_VALUE - added) {
            heat = Long.MAX_VALUE;
            return;
        }
        heat += added;
    }

    @Override
    public void replaceRod(int slot, ItemStack replacement) {
        inventory.setStackInSlot(slot, replacement);
    }

    private void emitAndReflect(long gameTime) {
        if (gameTime % 20L != 19L || stopped) {
            return;
        }
        if (slots == 1) {
            int emission = (int) ReactorRodPhysics.divUp(emission(0), 2L);
            boolean moderated = moderated(0);
            if (emission != 0 || moderated) {
                reflectFromNeighbors(0, emission, moderated);
            }
            return;
        }
        pulse(0, 1, 2, Direction.NORTH, S2103, Direction.WEST, S0312);
        pulse(1, 0, 3, Direction.SOUTH, S0312, Direction.WEST, S2103);
        pulse(2, 0, 3, Direction.NORTH, S0312, Direction.EAST, S2103);
        pulse(3, 1, 2, Direction.SOUTH, S2103, Direction.EAST, S0312);
    }

    private void pulse(
            int slot,
            int firstNeighbor,
            int secondNeighbor,
            Direction firstSide,
            int[] firstMap,
            Direction secondSide,
            int[] secondMap) {
        int emission = emission(slot);
        boolean moderated = moderated(slot);
        if (emission == 0 && !moderated) {
            return;
        }
        neutrons[slot] += reflect(firstNeighbor, emission, moderated);
        neutrons[slot] += reflect(secondNeighbor, emission, moderated);
        neutrons[slot] += neighborReflect(firstSide, firstMap, emission, moderated);
        neutrons[slot] += neighborReflect(secondSide, secondMap, emission, moderated);
    }

    private void reactAndConvert(long gameTime) {
        if (gameTime % 20L == 19L) {
            if (slots == 1) {
                neutrons[0] += neutrons[1] + neutrons[2] + neutrons[3];
                neutrons[1] = neutrons[2] = neutrons[3] = 0;
                oldNeutrons[1] = oldNeutrons[2] = oldNeutrons[3] = 0;
            }
            for (int slot = 0; slot < slots; slot++) {
                ReactorRodPhysics.updateModeration(inventory.getStackInSlot(slot));
            }
        }
        long total = 0L;
        for (int slot = 0; slot < slots; slot++) {
            oldNeutrons[slot] = neutrons[slot];
            total += oldNeutrons[slot];
        }
        running = ReactorRodPhysics.divUp(total, 256L) != 0L;
        long before = heat;
        for (int slot = 0; slot < slots; slot++) {
            if (gameTime % 20L == 18L) {
                neutrons[slot] -= oldNeutrons[slot];
            }
            if (slotActive(slot)
                    && ReactorRodPhysics.react(
                            this, slot, inventory.getStackInSlot(slot))) {
                running = true;
            }
        }
        ReactorCoolant kind = coolant();
        int divider = coolant() == null ? 1 : coolant().heatDivider();
        heat = ReactorRodPhysics.divUp(heat - before, divider) + before;
        lastHeat = heat - before;
        convertCoolant();
        setChanged();
    }

    private void convertCoolant() {
        if (heat <= 0L) {
            return;
        }
        ReactorCoolant kind = coolant();
        if (kind == null) {
            if (lastHeat > 0L && !inventoryEmpty()) {
                destroyRods();
            }
            return;
        }
        long units = heat / kind.euPerUnit();
        if (units <= 0L) {
            return;
        }
        FluidStack hot = kind.hotOutput((int) Math.min(Integer.MAX_VALUE, units));
        if (coolant.getFluidAmount() < units
                || hot.isEmpty()
                || output.fill(hot, IFluidHandler.FluidAction.SIMULATE)
                        != hot.getAmount()) {
            if (!inventoryEmpty()) {
                destroyRods();
            }
            return;
        }
        coolant.drain((int) units, IFluidHandler.FluidAction.EXECUTE);
        output.fill(hot, IFluidHandler.FluidAction.EXECUTE);
        heat -= units * kind.euPerUnit();
    }

    private void destroyRods() {
        for (int slot = 0; slot < slots; slot++) {
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
        running = false;
    }

    private boolean inventoryEmpty() {
        for (int slot = 0; slot < slots; slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private int emission(int slot) {
        if (!slotActive(slot)) {
            neutrons[slot] = 0;
            return 0;
        }
        return ReactorRodPhysics.emit(this, slot, inventory.getStackInSlot(slot));
    }

    private int reflect(int slot, int neutrons, boolean moderated) {
        if (!slotActive(slot)) {
            return 0;
        }
        return ReactorRodPhysics.reflect(
                this, slot, inventory.getStackInSlot(slot), neutrons, moderated);
    }

    private boolean moderated(int slot) {
        if (stopped || (mode & (1 << slot)) != 0) {
            return false;
        }
        return ReactorRodPhysics.moderated(inventory.getStackInSlot(slot));
    }

    private boolean slotActive(int slot) {
        return !stopped
                && (mode & (1 << slot)) == 0
                && slot < slots
                && inventory.getStackInSlot(slot).getItem() instanceof ReactorRodItem;
    }

    private int neighborReflect(
            Direction side,
            int[] map,
            int neutrons,
            boolean moderated) {
        if (level == null) {
            return 0;
        }
        BlockPos neighborPos = worldPosition.relative(side);
        if (!(level.getBlockEntity(neighborPos)
                instanceof ReactorCoreBlockEntity neighbor)) {
            return 0;
        }
        int neighborSlot = neighbor.slots == 1 ? 0 : map[side.ordinal()];
        return neighbor.reflect(neighborSlot, neutrons, moderated);
    }

    private void reflectFromNeighbors(
            int slot, int neutrons, boolean moderated) {
        for (Direction side : new Direction[] {
                Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            this.neutrons[slot] += neighborReflect(
                    side,
                    side == Direction.NORTH || side == Direction.EAST
                            ? S2103
                            : S0312,
                    neutrons,
                    moderated);
        }
    }

    private static int slotsOf(BlockState state) {
        return state.getBlock() instanceof ReactorCoreBlock core
                ? core.slots()
                : 4;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("rods", inventory.serializeNBT(registries));
        tag.put("coolant", coolant.writeToNBT(registries, new CompoundTag()));
        tag.put("output", output.writeToNBT(registries, new CompoundTag()));
        tag.putLong("heat", heat);
        tag.putBoolean("stopped", stopped);
        tag.putBoolean("running", running);
        tag.putByte("mode", mode);
        for (int slot = 0; slot < 4; slot++) {
            tag.putInt("n." + slot, neutrons[slot]);
            tag.putInt("o." + slot, oldNeutrons[slot]);
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("rods"));
        coolant.readFromNBT(registries, tag.getCompound("coolant"));
        output.readFromNBT(registries, tag.getCompound("output"));
        heat = tag.getLong("heat");
        stopped = tag.getBoolean("stopped");
        running = tag.getBoolean("running");
        mode = tag.getByte("mode");
        for (int slot = 0; slot < 4; slot++) {
            neutrons[slot] = tag.getInt("n." + slot);
            oldNeutrons[slot] = tag.getInt("o." + slot);
        }
    }

    private final class Fluids implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? coolant.getFluid() : output.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? coolant.getCapacity() : output.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && ReactorCoolant.of(stack) != null;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return coolant.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack fromOutput = output.drain(resource, action);
            if (!fromOutput.isEmpty()) {
                return fromOutput;
            }
            return coolant.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!output.getFluid().isEmpty()) {
                return output.drain(maxDrain, action);
            }
            return coolant.drain(maxDrain, action);
        }
    }
}
