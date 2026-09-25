package com.masson.cruciblecraft.content.item;

import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.content.item.tool.HammerCrush;
import com.masson.cruciblecraft.content.item.tool.ToolMining;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import com.masson.cruciblecraft.CrucibleCraft;

/** Sense/plow area harvest, axe column felling, and GT6 convertBlockDrops. */
@EventBusSubscriber(modid = CrucibleCraft.MODID)
public final class ToolHarvestEvents {
    private static final ThreadLocal<Boolean> AREA_HARVESTING =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private ToolHarvestEvents() {}

    @SubscribeEvent
    public static void areaHarvest(BlockEvent.BreakEvent event) {
        if (Boolean.TRUE.equals(AREA_HARVESTING.get())) {
            return;
        }
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        boolean sense = stack.getItem() instanceof MaterialSenseItem;
        boolean plow = stack.getItem() instanceof MaterialPlowItem;
        if (sense || plow) {
            harvestNeighbors(event, serverPlayer, stack);
            return;
        }
        if (player.isShiftKeyDown()) {
            return;
        }
        if (!(stack.getItem() instanceof MaterialAxeItem)
                && !(stack.getItem() instanceof MaterialDoubleAxeItem)
                && !electricChainsaw(stack)) {
            return;
        }
        BlockState origin = event.getState();
        if (!ToolMining.isLog(origin)
                && !(origin.getBlock() instanceof HugeMushroomBlock)) {
            return;
        }
        AREA_HARVESTING.set(Boolean.TRUE);
        try {
            BlockPos pos = event.getPos();
            Block log = origin.getBlock();
            for (int y = 1; y < 32; y++) {
                BlockPos above = pos.above(y);
                BlockState state = event.getLevel().getBlockState(above);
                if (state.getBlock() != log) {
                    break;
                }
                serverPlayer.gameMode.destroyBlock(above);
            }
        } finally {
            AREA_HARVESTING.set(Boolean.FALSE);
        }
    }

