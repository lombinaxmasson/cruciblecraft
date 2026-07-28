package com.masson.cruciblecraft.content.blockentity;

import java.util.Optional;
import com.masson.cruciblecraft.api.kinetic.KineticType;
import com.masson.cruciblecraft.content.block.CrusherBlock;
import com.masson.cruciblecraft.content.menu.CrusherMenu;
import com.masson.cruciblecraft.recipe.CrusherRecipe;
import com.masson.cruciblecraft.recipe.CrusherRecipeInput;
import com.masson.cruciblecraft.recipe.CrusherProgress;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModRecipes;
import com.masson.cruciblecraft.steam.MachineSideRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class CrusherBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOT = 0, OUTPUT_SLOT = 1;
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == INPUT_SLOT; }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final IItemHandler inputHandler = new SidedItems(true);
    private final IItemHandler outputHandler = new SidedItems(false);
    private int progress, duration, powerDemand;
    private String pausedReason = "idle";
    private String activeOutputId = "";
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return switch (index) {
            case 0 -> progress; case 1 -> duration; case 2 -> powerDemand; default -> 0; }; }
        @Override public void set(int index, int value) {
            if (index == 0) progress = value; else if (index == 1) duration = value; else if (index == 2) powerDemand = value;
        }
        @Override public int getCount() { return 3; }
    };

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrusherBlockEntity crusher) {
        Optional<RecipeHolder<CrusherRecipe>> found = crusher.findRecipe();
        if (found.isEmpty()) {
            crusher.progress = crusher.duration = crusher.powerDemand = 0;
            crusher.activeOutputId = "";
            crusher.setStatus("idle");
            return;
        }
        CrusherRecipe recipe = found.get().value();
        ItemStack result = recipe.assemble(
                new CrusherRecipeInput(crusher.inventory.getStackInSlot(INPUT_SLOT)), level.registryAccess());
        if (result.isEmpty()) { crusher.setStatus("invalid_output"); return; }
        String outputId = BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
        if (!outputId.equals(crusher.activeOutputId)) {
            crusher.progress = 0;
            crusher.activeOutputId = outputId;
        }
        crusher.duration = recipe.duration();
        crusher.powerDemand = recipe.power();
        if (!crusher.canOutput(result)) { crusher.setStatus("output_blocked"); return; }
        Direction facing = state.getValue(CrusherBlock.FACING);
        BlockPos sourcePos = pos.relative(facing.getOpposite());
        var source = level.getCapability(ModCapabilities.KINETIC_SOURCE, sourcePos, facing);
        if (source == null || source.type() != KineticType.KU
                || source.extract(recipe.power(), true) < recipe.power()) {
            crusher.setStatus("underpowered"); return;
        }
        if (source.extract(recipe.power(), false) < recipe.power()) {
            crusher.setStatus("underpowered"); return;
        }
        crusher.pausedReason = "";
        crusher.progress = CrusherProgress.advance(crusher.progress, recipe.duration(), true, true);
        if (crusher.progress >= recipe.duration()) {
            crusher.inventory.extractItem(INPUT_SLOT, 1, false);
            ItemStack current = crusher.inventory.getStackInSlot(OUTPUT_SLOT);
            if (current.isEmpty()) crusher.inventory.setStackInSlot(OUTPUT_SLOT, result);
            else { current.grow(result.getCount()); crusher.inventory.setStackInSlot(OUTPUT_SLOT, current); }
            crusher.progress = 0;
            crusher.activeOutputId = "";
        }
        crusher.changedAndSync();
    }

    private Optional<RecipeHolder<CrusherRecipe>> findRecipe() {
        if (level == null || inventory.getStackInSlot(INPUT_SLOT).isEmpty()) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(
                ModRecipes.CRUSHER_TYPE.get(), new CrusherRecipeInput(inventory.getStackInSlot(INPUT_SLOT)), level);
    }
    private boolean canOutput(ItemStack result) {
        ItemStack current = inventory.getStackInSlot(OUTPUT_SLOT);
        return current.isEmpty() || (ItemStack.isSameItemSameComponents(current, result)
                && current.getCount() + result.getCount() <= current.getMaxStackSize());
    }
    public IItemHandler items(Direction side) {
        Direction front = getBlockState().getValue(CrusherBlock.FACING);
        return MachineSideRules.crusherExtractsItems(front, side) ? outputHandler : inputHandler;
    }
    public ItemStackHandler inventory() { return inventory; }
    public ContainerData data() { return data; }
    public int progress() { return progress; }
    public int duration() { return duration; }
    public int powerDemand() { return powerDemand; }
    public String pausedReason() { return pausedReason; }
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64;
    }
    public void dropContents() {
        if (level != null) for (int i = 0; i < 2; i++) Containers.dropItemStack(
                level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inventory.getStackInSlot(i));
    }
    @Override public Component getDisplayName() { return Component.translatable("block.cruciblecraft.bronze_crusher"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CrusherMenu(id, inv, this);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("progress", progress); tag.putInt("duration", duration);
        tag.putString("active_output", activeOutputId);
        tag.putInt("power_demand", powerDemand);
        tag.putString("paused_reason", pausedReason);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = Math.max(0, tag.getInt("progress")); duration = Math.max(0, tag.getInt("duration"));
        activeOutputId = tag.getString("active_output");
        powerDemand = Math.max(0, tag.getInt("power_demand"));
        pausedReason = tag.contains("paused_reason") ? tag.getString("paused_reason") : "idle";
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    private void setStatus(String status) {
        if (!status.equals(pausedReason)) {
            pausedReason = status;
            changedAndSync();
        }
    }

    private void changedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    private final class SidedItems implements IItemHandler {
        private final boolean input;
        private SidedItems(boolean input) { this.input = input; }
        @Override public int getSlots() { return 1; }
        private int actual() { return input ? INPUT_SLOT : OUTPUT_SLOT; }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(actual()); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return input ? inventory.insertItem(INPUT_SLOT, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return input ? ItemStack.EMPTY : inventory.extractItem(OUTPUT_SLOT, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(actual()); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return input; }
    }
}
