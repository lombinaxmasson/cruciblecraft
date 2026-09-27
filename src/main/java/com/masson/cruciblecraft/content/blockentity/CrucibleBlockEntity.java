package com.masson.cruciblecraft.content.blockentity;

import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.block.SmelteryHosts;
import com.masson.cruciblecraft.content.mold.CruciblePour;
import com.masson.cruciblecraft.content.mold.MoldHost;
import com.masson.cruciblecraft.content.sensor.TemperatureHost;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.InsertResult;
import com.masson.cruciblecraft.heat.ItemHeat;
import com.masson.cruciblecraft.heat.TemperatureDamage;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.component.CheckpointTracker;
import com.masson.cruciblecraft.machine.component.CrucibleProcessCore;
import com.masson.cruciblecraft.machine.component.SteelmakingController;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CrucibleBlockEntity extends BlockEntity
        implements IEnergyHandler, CruciblePour, MoldHost, TemperatureHost {
    public static final int MAX_INGOTS = CrucibleProcessCore.SINGLE_BLOCK_MAX_INGOTS;
    public static final float AMBIENT_TEMPERATURE = CrucibleProcessCore.AMBIENT_TEMPERATURE;
    public static final long HEAT_DISPLAY_CAPACITY = CrucibleProcessCore.HEAT_DISPLAY_CAPACITY;

    private static final double SUCK_INSET = 2.0 / 16.0;

    private final CrucibleProcessCore process = CrucibleProcessCore.singleBlock();
    private final ItemStackHandler inputBuffer = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler topInsert = new TopInsertHandler();
    private final CheckpointTracker checkpoint = new CheckpointTracker();
    private boolean quarantineWarningLogged;

    public CrucibleBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CRUCIBLE.get(), pos, blockState);
        process.setOnMutation(this::markMutation);
        SmelteryHosts.bakedMaterial(blockState)
                .ifPresent(process.casing()::setMaterialId);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!quarantineWarningLogged
                && level != null
                && !level.isClientSide
                && process.casing().quarantined()) {
            quarantineWarningLogged = true;
            CrucibleCraft.LOGGER.warn(
                    "Quarantined crucible at {} {}: unsupported casing material {}",
                    level.dimension().location(),
                    worldPosition,
                    process.casing().quarantinedMaterialId());
        }
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CrucibleBlockEntity crucible) {
        if (crucible.process.casing().quarantined()) {
            return;
        }
        crucible.suckDroppedItems(level, pos);
        crucible.ingestBuffer(level);
        crucible.process.addRainWater(
                level.getGameTime(),
                level.isRainingAt(pos.above()) ? 1.0F : 0.0F,
                level.isThundering());
        long incomingEnergy = crucible.process.thermal().takePendingHeat();

        float previousTemperature = crucible.process.thermal().authoritativeTemperature();
        long previousStoredEnergy = crucible.process.thermal().storedEnergy();
        int previousCooldown = crucible.process.thermal().cooldownTicks();
        long previousAir = crucible.process.steelmaking().storedAir();
        int previousReactionTicks = crucible.process.steelmaking().reactionTicks();
        int shownAir = crucible.process.visibleAirUnits();
        boolean meltedDown = incomingEnergy == 0L && crucible.isThermallyQuiescent()
                ? false
                : crucible.advance(incomingEnergy, true);
        if (crucible.process.visibleAirUnits() != shownAir) {
            crucible.syncToClient();
        }
        if (meltedDown) {
            return;
        }
        boolean processChanged = Float.compare(
                        previousTemperature,
                        crucible.process.thermal().authoritativeTemperature()) != 0
                || previousStoredEnergy != crucible.process.thermal().storedEnergy()
                || previousCooldown != crucible.process.thermal().cooldownTicks()
                || previousAir != crucible.process.steelmaking().storedAir()
                || previousReactionTicks != crucible.process.steelmaking().reactionTicks();
        if (processChanged) {
            crucible.checkpoint.markDirty();
        }
        long phaseKey = CheckpointDecisions.phaseKey(pos.getX(), pos.getY(), pos.getZ());
        if (crucible.checkpoint.shouldCheckpoint(level.getGameTime(), phaseKey, 20)) {
            crucible.setChanged();
            crucible.checkpoint.checkpointed();
        }
        if (crucible.checkpoint.shouldSync(
                crucible.isActiveProcess(),
                level.getGameTime(),
                phaseKey,
                20)) {
            crucible.syncToClient();
            crucible.checkpoint.synced();
        }
    }

    public static void clientTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CrucibleBlockEntity crucible) {
        crucible.process.thermal().clientTick();
    }

    private boolean advance(long incomingEnergy, boolean authoritative) {
        CrucibleProcessCore.TickOutcome outcome = process.advance(incomingEnergy, authoritative);
        if (outcome.boiled()) {
            markMutation();
            emitBoilingEffects();
            CrucibleWorldHazards.boilHazards(
                    level,
                    worldPosition,
                    process.authoritativeTemperature(),
                    CrucibleWorldHazards.SMALL_GAS_RANGE,
                    4);
        } else if (outcome.mutated()) {
            markMutation();
        }
        if (level != null && outcome.destroysHost()) {
            applyDestruction(outcome);
            return true;
        }
        return false;
    }

    private void applyDestruction(CrucibleProcessCore.TickOutcome outcome) {
        if (level == null) {
            return;
        }
        if (outcome.exploded()) {
            level.explode(
                    null,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    outcome.explodeRadius(),
                    false,
                    Level.ExplosionInteraction.BLOCK);
            level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        if (outcome.acidDestroyed()) {
            level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        CrucibleWorldHazards.boilHazards(
                level,
                worldPosition,
                process.authoritativeTemperature(),
                CrucibleWorldHazards.SMALL_GAS_RANGE,
                Math.max(1, (int) (TemperatureDamage.kelvin(
                        process.authoritativeTemperature()) / 25L)));
        level.setBlock(
                worldPosition,
                CrucibleWorldHazards.meltdownLavaState(),
                Block.UPDATE_ALL);
    }

    private boolean isThermallyQuiescent() {
        return process.isThermallyQuiescent();
    }

    private boolean isActiveProcess() {
        return !isThermallyQuiescent();
    }

    private void emitBoilingEffects() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(
                null,
                worldPosition,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.7F,
                1.3F);
        serverLevel.sendParticles(
                ParticleTypes.CLOUD,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.8,
                worldPosition.getZ() + 0.5,
                8,
                0.2,
                0.08,
                0.2,
                0.02);
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return switch (type) {
            case HEAT, CU -> true;
            case AIR, KINETIC, KINETIC_PUSH, KINETIC_ROTATION -> true;
            default -> false;
        };
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (process.casing().quarantined()
                || !handles(type, side)
                || size == 0L
                || amount <= 0L) {
            return 0L;
        }
        if (type == EnergyType.HEAT) {
            if (process.hasUnknownMaterials()) {
                return 0L;
            }
            long accepted = process.thermal().queueHeat(size, amount, simulate);
            if (!simulate && accepted > 0L) {
                checkpoint.markDirty();
            }
            return accepted;
        }
        if (type == EnergyType.CU) {
            long accepted = process.thermal().queueCooling(size, amount, simulate);
            if (!simulate && accepted > 0L) {
                checkpoint.markDirty();
            }
            return accepted;
        }
        int shownBefore = process.visibleAirUnits();
        long accepted = process.acceptKineticAir(size, amount, simulate);
        if (!simulate && accepted > 0L && process.visibleAirUnits() != shownBefore) {
            syncToClient();
        }
        return accepted;
    }

    @Override
    public long stored(EnergyType type) {
        return switch (type) {
            case HEAT -> process.thermal().totalStoredHeat();
            case AIR -> process.visibleAirUnits();
            default -> 0L;
        };
    }

    @Override
    public long capacity(EnergyType type) {
        return switch (type) {
            case HEAT -> HEAT_DISPLAY_CAPACITY;
            case AIR -> maxUnits();
            default -> 0L;
        };
    }

    public AirInjectionResult injectAir(long air) {
        if (process.casing().quarantined()) {
            return AirInjectionResult.INVALID_CHARGE;
        }
        SteelmakingController.InjectionResult result = process.insertAir(air);
        if (acceptsAir(result)) {
            checkpoint.markDirty();
        }
        return mapInjectionResult(result);
    }

    private static boolean acceptsAir(SteelmakingController.InjectionResult result) {
        return result == SteelmakingController.InjectionResult.STARTED
                || result == SteelmakingController.InjectionResult.CONTINUED;
    }

    private static AirInjectionResult mapInjectionResult(
            SteelmakingController.InjectionResult result) {
        return switch (result) {
            case STARTED -> AirInjectionResult.STARTED;
            case CONTINUED -> AirInjectionResult.CONTINUED;
            case TOO_COLD -> AirInjectionResult.TOO_COLD;
            case INVALID_CHARGE -> AirInjectionResult.INVALID_CHARGE;
        };
    }

    public CrucibleProcessCore process() {
        return process;
    }

    public IItemHandler itemHandler(Direction side) {
        return side == null || side == Direction.UP ? topInsert : EmptyRejectHandler.INSTANCE;
    }

    public ItemStackHandler inputBuffer() {
        return inputBuffer;
    }

    public InsertResult insert(MaterialUnits.Entry entry, float inputTemperature) {
        InsertResult result = process.insert(entry, inputTemperature);
        if (result == InsertResult.SUCCESS) {
            markVisibleMutation();
        }
        return result;
    }

    public void dropBuffer(Level level, BlockPos pos) {
        ItemStack stack = inputBuffer.getStackInSlot(0);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(
                    level, pos.getX(), pos.getY(), pos.getZ(), stack);
            inputBuffer.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    private void suckDroppedItems(Level level, BlockPos pos) {
        AABB box = new AABB(
                pos.getX() + SUCK_INSET,
                pos.getY() + SUCK_INSET,
                pos.getZ() + SUCK_INSET,
                pos.getX() + 1.0 - SUCK_INSET,
                pos.getY() + SUCK_INSET + 1.0,
                pos.getZ() + 1.0 - SUCK_INSET);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (!entity.isAlive() || entity.getItem().isEmpty()) {
                continue;
            }
            ItemStack stack = entity.getItem();
            ItemStack leftover = inputBuffer.insertItem(0, stack, false);
            if (leftover.getCount() == stack.getCount()) {
                continue;
            }
            entity.setItem(leftover);
            if (leftover.isEmpty()) {
                entity.discard();
            }
            break;
        }
    }

    private void ingestBuffer(Level level) {
        ItemStack stack = inputBuffer.getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }
        Optional<MaterialUnits.Entry> entry = MaterialUnits.resolve(stack);
        if (entry.isEmpty()) {
            inputBuffer.setStackInSlot(0, ItemStack.EMPTY);
            emitBoilingEffects();
            return;
        }
        InsertResult result = insert(
                entry.get(), ItemHeat.temperature(stack, level.getGameTime()));
        if (result == InsertResult.SUCCESS) {
            stack.shrink(1);
            inputBuffer.setStackInSlot(0, stack);
        }
    }

    public IFluidHandler externalFluids() {
        return process.fluids();
    }

    public Optional<MaterialDefinition> castIngot() {
        Optional<MaterialDefinition> result = process.castIngot();
        if (result.isPresent()) {
            markVisibleMutation();
        }
        return result;
    }

    public Optional<CastTransfer> cast(MaterialPrefix form) {
        Optional<CrucibleProcessCore.CastTransfer> result = process.cast(form);
        if (result.isPresent()) {
            markVisibleMutation();
        }
        return result.map(transfer -> new CastTransfer(
                transfer.material(),
                transfer.form(),
                transfer.count(),
                transfer.temperature()));
    }

    @Override
    public boolean fillMoldAtSide(MoldHost mold, Direction crucibleSide, Direction moldSide) {
        if (process.fillMoldAtSide(mold, moldSide)) {
            markVisibleMutation();
            return true;
        }
        return false;
    }

    @Override
    public boolean isMoldInputSide(Direction side) {
        return side == Direction.UP;
    }

    @Override
    public float moldMaxTemperatureCelsius() {
        return process.casing().maxTemperature();
    }

    @Override
    public int moldRequiredMaterialUnits() {
        return 1;
    }

    @Override
    public int fillMold(
            String materialId,
            int availableUnits,
            float temperature,
            Direction side) {
        if (!isMoldInputSide(side)) {
            return 0;
        }
        int consumed = process.acceptMoldPour(materialId, availableUnits, temperature);
        if (consumed > 0) {
            markVisibleMutation();
        }
        return consumed;
    }

    public float temperature() {
        return process.temperature(level != null && level.isClientSide);
    }

    /** Display helper; temperature is already Celsius. */
    public float temperatureCelsius() {
        return temperature();
    }

    @Override
    public float temperatureCelsius(Direction side) {
        return temperatureCelsius();
    }

    @Override
    public float temperatureMaxCelsius(Direction side) {
        return casingMaxTemperature();
    }

    public boolean isMolten() {
        return process.isMolten();
    }

    public float fillFraction() {
        return process.fillFraction();
    }

    public Map<String, Integer> composition() {
        return process.composition();
    }

    public Map<String, Integer> displayComposition() {
        return process.displayComposition();
    }

    public int totalUnits() {
        return process.totalUnits();
    }

    public int moltenColor() {
        return process.moltenColor();
    }

    public long storedAir() {
        return process.storedAir();
    }

    public String casingMaterialId() {
        return process.casing().materialId();
    }

    public void setCasingMaterialId(String materialId) {
        if (process.casing().setMaterialId(materialId)) {
            setChanged();
            syncToClient();
            checkpoint.synced();
        }
    }

    public int casingTier() {
        return process.casing().materialTier();
    }

    public int processingTier() {
        return process.casing().processingTier();
    }

    public boolean casingMaterialQuarantined() {
        return process.casing().quarantined();
    }

    public String quarantinedCasingMaterialId() {
        return process.casing().quarantinedMaterialId();
    }

    public float casingMaxTemperature() {
        return process.casingMaxTemperature();
    }

    /** Remainder HU in the GT6-style thermal accumulator. */
    public long bufferedHeatHu() {
        return process.thermal().storedEnergy();
    }

    public boolean hasCacheSlot() {
        return !inputBuffer.getStackInSlot(0).isEmpty();
    }

    public boolean processActive() {
        return process.steelmakingActive() || !process.thermal().isQuiescent();
    }

    public String renderState() {
        if (processActive() && isMolten()) {
            return "active";
        }
        if (totalUnits() <= 0) {
            return "empty";
        }
        if (isMolten()) {
            return "molten";
        }
        return "solid";
    }

    public boolean steelmakingActive() {
        return process.steelmakingActive();
    }

    public boolean hasUnknownMaterials() {
        return process.hasUnknownMaterials();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        restoreState(tag, false);
        loadBuffer(tag, registries);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readClientTag(tag, registries);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            readClientTag(tag, registries);
        }
    }

    private void readClientTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        restoreState(tag, true);
        loadBuffer(tag, registries);
    }

    private void restoreState(CompoundTag tag, boolean clientUpdate) {
        process.restore(tag, clientUpdate);
        SmelteryHosts.bakedMaterial(getBlockState())
                .ifPresent(process.casing()::setMaterialId);
    }

    private void loadBuffer(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("input_buffer")) {
            inputBuffer.deserializeNBT(registries, tag.getCompound("input_buffer"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        process.save(tag);
        tag.put("input_buffer", inputBuffer.serializeNBT(registries));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return process.clientTag();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void markMutation() {
        setChanged();
        checkpoint.checkpointed();
        checkpoint.markSyncPending();
    }

    private void markVisibleMutation() {
        setChanged();
        checkpoint.checkpointed();
        syncToClient();
        checkpoint.synced();
    }

    public record CastTransfer(
            MaterialDefinition material,
            MaterialPrefix form,
            int count,
            float temperature) {}

    public static int maxUnits() {
        return MaterialPrefixes.INGOT.units() * MAX_INGOTS;
    }

    public enum AirInjectionResult {
        STARTED,
        CONTINUED,
        TOO_COLD,
        INVALID_CHARGE
    }

    private final class TopInsertHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inputBuffer.getStackInSlot(0);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!inputBuffer.getStackInSlot(0).isEmpty()) {
                return stack;
            }
            return inputBuffer.insertItem(0, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inputBuffer.getSlotLimit(0);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return inputBuffer.isItemValid(0, stack);
        }
    }

    private static final class EmptyRejectHandler implements IItemHandler {
        private static final EmptyRejectHandler INSTANCE = new EmptyRejectHandler();

        @Override
        public int getSlots() {
            return 0;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }
}
