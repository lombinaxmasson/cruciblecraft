package com.masson.cruciblecraft.content.item.tool;

import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.block.GtBlockObjectBlock;
import com.masson.cruciblecraft.content.block.GtStoneBlock;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code GT_Tool_HandDrill}: bricks and concrete take an iron/steel-family
 * rod and become the matching reinforced identity. Slabs stay unmapped.
 */
public final class DrillReinforce {
    private DrillReinforce() {}

    public static ToolResult use(UseOnContext context) {
        Level level = context.getLevel();
        BlockState state = level.getBlockState(context.getClickedPos());
        Optional<Block> reinforced = reinforcedBlock(state.getBlock());
        if (reinforced.isEmpty()) {
            return ToolResult.PASS;
        }
        Player player = context.getPlayer();
        if (player == null || !player.mayBuild()) {
            return ToolResult.PASS;
        }
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        if (!player.getAbilities().instabuild && !consumeRod(player)) {
            return ToolResult.REJECT;
        }
        level.setBlock(
                context.getClickedPos(),
                reinforced.orElseThrow().defaultBlockState(),
                Block.UPDATE_ALL);
        ToolClick.hurt(context);
        return ToolResult.SUCCESS;
    }

    public static Optional<String> reinforcedPath(String registryPath) {
        if (registryPath == null || registryPath.contains("/slab_")) {
            return Optional.empty();
        }
        if (registryPath.endsWith("/bricks")
                && !registryPath.endsWith("/reinforced_bricks")
                && !registryPath.endsWith("/small_bricks")
                && !registryPath.endsWith("/cracked_bricks")
                && !registryPath.endsWith("/mossy_bricks")
                && !registryPath.endsWith("/redstoned_bricks")) {
            return Optional.of(
                    registryPath.substring(0, registryPath.length() - "bricks".length())
                            + "reinforced_bricks");
        }
        if (registryPath.equals("concrete/concrete")) {
            return Optional.of("concrete_reinforced/reinforced_concrete");
        }
        if (registryPath.startsWith("concrete/")
                && !registryPath.startsWith("concrete_reinforced/")) {
            return Optional.of("concrete_reinforced/" + registryPath.substring("concrete/".length()));
        }
        return Optional.empty();
    }

    static boolean ironOrSteelRod(ItemStack stack) {
        Optional<MaterialUnits.Entry> entry = MaterialUnits.resolve(stack);
        if (entry.isEmpty()
                || !entry.orElseThrow().form().serializedName().equals("rod")) {
            return false;
        }
        return ironOrSteel(entry.orElseThrow().materialId());
    }

    static boolean ironOrSteel(String materialId) {
        return MaterialCatalog.find(materialId)
                .map(material -> {
                    List<String> aliases = material.gt6Metadata()
                            .map(metadata -> metadata.aliases())
                            .orElse(List.of());
                    return aliases.contains("AnyIron")
                            || aliases.contains("AnySteel")
                            || aliases.contains("AnyIronOrSteel")
                            || aliases.contains("AnyIronSteel");
                })
                .orElse(false);
    }

    private static Optional<Block> reinforcedBlock(Block block) {
        Optional<String> path = registryPath(block).flatMap(DrillReinforce::reinforcedPath);
        if (path.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, path.orElseThrow());
        var stones = ModBlocks.gtStoneBlocksById().get(id);
        if (stones != null) {
            return Optional.of(stones.get());
        }
        var remainder = ModBlocks.bathRemainderBlockObjectBlocksById().get(id);
        if (remainder != null) {
            return Optional.of(remainder.get());
        }
        var objects = ModBlocks.gtBlockObjectBlocksById().get(id);
        if (objects != null) {
            return Optional.of(objects.get());
        }
        var building = ModBlocks.gtBuildingBlockObjectBlocksById().get(id);
        if (building != null) {
            return Optional.of(building.get());
        }
        return Optional.empty();
    }

    private static Optional<String> registryPath(Block block) {
        if (block instanceof GtStoneBlock stone) {
            return Optional.of(stone.variant().registryPath());
        }
        if (block instanceof GtBlockObjectBlock object) {
            return Optional.of(object.variant().registryPath());
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id != null && CrucibleCraft.MODID.equals(id.getNamespace())) {
            return Optional.of(id.getPath());
        }
        return Optional.empty();
    }

    private static boolean consumeRod(Player player) {
        Inventory inventory = player.getInventory();
        for (int index = inventory.getContainerSize() - 1; index >= 0; index--) {
            ItemStack candidate = inventory.getItem(index);
            if (candidate.isEmpty() || !ironOrSteelRod(candidate)) {
                continue;
            }
            candidate.shrink(1);
            if (candidate.isEmpty()) {
                inventory.setItem(index, ItemStack.EMPTY);
            }
            return true;
        }
        return false;
    }
}
