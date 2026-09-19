package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.CeramicMoldBlock;
import com.masson.cruciblecraft.content.mold.CruciblePour;
import com.masson.cruciblecraft.content.mold.MoldCastingRules;
import com.masson.cruciblecraft.content.mold.MoldHost;
import com.masson.cruciblecraft.content.mold.MoldRecipes;
import com.masson.cruciblecraft.content.mold.MoldShape;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.heat.ItemHeat;
import com.masson.cruciblecraft.heat.TemperatureDamage;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.Nullable;

public final class CeramicMoldBlockEntity extends BlockEntity
        implements MoldHost, IEnergyHandler {
    public static final float AMBIENT_TEMPERATURE = 20.0F;
    public static final float HOPPER_EXTRACT_SLACK = 50.0F;

    private int pattern = MoldShape.INGOT.mask();
    private String materialId = "";
    private int outputCount;
    private float temperature = AMBIENT_TEMPERATURE;
    private boolean solidified;
    private byte autoPullDirections;
    private boolean useRedstone;
    private boolean blockUpdated;
    private boolean inventoryChanged;
    private final IItemHandler extractHandler = new CoolExtractHandler();

    public CeramicMoldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CERAMIC_MOLD.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CeramicMoldBlockEntity mold) {
        if (mold.isFilled()) {
            if (MaterialLookup.byId(mold.materialId).isEmpty()) {
                mold.blockUpdated = false;
                mold.inventoryChanged = false;
                return;
            }
            float maximum = mold.moldMaxTemperatureCelsius();
            if (mold.temperature > maximum) {
                level.setBlock(pos, Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
                return;
            }

            float previous = mold.temperature;
            mold.temperature = MoldCastingRules.cool(mold.temperature, AMBIENT_TEMPERATURE);
            boolean wasSolidified = mold.solidified;
            mold.solidified = mold.temperature
                    < MaterialCatalog.require(mold.materialId).thermal().meltingPoint();
            if (wasSolidified != mold.solidified || (level.getGameTime() % 20L == 0L
                    && Float.compare(previous, mold.temperature) != 0)) {
                mold.setChanged();
                mold.sync();
            }
        } else {
            mold.tryAutoPull(level, pos);
        }
        mold.blockUpdated = false;
        mold.inventoryChanged = false;
    }

    public void setShape(MoldShape shape) {
        this.pattern = shape.mask();
        setChanged();
        sync();
    }

    public void setPattern(int pattern) {
        this.pattern = pattern & ((1 << MoldRecipes.CELL_COUNT) - 1);
        setChanged();
        sync();
    }

    public boolean chiselBit(int bit) {
        if (isFilled() || bit == 0 || (pattern & bit) != 0) {
            return false;
        }
        pattern |= bit;
        setChanged();
        sync();
        return true;
    }

    public int rotatePattern() {
        pattern = MoldRecipes.rotateClockwise(pattern);
        setChanged();
        sync();
        return pattern;
    }

    public void fill(CrucibleBlockEntity.CastTransfer transfer) {
        materialId = transfer.material().id();
        outputCount = transfer.count();
        temperature = transfer.temperature();
        solidified = temperature < transfer.material().thermal().meltingPoint();
        inventoryChanged = true;
        setChanged();
        if (level != null) {
            level.setBlock(
                    worldPosition,
                    getBlockState().setValue(CeramicMoldBlock.FILLED, true),
                    Block.UPDATE_CLIENTS);
        }
        sync();
    }

    @Override
    public boolean isMoldInputSide(Direction side) {
        return side != null && side != Direction.DOWN;
    }

    @Override
    public float moldMaxTemperatureCelsius() {
        return MoldCastingRules.maximumTemperature((float)
                MaterialCatalog.require("ceramic").thermal().meltingPoint());
    }

    @Override
    public int moldRequiredMaterialUnits() {
        return MoldRecipes.requiredUnits(pattern);
    }

    @Override
    public int fillMold(
            String offeredMaterialId,
            int availableUnits,
            float offeredTemperature,
            Direction side) {
        if (isFilled()
                || availableUnits <= 0
                || offeredMaterialId == null
                || offeredMaterialId.isEmpty()
                || !isMoldInputSide(side)) {
            return 0;
        }
        OptionalPrefix recipe = recipePrefix();
        if (recipe.prefix == null) {
            return 0;
        }
        MaterialDefinition material = MaterialCatalog.contains(offeredMaterialId)
                ? MaterialCatalog.require(offeredMaterialId)
                : null;
        if (material == null
                || !MaterialCatalog.isFormRegistered(material, recipe.prefix)) {
            return 0;
        }
        int required = moldRequiredMaterialUnits();
        if (required <= 0 || availableUnits < required) {
            return 0;
        }
        materialId = material.id();
        outputCount = Math.max(1, required / Math.max(1, recipe.prefix.units()));
        temperature = offeredTemperature;
        solidified = temperature < material.thermal().meltingPoint();
        inventoryChanged = true;
        setChanged();
        if (level != null) {
            level.setBlock(
                    worldPosition,
                    getBlockState().setValue(CeramicMoldBlock.FILLED, true),
                    Block.UPDATE_CLIENTS);
        }
        sync();
        return required;
    }

    @Override
    public ItemStack takeOutput(Player player, boolean causeDamage) {
        ItemStack result = takeOutput();
        if (!result.isEmpty() && causeDamage) {
            TemperatureDamage.apply(player, temperature, 1.0F, 5.0F);
        }
        return result;
    }

    public ItemStack takeOutput() {
        if (!solidified || !isFilled() || MaterialLookup.byId(materialId).isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = contentsStack();
        if (!result.isEmpty()) {
            clear();
        }
        return result;
    }

    public ItemStack contentsStack() {
        if (!isFilled() || MaterialLookup.byId(materialId).isEmpty()) {
            return ItemStack.EMPTY;
        }
        MaterialPrefix form = recipePrefix().prefix;
        if (form == null) {
            form = MaterialPrefixes.INGOT;
        }
        ItemStack result = MaterialLookup.tryStack(materialId, form, outputCount)
                .orElse(ItemStack.EMPTY);
        if (!result.isEmpty()) {
            ItemHeat.set(result, temperature, level == null ? 0L : level.getGameTime());
        }
        return result;
    }

    private void clear() {
        materialId = "";
        outputCount = 0;
        temperature = AMBIENT_TEMPERATURE;
        solidified = false;
        inventoryChanged = true;
        setChanged();
        if (level != null) {
            level.setBlock(
                    worldPosition,
                    getBlockState().setValue(CeramicMoldBlock.FILLED, false),
                    Block.UPDATE_CLIENTS);
        }
        sync();
    }

    public ItemStack moldStack() {
        return new ItemStack(ModItems.moldItem(shape()).get());
    }

    public MoldShape shape() {
        return MoldShape.fromMask(pattern).orElse(MoldShape.INGOT);
    }

    public int pattern() {
        return pattern;
    }

    public String materialId() {
        return materialId;
    }

    public int outputCount() {
        return outputCount;
    }

    public float temperature() {
        return temperature;
    }

    public boolean isSolidified() {
        return solidified;
    }

    public boolean isFilled() {
        return !materialId.isEmpty() && outputCount > 0;
    }

    public boolean usesRedstone() {
        return useRedstone;
    }

    public boolean autoPulls(Direction side) {
        if (side == null || !side.getAxis().isHorizontal()) {
            return false;
        }
        return (autoPullDirections & sideBit(side)) != 0;
    }

    public boolean toggleAutoPull(Direction side) {
        if (side == null || !side.getAxis().isHorizontal()) {
            return false;
        }
        autoPullDirections ^= sideBit(side);
        setChanged();
        return autoPulls(side);
    }

    public boolean toggleRedstoneMode() {
        useRedstone = !useRedstone;
        setChanged();
        return useRedstone;
    }

    public void clearAutoInput() {
        autoPullDirections = 0;
        useRedstone = false;
        setChanged();
    }

    public void onNeighborChanged() {
        blockUpdated = true;
    }

    public IItemHandler itemHandler(Direction side) {
        return side == Direction.DOWN ? extractHandler : EmptyRejectHandler.INSTANCE;
    }

    public boolean canHopperExtract() {
        return isFilled()
                && solidified
                && temperature - HOPPER_EXTRACT_SLACK < AMBIENT_TEMPERATURE;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.CU;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (type != EnergyType.CU || size == 0L || amount <= 0L) {
            return 0L;
        }
        if (!simulate) {
            long kelvin = Math.max(
                    1L,
                    TemperatureDamage.kelvin(temperature)
                            - EnergyPackets.units(size, amount));
            temperature = kelvin - TemperatureDamage.KELVIN_OFFSET;
            setChanged();
        }
        return amount;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.CU ? Math.max(0L, TemperatureDamage.kelvin(temperature)) : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.CU ? Math.max(1L, TemperatureDamage.kelvin(temperature)) : 0L;
    }

    private void tryAutoPull(Level level, BlockPos pos) {
        if (isFilled() || autoPullDirections == 0) {
            return;
        }
        if (useRedstone && !level.hasNeighborSignal(pos)) {
            return;
        }
        boolean cadence = level.getGameTime() % 20L == 5L;
        if (!(inventoryChanged || cadence || (blockUpdated && useRedstone))) {
            return;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (!autoPulls(side)) {
                continue;
            }
            CruciblePour crucible = CruciblePour.at(level, pos.relative(side));
            if (crucible != null
                    && crucible.fillMoldAtSide(this, side.getOpposite(), side)) {
                return;
            }
        }
    }

    private OptionalPrefix recipePrefix() {
        return new OptionalPrefix(MoldRecipes.recipe(pattern).orElse(null));
    }

    private static byte sideBit(Direction side) {
        return (byte) (1 << side.get3DDataValue());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("pattern")) {
            pattern = tag.getInt("pattern");
        } else {
            try {
                pattern = MoldShape.parse(tag.getString("shape")).mask();
            } catch (IllegalArgumentException exception) {
                pattern = MoldShape.INGOT.mask();
            }
        }
        materialId = tag.getString("material");
        outputCount = Math.max(0, tag.getInt("output_count"));
        temperature = tag.contains("temperature") ? tag.getFloat("temperature") : AMBIENT_TEMPERATURE;
        solidified = tag.getBoolean("solidified");
        autoPullDirections = tag.getByte("auto_pull_directions");
        useRedstone = tag.getBoolean("use_redstone");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("pattern", pattern);
        tag.putString("shape", shape().serializedName());
        tag.putString("material", materialId);
        tag.putInt("output_count", outputCount);
        tag.putFloat("temperature", temperature);
        tag.putBoolean("solidified", solidified);
        tag.putByte("auto_pull_directions", autoPullDirections);
        tag.putBoolean("use_redstone", useRedstone);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private record OptionalPrefix(MaterialPrefix prefix) {}

    private final class CoolExtractHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return canHopperExtract() ? contentsStack() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || !canHopperExtract()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = contentsStack();
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int taken = Math.min(amount, stack.getCount());
            ItemStack result = stack.copy();
            result.setCount(taken);
            if (!simulate) {
                if (taken >= stack.getCount()) {
                    takeOutput();
                } else {
                    outputCount -= taken;
                    setChanged();
                    sync();
                }
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
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
