package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.api.fluid.LongFluidHandler;
import com.masson.cruciblecraft.api.tool.MagnifyingInspectable;
import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.block.FluidBarrelBlock;
import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelContents;
import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelFluids;
import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelLogic;
import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelProfile;
import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelTank;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.steam.ExactFluidTransfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.Connection;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * GT6 {@code TileEntityBase08Barrel} for one registered barrel or drum.
 */
public final class FluidBarrelBlockEntity extends BlockEntity
        implements IFluidHandler, LongFluidHandler, MagnifyingInspectable {
    private final FluidBarrelTank tank;
    private boolean autoOutput;
    private boolean sealed;
    private long sealedTime;
    private long sealedMax;
    private GTRecipe sealedRecipe;
    private String fermentMiss = "";
    private boolean fluidDirty;
    private int syncDelay;

    public FluidBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_BARREL.get(), pos, state);
        FluidBarrelProfile profile = profileOf(state);
        this.tank = new FluidBarrelTank(
                profile.capacity(),
                profile.keepsFilter(),
                () -> sealed,
                this::onTankChanged);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FluidBarrelBlockEntity barrel) {
        if (level.getBlockState(pos).getBlock() != state.getBlock()) {
            return;
        }
        barrel.tickServer();
    }

    public FluidBarrelProfile profile() {
        return profileOf(getBlockState());
    }

    public FluidBarrelTank tank() {
        return tank;
    }

    public boolean autoOutput() {
        return autoOutput;
    }

    public boolean sealed() {
        return sealed;
    }

    public long sealedTime() {
        return sealedTime;
    }

    public long sealedMax() {
        return sealedMax;
    }

    public void readFromItem(ItemStack stack) {
        FluidBarrelContents contents = stack.getOrDefault(
                ModComponents.FLUID_BARREL.get(), FluidBarrelContents.EMPTY);
        autoOutput = contents.autoOutput();
        sealed = contents.sealed();
        sealedTime = contents.sealedTime();
        sealedMax = 0L;
        sealedRecipe = null;
        fermentMiss = "";
        tank.readFluid(contents.fluidId(), contents.amount());
        setChanged();
    }

    public void writeToItem(ItemStack stack) {
        FluidBarrelContents contents = new FluidBarrelContents(
                tank.storedFluidId(),
                tank.amount(),
                autoOutput,
                sealed,
                sealedTime);
        if (contents.isDefault()) {
            stack.remove(ModComponents.FLUID_BARREL.get());
        } else {
            stack.set(ModComponents.FLUID_BARREL.get(), contents);
        }
    }

    public ToolResult useTool(ToolAction action, UseOnContext context) {
        if (action == ToolAction.MAGNIFYING_GLASS) {
            return ToolResult.PASS;
        }
        Level level = context.getLevel();
        if (action != ToolAction.SOFT_HAMMER
                && action != ToolAction.WRENCH
                && action != ToolAction.MONKEY_WRENCH
                && action != ToolAction.PLUNGER) {
            return ToolResult.PASS;
        }
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        FluidBarrelProfile profile = profile();
        FluidBarrelLogic.Mode mode = new FluidBarrelLogic.Mode(
                autoOutput, sealed, sealedTime);
        if (action == ToolAction.SOFT_HAMMER) {
            if (tank.amount() <= 0L) {
                tank.setEmpty();
            }
            FluidBarrelLogic.Mode next = FluidBarrelLogic.softHammer(
                    mode, tank.amount(), profile.canSeal());
            applyMode(next);
            if (tank.amount() > 0L && profile.canSeal() && context.getPlayer() != null) {
                tell(context.getPlayer(), next.sealed()
                        ? Component.translatable(
                                "message.cruciblecraft.fluid_barrel.sealed")
                        : Component.translatable(
                                "message.cruciblecraft.fluid_barrel.normal"));
            }
            ToolClick.hurt(context);
            sync(true);
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.PLUNGER) {
            long removed = tank.removeAmount(
                    1000L, IFluidHandler.FluidAction.EXECUTE).amount();
            sealedTime = 0L;
            sealedMax = 0L;
            sealedRecipe = null;
            fermentMiss = "";
            setChanged();
            sync(true);
            return ToolClick.plunger(context, removed > 0L);
        }
        FluidBarrelLogic.Mode next = FluidBarrelLogic.wrench(mode);
        applyMode(next);
        if (context.getPlayer() != null) {
            tell(context.getPlayer(), next.autoOutput()
                    ? Component.translatable(
                            "message.cruciblecraft.fluid_barrel.auto_on")
                    : Component.translatable(
                            "message.cruciblecraft.fluid_barrel.auto_off"));
        }
        ToolClick.hurt(context);
        sync(true);
        return ToolResult.SUCCESS;
    }

    @Override
    public List<Component> magnifyingInspect(UseOnContext context) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(autoOutput
                ? "message.cruciblecraft.fluid_barrel.auto_on"
                : "message.cruciblecraft.fluid_barrel.auto_off"));
        lines.add(contentsLine());
        if (tank.hasType() && sealed) {
            if (sealedMax > 0L) {
                lines.add(Component.translatable(
                        "message.cruciblecraft.fluid_barrel.sealed_progress",
                        sealedTime,
                        sealedMax));
            } else {
                lines.add(Component.translatable(
                        "message.cruciblecraft.fluid_barrel.sealed"));
            }
        }
        return lines;
    }

    public int temperatureKelvin() {
        return TankFluidSafety.temperature(tank.sample());
    }

    private void tickServer() {
        FluidStack sample = tank.sample();
        FluidBarrelProfile profile = profile();
        FluidBarrelLogic.Reaction reaction = FluidBarrelLogic.reaction(
                tank.amount(),
                TankFluidSafety.temperature(sample),
                profile.meltingPoint(),
                FluidBarrelFluids.vanillaLava(tank.fluid()),
                TankFluidSafety.isMagic(sample),
                profile.magicProof(),
                TankFluidSafety.isAcid(sample),
                profile.acidProof(),
                TankFluidSafety.isPlasma(sample),
                profile.plasmaProof(),
                FluidBarrelFluids.isGas(sample),
                profile.gasProof(),
                FluidBarrelFluids.allow(profile, sample),
                sealed,
                autoOutput);
        switch (reaction) {
            case MELTDOWN_LAVA -> meltdown(true, sample);
            case MELTDOWN_FIRE -> meltdown(false, sample);
            case MAGIC_DESTROY -> destroy(true, sample);
            case ACID_DESTROY -> destroy(false, sample);
            case TRASH -> {
                fizz();
                tank.removeAmount(Long.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            }
            case FERMENT -> ferment();
            case AUTO_OUTPUT -> autoOutput(sample);
            case NONE -> {
            }
        }
        sync(false);
    }

    private void meltdown(boolean lava, FluidStack sample) {
        fizz();
        float celsius = TankFluidSafety.temperature(sample) - 273.15F;
        Level level = this.level;
        BlockPos pos = this.worldPosition;
        if (lava) {
            tank.removeAmount(1000L, IFluidHandler.FluidAction.EXECUTE);
        }
        tank.removeAmount(Long.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        if (level == null) {
            return;
        }
        if (lava) {
            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
        } else {
            BlockState fire = Blocks.FIRE.defaultBlockState();
            level.setBlock(
                    pos,
                    fire.canSurvive(level, pos)
                            ? fire
                            : Blocks.AIR.defaultBlockState(),
                    3);
        }
        CrucibleWorldHazards.boilHazards(
                level, pos, celsius, CrucibleWorldHazards.SMALL_GAS_RANGE, 1);
    }

    private void destroy(boolean magic, FluidStack sample) {
        fizz();
        float celsius = TankFluidSafety.temperature(sample) - 273.15F;
        tank.removeAmount(Long.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        Level level = this.level;
        BlockPos pos = this.worldPosition;
        if (level == null) {
            return;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        if (magic) {
            CrucibleWorldHazards.boilHazards(
                    level, pos, celsius, CrucibleWorldHazards.SMALL_GAS_RANGE, 1);
        }
    }

    private void ferment() {
        String key = tank.storedFluidId() + ":" + tank.amount();
        if (sealedMax <= 0L || sealedRecipe == null) {
            if (key.equals(fermentMiss)) {
                if (sealedTime != 0L) {
                    sealedTime = 0L;
                    setChanged();
                }
                return;
            }
            FluidStack sample = tank.sample();
            Optional<GTRecipe> found = sample.isEmpty()
                    ? Optional.empty()
                    : ModRecipeMaps.FERMENTER.find(
                            new GTRecipeQuery(List.of(), List.of(sample)));
            if (found.isPresent() && fermentable(found.get())) {
                GTRecipe recipe = found.get();
                sealedRecipe = recipe;
                sealedMax = FluidBarrelLogic.sealedDuration(
                        tank.amount(),
                        recipe.eut(),
                        recipe.duration(),
                        recipe.fluidInputs().getFirst().getAmount());
                fermentMiss = "";
                setChanged();
            } else {
                fermentMiss = key;
                sealedRecipe = null;
                sealedMax = 0L;
                if (sealedTime != 0L) {
                    sealedTime = 0L;
                    setChanged();
                }
            }
            return;
        }
        if (sealedTime < sealedMax) {
            sealedTime++;
            setChanged();
            return;
        }
        FluidStack output = sealedRecipe.fluidOutputs().getFirst().copy();
        long scaled = FluidBarrelLogic.scaledOutput(
                output.getAmount(),
                tank.amount(),
                sealedRecipe.fluidInputs().getFirst().getAmount());
        sealedTime = 0L;
        sealedMax = 0L;
        sealedRecipe = null;
        fermentMiss = "";
        if (output.isEmpty()) {
            tank.setEmpty();
        } else {
            tank.setContents(output.getFluid(), scaled);
        }
    }

    private static boolean fermentable(GTRecipe recipe) {
        if (recipe.fluidInputs().isEmpty() || recipe.fluidOutputs().isEmpty()) {
            return false;
        }
        FluidStack input = recipe.fluidInputs().getFirst();
        FluidStack output = recipe.fluidOutputs().getFirst();
        return !input.isEmpty()
                && !output.isEmpty()
                && !FluidBarrelFluids.isGas(input)
                && !FluidBarrelFluids.isGas(output);
    }

    private void autoOutput(FluidStack sample) {
        if (level == null || sample.isEmpty()) {
            return;
        }
        for (Direction side : FluidBarrelLogic.autoOutputSides(
                FluidBarrelFluids.isGas(sample),
                TankFluidSafety.isLighter(sample))) {
            if (tank.amount() <= 0L) {
                return;
            }
            IFluidHandler neighbor = level.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    worldPosition.relative(side),
                    side.getOpposite());
            if (neighbor != null) {
                ExactFluidTransfer.move(tank, neighbor, Integer.MAX_VALUE);
            }
        }
    }

    private void fizz() {
        if (level != null) {
            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.LAVA_EXTINGUISH,
                    SoundSource.BLOCKS,
                    0.6F,
                    1.0F);
        }
    }

    private void applyMode(FluidBarrelLogic.Mode mode) {
        autoOutput = mode.autoOutput();
        sealed = mode.sealed();
        sealedTime = mode.sealedTime();
        if (sealedTime == 0L) {
            sealedMax = 0L;
            sealedRecipe = null;
            fermentMiss = "";
        }
        setChanged();
    }

    private void onTankChanged() {
        sealedMax = 0L;
        sealedRecipe = null;
        fermentMiss = "";
        fluidDirty = true;
        setChanged();
    }

    private void sync(boolean immediate) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!immediate) {
            if (!fluidDirty) {
                return;
            }
            if (syncDelay > 0) {
                syncDelay--;
                return;
            }
        }
        level.sendBlockUpdated(
                worldPosition, getBlockState(), getBlockState(), 3);
        fluidDirty = false;
        syncDelay = 20;
    }

    private Component contentsLine() {
        if (!tank.hasType()) {
            return Component.translatable(
                    "tooltip.cruciblecraft.fluid_barrel.capacity",
                    tank.capacity());
        }
        FluidStack named = new FluidStack(tank.fluid(), 1);
        return Component.translatable(
                "tooltip.cruciblecraft.fluid_barrel.contents",
                named.getHoverName(),
                tank.amount(),
                tank.capacity());
    }

    private static void tell(Player player, Component message) {
        player.displayClientMessage(message, false);
    }

    private static FluidBarrelProfile profileOf(BlockState state) {
        if (state.getBlock() instanceof FluidBarrelBlock block) {
            return block.profile();
        }
        throw new IllegalStateException(
                "Fluid barrel block entity on " + state.getBlock());
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        autoOutput = tag.getBoolean("auto_output");
        sealed = tag.getBoolean("sealed");
        sealedTime = Math.max(0L, tag.getLong("sealed_time"));
        sealedMax = 0L;
        sealedRecipe = null;
        fermentMiss = "";
        tank.readFluid(tag.getString("fluid"), tag.getLong("amount"));
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("auto_output", autoOutput);
        tag.putBoolean("sealed", sealed);
        tag.putLong("sealed_time", sealedTime);
        tag.putString("fluid", tank.storedFluidId());
        tag.putLong("amount", tank.amount());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
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
            loadAdditional(packet.getTag(), registries);
        }
    }

    @Override
    public int getTanks() {
        return tank.getTanks();
    }

    @Override
    public int tanks() {
        return tank.tanks();
    }

    @Override
    public FluidStack getFluidInTank(int index) {
        return tank.getFluidInTank(index);
    }

    @Override
    public FluidStack fluid(int index) {
        return tank.fluid(index);
    }

    @Override
    public long amount(int index) {
        return tank.amount(index);
    }

    @Override
    public int getTankCapacity(int index) {
        return tank.getTankCapacity(index);
    }

    @Override
    public long capacity(int index) {
        return tank.capacity(index);
    }

    @Override
    public boolean isFluidValid(int index, FluidStack stack) {
        return tank.isFluidValid(index, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return tank.fill(resource, action);
    }

    @Override
    public long fill(
            int index,
            FluidStack resource,
            long maxFill,
            FluidAction action) {
        return tank.fill(index, resource, maxFill, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return tank.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return tank.drain(maxDrain, action);
    }

    @Override
    public LongFluidHandler.LongFluidStack drain(
            int index, long maxDrain, FluidAction action) {
        return tank.drain(index, maxDrain, action);
    }
}
