package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.air.AirOutputModel;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.item.CeramicMoldBlockItem;
import com.masson.cruciblecraft.content.mold.CruciblePour;
import com.masson.cruciblecraft.content.mold.MoldHost;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortAggregator;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortHost;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator;
import com.masson.cruciblecraft.content.multiblock.PluginQuarantinePolicy;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.InsertResult;
import com.masson.cruciblecraft.heat.ItemHeat;
import com.masson.cruciblecraft.heat.TemperatureDamage;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.component.CrucibleProcessCore;
import com.masson.cruciblecraft.machine.component.SteelmakingController;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModMultiblockPlugins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Bounded thermal/steelmaking multiblock. Not a processing_host: the
 * fourth behavior family reuses CrucibleProcessCore with 432-ingot
 * capacity, HU on the bottom energy layer, and mold slots in the
 * controller inventory.
 */
public final class LargeCrucibleBlockEntity extends BlockEntity
        implements MultiblockControllerBinding, MultiblockPortHost, CruciblePour {
    private static final String PLUGIN_TAG = "multiblock_plugins";
    public static final ResourceLocation STRUCTURE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "large_crucible");
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_MOLD = 1;
    public static final int SLOT_OUTPUT = 2;

    private final CrucibleProcessCore process = CrucibleProcessCore.large();
    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_OUTPUT) {
                return false;
            }
            if (slot == SLOT_MOLD) {
                return stack.getItem() instanceof CeramicMoldBlockItem;
            }
            return true;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final FluidTank molten = new ProcessFluidTank();
    private final List<FluidTank> tanks = List.of(molten);

    private boolean structureValid;
    private Set<BlockPos> boundPorts = Set.of();
    private MultiblockStructureValidator.ValidationResult lastValidation;
    private boolean pluginQuarantined;
    private String pluginQuarantineReason = "";
    private boolean outputJammed;

    public LargeCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LARGE_CRUCIBLE.get(), pos, state);
        process.setOnMutation(this::setChanged);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LargeCrucibleBlockEntity crucible) {
        long phaseKey = CheckpointDecisions.phaseKey(
                pos.getX(), pos.getY(), pos.getZ());
        if (CheckpointDecisions.onPositionPhase(
                level.getGameTime(), phaseKey, 20)) {
            crucible.recheckStructure(level, pos, state);
        }
        crucible.process.setFrozen(crucible.pluginQuarantined);
        if (crucible.pluginQuarantined) {
            return;
        }
        if (!crucible.structureValid) {
            crucible.process.driftTowardAmbient();
            return;
        }
        crucible.tickProcess(level);
    }

    private void tickProcess(Level level) {
        if (!outputJammed) {
            suckDroppedItems(level);
            ingestInput(level);
        }
        process.addRainWater(
                level.getGameTime(),
                level.isRainingAt(worldPosition.above(2)) ? 1.0F : 0.0F,
                level.isThundering());
        long incoming = process.thermal().takePendingHeat();
        CrucibleProcessCore.TickOutcome outcome = process.advance(incoming, true);
        if (outcome.boiled()) {
            CrucibleWorldHazards.boilHazards(
                    level,
                    worldPosition,
                    process.authoritativeTemperature(),
                    CrucibleWorldHazards.LARGE_GAS_RANGE,
                    4);
        }
        if (outcome.destroysHost()) {
            applyDestruction(level, outcome);
            return;
        }
        tryCast(level);
    }

    private void suckDroppedItems(Level level) {
        if (!inventory.getStackInSlot(SLOT_INPUT).isEmpty()) {
            return;
        }
        AABB box = new AABB(
                worldPosition.getX() - 0.5,
                worldPosition.getY() + 2.0 / 16.0,
                worldPosition.getZ() - 0.5,
                worldPosition.getX() + 1.5,
                worldPosition.getY() + 2.0 / 16.0 + 3.0,
                worldPosition.getZ() + 1.5);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (!entity.isAlive() || entity.getItem().isEmpty()) {
                continue;
            }
            ItemStack stack = entity.getItem();
            int before = stack.getCount();
            ItemStack leftover = inventory.insertItem(SLOT_INPUT, stack, false);
            if (leftover.getCount() == before) {
                continue;
            }
            entity.setItem(leftover);
            if (leftover.isEmpty()) {
                entity.discard();
            }
            break;
        }
    }

    private void applyDestruction(Level level, CrucibleProcessCore.TickOutcome outcome) {
        BlockPos origin = worldPosition.immutable();
        if (outcome.exploded()) {
            level.explode(
                    null,
                    origin.getX() + 0.5,
                    origin.getY() + 0.5,
                    origin.getZ() + 0.5,
                    outcome.explodeRadius(),
                    false,
                    Level.ExplosionInteraction.BLOCK);
            return;
        }
        if (outcome.acidDestroyed()) {
            level.setBlock(origin, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        CrucibleWorldHazards.boilHazards(
                level,
                origin,
                process.authoritativeTemperature(),
                CrucibleWorldHazards.LARGE_GAS_RANGE,
                Math.max(1, (int) (TemperatureDamage.kelvin(
                        process.authoritativeTemperature()) / 25L)));
        fillMeltdownLava(level, origin);
    }

    static void fillMeltdownLava(Level level, BlockPos origin) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlock(
                            origin.offset(dx, dy, dz),
                            Blocks.LAVA.defaultBlockState(),
                            Block.UPDATE_ALL);
                }
            }
        }
    }

    private void ingestInput(Level level) {
        ItemStack stack = inventory.getStackInSlot(SLOT_INPUT);
        if (stack.isEmpty()) {
            return;
        }
        Optional<MaterialUnits.Entry> entry = MaterialUnits.resolve(stack);
        if (entry.isEmpty()) {
            inventory.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.playSound(
                        null,
                        worldPosition,
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS,
                        0.7F,
                        1.3F);
            }
            return;
        }
        InsertResult result = process.insert(
                entry.get(),
                ItemHeat.temperature(stack, level.getGameTime()));
        if (result == InsertResult.SUCCESS) {
            stack.shrink(1);
            inventory.setStackInSlot(SLOT_INPUT, stack);
        } else if (result == InsertResult.FULL) {
            outputJammed = true;
        }
    }

    private void tryCast(Level level) {
        ItemStack mold = inventory.getStackInSlot(SLOT_MOLD);
        if (!(mold.getItem() instanceof CeramicMoldBlockItem moldItem)) {
            return;
        }
        Optional<CrucibleProcessCore.CastTransfer> preview =
                process.previewCast(moldItem.shape().form());
        if (preview.isEmpty()) {
            return;
        }
        ItemStack produced = MaterialLookup.tryStack(
                        preview.get().material().id(),
                        preview.get().form(),
                        preview.get().count())
                .orElse(ItemStack.EMPTY);
        if (produced.isEmpty()) {
            return;
        }
        ItemHeat.set(produced, preview.get().temperature(), level.getGameTime());
        ItemStack existing = inventory.getStackInSlot(SLOT_OUTPUT);
        if (!existing.isEmpty()
                && (!ItemStack.isSameItemSameComponents(existing, produced)
                        || existing.getCount() + produced.getCount()
                                > existing.getMaxStackSize())) {
            outputJammed = true;
            return;
        }
        if (process.cast(moldItem.shape().form()).isEmpty()) {
            return;
        }
        if (existing.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, produced);
        } else {
            existing.grow(produced.getCount());
            inventory.setStackInSlot(SLOT_OUTPUT, existing);
        }
        outputJammed = false;
    }

    public CrucibleProcessCore process() {
        return process;
    }

    public boolean pluginQuarantined() {
        return pluginQuarantined;
    }

    public String pluginQuarantineReason() {
        return pluginQuarantineReason;
    }

    public boolean outputJammed() {
        return outputJammed;
    }

    public InsertResult insertMaterial(
            MaterialUnits.Entry entry, float temperature) {
        process.setFrozen(pluginQuarantined);
        return process.insert(entry, temperature);
    }

    @Override
    public boolean fillMoldAtSide(MoldHost mold, Direction crucibleSide, Direction moldSide) {
        if (!structureValid || pluginQuarantined) {
            return false;
        }
        return process.fillMoldAtSide(mold, moldSide);
    }

    /**
     * GT6 middle-layer {@code ONLY_CRUCIBLE} casings have no BE here, so a
     * mold or faucet next to a dummy wall still has to reach the controller.
     */
    public static LargeCrucibleBlockEntity pourHostAtWall(
            BlockGetter level, BlockPos wall) {
        if (level == null
                || !level.getBlockState(wall).is(ModBlocks.MULTIBLOCK_CASING.get())) {
            return null;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (!(level.getBlockEntity(wall.offset(-dx, -1, -dz))
                        instanceof LargeCrucibleBlockEntity crucible)) {
                    continue;
                }
                if (crucible.structureValid() && !crucible.pluginQuarantined()) {
                    return crucible;
                }
            }
        }
        return null;
    }

    public SteelmakingController.InjectionResult injectAir(long air) {
        process.setFrozen(pluginQuarantined);
        return process.insertAir(air);
    }

    public Map<String, Integer> composition() {
        return process.composition();
    }

    private void recheckStructure(
            Level level,
            BlockPos pos,
            BlockState state) {
        var definition = MultiblockStructureCatalog.find(STRUCTURE_ID);
        if (definition.isEmpty()) {
            clearBindings();
            lastValidation = null;
            updateStructureValid(false);
            return;
        }
        MultiblockStructureValidator.ValidationResult validation =
                MultiblockStructureValidator.validate(
                        definition.orElseThrow(),
                        level,
                        pos,
                        state.getValue(ProcessingMachineBlock.FACING));
        lastValidation = validation;
        boundPorts = MultiblockPortAggregator.refresh(
                level, pos, STRUCTURE_ID, validation, boundPorts);
        updateStructureValid(validation.valid());
    }

    private void updateStructureValid(boolean valid) {
        if (structureValid != valid) {
            structureValid = valid;
            setChanged();
        }
    }

    public void clearBindings() {
        if (level != null && !level.isClientSide) {
            MultiblockPortAggregator.unbindLoaded(
                    level, worldPosition, boundPorts);
        }
        boundPorts = Set.of();
        structureValid = false;
    }

    public MultiblockStructureValidator.ValidationResult lastValidation() {
        return lastValidation;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        process.save(tag);
        tag.put("inventory", inventory.serializeNBT(registries));
        ListTag ids = new ListTag();
        for (ResourceLocation id
                : ModMultiblockPlugins.LARGE_CRUCIBLE_PLUGINS) {
            ids.add(StringTag.valueOf(id.toString()));
        }
        tag.put(PLUGIN_TAG, ids);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        process.restore(tag, false);
        if (tag.contains("inventory", Tag.TAG_COMPOUND)) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        ListTag ids = tag.getList(PLUGIN_TAG, Tag.TAG_STRING);
        List<String> saved = new ArrayList<>();
        for (Tag entry : ids) {
            saved.add(entry.getAsString());
        }
        pluginQuarantined = false;
        pluginQuarantineReason = "";
        int index = 0;
        for (ResourceLocation current
                : ModMultiblockPlugins.LARGE_CRUCIBLE_PLUGINS) {
            String savedId = index < saved.size() ? saved.get(index) : "";
            PluginQuarantinePolicy.Decision decision =
                    PluginQuarantinePolicy.resolve(
                            savedId, current.toString());
            if (decision.resolution()
                    == PluginQuarantinePolicy.Resolution.QUARANTINED) {
                pluginQuarantined = true;
                pluginQuarantineReason =
                        decision.quarantineReason().orElse("");
            }
            index++;
        }
        process.setFrozen(pluginQuarantined);
        if (pluginQuarantined) {
            setChanged();
        }
    }

    @Override
    public ResourceLocation structureId() {
        return STRUCTURE_ID;
    }

    @Override
    public boolean structureValid() {
        return structureValid;
    }

    @Override
    public ProcessingMachineBlockEntity processingHost() {
        return null;
    }

    @Override
    public MultiblockPortHost portHost() {
        return this;
    }

    @Override
    public ItemStackHandler inventory() {
        return inventory;
    }

    @Override
    public List<FluidTank> tanks() {
        return tanks;
    }

    @Override
    public List<Integer> itemInputSlots() {
        return List.of(SLOT_INPUT, SLOT_MOLD);
    }

    @Override
    public List<Integer> itemOutputSlots() {
        return List.of(SLOT_OUTPUT);
    }

    @Override
    public List<Integer> fluidInputTanks() {
        return List.of(0);
    }

    @Override
    public List<Integer> fluidOutputTanks() {
        return List.of(0);
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
        if (!structureValid
                || pluginQuarantined
                || !handles(type, side)
                || size == 0L
                || amount <= 0L) {
            return 0L;
        }
        if (type == EnergyType.HEAT) {
            if (process.hasUnknownMaterials()) {
                return 0L;
            }
            return process.thermal().queueHeat(size, amount, simulate);
        }
        if (type == EnergyType.CU) {
            return process.thermal().queueCooling(size, amount, simulate);
        }
        if (level == null || !level.getBlockState(worldPosition.above()).isAir()) {
            return 0L;
        }
        SteelmakingController.InjectionResult acceptance =
                process.previewAirInjection();
        if (acceptance != SteelmakingController.InjectionResult.STARTED
                && acceptance != SteelmakingController.InjectionResult.CONTINUED) {
            return 0L;
        }
        long room = Math.max(
                0L,
                AirOutputModel.MAX_STORED_AIR - process.storedAir());
        long accepted = Math.min(amount, EnergyPackets.packetsForUnits(size, room));
        if (!simulate && accepted > 0L) {
            process.insertAir(EnergyPackets.units(size, accepted));
        }
        return accepted;
    }

    @Override
    public long stored(EnergyType type) {
        return switch (type) {
            case HEAT -> process.thermal().totalStoredHeat();
            case AIR -> Math.max(0L, process.storedAir());
            default -> 0L;
        };
    }

    @Override
    public long capacity(EnergyType type) {
        return switch (type) {
            case HEAT -> CrucibleProcessCore.HEAT_DISPLAY_CAPACITY;
            case AIR -> AirOutputModel.MAX_STORED_AIR;
            default -> 0L;
        };
    }

    @Override
    public BlockState blockState() {
        return getBlockState();
    }

    private final class ProcessFluidTank extends FluidTank {
        private ProcessFluidTank() {
            super(process.maxUnits());
        }

        @Override
        public FluidStack getFluid() {
            return process.fluids().getFluidInTank(0).copy();
        }

        @Override
        public int getFluidAmount() {
            return getFluid().getAmount();
        }

        @Override
        public int getCapacity() {
            return process.maxUnits();
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return process.fluids().isFluidValid(0, stack);
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            if (pluginQuarantined || !structureValid) {
                return 0;
            }
            int filled = process.fluids().fill(resource, action);
            if (filled > 0 && action.execute()) {
                outputJammed = false;
            }
            return filled;
        }

        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            if (pluginQuarantined) {
                return FluidStack.EMPTY;
            }
            return process.fluids().drain(maxDrain, action);
        }

        @Override
        public FluidStack drain(
                FluidStack resource, IFluidHandler.FluidAction action) {
            if (pluginQuarantined) {
                return FluidStack.EMPTY;
            }
            return process.fluids().drain(resource, action);
        }
    }
}
