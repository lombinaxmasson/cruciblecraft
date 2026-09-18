package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortAggregator;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortHost;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator;
import com.masson.cruciblecraft.content.multiblock.PluginQuarantinePolicy;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.processing.MachineEnergyBuffer;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModMultiblockPlugins;
import com.masson.cruciblecraft.steam.SteamConversion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Steam-chain large boiler: a conversion controller, not a processing
 * host. The nine base-layer energy ports feed the HU buffer (the
 * heat_energy_input plugin declares the non-default energy identity);
 * the nine water and seventeen steam ports bridge the two host tanks.
 * Conversion reuses the source-backed SteamConversion constants
 * (80 HU + 1 water -> 160 steam per batch).
 */
public final class LargeBoilerBlockEntity extends BlockEntity
        implements MultiblockControllerBinding, MultiblockPortHost {
    private static final String PLUGIN_TAG = "multiblock_plugins";
    public static final ResourceLocation STRUCTURE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "large_boiler");
    private static final int WATER_CAPACITY = 8_000;
    private static final int STEAM_CAPACITY = 16_000;
    private static final long HEAT_CAPACITY = 4_096L;
    private static final long HEAT_MAX_PACKET = 512L;

    private final FluidTank water = new FluidTank(WATER_CAPACITY);
    private final FluidTank steam = new FluidTank(STEAM_CAPACITY);
    private final List<FluidTank> tanks = List.of(water, steam);
    private final ItemStackHandler inventory = new ItemStackHandler(0);
    private final MachineEnergyBuffer heat =
            new MachineEnergyBuffer(HEAT_CAPACITY, HEAT_MAX_PACKET);

    private boolean structureValid;
    private Set<BlockPos> boundPorts = Set.of();
    private MultiblockStructureValidator.ValidationResult lastValidation;
    private boolean pluginQuarantined;
    private String pluginQuarantineReason = "";

    public LargeBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LARGE_BOILER.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LargeBoilerBlockEntity boiler) {
        long phaseKey = CheckpointDecisions.phaseKey(
                pos.getX(), pos.getY(), pos.getZ());
        if (CheckpointDecisions.onPositionPhase(
                level.getGameTime(), phaseKey, 20)) {
            boiler.recheckStructure(level, pos, state);
        }
        if (boiler.structureValid && !boiler.pluginQuarantined) {
            boiler.convert();
        }
    }

    private void convert() {
        int batches = SteamConversion.boilerBatches(
                water.getFluidAmount(),
                steam.getSpace(),
                (int) Math.min(Integer.MAX_VALUE, heat.stored()));
        if (batches <= 0) {
            return;
        }
        water.drain(
                batches * SteamConversion.WATER_PER_BATCH,
                net.neoforged.neoforge.fluids.capability.IFluidHandler
                        .FluidAction.EXECUTE);
        heat.consume((long) batches * SteamConversion.HU_PER_BATCH);
        steam.fill(
                new FluidStack(
                        com.masson.cruciblecraft.registry.ModFluids.STEAM_SOURCE,
                        batches * SteamConversion.STEAM_PER_BATCH),
                net.neoforged.neoforge.fluids.capability.IFluidHandler
                        .FluidAction.EXECUTE);
        setChanged();
    }

    public FluidTank waterTank() {
        return water;
    }

    public FluidTank steamTank() {
        return steam;
    }

    /** GT6 LargeBoiler: trash water if present, otherwise steam. */
    public boolean trashWithPlunger() {
        if (!water.isEmpty()) {
            water.setFluid(FluidStack.EMPTY);
            setChanged();
            return true;
        }
        if (steam.isEmpty()) {
            return false;
        }
        steam.setFluid(FluidStack.EMPTY);
        setChanged();
        return true;
    }

    public boolean pluginQuarantined() {
        return pluginQuarantined;
    }

    public String pluginQuarantineReason() {
        return pluginQuarantineReason;
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
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("steam", steam.writeToNBT(registries, new CompoundTag()));
        tag.putLong("heat", heat.stored());
        ListTag ids = new ListTag();
        for (ResourceLocation id
                : ModMultiblockPlugins.LARGE_BOILER_PLUGINS) {
            ids.add(StringTag.valueOf(id.toString()));
        }
        tag.put(PLUGIN_TAG, ids);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water.readFromNBT(registries, tag.getCompound("water"));
        steam.readFromNBT(registries, tag.getCompound("steam"));
        heat.restore(tag.getLong("heat"));
        ListTag ids = tag.getList(PLUGIN_TAG, Tag.TAG_STRING);
        List<String> saved = new ArrayList<>();
        for (Tag entry : ids) {
            saved.add(entry.getAsString());
        }
        pluginQuarantined = false;
        pluginQuarantineReason = "";
        int index = 0;
        for (ResourceLocation current
                : ModMultiblockPlugins.LARGE_BOILER_PLUGINS) {
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

    // --- MultiblockPortHost ---

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
        return List.of();
    }

    @Override
    public List<Integer> itemOutputSlots() {
        return List.of();
    }

    @Override
    public List<Integer> fluidInputTanks() {
        return List.of(0);
    }

    @Override
    public List<Integer> fluidOutputTanks() {
        return List.of(1);
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.HEAT;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        return type == EnergyType.HEAT
                ? heat.insert(size, amount, simulate)
                : 0L;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.HEAT ? heat.stored() : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.HEAT ? heat.capacity() : 0L;
    }

    @Override
    public BlockState blockState() {
        return getBlockState();
    }
}
