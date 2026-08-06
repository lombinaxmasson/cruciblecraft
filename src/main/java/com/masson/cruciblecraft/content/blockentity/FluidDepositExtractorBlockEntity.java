package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.material.HydrocarbonRuntimePolicy;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Column-linked wellhead with an extract-only automation buffer. */
public final class FluidDepositExtractorBlockEntity extends BlockEntity {
    public static final int BUFFER_CAPACITY_MB = 8_000;
    private static final int LOST_LINK_RETRY_TICKS = 20;
    private static final int NEGATIVE_LINK_RETRY_TICKS = 200;
    private static final int MIN_DEPOSIT_Y = -64;
    private static final int MAX_DEPOSIT_Y = 16;
    private final FluidTank buffer = new FluidTank(
            BUFFER_CAPACITY_MB,
            stack -> ModFluids.material(stack.getFluid())
                    .map(material -> HydrocarbonRuntimePolicy.supportsProduction(
                            net.minecraft.resources.ResourceLocation
                                    .fromNamespaceAndPath(
                                            "cruciblecraft",
                                            material.id())))
                    .orElse(false)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler external = new ExtractOnlyHandler();
    private BlockPos linkedDeposit;
    private long nextLinkAttempt;
    private long lastExternalDrainGameTime;
    private long lastVentGameTime;
    private String status = "unlinked";

    public FluidDepositExtractorBlockEntity(
            BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_DEPOSIT_EXTRACTOR.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FluidDepositExtractorBlockEntity extractor) {
        SubsurfaceFluidDepositBlockEntity deposit =
                extractor.resolveDeposit(level);
        if (deposit == null) {
            extractor.status = "unlinked";
            return;
        }
        int room = extractor.buffer.getTankCapacity(0)
                - extractor.buffer.getFluidAmount();
        if (room > 0) {
            int planned = deposit.extract(
                    room, level.getGameTime(), true);
            if (planned > 0) {
                var fluid = ModFluids.materialFluid(
                                deposit.materialId().getPath())
                        .orElseThrow(() -> new IllegalStateException(
                                "Deposit material has no registered fluid: "
                                        + deposit.materialId()));
                FluidStack offered = new FluidStack(fluid, planned);
                int accepted = extractor.buffer.fill(
                        offered, IFluidHandler.FluidAction.SIMULATE);
                if (accepted > 0) {
                    int extracted = deposit.extract(
                            accepted, level.getGameTime(), false);
                    if (extracted != accepted
                            || extractor.buffer.fill(
                                            new FluidStack(fluid, extracted),
                                            IFluidHandler.FluidAction.EXECUTE)
                                    != extracted) {
                        throw new IllegalStateException(
                                "Deposit extraction changed after simulation");
                    }
                    extractor.status = "extracting";
                    extractor.setChanged();
                    extractor.maybeVent(level, pos, deposit);
                    return;
                }
            }
            extractor.status = "waiting";
            extractor.maybeVent(level, pos, deposit);
            return;
        }
        extractor.status = "buffer_full";
        if (!deposit.ventsOverflow()) {
            return;
        }
        BlockPos cloudPos = pos.above();
        if (!GasCloudBlockEntity.canAccept(
                level, cloudPos, deposit.materialId())) {
            extractor.status = "vent_blocked";
            return;
        }
        int maximum = HydrocarbonRuntimePolicy.cloud().parcelMb();
        int planned = deposit.extract(
                maximum, level.getGameTime(), true);
        if (planned <= 0) {
            return;
        }
        int emitted = GasCloudBlockEntity.placeOrMerge(
                level, cloudPos, deposit.materialId(), planned);
        if (emitted <= 0) {
            return;
        }
        int extracted = deposit.extract(
                emitted, level.getGameTime(), false);
        if (extracted != emitted) {
            throw new IllegalStateException(
                    "Gas vent changed after simulation");
        }
        extractor.status = "venting";
        extractor.lastVentGameTime = level.getGameTime();
        extractor.setChanged();
    }

    private void maybeVent(
            Level level,
            BlockPos pos,
            SubsurfaceFluidDepositBlockEntity deposit) {
        if (lastExternalDrainGameTime == 0L) {
            lastExternalDrainGameTime = level.getGameTime();
            setChanged();
            return;
        }
        if (!deposit.ventsOverflow()
                || buffer.isEmpty()
                || level.getGameTime() - lastExternalDrainGameTime < 40L
                || level.getGameTime() - lastVentGameTime < 20L) {
            return;
        }
        int amount = Math.min(
                buffer.getFluidAmount(),
                HydrocarbonRuntimePolicy.production(
                        deposit.materialId()).amountMb());
        BlockPos cloudPos = pos.above();
        if (amount <= 0
                || !GasCloudBlockEntity.canAccept(
                        level, cloudPos, deposit.materialId())) {
            return;
        }
        int emitted = GasCloudBlockEntity.placeOrMerge(
                level, cloudPos, deposit.materialId(), amount);
        if (emitted <= 0) {
            return;
        }
        FluidStack drained = buffer.drain(
                emitted, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != emitted) {
            throw new IllegalStateException(
                    "Extractor vent buffer changed after simulation");
        }
        lastVentGameTime = level.getGameTime();
        status = "venting";
        setChanged();
    }

    private SubsurfaceFluidDepositBlockEntity resolveDeposit(Level level) {
        if (linkedDeposit != null
                && level.hasChunkAt(linkedDeposit)
                && level.getBlockEntity(linkedDeposit)
                        instanceof SubsurfaceFluidDepositBlockEntity deposit
                && HydrocarbonRuntimePolicy.supportsProduction(
                        deposit.materialId())) {
            return deposit;
        }
        long gameTime = level.getGameTime();
        if (gameTime < nextLinkAttempt) {
            return null;
        }
        boolean retryingLostLink = linkedDeposit != null;
        nextLinkAttempt = gameTime + (retryingLostLink
                ? LOST_LINK_RETRY_TICKS
                : NEGATIVE_LINK_RETRY_TICKS);
        linkedDeposit = null;
        BlockPos.MutableBlockPos cursor = worldPosition.mutable();
        int maximumY = Math.min(
                worldPosition.getY() - 1, MAX_DEPOSIT_Y);
        int minimumY = Math.max(
                level.getMinBuildHeight(), MIN_DEPOSIT_Y);
        for (int y = maximumY;
                y >= minimumY;
                y--) {
            cursor.setY(y);
            if (level.getBlockEntity(cursor)
                    instanceof SubsurfaceFluidDepositBlockEntity deposit
                    && HydrocarbonRuntimePolicy.supportsProduction(
                            deposit.materialId())) {
                linkedDeposit = cursor.immutable();
                setChanged();
                return deposit;
            }
        }
        return null;
    }

    public IFluidHandler externalFluid() {
        return external;
    }

    public int fluidAmount() {
        return buffer.getFluidAmount();
    }

    public String status() {
        return status;
    }

    public BlockPos linkedDeposit() {
        return linkedDeposit;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("buffer", buffer.writeToNBT(
                registries, new CompoundTag()));
        if (linkedDeposit != null) {
            tag.putLong("linked_deposit", linkedDeposit.asLong());
        }
        tag.putLong("next_link_attempt", nextLinkAttempt);
        tag.putLong(
                "last_external_drain_game_time",
                lastExternalDrainGameTime);
        tag.putLong("last_vent_game_time", lastVentGameTime);
        tag.putString("status", status);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("buffer")) {
            buffer.readFromNBT(registries, tag.getCompound("buffer"));
        }
        linkedDeposit = tag.contains("linked_deposit")
                ? BlockPos.of(tag.getLong("linked_deposit"))
                : null;
        nextLinkAttempt = Math.max(
                0L, tag.getLong("next_link_attempt"));
        lastExternalDrainGameTime = Math.max(
                0L, tag.getLong("last_external_drain_game_time"));
        lastVentGameTime = Math.max(
                0L, tag.getLong("last_vent_game_time"));
        status = tag.getString("status");
        if (status.isBlank()) {
            status = "unlinked";
        }
    }

    private final class ExtractOnlyHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? buffer.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? buffer.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(
                FluidStack resource, FluidAction action) {
            FluidStack drained = buffer.drain(resource, action);
            recordExternalDrain(drained, action);
            return drained;
        }

        @Override
        public FluidStack drain(int maximum, FluidAction action) {
            FluidStack drained = buffer.drain(maximum, action);
            recordExternalDrain(drained, action);
            return drained;
        }

        private void recordExternalDrain(
                FluidStack drained, FluidAction action) {
            if (action.execute()
                    && !drained.isEmpty()
                    && level != null) {
                lastExternalDrainGameTime = level.getGameTime();
                setChanged();
            }
        }
    }
}
