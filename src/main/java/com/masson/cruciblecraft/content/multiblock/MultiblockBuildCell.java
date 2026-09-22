package com.masson.cruciblecraft.content.multiblock;

import java.util.Objects;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One structure cell exposed to the builder wand.
 *
 * <p>A cell can be validation-only (controller, air cavity, or an environment
 * requirement) or placeable. Item matching is intentionally performed against
 * the complete stack so component-bearing block items remain exact.</p>
 */
public record MultiblockBuildCell(
        BlockPos position,
        Predicate<BlockState> stateMatcher,
        Predicate<ItemStack> itemMatcher,
        boolean placeable,
        String description,
        Optional<String> uniformGroup) {
    public MultiblockBuildCell(
            BlockPos position,
            Predicate<BlockState> stateMatcher,
            Predicate<ItemStack> itemMatcher,
            boolean placeable,
            String description) {
        this(
                position,
                stateMatcher,
                itemMatcher,
                placeable,
                description,
                Optional.empty());
    }

    public MultiblockBuildCell {
        position = Objects.requireNonNull(position, "position").immutable();
        Objects.requireNonNull(stateMatcher, "stateMatcher");
        Objects.requireNonNull(itemMatcher, "itemMatcher");
        description = Objects.requireNonNull(description, "description");
        uniformGroup = Objects.requireNonNull(uniformGroup, "uniformGroup")
                .filter(group -> !group.isBlank());
    }

    public boolean matches(BlockState state) {
        return stateMatcher.test(state);
    }

    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && itemMatcher.test(stack);
    }

    public boolean accepts(
            ItemStack stack,
            Map<String, Block> selectedUniformBlocks) {
        if (!accepts(stack)) {
            return false;
        }
        if (uniformGroup.isEmpty()) {
            return true;
        }
        Block selected = selectedUniformBlocks.get(uniformGroup.orElseThrow());
        return selected == null
                || stack.getItem() instanceof BlockItem blockItem
                        && blockItem.getBlock() == selected;
    }
}
