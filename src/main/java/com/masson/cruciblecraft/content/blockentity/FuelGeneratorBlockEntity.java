package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.machine.generation.FuelGeneratorEnergy;
import com.masson.cruciblecraft.machine.generation.FuelGeneratorIdentityPolicy;
import com.masson.cruciblecraft.machine.generation.FuelGeneratorSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Signed-eut fuel consumer. Recipe power is unit-buffered independently from
 * the configured output packet, and every exhaust/energy mutation is gated by
 * a simulation-first check.
 */
public final class FuelGeneratorBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private static final String SCHEMA_KEY =
            "fuel_generator_schema_version";
    private static final String IDENTITY_KEY = "fuel_generator_id";
    private static final String ENERGY_IDENTITY_KEY = "energy_identity";
    private static final String QUARANTINE_KEY =
            "fuel_generator_quarantine";
    private final FuelGeneratorSpec spec;
    private final FuelGeneratorEnergy energy;
    private final FluidTank input;
    private final List<FluidTank> outputs;
    private final IFluidHandler inputView = new InputHandler();
    private final List<IFluidHandler> outputViews;
    private final PerTickEnergyBudget outputBudget =
            new PerTickEnergyBudget();
    private ResourceLocation activeRecipe;
    private int progress;
    private int duration;
    private String status = "idle";
    private boolean clientSyncPending;
    private long lastClientSyncGameTime = Long.MIN_VALUE;
    private FuelGeneratorIdentityPolicy.Identity persistedIdentity;
    private String identityQuarantine = "";
    private boolean quarantineWarningLogged;

    public FuelGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_GENERATOR.get(), pos, state);
        if (!(state.getBlock() instanceof FuelGeneratorBlock block)) {
            throw new IllegalArgumentException(
                    "Fuel generator block entity requires a configured block");
        }
        spec = block.spec();
        energy = new FuelGeneratorEnergy(
                spec.outputPacketSize(), spec.energyCapacity());
        persistedIdentity = FuelGeneratorIdentityPolicy.current(spec);
        input = new FluidTank(
                spec.inputCapacityMb(), spec::acceptsInput) {
            @Override
            protected void onContentsChanged() {
                markPersistentMutation();
            }
        };
        List<FluidTank> created = new ArrayList<>();
        for (int index = 0; index < spec.outputTanks(); index++) {
            created.add(new FluidTank(spec.outputCapacityMb()) {
                @Override
                protected void onContentsChanged() {
                    markPersistentMutation();
                }
            });
        }
        outputs = List.copyOf(created);
        List<IFluidHandler> views = new ArrayList<>();
        for (int index = 0; index < outputs.size(); index++) {
            views.add(new OutputHandler(index));
        }
        outputViews = List.copyOf(views);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!quarantineWarningLogged
                && !identityQuarantine.isBlank()
                && level != null
                && !level.isClientSide) {
            quarantineWarningLogged = true;
            CrucibleCraft.LOGGER.warn(
                    "Quarantined fuel generator {} at {} {}: {}",
                    spec.id(),
                    level.dimension().location(),
                    worldPosition,
                    identityQuarantine);
        }
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FuelGeneratorBlockEntity generator) {
        generator.tickGeneration();
        long delivered = EnergyEmitter.emit(
                level,
                pos,
                generator,
                generator.spec.outputEnergyType(),
                generator.energyOutputSide());
        if (delivered == 0L
                && generator.energy.stored()
                        >= generator.spec.outputPacketSize()) {
            generator.setStatus("energy_output_blocked");
        }
        generator.updateLitState();
        generator.flushClientSync(level.getGameTime());
    }

    private void tickGeneration() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!identityQuarantine.isBlank()) {
            setStatus("identity_quarantined");
            return;
        }
        GTRecipe recipe;
        if (activeRecipe == null) {
            if (input.isEmpty()) {
                reset("idle");
                return;
            }
            RecipeMap.Match match = spec.requireRecipeMap().findMatch(
                            new GTRecipeQuery(
                                    List.of(),
                                    List.of(input.getFluid())))
                    .orElse(null);
            if (match == null) {
                reset("invalid_fuel");
                return;
            }
            recipe = match.recipe();
            if (spec.validate(recipe).isPresent()) {
                reset("invalid_recipe");
                return;
            }
            if (!hasOutputRoom(recipe)) {
                setStatus("exhaust_blocked");
                return;
            }
            long firstTickEnergy =
                    spec.generatedEnergyAtTick(recipe, 0);
            if (firstTickEnergy > 0L
                    && !energy.canGenerate(firstTickEnergy)) {
                setStatus("energy_output_blocked");
                return;
            }
            consumeFuel(recipe);
            activeRecipe = match.id();
            progress = 0;
            duration = recipe.duration();
            markPersistentMutation();
        } else {
            recipe = spec.requireRecipeMap().entry(activeRecipe)
                    .map(RecipeMap.Entry::recipe)
                    .orElse(null);
            if (recipe == null || spec.validate(recipe).isPresent()) {
                reset("invalid_recipe");
                return;
            }
        }
        if (!hasOutputRoom(recipe)) {
            setStatus("exhaust_blocked");
            return;
        }
        long generatedThisTick =
                spec.generatedEnergyAtTick(recipe, progress);
        if (generatedThisTick > 0L
                && !energy.canGenerate(generatedThisTick)) {
            setStatus("energy_output_blocked");
            return;
        }
        boolean completes = progress + 1 >= duration;
        if (completes) {
            commitOutputs(recipe);
        }
        if (generatedThisTick > 0L) {
            energy.generate(generatedThisTick);
        }
        progress++;
        setStatus("running");
        if (completes) {
            progress = 0;
            activeRecipe = null;
            duration = 0;
        }
        markTickMutation();
    }

    private boolean hasOutputRoom(GTRecipe recipe) {
        for (int index = 0;
                index < recipe.fluidOutputs().size();
                index++) {
            FluidStack output = recipe.fluidOutputs().get(index);
            if (outputs.get(index).fill(
                            output, IFluidHandler.FluidAction.SIMULATE)
                    != output.getAmount()) {
                return false;
            }
        }
        return true;
    }

    private void consumeFuel(GTRecipe recipe) {
        FluidStack required = recipe.fluidInputs().getFirst();
        FluidStack drained = input.drain(
                required, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != required.getAmount()
                || !FluidStack.isSameFluidSameComponents(
                        drained, required)) {
            throw new IllegalStateException(
                    "Fuel changed after generator simulation");
        }
    }

    private void commitOutputs(GTRecipe recipe) {
        for (int index = 0;
                index < recipe.fluidOutputs().size();
                index++) {
            FluidStack output = recipe.fluidOutputs().get(index);
            if (outputs.get(index).fill(
                            output, IFluidHandler.FluidAction.EXECUTE)
                    != output.getAmount()) {
                throw new IllegalStateException(
                        "Generator exhaust changed after simulation");
            }
        }
    }

    private void reset(String nextStatus) {
        boolean changed = activeRecipe != null
                || progress != 0
                || duration != 0
                || !status.equals(nextStatus);
        activeRecipe = null;
        progress = 0;
        duration = 0;
        status = nextStatus;
        if (changed) {
            markPersistentMutation();
        }
    }

    public IFluidHandler fluids(Direction side) {
        if (side == null || front() == null) {
            return null;
        }
        int outputIndex = spec.exhaustOutputSides().indexOf(side);
        if (outputIndex >= 0) {
            return outputViews.get(outputIndex);
        }
        return side == energyOutputSide() ? null : inputView;
    }

    public String status() {
        return status;
    }

    public int progress() {
        return progress;
    }

    public int inputAmount() {
        return input.getFluidAmount();
    }

    public int outputAmount(int index) {
        return outputs.get(index).getFluidAmount();
    }

    public long energyStored() {
        return energy.stored();
    }

    public long energyGenerated() {
        return energy.generated();
    }

    public long energyExtracted() {
        return energy.extracted();
    }

    public EnergyType outputEnergyType() {
        return spec.outputEnergyType();
    }

    public boolean identityQuarantined() {
        return !identityQuarantine.isBlank();
    }

    public String identityQuarantine() {
        return identityQuarantine;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return identityQuarantine.isBlank()
                && type == spec.outputEnergyType()
                && side != null
                && side == energyOutputSide();
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        return handles(type, side)
                        && energy.stored() >= spec.outputPacketSize()
                        && outputBudget.claim(
                                        gameTime(),
                                        1L,
                                        spec.maximumOutputPacketsPerTick(),
                                        true)
                                > 0L
                ? spec.outputPacketSize()
                : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maximum,
            Direction side,
            boolean simulate) {
        if (!handles(type, side)
                || size != spec.outputPacketSize()
                || maximum <= 0L
                || energy.stored() < spec.outputPacketSize()
                || outputBudget.claim(
                                gameTime(),
                                maximum,
                                spec.maximumOutputPacketsPerTick(),
                                true)
                        <= 0L) {
            return 0L;
        }
        long available = energy.extract(
                size,
                outputBudget.claim(
                        gameTime(),
                        maximum,
                        spec.maximumOutputPacketsPerTick(),
                        true),
                true);
        if (!simulate && level != null && !level.isClientSide) {
            long claimed = outputBudget.claim(
                    gameTime(),
                    available,
                    spec.maximumOutputPacketsPerTick(),
                    false);
            if (claimed != available
                    || energy.extract(size, available, false)
                            != available) {
                throw new IllegalStateException(
                        "Generator output changed after simulation");
            }
            markPersistentMutation();
        }
        return available;
    }

    @Override
    public long stored(EnergyType type) {
        return type == spec.outputEnergyType()
                ? energy.stored()
                : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == spec.outputEnergyType()
                ? spec.energyCapacity()
                : 0L;
    }

    private long gameTime() {
        if (level == null) {
            outputBudget.reset();
            return 0L;
        }
        return level.getGameTime();
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(FuelGeneratorBlock.FACING)
                ? state.getValue(FuelGeneratorBlock.FACING)
                : null;
    }

    private Direction energyOutputSide() {
        return spec.energyOutputSide(front());
    }

    private void setStatus(String nextStatus) {
        if (!status.equals(nextStatus)) {
            status = nextStatus;
            markTickMutation();
        }
    }

    private void updateLitState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        boolean lit = "running".equals(status);
        if (state.hasProperty(FuelGeneratorBlock.LIT)
                && state.getValue(FuelGeneratorBlock.LIT) != lit) {
            level.setBlock(
                    worldPosition,
                    state.setValue(FuelGeneratorBlock.LIT, lit),
                    Block.UPDATE_CLIENTS);
        }
    }

    private void markPersistentMutation() {
        setChanged();
        markTickMutation();
    }

    private void markTickMutation() {
        clientSyncPending = true;
    }

    private void flushClientSync(long gameTime) {
        if (!clientSyncPending
                || lastClientSyncGameTime != Long.MIN_VALUE
                        && gameTime - lastClientSyncGameTime < 20L) {
            return;
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
        clientSyncPending = false;
        lastClientSyncGameTime = gameTime;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("input", input.writeToNBT(
                registries, new CompoundTag()));
        for (int index = 0; index < outputs.size(); index++) {
            tag.put("output_" + index, outputs.get(index).writeToNBT(
                    registries, new CompoundTag()));
        }
        FuelGeneratorEnergy.State energyState = energy.snapshot();
        tag.putLong("energy", energyState.stored());
        tag.putLong("energy_generated", energyState.generated());
        tag.putLong("energy_extracted", energyState.extracted());
        tag.putLong("packet_size", spec.outputPacketSize());
        if (activeRecipe != null) {
            tag.putString("active_recipe", activeRecipe.toString());
        }
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        tag.putString("status", status);
        tag.putInt(
                SCHEMA_KEY, persistedIdentity.schemaVersion());
        tag.putString(
                IDENTITY_KEY, persistedIdentity.generatorId());
        tag.putString(
                ENERGY_IDENTITY_KEY,
                persistedIdentity.energyIdentity());
        if (!identityQuarantine.isBlank()) {
            tag.putString(QUARANTINE_KEY, identityQuarantine);
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        FuelGeneratorIdentityPolicy.Identity savedIdentity =
                readIdentity(tag);
        FuelGeneratorIdentityPolicy.Decision identity =
                FuelGeneratorIdentityPolicy.resolve(
                        spec, savedIdentity);
        persistedIdentity = identity.persistedIdentity();
        identityQuarantine =
                identity.quarantineReason().orElse("");
        if (tag.contains("input")) {
            input.readFromNBT(registries, tag.getCompound("input"));
        }
        for (int index = 0; index < outputs.size(); index++) {
            if (tag.contains("output_" + index)) {
                outputs.get(index).readFromNBT(
                        registries,
                        tag.getCompound("output_" + index));
            }
        }
        long stored = Math.max(
                0L,
                Math.min(
                        spec.energyCapacity(),
                        tag.getLong("energy")));
        long generated = tag.contains(
                        "energy_generated", Tag.TAG_ANY_NUMERIC)
                ? Math.max(stored, tag.getLong("energy_generated"))
                : stored;
        long extracted = Math.max(
                0L, tag.getLong("energy_extracted"));
        energy.restore(new FuelGeneratorEnergy.State(
                stored, generated, extracted));
        activeRecipe = ResourceLocation.tryParse(
                tag.getString("active_recipe"));
        progress = Math.max(0, tag.getInt("progress"));
        duration = Math.max(0, tag.getInt("duration"));
        status = tag.getString("status");
        if (status.isBlank()) {
            status = "idle";
        }
        if (!identityQuarantine.isBlank()) {
            status = "identity_quarantined";
        }
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("energy", energy.stored());
        tag.putString("status", status);
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        if (!identityQuarantine.isBlank()) {
            tag.putString(QUARANTINE_KEY, identityQuarantine);
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        energy.restore(new FuelGeneratorEnergy.State(
                Math.max(
                        0L,
                        Math.min(
                                spec.energyCapacity(),
                                tag.getLong("energy"))),
                energy.generated(),
                energy.extracted()));
        status = tag.getString("status");
        progress = Math.max(0, tag.getInt("progress"));
        duration = Math.max(0, tag.getInt("duration"));
        identityQuarantine = tag.getString(QUARANTINE_KEY);
    }

    private FuelGeneratorIdentityPolicy.Identity readIdentity(
            CompoundTag tag) {
        boolean anyIdentity = tag.contains(SCHEMA_KEY)
                || tag.contains(IDENTITY_KEY)
                || tag.contains(ENERGY_IDENTITY_KEY);
        if (!anyIdentity) {
            return null;
        }
        return new FuelGeneratorIdentityPolicy.Identity(
                tag.contains(SCHEMA_KEY, Tag.TAG_ANY_NUMERIC)
                        ? tag.getInt(SCHEMA_KEY)
                        : FuelGeneratorIdentityPolicy
                                .MISSING_SCHEMA_VERSION,
                tag.contains(IDENTITY_KEY, Tag.TAG_STRING)
                        ? tag.getString(IDENTITY_KEY)
                        : "",
                tag.contains(ENERGY_IDENTITY_KEY, Tag.TAG_STRING)
                        ? tag.getString(ENERGY_IDENTITY_KEY)
                        : "");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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

    private final class InputHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? input.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? input.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && input.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return input.fill(resource, action);
        }

        @Override
        public FluidStack drain(
                FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maximum, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    private final class OutputHandler implements IFluidHandler {
        private final int outputIndex;

        private OutputHandler(int outputIndex) {
            this.outputIndex = outputIndex;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0
                    ? outputs.get(outputIndex).getFluid()
                    : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0
                    ? outputs.get(outputIndex).getCapacity()
                    : 0;
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
            return outputs.get(outputIndex).drain(resource, action);
        }

        @Override
        public FluidStack drain(int maximum, FluidAction action) {
            return outputs.get(outputIndex).drain(maximum, action);
        }
    }
}