    private static void harvestNeighbors(
            BlockEvent.BreakEvent event,
            ServerPlayer serverPlayer,
            ItemStack stack) {
        AREA_HARVESTING.set(Boolean.TRUE);
        try {
            BlockPos origin = event.getPos();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos neighbor = origin.offset(dx, dy, dz);
                        BlockState state = event.getLevel().getBlockState(neighbor);
                        if (stack.getDestroySpeed(state) > 1.0F
                                || stack.isCorrectToolForDrops(state)) {
                            serverPlayer.gameMode.destroyBlock(neighbor);
                        }
                    }
                }
            }
        } finally {
            AREA_HARVESTING.set(Boolean.FALSE);
        }
    }

    @SubscribeEvent
    public static void convertDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (tool.isEmpty()
                && event.getBreaker() instanceof net.minecraft.world.entity.LivingEntity breaker) {
            tool = breaker.getMainHandItem();
        }
        BlockState state = event.getState();
        if (!(event.getLevel() instanceof ServerLevel server)) {
            return;
        }
        if (silkTool(tool, state)) {
            replaceWithSilk(event, server, tool, state);
            return;
        }
        ToolKind kind = harvestKind(tool);
        if (kind == null) {
            return;
        }
        if (kind == ToolKind.CONSTRUCTION_PICK && state.is(Blocks.ENDER_CHEST)) {
            replaceDrops(event, server, new ItemStack(Blocks.ENDER_CHEST));
            return;
        }
        if (kind == ToolKind.SMITHING_HAMMER
                || kind == ToolKind.JACKHAMMER_HV
                || kind == ToolKind.JACKHAMMER_HV_NO_ORES) {
            hammerDrops(event, server, state);
            return;
        }
        if (kind == ToolKind.CHISEL) {
            chiselDrops(event, server, state);
            return;
        }
        if (kind == ToolKind.CLUB) {
            clubDrops(event, server, state);
            return;
        }
        if (kind == ToolKind.SAW
                || kind == ToolKind.CHAINSAW_LV
                || kind == ToolKind.CHAINSAW_MV
                || kind == ToolKind.CHAINSAW_HV) {
            sawDrops(event, server, state);
            return;
        }
        if (kind == ToolKind.BRANCH_CUTTER || kind == ToolKind.TRIMMER_LV) {
            branchCutterDrops(event, server, state);
            return;
        }
        if (kind == ToolKind.SCISSORS
                || kind == ToolKind.KNIFE
                || kind == ToolKind.BUTCHERY_KNIFE) {
            vineSilk(event, server, state);
        }
    }

    private static boolean electricChainsaw(ItemStack stack) {
        if (!(stack.getItem() instanceof MaterialElectricToolItem tool)) {
            return false;
        }
        return switch (tool.kind()) {
            case CHAINSAW_LV, CHAINSAW_MV, CHAINSAW_HV -> true;
            default -> false;
        };
    }

    private static boolean silkTool(ItemStack tool, BlockState state) {
        boolean gem = tool.getItem() instanceof MaterialGemPickItem;
        boolean scoop = tool.getItem() instanceof MaterialScoopItem
                && MaterialScoopItem.harvestable(state);
        return gem || scoop;
    }

    private static ToolKind harvestKind(ItemStack tool) {
        if (tool.getItem() instanceof MaterialPocketMultitoolItem pocket) {
            return pocket.mode(tool).kind();
        }
        if (tool.getItem() instanceof MaterialToolItem material) {
            return material.kind();
        }
        return null;
    }

    private static void chiselDrops(
            BlockDropsEvent event, ServerLevel server, BlockState state) {
        ItemStack replacement = null;
        if (state.is(Blocks.STONE)) {
            replacement = new ItemStack(Blocks.CHISELED_STONE_BRICKS);
        } else if (state.is(Blocks.STONE_BRICKS)) {
            replacement = new ItemStack(Blocks.CRACKED_STONE_BRICKS);
        } else if (state.is(Blocks.MOSSY_STONE_BRICKS)) {
            replacement = new ItemStack(Blocks.MOSSY_COBBLESTONE);
        } else if (state.is(Blocks.CRACKED_STONE_BRICKS)) {
            replacement = new ItemStack(Blocks.COBBLESTONE);
        }
        if (replacement != null) {
            replaceDrops(event, server, replacement);
        }
    }

    private static void hammerDrops(
            BlockDropsEvent event, ServerLevel server, BlockState state) {
        if (event.getBlockEntity() == null) {
            Optional<ItemStack> crushed = HammerCrush.outputForBlock(
                    state, event.getBlockEntity());
            if (crushed.isPresent()) {
                replaceDrops(event, server, crushed.get());
                return;
            }
        }
        boolean converted = false;
        List<ItemStack> next = new java.util.ArrayList<>();
        for (ItemEntity entity : event.getDrops()) {
            Optional<ItemStack> crushed = HammerCrush.convert(entity.getItem());
            if (crushed.isPresent()) {
                next.add(crushed.get());
                converted = true;
            } else {
                next.add(entity.getItem().copy());
            }
        }
        if (converted) {
            event.getDrops().clear();
            addDrops(event, server, next);
            event.setDroppedExperience(0);
        }
    }

    private static void clubDrops(
            BlockDropsEvent event, ServerLevel server, BlockState state) {
        String material = clubRockMaterial(state);
        if (material == null) {
            return;
        }
        int count = 1 + server.getRandom().nextInt(4);
        Optional<ItemStack> rock = MaterialLookup.tryStack(
                material, MaterialPrefixCatalog.require("rock"), count);
        rock.ifPresent(stack -> replaceDrops(event, server, stack));
    }

    private static String clubRockMaterial(BlockState state) {
        if (state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.MOSSY_STONE_BRICKS)
                || state.is(Blocks.CRACKED_STONE_BRICKS)
                || state.is(Blocks.CHISELED_STONE_BRICKS)
                || state.is(Blocks.STONE_BRICK_STAIRS)
                || state.is(Blocks.COBBLESTONE_STAIRS)
                || state.is(Blocks.COBBLESTONE_WALL)
                || state.is(Blocks.STONE_BUTTON)
                || state.is(Blocks.STONE_PRESSURE_PLATE)
                || state.is(Blocks.SMOOTH_STONE)) {
            return "stone";
        }
        if (state.is(Blocks.NETHER_BRICKS)
                || state.is(Blocks.NETHER_BRICK_STAIRS)
                || state.is(Blocks.NETHER_BRICK_FENCE)
                || state.is(Blocks.NETHER_BRICK_WALL)
                || state.is(Blocks.CRACKED_NETHER_BRICKS)
                || state.is(Blocks.CHISELED_NETHER_BRICKS)) {
            return "nether_brick";
        }
        if (state.is(Blocks.NETHERRACK)) {
            return "netherrack";
        }
        if (state.is(Blocks.END_STONE)) {
            return "endstone";
        }
        if (state.is(Blocks.OBSIDIAN)) {
            return "obsidian";
        }
        if (state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.POLISHED_BLACKSTONE)
                || state.is(Blocks.POLISHED_BLACKSTONE_BRICKS)
                || state.is(Blocks.CHISELED_POLISHED_BLACKSTONE)
                || state.is(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS)) {
            return "blackstone";
        }
        if (state.is(Blocks.BASALT)
                || state.is(Blocks.POLISHED_BASALT)
                || state.is(Blocks.SMOOTH_BASALT)) {
            return "basalt";
        }
        return null;
    }

    private static void sawDrops(
            BlockDropsEvent event, ServerLevel server, BlockState state) {
        if (state.is(Blocks.BOOKSHELF)) {
            replaceDrops(event, server, new ItemStack(Blocks.BOOKSHELF));
            return;
        }
        if (state.is(BlockTags.ICE) || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE)) {
            replaceWithSilk(event, server, event.getTool(), state);
            return;
        }
        if (state.getBlock() instanceof LeavesBlock) {
            replaceWithSilk(event, server, event.getTool(), state);
        }
    }

    private static void branchCutterDrops(
            BlockDropsEvent event, ServerLevel server, BlockState state) {
        if (state.is(Blocks.VINE)) {
            replaceDrops(event, server, new ItemStack(Blocks.VINE));
            return;
        }
        Item sapling = saplingFor(state);
        if (sapling != null) {
            replaceDrops(event, server, new ItemStack(sapling));
        }
    }

    private static void vineSilk(
            BlockDropsEvent event, ServerLevel server, BlockState state) {
        if (state.is(Blocks.VINE)) {
            replaceDrops(event, server, new ItemStack(Blocks.VINE));
        }
    }

    private static Item saplingFor(BlockState state) {
        if (state.is(Blocks.OAK_LEAVES)) {
            return Items.OAK_SAPLING;
        }
        if (state.is(Blocks.SPRUCE_LEAVES)) {
            return Items.SPRUCE_SAPLING;
        }
        if (state.is(Blocks.BIRCH_LEAVES)) {
            return Items.BIRCH_SAPLING;
        }
        if (state.is(Blocks.JUNGLE_LEAVES)) {
            return Items.JUNGLE_SAPLING;
        }
        if (state.is(Blocks.ACACIA_LEAVES)) {
            return Items.ACACIA_SAPLING;
        }
        if (state.is(Blocks.DARK_OAK_LEAVES)) {
            return Items.DARK_OAK_SAPLING;
        }
        if (state.is(Blocks.MANGROVE_LEAVES)) {
            return Items.MANGROVE_PROPAGULE;
        }
        if (state.is(Blocks.CHERRY_LEAVES)) {
            return Items.CHERRY_SAPLING;
        }
        if (state.is(Blocks.AZALEA_LEAVES) || state.is(Blocks.FLOWERING_AZALEA_LEAVES)) {
            return Items.AZALEA;
        }
        return null;
    }

    private static void replaceWithSilk(
            BlockDropsEvent event,
            ServerLevel server,
            ItemStack tool,
            BlockState state) {
        ItemStack silk = tool.copy();
        var silkTouch = server.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
        silk.enchant(silkTouch, 1);
        var drops = Block.getDrops(
                state,
                server,
                event.getPos(),
                event.getBlockEntity(),
                event.getBreaker(),
                silk);
        event.getDrops().clear();
        addDrops(event, server, drops);
        event.setDroppedExperience(0);
    }

    private static void replaceDrops(
            BlockDropsEvent event, ServerLevel server, ItemStack drop) {
        event.getDrops().clear();
        addDrops(event, server, List.of(drop));
        event.setDroppedExperience(0);
    }

    private static void addDrops(
            BlockDropsEvent event, ServerLevel server, List<ItemStack> drops) {
        double x = event.getPos().getX() + 0.5;
        double y = event.getPos().getY() + 0.5;
        double z = event.getPos().getZ() + 0.5;
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                event.getDrops().add(new ItemEntity(server, x, y, z, drop));
            }
        }
    }
}
