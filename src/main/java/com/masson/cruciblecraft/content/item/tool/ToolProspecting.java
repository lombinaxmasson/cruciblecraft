package com.masson.cruciblecraft.content.item.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.block.GtStoneBlock;
import com.masson.cruciblecraft.content.block.StoneLayerStoneBlock;
import com.masson.cruciblecraft.content.item.MaterialFormItem;
import com.masson.cruciblecraft.content.item.MaterialToolItem;
import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.worldgen.StoneLayerStones;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * GT6 {@code ToolCompat.prospectOre} / {@code prospectStone} on the live
 * hard hammer's {@code TOOL_prospector} behavior.
 */
final class ToolProspecting {
    private ToolProspecting() {}

    static ToolResult use(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return ToolResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        List<Component> messages = new ArrayList<>();
        boolean foundOre = prospectOre(state, level, pos, messages);
        if (foundOre) {
            return finish(context, player, messages);
        }
        if (!prospectableStone(state)) {
            return ToolResult.PASS;
        }
        prospectStone(
                context.getItemInHand(),
                level,
                pos,
                context.getClickedFace(),
                state,
                messages);
        return finish(context, player, messages);
    }

    private static ToolResult finish(
            UseOnContext context, Player player, List<Component> messages) {
        if (context.getLevel().isClientSide) {
            return ToolResult.SUCCESS;
        }
        for (Component message : messages) {
            player.displayClientMessage(message, false);
        }
        ToolClick.hurt(context);
        return ToolResult.SUCCESS;
    }

    private static boolean prospectOre(
            BlockState state,
            Level level,
            BlockPos pos,
            List<Component> messages) {
        Optional<HammerCrush.Target> target = HammerCrush.identify(
                state, level.getBlockEntity(pos));
        if (target.isEmpty() && ToolMining.isOre(state)) {
            target = HammerCrush.identify(new ItemStack(state.getBlock()));
        }
        if (target.isEmpty()) {
            return false;
        }
        messages.add(Component.translatable(
                "message.cruciblecraft.prospect.ore",
                MaterialFormItem.formName(
                        target.get().materialId(),
                        com.masson.cruciblecraft.api.material.MaterialPrefixes.ORE)));
        return true;
    }

    private static void prospectStone(
            ItemStack tool,
            Level level,
            BlockPos origin,
            Direction face,
            BlockState originState,
            List<Component> messages) {
        int quality = quality(tool);
        BlockPos cursor = origin;
        for (int i = 0; i < quality; i++) {
            cursor = cursor.relative(face.getOpposite());
            BlockState behind = level.getBlockState(cursor);
            FluidState fluid = level.getFluidState(cursor);
            if (fluid.is(FluidTags.LAVA)) {
                messages.add(Component.translatable(
                        "message.cruciblecraft.prospect.lava"));
                break;
            }
            if (!fluid.isEmpty()) {
                messages.add(Component.translatable(
                        "message.cruciblecraft.prospect.fluid"));
                break;
            }
            if (behind.getBlock() instanceof InfestedBlock
                    || behind.isAir()
                    || behind.getCollisionShape(level, cursor).isEmpty()) {
                messages.add(Component.translatable(
                        "message.cruciblecraft.prospect.air"));
                break;
            }
            if (i < 4 && behind.getBlock() != originState.getBlock()) {
                messages.add(Component.translatable(
                        "message.cruciblecraft.prospect.material_change"));
                break;
            }
        }
        Random random = new Random(
                (long) origin.getX()
                        ^ origin.getY()
                        ^ origin.getZ()
                        ^ face.ordinal());
        int span = 1 + 2 * quality;
        int samples = quality * quality;
        for (int i = 0; i < samples; i++) {
            BlockPos sample = origin.offset(
                    random.nextInt(span) - quality,
                    random.nextInt(span) - quality,
                    random.nextInt(span) - quality);
            BlockState sampled = level.getBlockState(sample);
            if (sampled.is(Blocks.OBSIDIAN) || sampled.isAir()) {
                continue;
            }
            Optional<HammerCrush.Target> ore = HammerCrush.identify(
                    sampled, level.getBlockEntity(sample));
            if (ore.isEmpty() && ToolMining.isOre(sampled)) {
                ore = HammerCrush.identify(new ItemStack(sampled.getBlock()));
            }
            if (ore.isPresent()) {
                messages.add(Component.translatable(
                        "message.cruciblecraft.prospect.traces",
                        MaterialFormItem.materialDisplayName(ore.get().materialId())));
                return;
            }
        }
        if (messages.isEmpty()) {
            messages.add(Component.translatable(
                    "message.cruciblecraft.prospect.none"));
        }
    }

    private static int quality(ItemStack tool) {
        int raw = 4;
        if (tool.getItem() instanceof MaterialToolItem material) {
            raw = material.material(tool)
                    .map(id -> ToolMaterialRules.requireStats(
                            ToolKind.SMITHING_HAMMER, id).quality() + 4)
                    .orElse(4);
        }
        return Math.max(1, Math.min(20, raw));
    }

    private static boolean prospectableStone(BlockState state) {
        if (state.is(Blocks.OBSIDIAN)) {
            return false;
        }
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.END_STONE)
                || state.getBlock() instanceof StoneLayerStoneBlock
                || state.getBlock() instanceof GtStoneBlock
                || StoneLayerStones.isNaturalLayerCube(state);
    }
}
