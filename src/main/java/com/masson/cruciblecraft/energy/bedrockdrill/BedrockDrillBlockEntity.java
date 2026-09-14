package com.masson.cruciblecraft.energy.bedrockdrill;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.machine.processing.MachineEnergyBuffer;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Dedicated 17999 host. Accepts RU and lubricant, emits bedrock dust.
 * Not HEX, steam turbine, or transformer.
 */
public final class BedrockDrillBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private static final long PACKET = 1_024L;
    private final MachineEnergyBuffer ru =
            new MachineEnergyBuffer(32_768L, 4_096L);
    private final FluidTank lube = new FluidTank(
            BedrockDrillStructure.TANK_CAPACITY, this::acceptsLube) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private boolean formed;
    private boolean forceFormed;

    public BedrockDrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BEDROCK_DRILL.get(), pos, state);
    }

    public boolean formed() {
        return formed || forceFormed;
    }

    public void forceFormedForTest() {
        forceFormed = true;
        formed = true;
        setChanged();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            BedrockDrillBlockEntity drill) {
        if (!drill.forceFormed) {
            drill.formed = BedrockDrillStructure.check(level, pos);
        }
        if (drill.formed()) {
            drill.tryMine();
        }
        drill.updateLit();
    }

    private void tryMine() {
        if (ru.stored() < BedrockDrillStructure.RU_PER_TICK
                || lube.getFluidAmount() < BedrockDrillStructure.LUBE_PER_TICK) {
            return;
        }
        ItemStack produced = bedrockDust();
        if (produced.isEmpty()) {
            return;
        }
        ItemStack leftover = output.insertItem(0, produced, true);
        if (!leftover.isEmpty()) {
            return;
        }
        if (!ru.consume(BedrockDrillStructure.RU_PER_TICK)) {
            return;
        }
        lube.drain(
                BedrockDrillStructure.LUBE_PER_TICK,
                IFluidHandler.FluidAction.EXECUTE);
        output.insertItem(0, produced, false);
        setChanged();
    }

    private static ItemStack bedrockDust() {
        return MaterialLookup.item("bedrock", MaterialPrefixes.DUST)
                .map(item -> new ItemStack(item, 1))
                .orElse(ItemStack.EMPTY);
    }

    private boolean acceptsLube(FluidStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Fluid lubricant = ModFluids.chemical("lubricant")
                .map(entry -> entry.source().get())
                .orElse(null);
        return lubricant != null && stack.getFluid() == lubricant;
    }

    public IFluidHandler fluids(Direction side) {
        return formed() ? lube : null;
    }

    public net.neoforged.neoforge.items.IItemHandler items(Direction side) {
        return formed() ? output : null;
    }

    public boolean fillLube(FluidStack stack) {
        int filled = lube.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            setChanged();
        }
        return filled == stack.getAmount();
    }

    public ItemStack mined() {
        return output.getStackInSlot(0).copy();
    }

    private void updateLit() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean lit = formed() && ru.stored() > 0L;
        BlockState state = getBlockState();
        if (state.hasProperty(BedrockDrillBlock.LIT)
                && state.getValue(BedrockDrillBlock.LIT) != lit) {
            level.setBlock(
                    worldPosition,
                    state.setValue(BedrockDrillBlock.LIT, lit),
                    Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return formed() && type == EnergyType.KINETIC_ROTATION;
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
        long accepted = ru.insert(size, amount, simulate);
        if (!simulate && accepted > 0L) {
            setChanged();
        }
        return accepted;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.KINETIC_ROTATION ? ru.stored() : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.KINETIC_ROTATION ? ru.capacity() : 0L;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("ru", ru.stored());
        tag.put("lube", lube.writeToNBT(registries, new CompoundTag()));
        tag.put("output", output.serializeNBT(registries));
        tag.putBoolean("formed", formed);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ru.restore(tag.getLong("ru"));
        if (tag.contains("lube")) {
            lube.readFromNBT(registries, tag.getCompound("lube"));
        }
        if (tag.contains("output", Tag.TAG_COMPOUND)) {
            output.deserializeNBT(registries, tag.getCompound("output"));
        }
        formed = tag.getBoolean("formed");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
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
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
