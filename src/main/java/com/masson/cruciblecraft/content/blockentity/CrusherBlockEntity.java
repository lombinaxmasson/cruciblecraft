package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.content.block.CrusherBlock;
import com.masson.cruciblecraft.content.menu.CrusherMenu;
import com.masson.cruciblecraft.machine.processing.CrusherLegacyMigration;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Thin crusher host over the shared processing runtime. */
public final class CrusherBlockEntity extends ProcessingMachineBlockEntity
        implements MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final long KU_CAPACITY = 1_024L;
    private CrusherLegacyMigration.BoundHint legacyMigrationHint;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress();
                case 1 -> duration();
                case 2 -> powerDemand();
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            if (index == 0) {
                runtime().processor().setProgress(value);
            } else if (index == 1) {
                runtime().processor().setDuration(value);
            }
        }
        @Override public int getCount() { return 3; }
    };

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER.get(), pos, state, ModProcessingMachines.CRUSHER);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CrusherBlockEntity crusher) {
        crusher.tickProcessingServer();
    }

    public ContainerData data() {
        return data;
    }

    public int powerDemand() {
        return (int) Math.min(Integer.MAX_VALUE, powerDemandLong());
    }

    public boolean stillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                        worldPosition.getX() + .5,
                        worldPosition.getY() + .5,
                        worldPosition.getZ() + .5) <= 64;
    }

    public void dropContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (int slot = 0; slot < inventory().getSlots(); slot++) {
            ItemStack stack = inventory().getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(
                        level,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        stack);
                inventory().setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.cruciblecraft.bronze_crusher");
    }

    @Override public AbstractContainerMenu createMenu(
            int id,
            Inventory inventory,
            Player player) {
        return new CrusherMenu(id, inventory, this);
    }

    @Override protected Direction machineFront() {
        BlockState state = getBlockState();
        return state.hasProperty(CrusherBlock.FACING)
                ? state.getValue(CrusherBlock.FACING)
                : null;
    }

    @Override protected void beforeRuntimeSelect(RecipeMap.Match match) {
        if (legacyMigrationHint == null) {
            return;
        }
        ItemStack output = match.recipe().itemOutputs().stream()
                .findFirst().orElse(ItemStack.EMPTY);
        CrusherLegacyMigration.Adoption adoption = CrusherLegacyMigration.adopt(
                legacyMigrationHint,
                match.id(),
                match.recipe().duration(),
                output);
        runtime().restore(
                adoption.recipeId(),
                adoption.progress(),
                adoption.duration(),
                adoption.status());
        legacyMigrationHint = null;
        markMutation();
    }

    @Override protected void onItemSlotChanged(int slot) {
        if (slot != INPUT_SLOT || legacyMigrationHint == null) {
            return;
        }
        boolean unchanged = level != null && CrusherLegacyMigration.inputUnchanged(
                legacyMigrationHint,
                inventory().getStackInSlot(INPUT_SLOT),
                level.registryAccess());
        if (!unchanged) {
            legacyMigrationHint = null;
            runtime().reset();
            markMutation();
        }
    }

    @Override protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (!tag.contains("processing_version")) {
            CrusherLegacyMigration.Hint hint = new CrusherLegacyMigration.Hint(
                    tag.getString("active_output"),
                    tag.getInt("progress"),
                    tag.getInt("duration"),
                    tag.contains("paused_reason")
                            ? tag.getString("paused_reason") : "idle");
            legacyMigrationHint = CrusherLegacyMigration.bind(
                    hint, inventory().getStackInSlot(INPUT_SLOT), registries).orElse(null);
            runtime().restore(
                    "",
                    0,
                    tag.getInt("duration"),
                    tag.contains("paused_reason")
                            ? tag.getString("paused_reason") : "idle");
            restoreEnergy(tag.getLong("kinetic_energy"));
        } else if (tag.contains("legacy_output_hint")) {
            try {
                CrusherLegacyMigration.BoundHint loaded =
                        new CrusherLegacyMigration.BoundHint(
                        new CrusherLegacyMigration.Hint(
                                tag.getString("legacy_output_hint"),
                                tag.getInt("legacy_progress"),
                                tag.getInt("legacy_duration"),
                                tag.getString("legacy_status")),
                        tag.getString("legacy_input_fingerprint"));
                legacyMigrationHint = CrusherLegacyMigration.inputUnchanged(
                        loaded,
                        inventory().getStackInSlot(INPUT_SLOT),
                        registries) ? loaded : null;
                if (legacyMigrationHint == null) {
                    runtime().reset();
                }
            } catch (IllegalArgumentException exception) {
                legacyMigrationHint = null;
                runtime().reset();
            }
        }
    }

    @Override protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (legacyMigrationHint != null) {
            CrusherLegacyMigration.Hint hint = legacyMigrationHint.hint();
            tag.putString("legacy_output_hint", hint.outputItemId());
            tag.putInt("legacy_progress", hint.progress());
            tag.putInt("legacy_duration", hint.duration());
            tag.putString("legacy_status", hint.status());
            tag.putString(
                    "legacy_input_fingerprint", legacyMigrationHint.inputFingerprint());
        }
    }
}
