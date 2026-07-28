package com.masson.cruciblecraft.content.blockentity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.machine.MachineDurabilityComponent;
import com.masson.cruciblecraft.recipe.AnvilRecipe;
import com.masson.cruciblecraft.recipe.AnvilRecipeInput;
import com.masson.cruciblecraft.recipe.AnvilMode;
import com.masson.cruciblecraft.recipe.AnvilRecipeRules;
import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AnvilBlockEntity extends BlockEntity {
    private final ItemStack[] workpieces = {ItemStack.EMPTY, ItemStack.EMPTY};
    private int strikes;
    private String materialId = MachineMaterialRules.DEFAULT_ANVIL_MATERIAL;
    private long durability = MachineMaterialRules.anvilMaxDurability(materialId);
    private long maxDurability = durability;
    private String activeRecipe = "";

    public AnvilBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ANVIL.get(), pos, blockState);
    }

    public boolean insert(int slot, ItemStack heldStack) {
        if (level == null || slot < 0 || slot >= workpieces.length
                || !workpieces[slot].isEmpty() || heldStack.isEmpty()) {
            return false;
        }

        ItemStack inserted = heldStack.copy();
        ItemStack old = workpieces[slot];
        workpieces[slot] = inserted;
        if (!hasPotentialRecipe()) {
            workpieces[slot] = old;
            return false;
        }

        resetProgress();
        setChangedAndSync();
        return true;
    }

    public Optional<StrikeResult> strike(AnvilMode mode, int hammerTier) {
        if (level == null || (workpieces[0].isEmpty() && workpieces[1].isEmpty())) {
            return Optional.empty();
        }

        AnvilRecipeInput input = input(mode);
        Optional<RecipeHolder<AnvilRecipe>> matched = findRecipe(input);
        if (matched.isEmpty()) {
            return Optional.empty();
        }

        AnvilRecipe recipe = matched.get().value();
        AnvilRecipe.MatchPlan plan = recipe.matchPlan(input).orElseThrow();
        int workpieceTier = MaterialUnits.resolve(input.getItem(plan.primarySlot()))
                .map(entry -> entry.material().tier())
                .orElse(Integer.MAX_VALUE);
        if (hammerTier < workpieceTier
                || !MachineMaterialRules.supportsTier(Device.ANVIL, materialId, workpieceTier)) {
            return Optional.empty();
        }

        String recipeId = matched.get().id().toString();
        if (!recipeId.equals(activeRecipe)) {
            strikes = 0;
            activeRecipe = recipeId;
        }
        strikes++;
        boolean completed = strikes >= recipe.hits();
        ItemStack result = input.getItem(plan.primarySlot()).copy();
        List<ItemStack> overflow = new java.util.ArrayList<>();
        boolean exhausted = false;
        if (completed) {
            ItemStack primaryOutput = recipe.assemble(input, level.registryAccess());
            ItemStack craftedResult = primaryOutput.copy();
            ItemStack secondaryOutput = AnvilRecipeRules.secondarySucceeds(
                    recipe.secondaryChance(), level.random.nextDouble())
                    ? recipe.assembleSecondary(input)
                    : ItemStack.EMPTY;
            consume(plan);
            placeOutput(primaryOutput, plan.primarySlot(), overflow);
            placeOutput(secondaryOutput, plan.secondarySlot() >= 0 ? plan.secondarySlot() : 1 - plan.primarySlot(), overflow);
            durability = Math.max(0L, durability - MachineMaterialRules.anvilWear(recipe.recipePower()));
            exhausted = durability == 0L;
            result = craftedResult;
            resetProgress();
        }
        setChangedAndSync();
        return Optional.of(new StrikeResult(
                completed, strikes, recipe.hits(), result.copy(), List.copyOf(overflow), exhausted));
    }

    public ItemStack extract(int slot) {
        if (slot < 0 || slot >= workpieces.length || workpieces[slot].isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = workpieces[slot];
        workpieces[slot] = ItemStack.EMPTY;
        resetProgress();
        setChangedAndSync();
        return extracted;
    }

    public ItemStack splitBetweenSlots() {
        int source = workpieces[0].isEmpty() ? 1 : workpieces[1].isEmpty() ? 0 : -1;
        if (source < 0 || workpieces[source].isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = workpieces[source];
        ItemStack odd = ItemStack.EMPTY;
        if ((stack.getCount() & 1) != 0) {
            odd = stack.copyWithCount(1);
            stack.shrink(1);
        }
        int target = 1 - source;
        if (stack.getCount() > 1) {
            int half = stack.getCount() / 2;
            workpieces[target] = stack.copyWithCount(half);
            stack.setCount(half);
        }
        if (stack.isEmpty()) {
            workpieces[source] = ItemStack.EMPTY;
        }
        resetProgress();
        setChangedAndSync();
        return odd;
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < workpieces.length; slot++) {
            if (!workpieces[slot].isEmpty()) {
                Containers.dropItemStack(
                        level,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 1.0,
                        worldPosition.getZ() + 0.5,
                        workpieces[slot]);
                workpieces[slot] = ItemStack.EMPTY;
            }
        }
        resetProgress();
    }

    public ItemStack workpiece() {
        return workpieces[0];
    }

    public ItemStack workpiece(int slot) {
        return slot >= 0 && slot < workpieces.length ? workpieces[slot] : ItemStack.EMPTY;
    }

    public int strikes() {
        return strikes;
    }

    public String materialId() {
        return materialId;
    }

    public int materialTier() {
        return MachineMaterialRules.processingTier(Device.ANVIL, materialId);
    }

    public long durability() {
        return durability;
    }

    public long maxDurability() {
        return maxDurability;
    }

    public MachineDurabilityComponent durabilityComponent() {
        return new MachineDurabilityComponent(durability, maxDurability);
    }

    public void setMaterial(String materialId, MachineDurabilityComponent savedDurability) {
        String sanitized = MachineMaterialRules.sanitize(Device.ANVIL, materialId);
        this.materialId = sanitized;
        long expectedMax = MachineMaterialRules.anvilMaxDurability(sanitized);
        this.maxDurability = savedDurability == null ? expectedMax : savedDurability.max();
        this.durability = savedDurability == null
                ? expectedMax
                : Math.min(savedDurability.current(), this.maxDurability);
        setChangedAndSync();
    }

    public void setMaterialId(String materialId) {
        setMaterial(materialId, null);
    }

    private Optional<RecipeHolder<AnvilRecipe>> findRecipe(AnvilRecipeInput input) {
        return level.getRecipeManager().getRecipesFor(
                ModRecipes.ANVIL_TYPE.get(),
                input,
                level).stream()
                .max(Comparator.comparing(holder -> holder.value().material().isPresent()));
    }

    private boolean hasPotentialRecipe() {
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.ANVIL_TYPE.get()).stream()
                .anyMatch(holder -> {
                    AnvilRecipe recipe = holder.value();
                    if (recipe.matchPlan(input(recipe.mode())).isPresent()) {
                        return true;
                    }
                    return isPotentialIngredient(workpieces[0], recipe)
                            || isPotentialIngredient(workpieces[1], recipe);
                });
    }

    private static boolean isPotentialIngredient(ItemStack stack, AnvilRecipe recipe) {
        return MaterialUnits.resolve(stack)
                .filter(entry -> recipe.material().isEmpty()
                        || recipe.material().get().equals(entry.material().id()))
                .filter(entry -> entry.form() == recipe.input()
                        || recipe.secondInput().filter(form -> form == entry.form()).isPresent())
                .isPresent();
    }

    private AnvilRecipeInput input(AnvilMode mode) {
        return new AnvilRecipeInput(workpieces[0], workpieces[1], mode);
    }

    private void consume(AnvilRecipe.MatchPlan plan) {
        var remaining = AnvilRecipeRules.consume(
                workpieces[0].getCount(),
                workpieces[1].getCount(),
                plan.primarySlot(),
                plan.primaryCount(),
                plan.secondarySlot(),
                plan.secondaryCount());
        int[] counts = {remaining.first(), remaining.second()};
        for (int slot = 0; slot < workpieces.length; slot++) {
            if (counts[slot] == 0) {
                workpieces[slot] = ItemStack.EMPTY;
            } else {
                workpieces[slot].setCount(counts[slot]);
            }
        }
    }

    private void placeOutput(ItemStack output, int preferredSlot, List<ItemStack> overflow) {
        if (output.isEmpty()) {
            return;
        }
        int first = preferredSlot >= 0 ? preferredSlot : 0;
        for (int slot : new int[] {first, 1 - first}) {
            if (workpieces[slot].isEmpty()) {
                workpieces[slot] = output;
                return;
            }
            if (ItemStack.isSameItemSameComponents(workpieces[slot], output)) {
                int room = workpieces[slot].getMaxStackSize() - workpieces[slot].getCount();
                int moved = Math.min(room, output.getCount());
                workpieces[slot].grow(moved);
                output.shrink(moved);
                if (output.isEmpty()) {
                    return;
                }
            }
        }
        overflow.add(output);
    }

    private void resetProgress() {
        strikes = 0;
        activeRecipe = "";
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        workpieces[0] = tag.contains("workpiece_0", Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, tag.getCompound("workpiece_0"))
                : tag.contains("workpiece", Tag.TAG_COMPOUND)
                        ? ItemStack.parseOptional(registries, tag.getCompound("workpiece"))
                        : ItemStack.EMPTY;
        workpieces[1] = tag.contains("workpiece_1", Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, tag.getCompound("workpiece_1"))
                : ItemStack.EMPTY;
        strikes = tag.getInt("strikes");
        activeRecipe = tag.getString("active_recipe");
        materialId = MachineMaterialRules.sanitize(
                Device.ANVIL,
                tag.contains("material_id", Tag.TAG_STRING)
                        ? tag.getString("material_id")
                        : MachineMaterialRules.DEFAULT_ANVIL_MATERIAL);
        maxDurability = tag.contains("max_durability", Tag.TAG_LONG)
                ? tag.getLong("max_durability")
                : MachineMaterialRules.anvilMaxDurability(materialId);
        durability = tag.contains("durability", Tag.TAG_LONG)
                ? Math.min(tag.getLong("durability"), maxDurability)
                : maxDurability;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!workpieces[0].isEmpty()) {
            tag.put("workpiece_0", workpieces[0].save(registries));
        }
        if (!workpieces[1].isEmpty()) {
            tag.put("workpiece_1", workpieces[1].save(registries));
        }
        tag.putInt("strikes", strikes);
        tag.putString("active_recipe", activeRecipe);
        tag.putString("material_id", materialId);
        tag.putLong("durability", durability);
        tag.putLong("max_durability", maxDurability);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public record StrikeResult(
            boolean completed,
            int progress,
            int required,
            ItemStack result,
            List<ItemStack> overflow,
            boolean anvilExhausted) {}
}
