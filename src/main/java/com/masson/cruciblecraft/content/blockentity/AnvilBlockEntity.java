package com.masson.cruciblecraft.content.blockentity;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.block.AnvilBlock;
import com.masson.cruciblecraft.content.block.AnvilHosts;
import com.masson.cruciblecraft.heat.HeatComponent;
import com.masson.cruciblecraft.machine.MachineDurabilityComponent;
import com.masson.cruciblecraft.recipe.AnvilMode;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.AnvilRecipeExecutionRules;
import com.masson.cruciblecraft.recipe.gt.GTRecipeCache;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import com.masson.cruciblecraft.machine.processing.ChanceOutputs;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AnvilBlockEntity extends BlockEntity {
    private final ItemStack[] workpieces = {ItemStack.EMPTY, ItemStack.EMPTY};
    private int strikes;
    private String materialId = MachineMaterialRules.DEFAULT_ANVIL_MATERIAL;
    private String quarantinedMaterialId = "";
    private long durability = MachineMaterialRules.anvilMaxDurability(materialId);
    private long maxDurability = durability;
    private String activeRecipe = "";
    private boolean quarantineWarningLogged;
    private final Map<AnvilMode, GTRecipeCache> recipeCaches = createRecipeCaches();

    public AnvilBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ANVIL.get(), pos, blockState);
        applyBakedMaterial(blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!quarantineWarningLogged
                && level != null
                && !level.isClientSide
                && materialQuarantined()) {
            quarantineWarningLogged = true;
            CrucibleCraft.LOGGER.warn(
                    "Quarantined anvil at {} {}: unsupported material {}",
                    level.dimension().location(),
                    worldPosition,
                    quarantinedMaterialId);
        }
    }

    public int insertOrMerge(int slot, ItemStack heldStack) {
        if (!quarantinedMaterialId.isEmpty()
                || level == null
                || slot < 0
                || slot >= workpieces.length
                || heldStack.isEmpty()) {
            return 0;
        }

        ItemStack target = workpieces[slot];
        if (!target.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(target, heldStack)) {
                return 0;
            }
            int moved = Math.min(
                    heldStack.getCount(),
                    target.getMaxStackSize() - target.getCount());
            if (moved <= 0) {
                return 0;
            }
            target.grow(moved);
            resetProgress();
            setChangedAndSync();
            return moved;
        }

        int moved = Math.min(heldStack.getCount(), heldStack.getMaxStackSize());
        ItemStack inserted = heldStack.copyWithCount(moved);
        workpieces[slot] = inserted;
        if (!AnvilHosts.isHammer(inserted) && !hasPotentialRecipe()) {
            workpieces[slot] = ItemStack.EMPTY;
            return 0;
        }
        resetProgress();
        setChangedAndSync();
        return moved;
    }

    public Optional<StrikeResult> strike(AnvilMode mode, int hammerTier) {
        if (!quarantinedMaterialId.isEmpty()
                || level == null
                || (workpieces[0].isEmpty() && workpieces[1].isEmpty())) {
            return Optional.empty();
        }

        Optional<AnvilMatch> matched = findRecipe(mode);
        if (matched.isEmpty()) {
            return Optional.empty();
        }

        AnvilMatch match = matched.get();
        GTRecipe recipe = match.recipe();
        ItemStack source = workpieces[match.primarySlot()];
        int workpieceTier = MaterialUnits.resolve(source)
                .map(entry -> entry.material().tier())
                .orElse(Integer.MAX_VALUE);
        if (hammerTier < workpieceTier
                || !MachineMaterialRules.supportsTier(Device.ANVIL, materialId, workpieceTier)) {
            return Optional.empty();
        }

        String recipeId = match.id().toString();
        if (!recipeId.equals(activeRecipe)) {
            strikes = 0;
            activeRecipe = recipeId;
        }
        strikes++;
        int requiredHits = Math.toIntExact(recipe.specialValue());
        boolean completed = strikes >= requiredHits;
        ItemStack result = source.copy();
        List<ItemStack> overflow = new java.util.ArrayList<>();
        boolean exhausted = false;
        if (completed) {
            List<ItemStack> outputs = recipe.itemOutputs();
            ItemStack primaryOutput = outputs.getFirst();
            copyHeat(source, primaryOutput);
            ItemStack craftedResult = primaryOutput.copy();
            ItemStack secondaryOutput = outputs.size() > 1
                    ? ChanceOutputs.roll(
                            outputs.get(1),
                            recipe.outputChances().get(1),
                            level.random::nextInt)
                    : ItemStack.EMPTY;
            consume(match.allocation());
            placeOutput(primaryOutput, match.primarySlot(), overflow);
            placeOutput(secondaryOutput, 1 - match.primarySlot(), overflow);
            durability = Math.max(
                    0L,
                    durability - MachineMaterialRules.anvilWear(recipe.eut()));
            exhausted = durability == 0L;
            result = craftedResult;
            resetProgress();
        }
        setChangedAndSync();
        return Optional.of(new StrikeResult(
                completed, strikes, requiredHits, result.copy(), List.copyOf(overflow), exhausted));
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

    public boolean materialQuarantined() {
        return !quarantinedMaterialId.isEmpty();
    }

    public String quarantinedMaterialId() {
        return quarantinedMaterialId;
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
        Optional<String> baked = AnvilHosts.bakedMaterial(getBlockState());
        String material = baked.isPresent()
                ? baked.orElseThrow()
                : MachineMaterialRules.requireAllowed(Device.ANVIL, materialId);
        this.materialId = material;
        this.quarantinedMaterialId = "";
        long expectedMax = AnvilHosts.maxDurability(material);
        this.maxDurability = savedDurability == null ? expectedMax : savedDurability.max();
        this.durability = savedDurability == null
                ? expectedMax
                : Math.min(savedDurability.current(), this.maxDurability);
        syncMetallicState();
        setChangedAndSync();
    }

    public void setMaterialId(String materialId) {
        setMaterial(materialId, null);
    }

    private Optional<AnvilMatch> findRecipe(AnvilMode mode) {
        RecipeMap.Match cached = recipeCaches.get(mode)
                .findItems(workpieces[0], workpieces[1])
                .filter(match -> validAnvilRecipe(match.recipe()))
                .orElse(null);
        if (cached == null) {
            return Optional.empty();
        }
        GTRecipe recipe = cached.recipe();
        long[][] allocation = recipe.itemAllocation(List.of(workpieces)).orElse(null);
        if (allocation == null || allocation.length == 0) {
            return Optional.empty();
        }
        int primarySlot = -1;
        for (int slot = 0; slot < allocation[0].length; slot++) {
            if (allocation[0][slot] > 0L) {
                primarySlot = slot;
                break;
            }
        }
        return primarySlot < 0
                ? Optional.empty()
                : Optional.of(new AnvilMatch(
                        cached.id(),
                        recipe,
                        allocation,
                        primarySlot));
    }

    private boolean hasPotentialRecipe() {
        for (RecipeMap map : List.of(
                ModRecipeMaps.ANVIL,
                ModRecipeMaps.ANVIL_BEND_SMALL,
                ModRecipeMaps.ANVIL_BEND_BIG)) {
            if (map.hasCandidate(workpieces[0]) || map.hasCandidate(workpieces[1])) {
                return true;
            }
        }
        return false;
    }

    private void consume(long[][] allocation) {
        int[] counts = {
                workpieces[0].getCount(),
                workpieces[1].getCount()
        };
        for (long[] requirement : allocation) {
            for (int slot = 0; slot < requirement.length; slot++) {
                counts[slot] = Math.subtractExact(counts[slot], Math.toIntExact(requirement[slot]));
            }
        }
        for (int slot = 0; slot < workpieces.length; slot++) {
            if (counts[slot] == 0) {
                workpieces[slot] = ItemStack.EMPTY;
            } else {
                workpieces[slot].setCount(counts[slot]);
            }
        }
    }

    private static boolean validAnvilRecipe(GTRecipe recipe) {
        return recipe.specialValue() > 0L
                && recipe.specialValue() <= Integer.MAX_VALUE
                && recipe.eut() > 0L
                && AnvilRecipeExecutionRules.supportsOutputs(
                        recipe.itemOutputs().size(),
                        recipe.outputChances().isEmpty()
                                ? -1
                                : recipe.outputChances().getFirst())
                && recipe.fluidInputs().isEmpty()
                && recipe.fluidOutputs().isEmpty();
    }

    private static void copyHeat(ItemStack source, ItemStack output) {
        HeatComponent heat = source.get(ModComponents.HEAT.get());
        if (heat != null) {
            output.set(ModComponents.HEAT.get(), heat);
        }
    }

    private static Map<AnvilMode, GTRecipeCache> createRecipeCaches() {
        EnumMap<AnvilMode, GTRecipeCache> caches = new EnumMap<>(AnvilMode.class);
        for (AnvilMode mode : AnvilMode.values()) {
            caches.put(mode, new GTRecipeCache(ModRecipeMaps.anvil(mode)));
        }
        return Map.copyOf(caches);
    }

    private record AnvilMatch(
            ResourceLocation id,
            GTRecipe recipe,
            long[][] allocation,
            int primarySlot) {}

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
        Optional<String> baked = AnvilHosts.bakedMaterial(getBlockState());
        String requestedMaterial = tag.contains("material_id", Tag.TAG_STRING)
                ? tag.getString("material_id")
                : MachineMaterialRules.DEFAULT_ANVIL_MATERIAL;
        if (baked.isPresent()) {
            materialId = baked.orElseThrow();
            quarantinedMaterialId = "";
        } else {
            var resolution = MachineMaterialRules.resolveExternal(
                    Device.ANVIL, requestedMaterial);
            materialId = resolution.effectiveMaterial();
            quarantinedMaterialId = resolution.quarantinedMaterial().orElse("");
        }
        long expectedMax = AnvilHosts.maxDurability(materialId);
        long savedMax = tag.contains("max_durability", Tag.TAG_LONG)
                ? tag.getLong("max_durability")
                : expectedMax;
        maxDurability = savedMax > 0L ? savedMax : expectedMax;
        long savedDurability = tag.contains("durability", Tag.TAG_LONG)
                ? tag.getLong("durability")
                : maxDurability;
        durability = Math.max(0L, Math.min(savedDurability, maxDurability));
        syncMetallicState();
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
        tag.putString(
                "material_id",
                quarantinedMaterialId.isEmpty()
                        ? materialId
                        : quarantinedMaterialId);
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

    private void applyBakedMaterial(BlockState state) {
        Optional<String> baked = AnvilHosts.bakedMaterial(state);
        if (baked.isEmpty()) {
            return;
        }
        materialId = baked.orElseThrow();
        quarantinedMaterialId = "";
        maxDurability = AnvilHosts.maxDurability(materialId);
        durability = maxDurability;
    }

    private void syncMetallicState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(AnvilBlock.METALLIC)) {
            return;
        }
        boolean metallic = AnvilHosts.metallic(materialId);
        if (state.getValue(AnvilBlock.METALLIC) != metallic) {
            level.setBlock(worldPosition, state.setValue(AnvilBlock.METALLIC, metallic), Block.UPDATE_CLIENTS);
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            syncMetallicState();
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
