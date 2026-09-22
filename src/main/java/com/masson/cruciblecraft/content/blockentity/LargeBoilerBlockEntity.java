package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.fluid.LongFluidHandler;
import com.masson.cruciblecraft.api.tool.MagnifyingInspectable;
import com.masson.cruciblecraft.content.block.LargeBoilerBlock;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.blockentity.MachineCoverHostBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockControllerBinding;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortAggregator;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortHost;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator;
import com.masson.cruciblecraft.content.multiblock.PluginQuarantinePolicy;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModFluids;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import com.masson.cruciblecraft.nuclear.ReactorHazards;
import com.masson.cruciblecraft.steam.ExactFluidTransfer;
import com.masson.cruciblecraft.energy.EnergyPackets;

/**
 * Steam-chain large boiler: a conversion controller, not a processing
 * host. The nine base-layer energy ports feed the HU buffer (the
 * heat_energy_input plugin declares the non-default energy identity);
 * the eight bottom water and seventeen steam wall ports bridge the two host
 * tanks. Conversion follows the GT6 80 HU + 1 water -> 160 steam batch and
 * applies the tier profile, efficiency, cooldown and safety rules locally.
 */
public final class LargeBoilerBlockEntity extends MachineCoverHostBlockEntity
        implements MultiblockControllerBinding, MultiblockPortHost,
        MagnifyingInspectable {
    private static final String PLUGIN_TAG = "multiblock_plugins";
    private static final int STRUCTURE_RECHECK_TICKS = 600;
    public static final ResourceLocation STRUCTURE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "large_boiler_stainless_steel");
    public static final ResourceLocation LEGACY_STRUCTURE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "large_boiler");

    private final Optional<LargeBoilerTier> tier;
    private final LargeBoilerFluidTank water = new LargeBoilerFluidTank(
            Math.toIntExact(LargeBoilerTier.WATER_CAPACITY),
            LargeBoilerBlockEntity::acceptsBoilerWater,
            this::setChanged);
    private final LargeBoilerFluidTank steam;
    private final List<FluidTank> tanks;
    private final ItemStackHandler inventory = new ItemStackHandler(0);
    private long heat;
    private int efficiency = 10_000;
    private int coolDownResetTimer = 128;
    private int barometer;

    private boolean structureValid;
    private Set<BlockPos> boundPorts = Set.of();
    private MultiblockStructureValidator.ValidationResult lastValidation;
    private boolean pluginQuarantined;
    private String pluginQuarantineReason = "";
    private boolean exploding;

    public LargeBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LARGE_BOILER.get(), pos, state);
        tier = tierOf(state);
        steam = new LargeBoilerFluidTank(
                tier.map(LargeBoilerTier::capacity).orElse(16_000L),
                stack -> stack.is(ModFluids.STEAM_SOURCE.get()),
                this::setChanged);
        tanks = List.of(water, steam);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LargeBoilerBlockEntity boiler) {
        boiler.tickMountedCovers();
        if (boiler.tier.isEmpty()) {
            boiler.migrateLegacyController(level, state);
            return;
        }
        long phaseKey = CheckpointDecisions.phaseKey(
                pos.getX(), pos.getY(), pos.getZ());
        if (CheckpointDecisions.onPositionPhase(
                level.getGameTime(), phaseKey, STRUCTURE_RECHECK_TICKS)) {
            boiler.recheckStructure(level, pos, state);
        }
        if (boiler.structureValid && !boiler.pluginQuarantined) {
            boiler.convert();
            boiler.coolDown();
            boiler.pushSteam();
        }
        boiler.updateBarometer();
        boiler.explodeIfUnsafe();
    }

    private void convert() {
        long conversions = Math.min(
                steam.longCapacity() / 2_560L,
                Math.min(
                        heat / LargeBoilerTier.HEAT_PER_WATER,
                        water.getFluidAmount()));
        if (conversions <= 0L) {
            return;
        }

        int waterAmount = Math.toIntExact(conversions);
        water.drain(waterAmount, IFluidHandler.FluidAction.EXECUTE);
        if (level != null
                && level.random.nextInt(10) == 0
                && efficiency > 5_000
                && !water.isEmpty()
                && !isDistilledWater(water.getFluid())) {
            efficiency = Math.max(
                    5_000,
                    efficiency - Math.toIntExact(conversions));
        }
        long produced = conversions * LargeBoilerTier.STEAM_PER_WATER
                * efficiency / 10_000L;
        if (produced > 0L) {
            steam.addUnsafe(
                    new FluidStack(ModFluids.STEAM_SOURCE.get(), 1),
                    produced);
        }
        heat -= conversions * LargeBoilerTier.HEAT_PER_WATER;
        coolDownResetTimer = 128;
        setChanged();
    }

    public FluidTank waterTank() {
        return water;
    }

    public LargeBoilerFluidTank steamTank() {
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
        ResourceLocation structure = structureId();
        var definition = MultiblockStructureCatalog.find(structure);
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
                        facingOf(state));
        lastValidation = validation;
        boundPorts = MultiblockPortAggregator.refresh(
                level, pos, structure, validation, boundPorts);
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
        tag.put("water", water.writeLongNbt(
                registries, "fluid", "amount"));
        tag.put("steam", steam.writeLongNbt(
                registries, "fluid", "amount"));
        tag.putLong("heat", heat);
        tag.putInt("efficiency", efficiency);
        tag.putInt("cooldown", coolDownResetTimer);
        tag.putInt("barometer", barometer);
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
        if (tag.contains("water")) {
            water.readLongNbt(
                    registries,
                    tag.getCompound("water"),
                    "fluid",
                    "amount");
        }
        if (tag.contains("steam")) {
            steam.readLongNbt(
                    registries,
                    tag.getCompound("steam"),
                    "fluid",
                    "amount");
        }
        heat = Math.max(0L, tag.getLong("heat"));
        efficiency = Math.max(
                5_000,
                Math.min(10_000, tag.contains("efficiency")
                        ? tag.getInt("efficiency")
                        : 10_000));
        coolDownResetTimer = Math.max(
                0,
                tag.contains("cooldown") ? tag.getInt("cooldown") : 128);
        barometer = Math.max(
                0,
                Math.min(
                        31,
                        tag.contains("barometer")
                                ? tag.getInt("barometer")
                                : 0));
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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("barometer", barometer);
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("barometer")) {
            barometer = Math.max(0, Math.min(31, tag.getInt("barometer")));
        }
    }

    @Override
    public ResourceLocation structureId() {
        return tier.map(LargeBoilerTier::structureId)
                .orElse(LEGACY_STRUCTURE_ID);
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
        if (type != EnergyType.HEAT || size == 0L || amount <= 0L) {
            return 0L;
        }
        long units = safeEnergyUnits(size, amount);
        if (!simulate && units > 0L) {
            heat = saturatingAdd(heat, units);
            coolDownResetTimer = Math.max(coolDownResetTimer, 32);
            setChanged();
        }
        return amount;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.HEAT ? heat : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.HEAT
                ? tier.map(LargeBoilerTier::capacity).orElse(16_000L)
                : 0L;
    }

    @Override
    public boolean hasFluidTanks() {
        return true;
    }

    @Override
    public boolean hasEnergyBuffer() {
        return true;
    }

    @Override
    public long energyStored() {
        return heat;
    }

    @Override
    public long energyCapacity() {
        return tier.map(LargeBoilerTier::capacity).orElse(16_000L);
    }

    @Override
    public boolean runningPossible() {
        return structureValid && !pluginQuarantined;
    }

    @Override
    public boolean runningActively() {
        return runningPossible() && heat > 0L;
    }

    @Override
    public BlockState blockState() {
        return getBlockState();
    }

    public IFluidHandler fluids(Direction side) {
        return new BoilerFluidView(true, true);
    }

    public LongFluidHandler longFluids(Direction side) {
        return new BoilerFluidView(true, true);
    }

    public IFluidHandler fluidsForPort(PortType type) {
        return switch (type) {
            case ITEM_FLUID_IN -> new BoilerFluidView(true, false);
            case FLUID_OUT -> new BoilerFluidView(false, true);
            default -> null;
        };
    }

    public LongFluidHandler longFluidsForPort(PortType type) {
        return switch (type) {
            case ITEM_FLUID_IN -> new BoilerFluidView(true, false);
            case FLUID_OUT -> new BoilerFluidView(false, true);
            default -> null;
        };
    }

    public long heatAmount() {
        return heat;
    }

    public int coolDownResetTimer() {
        return coolDownResetTimer;
    }

    public int efficiency() {
        return efficiency;
    }

    public int barometer() {
        return barometer;
    }

    public long steamAmountLong() {
        return steam.longAmount();
    }

    public long steamCapacityLong() {
        return steam.longCapacity();
    }

    public boolean decalcify(Player player) {
        int damage = 10_000 - efficiency;
        if (damage <= 0) {
            return false;
        }
        if (barometer > 15) {
            explode(false);
            return true;
        }
        if (player != null
                && heat + steam.longAmount() / 2L > 2_000L) {
            ReactorHazards.applyHeatDamage(
                    player,
                    (heat + steam.longAmount() / 2L) / 2_000.0F);
        }
        steam.setFluid(FluidStack.EMPTY);
        efficiency = 10_000;
        heat = 0L;
        setChanged();
        return true;
    }

    public boolean removedByPlayer(Player player) {
        if (level != null
                && !level.isClientSide
                && player != null
                && !player.getAbilities().instabuild
                && barometer > 4) {
            explode(true);
        }
        return true;
    }

    public void onExploded() {
        if (level != null && !level.isClientSide && barometer > 4) {
            explode(true);
        }
    }

    @Override
    public List<Component> magnifyingInspect(UseOnContext context) {
        return List.of(
                Component.literal(
                        "Calcification: "
                                + ((10_000 - efficiency) / 100.0F)
                                + "%"),
                Component.literal(
                        "Water: "
                                + water.getFluidAmount()
                                + " / "
                                + LargeBoilerTier.WATER_CAPACITY
                                + " mB"),
                Component.literal(
                        "Steam: "
                                + steam.longAmount()
                                + " / "
                                + steam.longCapacity()
                                + " mB"),
                Component.literal(
                        "Pressure: "
                                + barometer
                                + " / 31"),
                Component.literal(
                        "Stored Heat Units: "
                                + heat
                                + " / "
                                + energyCapacity()
                                + " HU"));
    }

    private void coolDown() {
        if (coolDownResetTimer-- > 0) {
            return;
        }
        coolDownResetTimer = 0;
        LargeBoilerTier current = tier.orElseThrow();
        heat = Math.max(0L, heat - current.steamOutput() * 32L);
        long loss = current.steamOutput() * 64L;
        steam.drain(
                (int) Math.min(Integer.MAX_VALUE, loss),
                IFluidHandler.FluidAction.EXECUTE);
        if (heat <= 0L) {
            heat = 0L;
            coolDownResetTimer = 128;
        }
        setChanged();
    }

    private void pushSteam() {
        if (level == null || steam.isEmpty()) {
            return;
        }
        long excess = steam.longAmount() - steam.longCapacity() / 2L;
        if (excess <= 0L) {
            return;
        }
        LargeBoilerTier current = tier.orElseThrow();
        long desired = excess > steam.longCapacity() / 4L
                ? current.steamOutput() * 2L
                : current.steamOutput();
        int limit = (int) Math.min(Integer.MAX_VALUE, Math.min(desired, excess));
        if (limit <= 0) {
            return;
        }
        Direction facing = facingOf(getBlockState());
        BlockPos center = MultiblockStructureCatalog.anchor(
                structureId(), "center", worldPosition, facing);
        List<IFluidHandler> targets = new ArrayList<>();
        for (BoilerPushTarget pushTarget : BoilerPushTarget.values()) {
            BlockPos target = localOffset(
                    center,
                    facing,
                    pushTarget.x,
                    pushTarget.y,
                    pushTarget.z);
            if (!level.hasChunkAt(target)) {
                continue;
            }
            IFluidHandler handler = level.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    target,
                    pushTarget.outward(facing).getOpposite());
            if (handler != null) {
                targets.add(handler);
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        FluidStack offered = steam.drain(
                limit, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) {
            return;
        }
        List<Integer> accepted = new ArrayList<>();
        List<IFluidHandler> viableTargets = new ArrayList<>();
        int total = 0;
        for (IFluidHandler target : targets) {
            int amount = target.fill(
                    offered, IFluidHandler.FluidAction.SIMULATE);
            if (amount > 0) {
                viableTargets.add(target);
                accepted.add(amount);
                total += amount;
            }
        }
        if (total <= 0) {
            return;
        }
        targets = viableTargets;
        if (targets.size() > 1 && offered.getAmount() < targets.size()) {
            return;
        }
        if (total <= offered.getAmount()) {
            for (int index = 0; index < targets.size(); index++) {
                int amount = accepted.get(index);
                if (amount > 0) {
                    ExactFluidTransfer.move(steam, targets.get(index), amount);
                }
            }
            return;
        }
        int remaining = offered.getAmount();
        int[] allocations = new int[targets.size()];
        while (remaining > 0) {
            int active = 0;
            for (int index = 0; index < accepted.size(); index++) {
                if (accepted.get(index) > allocations[index]) {
                    active++;
                }
            }
            if (active == 0) {
                break;
            }
            int share = Math.max(1, remaining / active);
            boolean moved = false;
            for (int index = 0; index < targets.size() && remaining > 0; index++) {
                int available = accepted.get(index) - allocations[index];
                if (available <= 0) {
                    continue;
                }
                int amount = Math.min(remaining, Math.min(available, share));
                if (amount > 0) {
                    allocations[index] += amount;
                    remaining -= amount;
                    moved = true;
                }
            }
            if (!moved) {
                break;
            }
        }
        for (int index = 0; index < targets.size(); index++) {
            if (allocations[index] > 0) {
                ExactFluidTransfer.move(
                        steam, targets.get(index), allocations[index]);
            }
        }
    }

    private void updateBarometer() {
        long capacity = steam.longCapacity();
        if (capacity <= 0L) {
            setBarometer(0);
            return;
        }
        long amount = steam.longAmount();
        long scaled = amount > Long.MAX_VALUE / 31L
                ? 31L
                : amount * 31L / capacity;
        setBarometer((int) Math.max(0L, Math.min(31L, scaled)));
    }

    private void setBarometer(int value) {
        int bounded = Math.max(0, Math.min(31, value));
        if (barometer == bounded) {
            return;
        }
        barometer = bounded;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    private void explodeIfUnsafe() {
        if (level == null || level.isClientSide) {
            return;
        }
        if ((barometer > 4 && !structureValid)
                || heat > energyCapacity()
                || steam.longAmount() >= steam.longCapacity()) {
            explode(false);
        }
    }

    private void explode(boolean flaming) {
        if (exploding || level == null || level.isClientSide) {
            return;
        }
        exploding = true;
        float strength = (float) (2.0
                + Math.max(1.0, Math.sqrt(steam.longAmount()) / 1_000.0));
        level.explode(
                null,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                strength,
                Level.ExplosionInteraction.TNT);
    }

    private void migrateLegacyController(Level level, BlockState state) {
        if (!(state.getBlock() instanceof LargeBoilerBlock)) {
            structureValid = false;
            return;
        }
        var target = ModBlocks.mteInPlaceBlocksById().get(
                LargeBoilerTier.STAINLESS_STEEL.controllerId());
        if (target == null) {
            structureValid = false;
            return;
        }
        CompoundTag saved = new CompoundTag();
        saveAdditional(saved, level.registryAccess());
        BlockState migrated = target.get().defaultBlockState()
                .setValue(
                        MteInPlaceBlock.FACING,
                        facingOf(state));
        level.setBlock(worldPosition, migrated, Block.UPDATE_ALL);
        if (level.getBlockEntity(worldPosition)
                instanceof LargeBoilerBlockEntity replacement) {
            replacement.loadAdditional(saved, level.registryAccess());
        }
    }

    private static Optional<LargeBoilerTier> tierOf(BlockState state) {
        if (state.getBlock() instanceof MteInPlaceBlock controller) {
            return LargeBoilerTier.bySpec(controller.spec());
        }
        return Optional.empty();
    }

    private static Direction facingOf(BlockState state) {
        if (state.hasProperty(MteInPlaceBlock.FACING)) {
            return state.getValue(MteInPlaceBlock.FACING);
        }
        return state.getValue(ProcessingMachineBlock.FACING);
    }

    private static BlockPos localOffset(
            BlockPos origin,
            Direction facing,
            int x,
            int y,
            int z) {
        var rotated = new MultiblockStructureDefinition.Offset(x, y, z)
                .rotate(facing);
        return origin.offset(rotated.x(), rotated.y(), rotated.z());
    }

    private enum BoilerPushTarget {
        TOP(0, 2, 0),
        LOCAL_WEST(-2, 0, 0),
        LOCAL_EAST(2, 0, 0),
        LOCAL_NORTH(0, 0, -2),
        LOCAL_SOUTH(0, 0, 2);

        private final int x;
        private final int y;
        private final int z;

        BoilerPushTarget(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private Direction outward(Direction facing) {
            if (this == TOP) {
                return Direction.UP;
            }
            var rotated = new MultiblockStructureDefinition.Offset(x, 0, z)
                    .rotate(facing);
            if (rotated.x() < 0) {
                return Direction.WEST;
            }
            if (rotated.x() > 0) {
                return Direction.EAST;
            }
            return rotated.z() < 0 ? Direction.NORTH : Direction.SOUTH;
        }
    }

    private static boolean acceptsBoilerWater(FluidStack stack) {
        return !stack.isEmpty()
                && (stack.is(Fluids.WATER) || isDistilledWater(stack));
    }

    private static boolean isDistilledWater(FluidStack stack) {
        return SteamConversion.isDistilledWater(stack);
    }

    private static long safeEnergyUnits(long size, long amount) {
        return EnergyPackets.units(size, amount);
    }

    private static long saturatingAdd(long first, long second) {
        return first > Long.MAX_VALUE - second
                ? Long.MAX_VALUE
                : first + second;
    }

    private final class BoilerFluidView
            implements IFluidHandler, LongFluidHandler {
        private final boolean input;
        private final boolean output;

        private BoilerFluidView(boolean input, boolean output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public int tanks() {
            return 2;
        }

        @Override
        public FluidStack fluid(int tank) {
            return tank == 0 ? water.getFluid() : steam.getFluid();
        }

        @Override
        public long amount(int tank) {
            return tank == 0 ? water.longAmount() : steam.longAmount();
        }

        @Override
        public long capacity(int tank) {
            return tank == 0 ? water.longCapacity() : steam.longCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && input && acceptsBoilerWater(stack);
        }

        @Override
        public long fill(
                int tank,
                FluidStack resource,
                long maxFill,
                FluidAction action) {
            if (tank != 0 || !input) {
                return 0L;
            }
            return water.fillLong(resource, maxFill, action);
        }

        @Override
        public LongFluidHandler.LongFluidStack drain(
                int tank,
                long maxDrain,
                FluidAction action) {
            if (tank != 1 || !output) {
                return LongFluidHandler.LongFluidStack.empty();
            }
            return steam.drainLong(maxDrain, action);
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? water.getFluid() : steam.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0
                    ? water.getCapacity()
                    : steam.getCapacity();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return input ? water.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(
                FluidStack resource,
                FluidAction action) {
            return output
                    ? steam.drain(resource, action)
                    : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return output
                    ? steam.drain(maxDrain, action)
                    : FluidStack.EMPTY;
        }
    }
}
