package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.block.LargeCrucibleHosts;
import com.masson.cruciblecraft.content.block.LargeCrucibleWalls;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.mold.CastingMolds;
import com.masson.cruciblecraft.content.mold.CruciblePour;
import com.masson.cruciblecraft.content.mold.MoldHost;
import com.masson.cruciblecraft.content.mold.MoldRecipes;
import com.masson.cruciblecraft.content.sensor.TemperatureHost;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortAggregator;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortHost;
import com.masson.cruciblecraft.content.multiblock.PortStoreSync;
import com.masson.cruciblecraft.content.multiblock.PortStore;
import com.masson.cruciblecraft.content.multiblock.PortStoreCarrier;
import com.masson.cruciblecraft.content.multiblock.PortStoreRegistry;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator;
import com.masson.cruciblecraft.content.multiblock.PluginQuarantinePolicy;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.InsertResult;
import com.masson.cruciblecraft.heat.ItemHeat;
import com.masson.cruciblecraft.heat.TemperatureDamage;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.component.CheckpointTracker;
import com.masson.cruciblecraft.machine.component.CrucibleProcessCore;
import com.masson.cruciblecraft.machine.component.SteelmakingController;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModBlockTags;
import com.masson.cruciblecraft.registry.ModMultiblockPlugins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
        implements MultiblockControllerBinding, MultiblockPortHost, CruciblePour, MoldHost,
        TemperatureHost {
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
                return CastingMolds.isMoldItem(stack);
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
    private Set<BlockPos> boundPortStores = Set.of();
    private MultiblockStructureValidator.ValidationResult lastValidation;
    private boolean pluginQuarantined;
    private String pluginQuarantineReason = "";
    private boolean outputJammed;
    private final CheckpointTracker checkpoint = new CheckpointTracker();

    public LargeCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LARGE_CRUCIBLE.get(), pos, state);
        process.setOnMutation(this::markMutation);
        process.casing().setMaterialId(LargeCrucibleHosts.DEFAULT_MATERIAL);
        LargeCrucibleHosts.bakedMaterial(state)
                .ifPresent(process.casing()::setMaterialId);
    }

    public void setCasingMaterialId(String materialId) {
        if (LargeCrucibleHosts.isAllowed(materialId)
                && process.casing().setMaterialId(materialId)) {
            setChanged();
            syncToClient();
        }
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LargeCrucibleBlockEntity crucible) {
        long phaseKey = CheckpointDecisions.phaseKey(
                pos.getX(), pos.getY(), pos.getZ());
        if (crucible.lastValidation == null
                || CheckpointDecisions.onPositionPhase(
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
        float previousTemperature = crucible.process.thermal().authoritativeTemperature();
        long previousStoredEnergy = crucible.process.thermal().storedEnergy();
        int previousCooldown = crucible.process.thermal().cooldownTicks();
        long previousAir = crucible.process.steelmaking().storedAir();
        int previousReactionTicks = crucible.process.steelmaking().reactionTicks();
        int shownAir = crucible.process.visibleAirUnits();
        PortStoreSync.pullInputs(crucible);
        crucible.tickProcess(level);
        if (crucible.process.visibleAirUnits() != shownAir) {
            crucible.syncToClient();
        }
        PortStoreSync.pushOutputs(crucible);
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
        if (crucible.checkpoint.shouldCheckpoint(level.getGameTime(), phaseKey, 20)) {
            crucible.setChanged();
            crucible.checkpoint.checkpointed();
        }
        if (crucible.checkpoint.shouldSync(
                !crucible.process.isThermallyQuiescent(),
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
            LargeCrucibleBlockEntity crucible) {
        crucible.process.thermal().clientTick();
    }

    private void tickProcess(Level level) {
        if (outputStackFull()) {
            outputJammed = true;
        }
        if (!outputJammed) {
            suckDroppedItems(level);
            ingestInput(level);
        }
        CrucibleProcessCore.TickOutcome outcome = null;
        if (!outputStackFull()) {
            process.addRainWater(
                    level.getGameTime(),
                    level.isRainingAt(worldPosition.above(2)) ? 1.0F : 0.0F,
                    level.isThundering());
            long incoming = process.thermal().takePendingHeat();
            outcome = process.advance(incoming, true);
        }
        if (outcome != null && outcome.boiled()) {
            CrucibleWorldHazards.boilHazards(
                    level,
                    worldPosition,
                    process.authoritativeTemperature(),
                    CrucibleWorldHazards.LARGE_GAS_RANGE,
                    4);
        }
        if (outcome != null && outcome.destroysHost()) {
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
                            CrucibleWorldHazards.meltdownLavaState(),
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
        Optional<MaterialPrefix> recipe = CastingMolds.castingForm(mold);
        if (recipe.isEmpty()) {
            return;
        }
        Optional<CrucibleProcessCore.CastTransfer> preview =
                process.previewCast(recipe.get());
        if (preview.isEmpty()) {
            return;
        }
        var solid = MoldRecipes.solidifyingMaterial(preview.get().material());
        MaterialPrefix outputForm = MoldRecipes.outputForm(
                recipe.get(), preview.get().material());
        if (!MaterialCatalog.isFormRegistered(solid, outputForm)) {
            return;
        }
        ItemStack produced = MaterialLookup.tryStack(
                        solid.id(),
                        outputForm,
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
        if (process.cast(recipe.get()).isEmpty()) {
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

    public float temperature() {
        return process.temperature(level != null && level.isClientSide);
    }

    @Override
    public float temperatureCelsius(Direction side) {
        return temperature();
    }

    @Override
    public float temperatureMaxCelsius(Direction side) {
        return moldMaxTemperatureCelsius();
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

    private boolean outputStackFull() {
        ItemStack existing = inventory.getStackInSlot(SLOT_OUTPUT);
        return !existing.isEmpty()
                && existing.getCount() >= existing.getMaxStackSize();
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

    @Override
    public boolean isMoldInputSide(Direction side) {
        return side == Direction.UP && structureValid && !pluginQuarantined;
    }

    @Override
    public float moldMaxTemperatureCelsius() {
        return process.casing().maxTemperature(
                CrucibleProcessCore.LARGE_HEAT_RESISTANCE);
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
        return process.acceptMoldPour(materialId, availableUnits, temperature);
    }

    /**
     * GT6 middle-layer {@code ONLY_CRUCIBLE} casings have no BE here, so a
     * mold or faucet next to a dummy wall still has to reach the controller.
     */
    public static LargeCrucibleBlockEntity pourHostAtWall(
            BlockGetter level, BlockPos wall) {
        LargeCrucibleBlockEntity host = LargeCrucibleWalls.controllerAt(level, wall);
        if (host == null) {
            return null;
        }
        return wall.getY() - host.getBlockPos().getY() == 1 ? host : null;
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
        Direction facing = LargeCrucibleHosts.horizontalFacing(state);
        MultiblockStructureValidator.ValidationResult validation =
                MultiblockStructureValidator.validate(
                        definition.orElseThrow(),
                        level,
                        pos,
                        facing);
        lastValidation = validation;
        boundPorts = MultiblockPortAggregator.refresh(
                level, pos, STRUCTURE_ID, validation, boundPorts);
        updateStructureValid(
                validation.valid()
                        && wallsMatchCasing(
                                level,
                                definition.orElseThrow(),
                                pos,
                                facing));
        refreshPortStores(
                level,
                definition.orElseThrow(),
                pos,
                facing,
                structureValid);
        applyFormedVisuals(structureValid);
    }

    private void refreshPortStores(
            Level level,
            MultiblockStructureDefinition definition,
            BlockPos controller,
            Direction facing,
            boolean valid) {
        if (!valid) {
            PortStoreRegistry.clear(this);
            boundPortStores = Set.of();
            return;
        }
        Set<BlockPos> desired = new java.util.LinkedHashSet<>();
        for (var element : definition.structure()) {
            if (definition.predicate(element).kind()
                    != PredicateKind.TAG
                    || !ModBlockTags.LARGE_CRUCIBLE_WALLS.location().equals(
                            definition.predicate(element).tag().orElse(null))) {
                continue;
            }
            BlockPos world = definition.worldPosition(
                    controller, facing, element.offset());
            if (level.getBlockEntity(world)
                    instanceof MteInPlaceBlockEntity wall
                    && wall instanceof PortStoreCarrier carrier) {
                PortStore.Assignment assignment = element.offset().y() == 2
                        ? new PortStore.Assignment(
                                List.of(SLOT_INPUT, SLOT_MOLD),
                                List.of(),
                                List.of(),
                                List.of())
                        : PortStore.Assignment.EMPTY;
                carrier.configurePortStore(this, assignment);
                PortStoreRegistry.bind(this, world, carrier);
                desired.add(world.immutable());
            }
        }
        boundPortStores = Set.copyOf(desired);
    }

    private boolean wallsMatchCasing(
            Level level,
            MultiblockStructureDefinition definition,
            BlockPos controller,
            Direction facing) {
        String material = process.casing().materialId();
        for (var element : definition.structure()) {
            var predicate = definition.predicate(element);
            if (predicate.kind() != PredicateKind.TAG
                    || !ModBlockTags.LARGE_CRUCIBLE_WALLS.location().equals(
                            predicate.tag().orElse(null))) {
                continue;
            }
            BlockPos world = definition.worldPosition(controller, facing, element.offset());
            if (!(level.getBlockState(world).getBlock() instanceof MteInPlaceBlock inplace)
                    || !LargeCrucibleHosts.isWall(inplace.spec())
                    || !material.equals(LargeCrucibleHosts.materialId(inplace.spec()))) {
                return false;
            }
        }
        return true;
    }

    private void updateStructureValid(boolean valid) {
        if (structureValid != valid) {
            structureValid = valid;
            setChanged();
            checkpoint.markSyncPending();
            if (level != null && !level.isClientSide) {
                invalidateCrucibleCapabilities();
                syncToClient();
            }
        }
    }

    private void invalidateCrucibleCapabilities() {
        level.invalidateCapabilities(worldPosition);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    level.invalidateCapabilities(worldPosition.offset(dx, dy, dz));
                }
            }
        }
    }

    public void clearBindings() {
        if (level != null && !level.isClientSide) {
            MultiblockPortAggregator.unbindLoaded(
                    level, worldPosition, boundPorts);
            PortStoreRegistry.clear(this);
            applyFormedVisuals(false);
        }
        boundPorts = Set.of();
        boundPortStores = Set.of();
        structureValid = false;
    }

    private void applyFormedVisuals(boolean formed) {
        if (level == null || level.isClientSide) {
            return;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos part = worldPosition.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(part);
                    if (!state.hasProperty(LargeCrucibleHosts.FORMED)
                            || state.getValue(LargeCrucibleHosts.FORMED)
                                    == formed) {
                        continue;
                    }
                    level.setBlock(
                            part,
                            state.setValue(LargeCrucibleHosts.FORMED, formed),
                            Block.UPDATE_CLIENTS);
                }
            }
        }
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
        LargeCrucibleHosts.bakedMaterial(getBlockState())
                .ifPresent(process.casing()::setMaterialId);
        if (pluginQuarantined) {
            setChanged();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = process.clientTag();
        tag.putBoolean("structure_valid", structureValid);
        tag.putBoolean("plugin_quarantined", pluginQuarantined);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        process.restore(tag, true);
        if (tag.contains("structure_valid")) {
            structureValid = tag.getBoolean("structure_valid");
        }
        if (tag.contains("plugin_quarantined")) {
            pluginQuarantined = tag.getBoolean("plugin_quarantined");
        }
        LargeCrucibleHosts.bakedMaterial(getBlockState())
                .ifPresent(process.casing()::setMaterialId);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            handleUpdateTag(tag, registries);
        }
    }

    private void markMutation() {
        setChanged();
        checkpoint.checkpointed();
        checkpoint.markSyncPending();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
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
    public void requestBuilderRecheck() {
        if (level != null && !level.isClientSide) {
            recheckStructure(level, worldPosition, getBlockState());
        }
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
            case HEAT -> CrucibleProcessCore.HEAT_DISPLAY_CAPACITY;
            case AIR -> process.maxUnits();
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
            if (filled > 0 && action.execute() && !outputStackFull()) {
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
