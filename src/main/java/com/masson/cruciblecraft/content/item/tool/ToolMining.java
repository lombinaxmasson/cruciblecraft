package com.masson.cruciblecraft.content.item.tool;

import com.masson.cruciblecraft.content.block.BedrockOreBlock;
import com.masson.cruciblecraft.content.block.BoilerBlock;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.content.block.GtBrokenOreBlock;
import com.masson.cruciblecraft.content.block.GtHostedOreBlock;
import com.masson.cruciblecraft.content.block.GtSmallOreBlock;
import com.masson.cruciblecraft.content.block.HopperBlock;
import com.masson.cruciblecraft.content.block.LargeBoilerBlock;
import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.block.RedstoneWireBlock;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;
import com.masson.cruciblecraft.content.block.StoneLayerRockOreBlock;
import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code isMinableBlock}/{@code getMiningSpeed} projection for tools that
 * are not a single vanilla harvest tag.
 */
public final class ToolMining {
    private ToolMining() {}

    public static float destroySpeed(
            ToolKind kind, String materialId, BlockState state) {
        if (!mineable(kind, state)) {
            return 1.0F;
        }
        float speed = ToolMaterialRules.miningSpeed(kind, materialId);
        return switch (kind) {
            case SAW -> sawSpeed(state, speed);
            case FILE -> state.is(Blocks.IRON_BARS) ? speed * 3.0F : speed;
            case CONSTRUCTION_PICK -> constructionSpeed(state, speed);
            case SMITHING_HAMMER ->
                    state.is(Blocks.SPAWNER) ? speed * 128.0F : speed;
            case UNIVERSAL_SPADE -> speed * 0.75F;
            case BRANCH_CUTTER -> state.is(BlockTags.LEAVES) ? 15.0F : speed * 0.25F;
            case SCISSORS -> (state.is(BlockTags.LEAVES)
                    || state.is(BlockTags.WOOL)
                    || state.is(BlockTags.WOOL_CARPETS))
                    ? 15.0F
                    : speed;
            case KNIFE, BUTCHERY_KNIFE ->
                    state.is(Blocks.COBWEB)
                            ? 15.0F
                            : (state.is(BlockTags.SWORD_EFFICIENT) ? 1.5F : speed);
            default -> speed;
        };
    }

    public static boolean mineable(ToolKind kind, BlockState state) {
        return switch (kind) {
            case SAW -> sawMineable(state);
            case FILE -> state.getBlock() instanceof IronBarsBlock
                    || state.is(Blocks.IRON_BARS);
            case WRENCH, MONKEY_WRENCH -> wrenchMineable(state);
            case CROWBAR -> state.getBlock() instanceof BaseRailBlock;
            case WIRE_CUTTER -> wireCutterMineable(state);
            case CHISEL -> chiselMineable(state);
            case CLUB -> state.is(BlockTags.MINEABLE_WITH_PICKAXE);
            case SMITHING_HAMMER -> hammerMineable(state);
            case KNIFE, BUTCHERY_KNIFE ->
                    state.is(Blocks.COBWEB)
                            || state.is(Blocks.VINE)
                            || state.is(BlockTags.SWORD_EFFICIENT);
            case UNIVERSAL_SPADE -> universalSpadeMineable(state);
            case BRANCH_CUTTER ->
                    state.is(BlockTags.LEAVES) || state.is(Blocks.VINE);
            case SCISSORS ->
                    state.is(BlockTags.LEAVES)
                            || state.is(BlockTags.WOOL)
                            || state.is(BlockTags.WOOL_CARPETS)
                            || state.is(BlockTags.MINEABLE_WITH_HOE)
                            || state.is(Blocks.VINE)
                            || state.is(Blocks.COBWEB);
            case SOFT_HAMMER -> state.is(Blocks.REDSTONE_LAMP);
            case CONSTRUCTION_PICK ->
                    state.is(BlockTags.MINEABLE_WITH_PICKAXE);
            default -> false;
        };
    }

    public static boolean correctTool(ToolKind kind, BlockState state) {
        if (!mineable(kind, state)) {
            return false;
        }
        return switch (kind) {
            case KNIFE, BUTCHERY_KNIFE -> state.is(Blocks.COBWEB);
            case SAW ->
                    sawMineable(state) && !state.is(BlockTags.LOGS);
            default -> true;
        };
    }

    public static boolean isOre(BlockState state) {
        Block block = state.getBlock();
        return state.is(Tags.Blocks.ORES)
                || block instanceof GtHostedOreBlock
                || block instanceof GtSmallOreBlock
                || block instanceof GtBrokenOreBlock
                || block instanceof BedrockOreBlock
                || block instanceof StoneLayerRockOreBlock;
    }

    public static boolean isLog(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    private static float constructionSpeed(BlockState state, float speed) {
        float doubled = speed * 2.0F;
        return isOre(state) ? doubled / 4.0F : doubled;
    }

    private static float sawSpeed(BlockState state, float speed) {
        if (isLog(state)) {
            return speed / 2.0F;
        }
        if (state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_TRAPDOORS)) {
            return speed * 2.0F;
        }
        if (state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SAPLINGS)
                || state.is(Blocks.VINE)) {
            return speed / 4.0F;
        }
        return speed;
    }

    private static boolean sawMineable(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.ICE)
                || state.is(Blocks.BOOKSHELF)
                || state.is(Blocks.VINE)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.IRON_BARS);
    }

    private static boolean wrenchMineable(BlockState state) {
        Block block = state.getBlock();
        return block instanceof net.minecraft.world.level.block.piston.PistonBaseBlock
                || block instanceof net.minecraft.world.level.block.DispenserBlock
                || block instanceof net.minecraft.world.level.block.HopperBlock
                || block instanceof net.minecraft.world.level.block.DropperBlock
                || block instanceof HopperBlock
                || block instanceof ProcessingMachineBlock
                || block instanceof BoilerBlock
                || block instanceof LargeBoilerBlock
                || block instanceof SteamEngineBlock
                || block instanceof FuelGeneratorBlock
                || state.is(Blocks.REDSTONE_LAMP);
    }

    private static boolean wireCutterMineable(BlockState state) {
        Block block = state.getBlock();
        return block instanceof CableBlock
                || block instanceof RedstoneWireBlock
                || block instanceof TripWireBlock
                || state.is(Blocks.TRIPWIRE)
                || state.is(Blocks.TRIPWIRE_HOOK);
    }

    private static boolean chiselMineable(BlockState state) {
        Block block = state.getBlock();
        return block instanceof InfestedBlock
                || state.is(Blocks.STONE)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.MOSSY_STONE_BRICKS)
                || state.is(Blocks.CRACKED_STONE_BRICKS)
                || state.is(Blocks.CHISELED_STONE_BRICKS);
    }

    private static boolean universalSpadeMineable(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.WOOL)
                || state.is(BlockTags.WOOL_CARPETS)
                || state.getBlock() instanceof BaseRailBlock
                || state.is(Blocks.VINE)
                || state.is(Blocks.COBWEB)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW);
    }

    private static boolean hammerMineable(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.getBlock() instanceof InfestedBlock
                || state.is(Blocks.SPAWNER)
                || state.is(BlockTags.ICE)
                || state.is(Tags.Blocks.GLASS_BLOCKS)
                || state.is(Tags.Blocks.GLASS_PANES)
                || state.is(Blocks.GLOWSTONE)
                || state.is(Blocks.SEA_LANTERN);
    }
}
